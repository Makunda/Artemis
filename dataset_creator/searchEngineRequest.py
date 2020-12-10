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
import requests
import re
import asyncio
import json
import Crawlers
import logging
import time

from bs4 import BeautifulSoup

def sanitize_html(text: str):
    return re.sub(r"<\/?[\w\s=\"]+\/?>*", "", text)

# Should be used as a thread, because of Sleep function 
class SearchEngineRequest():

    def __init__(self, resquest: str):
        self.req = resquest.replace("_", " ")
        
        self.req_w_keywords_list = []
        for keywords in config.additionalKeywords:
            self.req_w_keywords_list.append(self.req + " " + keywords)

    def __launch_against(self, crawler, use_keyword) -> str:
        res_list = []

        to_request = [self.req]

        if use_keyword:
            to_request.extend(self.req_w_keywords_list)

        for index, req in enumerate(to_request):
            res = crawler.run(req)
            res_list.append(res)
            to_wait = config.random_function(config.min_time_wait / config.max_time_wait)
            if index < len(to_request) -1  : time.sleep(to_wait)

        return " ".join(res_list)

    def parse_duck_duckgo(self, use_keywords=True) -> str:
        return self.__launch_against(Crawlers.DuckDuckGoCrawler, use_keywords)

    def parse_google(self, use_keywords=True) -> str:
        return self.__launch_against(Crawlers.GoogleCrawler, use_keywords)

    def bulk(self, use_keywords=True):
        res = ""

        to_request = [self.req]

        if use_keywords:
            to_request.extend(self.req_w_keywords_list)

        for index, req in enumerate(to_request):
            res += Crawlers.DuckDuckGoCrawler.run(req )
            res += Crawlers.GoogleCrawler.run(req)
            to_wait = config.random_function(config.min_time_wait / config.max_time_wait)
            if index < len(to_request): time.sleep(to_wait) # avoid waiting at the end

        return res
            
