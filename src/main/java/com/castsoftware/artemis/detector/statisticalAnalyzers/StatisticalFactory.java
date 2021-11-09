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

import com.castsoftware.artemis.detector.statisticalAnalyzers.java.JavaStatisticalAnalyzer;
import com.castsoftware.artemis.global.SupportedLanguage;
import com.castsoftware.artemis.neo4j.Neo4jAL;

public class StatisticalFactory {

	/**
	 * Get a new instance of specific analyzer by language
	 * @param language Language to process
	 * @return
	 */
	public static AStatisticalAnalyzer getAnalyzer(Neo4jAL neo4jAL, String application, SupportedLanguage language) throws Exception {
		switch (language) {
			case JAVA:
				return new JavaStatisticalAnalyzer(neo4jAL, application, language);
			default:
				throw new Exception(String.format("No analyzer found for language %s.", language.toString()));
		}
	}
}
