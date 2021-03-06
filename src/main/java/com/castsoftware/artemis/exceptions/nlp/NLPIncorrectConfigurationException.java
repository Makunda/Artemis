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

package com.castsoftware.artemis.exceptions.nlp;

import com.castsoftware.artemis.exceptions.ExtensionException;

/**
 * The <code>Neo4jNoResult</code> is thrown when the NLP Configuration is not correct. Neo4jNoResult
 */
public class NLPIncorrectConfigurationException extends ExtensionException {

  private static final long serialVersionUID = 8218353918930322258L;
  private static final String MESSAGE_PREFIX =
      "Error, the configuration of the NLP workspace is not correct : ";
  private static final String CODE_PREFIX = "NLP_BC_";

  public NLPIncorrectConfigurationException(String message, Throwable cause, String code) {
    super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
  }

  public NLPIncorrectConfigurationException(String message, String code) {
    super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
  }
}
