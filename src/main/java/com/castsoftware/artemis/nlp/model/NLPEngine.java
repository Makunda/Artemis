package com.castsoftware.artemis.nlp.model;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.nlp.NLPIncorrectConfigurationException;
import opennlp.tools.doccat.*;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.*;
import org.neo4j.logging.Log;
import org.neo4j.logging.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class NLPEngine {

    private static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    private static final String MODEL_FILE_PATH = ARTEMIS_WORKSPACE + Configuration.get("nlp.model_file.name");
    private static final String TRAIN_DT_PATH = ARTEMIS_WORKSPACE + Configuration.get("nlp.dataset_train.name");
    private static final String TEST_DT_PATH = ARTEMIS_WORKSPACE + Configuration.get("nlp.dataset_test.name");
    private static final String TOKENIZER_FILE_PATH = ARTEMIS_WORKSPACE + Configuration.get("nlp.tokenizer_file.name");

    private static final String NLP_FRAMEWORK_CATEGORY = ARTEMIS_WORKSPACE + Configuration.get("nlp.category.is_framework");


    private static final String ERROR_PREFIX = "NLPx";

    private SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

    private DocumentCategorizerME docCategorizer = null;
    private File modelFile = null;
    private DoccatModel model = null;

    private Log log;

    /**
     * Get the polarity of the actual dataset. The polarity is a ratio of the number in the two differed categories.
     * The closer to 0.5 it is, the better your results will be.
     * @param path Path to the Dataset
     * @return The polarity associated with the provide dataset
     * @throws FileNotFoundException If the file doesn't exist
     */
    private double getDatasetPolarity(String path) throws FileNotFoundException{

        Integer numFlagFramework = 0;
        Integer numFlagNotFramework = 0;

        // Testing the polarity of the dataset
        File myObj = new File(path);
        try(Scanner myReader = new Scanner(myObj)) {
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[]lineSplit = data.split("\t", 2);

                String groundTruth = lineSplit[0];
                if(groundTruth.equals(NLP_FRAMEWORK_CATEGORY)) {
                    numFlagFramework++;
                } else {
                    numFlagNotFramework++;
                }
            }
        }

        return numFlagFramework / (double) (numFlagFramework + numFlagNotFramework);
    }

    public static void load() {

    }

    /**
     * Train the model with the Train and Test dataset passed in parameters.
     * The model will be serialized into a file stored as a resource
     * @throws IOException
     */
    public void train() throws IOException {

        // Read file with classifications samples of sentences.
        InputStreamFactory inputStreamFactory = new MarkableFileInputStreamFactory(new File(TRAIN_DT_PATH));
        ObjectStream<String> lineStream = new PlainTextByLineStream(inputStreamFactory, StandardCharsets.UTF_8);
        ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

        TrainingParameters params = new TrainingParameters();
        params.put(TrainingParameters.ITERATIONS_PARAM, 500+"");
        params.put(TrainingParameters.CUTOFF_PARAM, 0);
        DoccatFactory factory = new DoccatFactory(new FeatureGenerator[] { new BagOfWordsFeatureGenerator() });

        // Train a model
        model = DocumentCategorizerME.train("en", sampleStream, params, factory);

        // Serialize model
        model.serialize(new File(MODEL_FILE_PATH));

        // Use the model to create the Categorizer
        docCategorizer = new DocumentCategorizerME(model);
    }

    /**
     * Load Datasets and evaluate the model
     */
    public Double evaluateModel() throws IOException {
        Integer positive = 0;
        Integer negative = 0;

        Integer falsePositive = 0;
        Integer falseNegative = 0;

        if( docCategorizer == null|| model == null) {
            this.train();
        }

        File myObj = new File(TEST_DT_PATH);
        try(Scanner myReader = new Scanner(myObj)) {
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[]lineSplit = data.split("\t", 2);

                String groundTruth = lineSplit[0];
                String toAnalyze = lineSplit[1];

                // Get the probabilities of all outcome i.e. positive & negative
                double[] probabilitiesOfOutcomes = docCategorizer.categorize(tokenizer.tokenize(toAnalyze));

                // Get name of category which had high probability
                String category = docCategorizer.getBestCategory(probabilitiesOfOutcomes);
                if(category.equals(groundTruth)) {
                    if(category.equals(NLP_FRAMEWORK_CATEGORY)) {
                        positive++;
                    } else {
                        negative ++;
                    }
                } else {
                    if(category.equals(NLP_FRAMEWORK_CATEGORY)) {
                        falseNegative++;
                    } else {
                        falsePositive ++;
                    }
                }
            }
        }

        Integer total = positive + negative;
        Integer totalFails = falseNegative + falsePositive;
        return 100 * (total) / (double) (total + totalFails);
    }


    /**
     * Tokenize sentence into tokens. TODO : Rework  the tokenizer
     * @param sentence The sentence to tokenize
     * @return Tokens found as a list of string
     */
    private static String[] getTokens(String sentence) {

        // Use model that was created in earlier tokenizer
        try (InputStream modelIn = new FileInputStream(TOKENIZER_FILE_PATH)) {

            TokenizerME categorizer = new TokenizerME(new TokenizerModel(modelIn));
            String[] tokens = categorizer.tokenize(sentence);
            return tokens;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Predict the category of the text
     * @param text
     * @return The category as a String
     * @throws IOException
     */
    public String predict(String text) throws IOException {
        if( docCategorizer == null|| model == null) {
            this.train();
        }

        double[] probabilitiesOfOutcomes = docCategorizer.categorize(tokenizer.tokenize(text));
        return docCategorizer.getBestCategory(probabilitiesOfOutcomes);
    }

    /**
     * Get the result of the dectection as a NLP results, allowing an easy retrieval of the confidence score
     * @param text The request to evaluate
     * @return the result of the detection as,
     * @throws IOException
     */
    public NLPResults getNLPResult(String text) throws IOException {
        if( docCategorizer == null|| model == null) {
            this.train();
        }

        double[] probabilitiesOfOutcomes = docCategorizer.categorize(tokenizer.tokenize(text));
        String category = docCategorizer.getBestCategory(probabilitiesOfOutcomes);
        return new NLPResults(category, probabilitiesOfOutcomes);
    }


    /**
     * Get the Category corresponding to the probabilities of outcomes
     * @param probabilitiesOfOutcomes
     * @return
     * @throws IOException
     */
    public String getBestCategory(double[] probabilitiesOfOutcomes) throws IOException {
        if( docCategorizer == null|| model == null) {
            this.train();
        }
        return docCategorizer.getBestCategory(probabilitiesOfOutcomes);
    }

    /**
     * Check if the model file exists int
     * @return
     */
    public boolean checkIfModelExists() {
        log.info("Checking the existence of the model file at '%s'.", MODEL_FILE_PATH);
        this.modelFile = new File(MODEL_FILE_PATH);
        return modelFile.exists();
    }

    /**
     * Import the file model from the Artemis workspace
     * @throws IOException
     */
    public void importModelFile() throws IOException, NLPIncorrectConfigurationException {

        if(!checkIfModelExists()){
            String message = String.format("No model file with name '%s' was found under workspace '%s'.", MODEL_FILE_PATH, ARTEMIS_WORKSPACE);
            throw new NLPIncorrectConfigurationException(message, ERROR_PREFIX);
        }

        InputStream is = new FileInputStream(this.modelFile);
        this.model = new DoccatModel(is);
        this.docCategorizer = new DocumentCategorizerME(model);
    }

    public NLPEngine(Log log) throws IOException, NLPIncorrectConfigurationException {
        this.log = log;
        importModelFile();
    }
}
