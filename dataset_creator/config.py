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
import random


# TODO Better parameters regroup
verbosity = False
redirectOutput = False

outputExtension=".req"
csv_delimiter = ";"

# Headers
pathToUserAgentFile = "/data/userAgent.txt"
default_referrer = "https://www.google.com/"
maxHeaderUseBeforeRotation = 2

#Timing 
min_time_wait = 1.3
max_time_wait = 5
random_function = random.expovariate


headerList = ["Name", "IsFramework"]
googleDefaultRequest = "https://google.com/search?"
googleCountryCode = "countryUS"
googleLangCode = "lang_en"

duckduckGoRequest = "https://api.duckduckgo.com/?q=%s&format=json&pretty=1&atb=v233-3"

defaultRequestHeaders = {"User-Agent": 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Cafari/537.36'}

# Keywords for requests
additionalKeywords = ["framework"]

def vPrint(message) :
    if verbosity == True:
        print(message)
        
        