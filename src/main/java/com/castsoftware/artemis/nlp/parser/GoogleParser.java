/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.nlp.parser;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.google.GoogleBadResponseCodeException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.neo4j.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleParser {
    private static final String GOOGLE_URL = "https://www.google.com/search?q=%s&sourceid=chrome&ie=UTF-8";
    private static final String ERROR_PREFIX = "GOOGPx";

    private static final String RAW_BLACKLISTED_WEBSITE = Configuration.get("artemis.online.website.blacklist");
    private static final String RAW_BLACKLISTED_KEYWORDS = Configuration.get("artemis.online.word.blacklist");
    private static final List<String> BLACKLISTED_WEBSITE = Arrays.asList(RAW_BLACKLISTED_WEBSITE.split(";"));
    private static final List<String> BLACKLISTED_KEYWORDS = Arrays.asList(RAW_BLACKLISTED_KEYWORDS.split(";"));

    private static final Pattern TITLE_REGEX_PATTERN = Pattern.compile("<a[\\s]+[^>]+>((?:.(?!\\<\\/a\\>))*.)<\\/a>");

    private Log log;
    private HeaderGenerator headerGenerator;

    /**
     * Get Random number following a normal law arrival
     * @param mean Mean of the poisson law
     * @return
     */
    private static int getNormalLawArrival(double mean, double std) {
        Random r = new Random();
        double delay = r.nextGaussian()*std + mean;
        return (int) Math.round(delay);
    }


    /**
     * Wait to avoid Google Bot detector to reject the query
     */
    private void botBusterWait() {
        try {
            int delay = getNormalLawArrival(1200, 500);
            if(delay < 0) delay = 1500;
            log.info("Will wait : "+delay+" ms");
            Thread.sleep(delay);
        } catch(InterruptedException ignore) {
        }
    }

    public GoogleResult request(String query) throws IOException, GoogleBadResponseCodeException {
        GoogleResult googleResult = new GoogleResult();
        googleResult.setTitle(query);

        // Wait
        botBusterWait();

        query = query.replaceAll("([^A-z0-9]|[\\\\])", "");
        query = query.replace(" ", "+").toLowerCase();

        String req = String.format(GOOGLE_URL, query);
        URL obj = new URL(req);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        String header = headerGenerator.getRandomHeader();
        log.info("Now using header : "+ header);
        con.setRequestProperty("User-Agent", header);


        int responseCode = con.getResponseCode();

        if(responseCode != 200) {
            String respMessage = con.getResponseMessage();
            log.info("An unexpected behavior was detected during framework list gathering.");
            for(Map.Entry<String, List<String>> en :  con.getHeaderFields().entrySet()) {
                log.info(String.format("Header : %s Value : %s", en.getKey(), String.join(", ", en.getValue())));
            }


            throw new GoogleBadResponseCodeException(String.format("The request return a bad response code. Code : %d , Response Message : %s",
                    responseCode, respMessage)
                    , ERROR_PREFIX+"RESC1");
        }

        String inputLine;
        StringBuffer response = new StringBuffer();

        try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        StringBuilder fullResults = new StringBuilder();

        // Analyze response with JSOUP
        Document document = Jsoup.parse(response.toString());
        List<String> extractedTitle = new ArrayList<>();
        List<String> extractedBody = new ArrayList<>();

        // Get the title and verify the presence of blacklisted words in the first 4 elements
        List<Element> titles = document.getElementsByClass("rc");
        googleResult.setNumberResult(titles.size());

        int itTitle = 0;
        for (Element title: titles) {
            String titleText = null;
            if( !title.getElementsByTag("h3").isEmpty() ) {
                titleText = title.getElementsByTag("h3").get(0).text();
                if(itTitle < 4 ) {
                    for(String blackListed : BLACKLISTED_KEYWORDS) {
                        if(titleText.contains(blackListed)) {
                            googleResult.setBlacklisted(true);
                            break;
                        }
                    }
                }
            }
            extractedTitle.add(titleText);

            itTitle++;
        }

        // Add aCOpRe / st
        List<Element> bodies = document.getElementsByClass("rc");
        for (Element body: bodies) {
            extractedBody.add(body.text());
        }

        // Check website url and verify the first 4 results aren't blacklisted
        // Regex
        Matcher titleMatcher;
        int itWebUrl = 0;
        List<Element> urls = document.getElementsByClass("rc");
        for (Element body: urls) {
            if(itWebUrl > 4) {
                break;
            }
            titleMatcher = TITLE_REGEX_PATTERN.matcher(body.text());
            if(titleMatcher.matches()) {
                for(int i=0; i <= titleMatcher.groupCount(); i++) {
                    for(String forbidden : BLACKLISTED_WEBSITE) {
                        if(titleMatcher.group(i).toLowerCase().contains(forbidden)) {
                            googleResult.setBlacklisted(true);
                            break;
                        }
                    }
                }
            }
            itWebUrl ++;
        }

        Iterator<String> it1 = extractedTitle.iterator();
        Iterator<String> it2 = extractedBody.iterator();

        while (it1.hasNext() || it2.hasNext()) {
            if(it1.hasNext()) fullResults.append(it1.next()).append(" ");
            if(it2.hasNext()) fullResults.append(it2.next()).append(" ");
        }

        // Sanitize the text
        // Remove Html anchors , http links, special characters and isolated numbers
        String res =  fullResults.toString().replaceAll("<\\/?\\s*\\w[^>]*\\/?>|[\\.]{2,}|([\\<\\>›»])|(\\b(\\d+)\\.?\\b)|(w{3}\\.[A-z0-9-]*\\.[A-z]{2,})|(?!([a-zA-Z\\s:,\\.-]))", " ");
        // Remove extra spaces
        googleResult.setContent(res.replaceAll("\\s{2,}", ""));

        return googleResult;
    }

    public GoogleParser(Log log) throws IOException {
        this.log = log;
        this.headerGenerator = HeaderGenerator.getInstance();
    }
}
