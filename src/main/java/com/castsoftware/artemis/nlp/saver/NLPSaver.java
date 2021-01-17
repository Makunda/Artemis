package com.castsoftware.artemis.nlp.saver;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.nlp.model.NLPCategory;
import com.castsoftware.artemis.nlp.model.NLPResults;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class NLPSaver implements Closeable {

    public static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    public static final String REPORT_FOLDER = ARTEMIS_WORKSPACE + Configuration.get("artemis.nlp_enrichment.folder");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    private FileWriter fileWriter;
    private String application;

    /**
     * Write the results in the file with its associated category
     * @param category
     * @param content
     * @throws IOException
     */
    public void writeNLPResult(NLPCategory category, String content) throws IOException {
        content = content.replace(System.lineSeparator(), ""); // replace /n
        if(category == NLPCategory.FRAMEWORK) {
            fileWriter.write("Framework\t"+content+"\n");
            System.out.println("Framework\t"+content+"\n");
        } else {
            fileWriter.write("NotFramework\t"+content+"\n");
        }
    }

    /**
     * Init the repository
     * @throws IOException
     */
    private void init() throws IOException {
        Path reportFolderPath = Path.of(REPORT_FOLDER);
        if(!Files.exists(reportFolderPath)) {
            Files.createDirectories(reportFolderPath);
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fileLocation = reportFolderPath + "/ArtemisNLPEnrichment_" + application+ "_on" + SDF.format(timestamp) + ".txt";

        try {
            fileWriter = new FileWriter(fileLocation);
        } catch (IOException e) {
            System.err.println("Failed to create the NLP enrichment file.");
            e.printStackTrace();
            throw e;
        }
    }

    public NLPSaver(String application) throws IOException {
        this.application = application;
        init();
    }

    @Override
    public void close() throws IOException {
        fileWriter.close();
    }
}