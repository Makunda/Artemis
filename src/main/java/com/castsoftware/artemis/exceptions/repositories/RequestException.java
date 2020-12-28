/*
 *  Copyright (C) 2020  Hugo JOBY
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.castsoftware.artemis.exceptions.repositories;

import com.castsoftware.artemis.exceptions.ExtensionException;

public class RequestException extends ExtensionException {
    private static final long serialVersionUID = -4308015546591283998L;
    private static final String MESSAGE_PREFIX = "Error, the following request produced an error.";
    private static final String CODE_PREFIX = "REP_RE_";

    public RequestException(String message, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message), cause, CODE_PREFIX.concat(code));
    }

    public RequestException(String message, String code) {
        super(MESSAGE_PREFIX.concat(message), CODE_PREFIX.concat(code));
    }

    public RequestException(String message, String query, Throwable cause, String code) {
        super(MESSAGE_PREFIX.concat(message).concat(" ### Query : ").concat(query), cause, CODE_PREFIX.concat(code));
    }
}
