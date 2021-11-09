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

package com.castsoftware.artemis.detector.statisticalAnalyzers;

import com.castsoftware.artemis.config.detection.LanguageConfiguration;
import com.castsoftware.artemis.config.detection.LanguageProp;
import com.castsoftware.artemis.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;

/**
 * Abstract Analyzer handling the statistical research in the app
 */
public abstract class AStatisticalAnalyzer {
	private static final String PREFIX = "Statistical Analyzer :: ";
	protected LanguageProp languageProp;

	protected SupportedLanguage language;
	protected String applicationName;
	protected Neo4jAL neo4jAL;

	/**
	 * Get and flag the supposed core of the application
	 */
	public abstract void flagCore() throws Neo4jQueryException;

	/**
	 * Get a potential list of internal utilities
	 */
	public abstract void getUtilities() throws Exception;

	/**
	 * Get the name of the application
	 * @return The name sanitized for neo4j
	 */
	protected String getSanitizedApplication() {
		return String.format("`%s`", applicationName);
	}

	/**
	 * Log info for the class
	 * @param toLog Information to log
	 */
	protected void logInfo(String toLog) {
		this.neo4jAL.logInfo(String.format("%s %s", PREFIX, toLog));
	}

	/**
	 * Log an error during the execution
	 * @param toLog Information to log
	 * @param err Error to log
	 */
	protected void logError(String toLog, Exception err) {
		this.neo4jAL.logError(String.format("%s %s", PREFIX, toLog), err);
	}

	/**
	 * Log an error during the execution
	 * @param toLog Information to log
	 */
	protected void logError(String toLog) {
		this.neo4jAL.logError(String.format("%s %s", PREFIX, toLog));
	}

	/**
	 * Constructor
	 * @param neo4jAL Neo4j Access Layer
	 * @param application Name of the application
	 */
	public AStatisticalAnalyzer(Neo4jAL neo4jAL, String application, SupportedLanguage language) {
		this.neo4jAL = neo4jAL;
		this.applicationName = application;
		this.language = language;

		// Get the configuration
		LanguageConfiguration lc = LanguageConfiguration.getInstance();
		this.languageProp = lc.getLanguageProperties(language.toString());
	}
}
