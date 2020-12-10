package com.castsoftware.artemis.datasets;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.exceptions.dataset.InvalidDatasetException;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DatasetManager {

    public static final String ARTEMIS_LOAD_IN_MEM = Configuration.get("artemis.properties.load_framework_in_memory");

    public static final String ARTEMIS_WORKSPACE = Configuration.get("artemis.workspace.folder");
    public static final String EXPERT_SYSTEM_KNOWLEDGE_BASE_FRAMEWORKS = ARTEMIS_WORKSPACE + Configuration.get("nlp.expert.system.kb.frameworks");
    public static final String EXPERT_SYSTEM_KNOWLEDGE_BASE_NOT_FRAMEWORKS = ARTEMIS_WORKSPACE + Configuration.get("nlp.expert.system.kb.not_frameworks");
    public static final String EXPERT_SYSTEM_KNOWLEDGE_BASE_TO_INVESTIGATE_FRAMEWORKS = ARTEMIS_WORKSPACE + Configuration.get("nlp.expert.system.kb.to_investigate");

    private Path knownFramework;
    private Path knownNotFramework;
    private Path toInvestigateFramework;

    private List<String> frameworkList = new ArrayList<>();
    private List<String> notFrameworkList = new ArrayList<>();
    private List<String> toInvestigateList = new ArrayList<>();

    private static final String ERROR_PREFIX = "DATAMx";

    // Singleton instance
    private static DatasetManager INSTANCE = null;

    /**
     * Load the knowledge base and create them if necessary
     */
    public void getOrCreateDatabaseParts() throws IOException {
        // Create the known Framework entry file if it's not already existing
        if(!Files.exists(knownFramework)) {
            Files.createFile(knownFramework);
        }

        // Create the known Not-Framework entry file if it's not already existing
        if(!Files.exists(knownNotFramework)) {
            Files.createFile(knownNotFramework);
        }

        // Create the to investigate entry file if it's not already existing
        if(!Files.exists(toInvestigateFramework)) {
            Files.createFile(toInvestigateFramework);
        }
    }

    /**
     * Load Artemis Dataset in memory. Only the name of the Frameworks will be loaded.
     * @throws MissingFileException
     */
    public void loadDatasetInMemory() throws MissingFileException {
        Map<Path, List<String>> map = new HashMap<>();
        map.put(knownFramework, frameworkList);
        map.put(knownNotFramework, notFrameworkList);
        map.put(toInvestigateFramework, toInvestigateList);

        // Iterate and only load names of the framework to the list
        for(Map.Entry<Path, List<String>> en : map.entrySet()) {
            try (Reader reader = Files.newBufferedReader(en.getKey())){
                CsvToBean<FrameworkBean> csvToBean = new CsvToBeanBuilder(reader)
                        .withType(FrameworkBean.class)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build();

                Iterator<FrameworkBean> csIterator = csvToBean.iterator();

                FrameworkBean n = null;
                while(csIterator.hasNext()) {
                    n = csIterator.next();
                    en.getValue().add(n.getName());
                }

            } catch (IOException e) {
                String message = String.format("The Artemis dataset file at '%s' is missing.");
                throw new MissingFileException(message, e,  ERROR_PREFIX+ "LDSM1");
            }
        }

    }


    private void addBeanToFile(Path filepath, FrameworkBean entry) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        try(Writer writer  = new FileWriter(filepath.toString(), true)){
            StatefulBeanToCsv<FrameworkBean> sbc = new StatefulBeanToCsvBuilder(writer)
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .build();

            sbc.write(entry);
        }
    }

    /**
     * Add an entry to the database. Based on the Type provided the DatasetManager will dispatch it to specific file
     * @param type Type of entry
     * @param entry Framework bean containing the information to enter
     */
    public void addInput(FrameworkType type, FrameworkBean entry) throws InvalidDatasetException {
        try {
            switch (type) {
                case FRAMEWORK:
                    addBeanToFile(knownFramework,entry);
                    break;
                case NOT_FRAMEWORK:
                    addBeanToFile(knownNotFramework, entry);
                    break;
                case TO_INVESTIGATE:
                    addBeanToFile(toInvestigateFramework, entry);
                    break;
            }
        } catch ( IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException ex) {
            throw new InvalidDatasetException("An error occurred trying to append an entry to the Dataset", ex, ERROR_PREFIX+"ADDI1");
        }
    }

    /**
     * Check for a specific framework name is the Name is already present in the different datasets.
     * @param frameworkName The name to search for
     * @return The type detected by the dataset manager
     */
    public FrameworkType searchForEntry(String frameworkName) {
        if(frameworkList.contains(frameworkName)) {
            return FrameworkType.FRAMEWORK;
        }

        if (notFrameworkList.contains(frameworkName)) {
            return FrameworkType.NOT_FRAMEWORK;
        }

        if (toInvestigateList.contains(frameworkName)) {
            return FrameworkType.TO_INVESTIGATE;
        }

        // Default Return
        return FrameworkType.NOT_KNOWN;
    }

    /**
     * Get the current Instance of the dataset manager
     * @return the current instance
     * @throws IOException Dataset files are missing or cannot be created
     * @throws MissingFileException
     */
    public static DatasetManager getManager() throws IOException, MissingFileException {
        if(INSTANCE == null) {
            INSTANCE = new DatasetManager();
        }

        return INSTANCE;
    }

    private DatasetManager() throws IOException, MissingFileException {
        knownFramework = Path.of(EXPERT_SYSTEM_KNOWLEDGE_BASE_FRAMEWORKS);
        knownNotFramework = Path.of(EXPERT_SYSTEM_KNOWLEDGE_BASE_NOT_FRAMEWORKS);
        toInvestigateFramework = Path.of(EXPERT_SYSTEM_KNOWLEDGE_BASE_TO_INVESTIGATE_FRAMEWORKS);

        getOrCreateDatabaseParts(); // Make sure the files exists or create them in the workspace
        loadDatasetInMemory(); // Load the dataset in memory
    }
}
