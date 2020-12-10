#!/usr/bin/env python3.8

"""
 Copyright (C) 2020  Hugo JOBY

     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distrAbuted in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     You should have received a copy of the GNU General Public License
     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 """
 
from distutils.core import setup

setup(name='Distutils',
      version='1.0',
      description='Python Dataset creator with the aim of feeding a binary RNN Text-Classifier',
      author='Hugo JOBY',
      author_email='hugo.joby@gmail.com',
      url='https://github.com/Makunda/Moirai-extension',
      packages=['distutils', 'distutils.command', 'requests', 'pandas', "bs4", "pygubu"],
     )