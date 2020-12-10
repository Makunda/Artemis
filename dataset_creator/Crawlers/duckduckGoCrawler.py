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
from urllib.request import urlopen
from urllib.request import Request

import json
import logging
from .aCrawler import ACrawler

from bs4 import BeautifulSoup


def extract_results(json) -> str:
    extracted_text = ""
    for r in  json:
        if r.get("Result"):
            extracted_text += " " + r.get("Result")
        if r.get("Text"):
            extracted_text += " " + r.get("Text")
    return extracted_text
    
class DuckDuckGoCrawler(ACrawler):

    @staticmethod
    def run(req : str) -> str:
        san_request = req.replace(' ', '+')
        url = config.duckduckGoRequest % san_request

        user_agent = super(DuckDuckGoCrawler, DuckDuckGoCrawler).get_header()

        request = Request(url, headers=user_agent)
        response = urlopen(request)

        if response.getcode() != 200:
            config.vPrint("An error occurred in DuckDuckGo..")
            logging.error("Invalid status code with request : {}".format(url))
            logging.error("Status code : {}. Request content : {}".format(response.getcode(), response.read()) )
            return ""

        json_res = ""

        try:
            json_res = json.loads(response.read())
        except json.decoder.JSONDecodeError as e: 
            logging.exception("Cannot decrypt request coming from DuckDuckGo.")
            return ""

        response.close()
        
        extracted_text = ""

        #Abstract Tex t
        abstract_text = json_res.get('AbstractText', '')

        extracted_text += " " + abstract_text

        extracted_text += " " +  extract_results(json_res.get('Results'))
        extracted_text += " " +  extract_results(json_res.get('RelatedTopics'))

        logging.info("DuckDuckGo results for '{}' have a size of {} characters.".format(req, len(extracted_text)))

        return super(DuckDuckGoCrawler, DuckDuckGoCrawler).sanitize_html(extracted_text, req)
            
