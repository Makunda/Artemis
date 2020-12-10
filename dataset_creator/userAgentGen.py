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

import logging
import os
import config
import utils.singleton as singleton

from random import randrange

PROJECT_PATH = os.path.dirname(__file__)


class UserAgentGen():

    __metaclass__ = singleton.Singleton

    def __init__(self):
        
        self.__max_per_header = config.maxHeaderUseBeforeRotation
        self.__avaible_headers =  []
        with open(PROJECT_PATH+config.pathToUserAgentFile, "r") as f:
            for line in f:
                line = line.replace("\n", "") # Remove windows return sequence
                self.__avaible_headers.append(line)

        if len(self.__avaible_headers) == 0:
            raise ValueError("Header file is empty, and should not.")

        self.__counter = 0
        self.__current_header_index = randrange(0, len(self.__avaible_headers) - 1)
        print(self.__current_header_index)
    
    """Get a user agent. Rotation is operated above a certain level of usage of the current one.""" 
    def get_header(self) -> str:
        if self.__counter >= self.__max_per_header:
            self.__counter=0
            self.__current_header_index = randrange(0, len(self.__avaible_headers) - 1)
        
        self.__counter += 1
        logging.info("Header rotation. Will now use : {}".format(self.__avaible_headers[self.__current_header_index]))
        return self.__avaible_headers[self.__current_header_index]

    def print_headers(self):
        for line in self.__avaible_headers:
            print(line)