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

package com.castsoftware.artemis.nlp.model;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.utils.Workspace;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Tokenizer {

  public static void run() throws Exception {

    /** Read human understandable data & train a model */
    Path tokenizerFilePath =
        Workspace.getWorkspacePath().resolve(Configuration.get("nlp.tokenizer_file.name"));

    // Read file with examples of tokenization.
    InputStreamFactory inputStreamFactory =
        new MarkableFileInputStreamFactory(new File("tokenizerdata.txt"));
    ObjectStream<String> lineStream =
        new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
    ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

    // Train a model from the file read above
    TokenizerFactory factory = new TokenizerFactory("en", null, false, null);
    TokenizerModel model =
        TokenizerME.train(sampleStream, factory, TrainingParameters.defaultParams());

    // Serialize model to some file so that next time we don't have to again train a
    // model. Next time We can just load this file directly into model.
    model.serialize(tokenizerFilePath.toFile());
  }
}
