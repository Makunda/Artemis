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


import os
import config
import utils.exceptions as exceptions
import pandas as pd


from bs4 import BeautifulSoup

from searchEngineRequest import SearchEngineRequest

class DataGenerator(object):
    def __init__(self, inputPath : str, outputPath : str):
        #Test if input file is accesible 
        if not os.path.exists(inputPath):
            raise exceptions.MissingFileError(inputPath, "The specified input file is missing.")
        # Check if the output directory exists, or create
        if not os.path.exists(outputPath):
            try:
                os.mkdir(outputPath)
            except OSError as e:
                raise exceptions.DirectoryCreationError(e, "Creation of the directory %s failed" % outputPath) 
            else:
                config.vPrint("Successfully created the directory %s " % outputPath)
        else:
            config.vPrint("Directory already exist at path  %s" % outputPath)

        self.inputPath = inputPath
        self.outputPath = outputPath

        self.indexFile = open(outputPath+"/index.dti", "a")

    def __indexFile(self, name:str, complete:bool):
        toWrite = name + ";" + str(complete) +"\n"
        self.indexFile.write(toWrite)            

    def run(self):
        nameCol = config.headerList[0]
        IsFramework = config.headerList[1]

        df = pd.read_csv(self.inputPath, names=config.headerList, skiprows=1, delimiter=config.csv_delimiter)
        
        async def runRequest(name : str, groundTruth:bool):
            gRequest = SearchEngineRequest(name)
            name = name.replace(" ", "_") # Replace space by underscore in filename
            filePath = self.outputPath+"/"+name+config.outputExtension
            strRequest = gRequest.bulk()
            with  open(filePath, "w") as reqFile:
                reqFile.write(str(groundTruth) + "\n")
                reqFile.write(strRequest)
                self.__indexFile(name+config.outputExtension, len(strRequest) != 0)
                

        for _, row in df.iterrows():
            runRequest(row[nameCol], row[IsFramework])

        #await asyncio.gather(*reqList)
        #Extract request relatued text within 'st' balise

    def __del__(self):
        if self.indexFile:
            self.indexFile.close()