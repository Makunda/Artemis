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

package com.castsoftware.artemis.controllers.api;

import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.pythia.PythiaCom;

public class ConfigurationController {

    /**
     * Get the URI in the configuration of Pythia
     * @param neo4jAL
     * @return
     */
    public static String getURIPythia(Neo4jAL neo4jAL) {
        String uri = PythiaCom.getInstance(neo4jAL).getUri();
        if(uri == null) return "";
        return uri;
    }

    /**
     * Set the URI in the configuration of Pythia
     * @param neo4jAL
     * @param uri the new URI
     * @return
     */
    public static String setURIPythia(Neo4jAL neo4jAL, String uri) {
        String newUri = PythiaCom.getInstance(neo4jAL).getUri();
        if(newUri == null) return "";
        return newUri;
    }

    /**
     * Set the Token in the configuration of Pythia
     * @param neo4jAL
     * @param token the new access token
     * @return
     */
    public static Boolean setTokenPythia(Neo4jAL neo4jAL, String token) throws MissingFileException {
        Boolean changed = PythiaCom.getInstance(neo4jAL).setToken(token);
        return changed;
    }

    /**
     * Check if the Token in the configuration of Pythia is present
     * @param neo4jAL
=     * @return
     */
    public static Boolean getTokenPythia(Neo4jAL neo4jAL) throws MissingFileException {
        Boolean present = PythiaCom.getInstance(neo4jAL).isTokenPresent();
        return present;
    }

}
