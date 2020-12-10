package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.database.Neo4jAL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UtilsController {

    /**
     * Change the Artemis default workspace
     * @param directoryPath The new Artemis workspace
     * @return
     */
    public static String changeArtemisDirectory(String directoryPath) {
        if (!Files.exists(Path.of(directoryPath))) {
            return String.format("'%s' is not a valid path. Make sure the target folder exists and retry.", directoryPath);
        }

        Configuration.set("artemis.workspace.folder", directoryPath);
        return String.format("Artemis workspace folder was successfully changed to '%s'.", directoryPath);
    }


}


