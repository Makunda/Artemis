/*
 * Copyright (C) 2020  Hugo JOBY
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty ofnMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNUnLesser General Public License v3 for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public v3 License along with this library; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.castsoftware.artemis.nlp.parser;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.utils.Workspace;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

public class HeaderGenerator {

  private static HeaderGenerator instance = null;

  private Integer numberOfLine = 0;
  private File headerFile = null;
  private Neo4jAL neo4jAL;

  private HeaderGenerator(Neo4jAL neo4jAL) throws IOException {
    Path headerFilePath =
        Workspace.getWorkspacePath(neo4jAL).resolve(Configuration.get("artemis.parser.header_file.name"));
    this.headerFile = headerFilePath.toFile();
    this.neo4jAL = neo4jAL;

    if (!headerFile.exists()) {
      throw new IOException(String.format("File with name '%s' does not exist.", headerFile));
    }

    byte[] c = new byte[1024];
    int count = 0;
    int readChars = 0;
    boolean empty = true;

    try (InputStream is = new BufferedInputStream(new FileInputStream(headerFile))) {

      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }

      numberOfLine = (count == 0 && !empty) ? 1 : count;
    }
  }

  public static HeaderGenerator getInstance(Neo4jAL neo4jAL) throws IOException {
    if (instance == null) instance = new HeaderGenerator(neo4jAL);
    return instance;
  }

  public String getRandomHeader() throws IOException {
    int randomLine = ThreadLocalRandom.current().nextInt(0, numberOfLine + 1);
    return Files.readAllLines(headerFile.toPath()).get(randomLine);
  }
}
