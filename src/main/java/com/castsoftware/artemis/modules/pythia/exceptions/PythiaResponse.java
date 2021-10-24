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

package com.castsoftware.artemis.modules.pythia.exceptions;

public class PythiaResponse extends Throwable {
	private static final long serialVersionUID = 2486895502406071343L;
	private static final String APPEND_AUTO = "Pythia response  :: ";

	/**
	 * PythiaResponse constructor
	 *
	 * @param message Exception message that will be log.
	 * @param cause Throwable object
	 */
	public PythiaResponse(String message, Throwable cause) {
		super(APPEND_AUTO + message, cause);
	}

	/**
	 * PythiaResponse constructor
	 * @param message Exception message that will be log.
	 */
	public PythiaResponse(String message) {
		super(APPEND_AUTO + message);
	}
}
