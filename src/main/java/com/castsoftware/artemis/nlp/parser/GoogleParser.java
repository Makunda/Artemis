package com.castsoftware.artemis.nlp.parser;

import com.castsoftware.artemis.database.Neo4jAL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.neo4j.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GoogleParser {
    private static final String GOOGLE_URL = "https://google.com/search?q=%s&lr=lang_en";

    private HeaderGenerator headerGenerator;

    public String request(String query) throws IOException {

        query = query.replaceAll("([^A-z0-9]|[\\\\])", "");
        query = query.replace(" ", "+").toLowerCase();

        String req = String.format(GOOGLE_URL, query);
        URL obj = new URL(req);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        con.setRequestProperty("User-Agent", headerGenerator.getRandomHeader());

        int responseCode = con.getResponseCode();

        if(responseCode != 200) {
            return null; // TODO Throw an error here
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

        List<Element> titles = document.getElementsByClass("rc");
        for (Element title: titles) {
            String titleText = null;
            if( !title.getElementsByTag("h3").isEmpty() ) {
                titleText = title.getElementsByTag("h3").get(0).text();
            }
            extractedTitle.add(titleText);
        }

        // Add aCOpRe / st
        List<Element> bodies = document.getElementsByClass("rc");
        for (Element body: bodies) {
            extractedBody.add(body.text());
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
        return res.replaceAll("\\s{2,}", "");
    }

    public GoogleParser() throws IOException {
        this.headerGenerator = HeaderGenerator.getInstance();
    }
}
