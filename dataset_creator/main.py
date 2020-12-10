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
import argparse
import userAgentGen
import config
import logging
from gui import GUIDatasetCreator
from dataGenerator import DataGenerator

import logging

PROJECT_PATH = os.path.dirname(os.path.realpath(__file__))

def main():
    #Flush log file
    with open(PROJECT_PATH+'/logs/info.log', 'w'):
        pass

    #Configure logger
    logging.basicConfig(filename=PROJECT_PATH+'/logs/info.log', level=logging.INFO)
    logging.basicConfig(filename=PROJECT_PATH+'/logs/debug.log', level=logging.DEBUG)
    logging.basicConfig(filename=PROJECT_PATH+'/logs/error.log', level=logging.ERROR)

    #C Configure parser
    parser = argparse.ArgumentParser(formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=""" - To launch the web crawler : python main.py -v -f input.csv \n - To launch the GUI Helper : python main.py -v -g""")
    required_named = parser.add_argument_group('required named arguments')
    required_named.add_argument('-o', '--output', help='Output directory for the dataset', default='./dataset/')
    required_named.add_argument('-f', '--file', help='Input file containing ground truth values')

    parser.add_argument("-v", "--verbose", help="increase output verbosity",
                        action="store_true")

    parser.add_argument("-g", "--gui", help="Launch gui mode",
                        action="store_true")
    
    args = parser.parse_args()
    if args.verbose:
        config.verbosity = True
        print("Verbosity on")
    if not args.file and not args.gui:
         raise ValueError("No output file value was provided in arguments. Check the usage by using -h or --help parameters.")
    
    if not args.gui:
        d1 : DataGenerator = DataGenerator(args.file, args.output)
        d1.run()

    else:
        app = GUIDatasetCreator()
        app.run()


if __name__ == "__main__":
    main()