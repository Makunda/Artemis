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

package com.castsoftware.artemis.exceptions.repositories;

import com.castsoftware.artemis.exceptions.ExtensionException;

public class MalformedResultException extends ExtensionException {

  private static final long serialVersionUID = 3131823207835935799L;
  private static final String MESSAGE_PREFIX = "Error, the following request produced an error.";
  private static final String CODE_PREFIX = "REP_RE_";

  public MalformedResultException(String message, Throwable cause, String code) {
    super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
  }

  public MalformedResultException(String message, String code) {
    super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
  }

  public MalformedResultException(String message, String query, Throwable cause, String code) {
    super(
        MESSAGE_PREFIX.concat(message).concat(" ### Query : ").concat(query),
        cause,
        CODE_PREFIX.concat(code));
  }
}
