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

import abc
import config
import re
from userAgentGen import UserAgentGen

userAgentGen = UserAgentGen()


accept_h = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8," \
           "application/signed-exchange;v=b3 "
accept_encoding_h = "UTF-8"
accept_language_h = "en-US,en;q=0.9,es;q=0.8"
insecure_request_h = "1"

class ACrawler():

    @staticmethod
    def sanitize_html(text: str, nameElement: str):
        sanitized = re.sub(r"<\/?\s*\w[^>]*\/?>", " ", text)  # Remove all the html balise ( identified by <> )
        sanitized = re.sub(r"[.]{2,}", " ", sanitized)  # Remove google dots
        # inside the code
        sanitized = sanitized.replace(nameElement, "")  # Remove the name of the element for the NLP
        return re.sub(r"[\s]{2,}", "", sanitized)  # Remove extra space


    @staticmethod
    @abc.abstractmethod
    def run(req : str) -> str:
        """Run Request"""
        raise NotImplementedError
    
    @staticmethod
    def get_header():
        user_agent = userAgentGen.get_header()
        return {"User-Agent":  user_agent, "Referer" : config.default_referrer, 
        "Accept" : accept_h, "Accept-Encoding" : accept_encoding_h, 
        "Accept-Language": accept_language_h,
        "Upgrade-Insecure-Requests" : insecure_request_h
        }
