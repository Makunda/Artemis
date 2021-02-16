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

package com.castsoftware.artemis.nlp.saver;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.utils.Workspace;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class NLPSaver implements Closeable {

  private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

  private FileWriter fileWriter;
  private String application;
  private String language;

  public NLPSaver(String application, String language) throws IOException {
    this.application = application;
    this.language = language;
    init();
  }

  /**
   * Write the results in the file with its associated category
   *
   * @param category
   * @param content
   * @throws IOException
   */
  public void writeNLPResult(NLPCategory category, String content) throws IOException {
    content = content.replace(System.lineSeparator(), ""); // replace /n
    if (category == NLPCategory.FRAMEWORK) {
      fileWriter.write("Framework\t" + content + "\n");
      System.out.println("Framework\t" + content + "\n");
    } else {
      fileWriter.write("NotFramework\t" + content + "\n");
    }

    fileWriter.flush();
  }

  /**
   * Init the repository
   *
   * @throws IOException
   */
  private void init() throws IOException {
    Path reportFolderPath =
        Workspace.getWorkspacePath().resolve(Configuration.get("artemis.nlp_enrichment.folder"));
    if (!Files.exists(reportFolderPath)) {
      Files.createDirectories(reportFolderPath);
    }

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String fileLocation =
        reportFolderPath
            + "/ArtemisNLPEnrichment_"
            + application
            + "_on"
            + SDF.format(timestamp)
            + "_" + language
            + ".txt";

    try {
      fileWriter = new FileWriter(fileLocation);
      fileWriter.write(language + System.lineSeparator());
    } catch (IOException e) {
      System.err.println("Failed to create the NLP enrichment file.");
      e.printStackTrace();
      throw e;
    }
  }

  @Override
  public void close() throws IOException {
    fileWriter.close();
  }
}
