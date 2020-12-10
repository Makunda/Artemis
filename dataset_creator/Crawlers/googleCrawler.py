#!/usr/bin/env python3.8

"""
 Copyright (C) 2020  Hugo JOBY

     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     You should have received a copy of the GNU General Public License
     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 """

import config
import logging
import requests

from .aCrawler import ACrawler
from bs4 import BeautifulSoup

GOOGLE_URL = "https://google.com/search?q=%s&lr=lang_en"


class GoogleCrawler(ACrawler):

    @staticmethod
    def run(req: str) -> str:
        san_request = req.replace(' ', '+')
        url = GOOGLE_URL % san_request

        logging.info("Google crawler : Sent request to '%s'", url)

        user_agent = super(GoogleCrawler, GoogleCrawler).get_header()

        response = requests.get(url, headers=user_agent)

        if response.status_code != 200:
            config.vPrint("Google crawler : An error occurred in Google..")
            logging.error("Google crawler : Invalid status code with request : {}".format(url))
            logging.error("Google crawler : Status code : {}. Request content : {}".format(response.status_code,
                                                                                           response.content))

            if response.status_code == 429:
                logging.fatal("Google crawler : Banned from google")
                print("Google crawler : You've got banned from google :( ")  # Inform the user directly
            return ""

        extracted_text = ""
        raw_html = str(response.text)

        soup = BeautifulSoup(raw_html, 'html.parser')
        # Debug purpose
        for g in soup.find_all('span', class_='aCOpRe'):  # find all titles
            extracted_text += " " + g.find('span').text

        # Reset The soup Parser
        soup = BeautifulSoup(raw_html, 'html.parser')
        for g in soup.find_all('h3', class_='LC20lb DKV0Md'):  # find all titles
            extracted_text += " " + str(g.text)

        logging.info("Google results for '{}' have a size of {} characters.".format(req, len(extracted_text)))

        print("Results for " + req + " : " + extracted_text)

        # TODO Remove google dates
        return super(GoogleCrawler, GoogleCrawler).sanitize_html(extracted_text, req)
