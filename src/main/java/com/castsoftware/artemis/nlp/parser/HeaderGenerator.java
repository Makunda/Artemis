package com.castsoftware.artemis.nlp.parser;

import com.castsoftware.artemis.config.Configuration;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;

public class HeaderGenerator {

    private static final String HEADER_FILE_NAME = Configuration.get("artemis.parser.header_file.name");

    private static HeaderGenerator instance = null;

    private Integer numberOfLine = 0;
    private File headerFile = null;

    public static HeaderGenerator getInstance() throws IOException {
        if(instance == null) instance = new HeaderGenerator();
        return instance;
    }

    public String getRandomHeader() throws IOException {
        int randomLine = ThreadLocalRandom.current().nextInt(0, numberOfLine + 1);
        return Files.readAllLines(headerFile.toPath()).get(randomLine);
    }

    private HeaderGenerator () throws IOException {
        String headerFilePath =  Configuration.get("artemis.workspace.folder") + HEADER_FILE_NAME;
        headerFile = new File(headerFilePath);

        if(!headerFile.exists()) {
            throw new IOException(String.format("File with name '%s' does not exist.", headerFile));
        }

        byte[] c = new byte[1024];
        int count = 0;
        int readChars = 0;
        boolean empty = true;

        try (InputStream is = new BufferedInputStream(new FileInputStream(headerFile))){

            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }

            numberOfLine =  (count == 0 && !empty) ? 1 : count;
        }
    }
}
