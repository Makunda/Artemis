package com.castsoftware.artemis.reports;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.nlp.model.NLPResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class ReportGenerator {

    public static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    public static final String REPORT_FOLDER = ARTEMIS_WORKSPACE + Configuration.get("nlp.reports_generator.folder");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy_MM_dd_HHmmss");

    public String applicationContext;

    public void addResult(NLPResults result) {

    }

    public Path generate() throws IOException {
        Path reportFolderPath = Path.of(REPORT_FOLDER);
        if(!Files.exists(reportFolderPath)) {
            Files.createDirectories(reportFolderPath);
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String fileLocation = reportFolderPath + "temp.xlsx";

        FileOutputStream outputStream = new FileOutputStream(fileLocation);
        workbook.write(outputStream);
        workbook.close();
    }

    public ReportGenerator(String applicationContext) {
        this.applicationContext = applicationContext;
    }

}
