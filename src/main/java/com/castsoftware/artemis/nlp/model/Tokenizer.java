package com.castsoftware.artemis.nlp.model;

import com.castsoftware.artemis.config.Configuration;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.*;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Tokenizer {

    private static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    private static final String TOKENIZER_FILE_PATH = ARTEMIS_WORKSPACE + Configuration.get("nlp.tokenizer_file.name");


    public static void run() throws Exception {

        /**
         * Read human understandable data & train a model
         */
        // Read file with examples of tokenization.
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File("tokenizerdata.txt"));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<TokenSample> sampleStream = new TokenSampleStream(lineStream);

        // Train a model from the file read above
        TokenizerFactory factory = new TokenizerFactory("en", null, false, null);
        TokenizerModel model = TokenizerME.train(sampleStream, factory, TrainingParameters.defaultParams());

        // Serialize model to some file so that next time we don't have to again train a
        // model. Next time We can just load this file directly into model.
        model.serialize(new File(TOKENIZER_FILE_PATH));

    }
}
