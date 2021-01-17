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

package com.castsoftware.artemis.mailer;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MailerTemplate {

  /**
   * Generate a failure mail template for a specific application
   *
   * @param application Application concerned by the failure
   * @param error Error related to the failure
   * @return
   */
  public static String generateFailureMail(String application, Exception error) {
    StringBuilder sb = new StringBuilder();

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    Date date = new Date();

    sb.append("The analysis failed for application : ").append(application);
    sb.append(" on ").append(formatter.format(date));
    sb.append("<br");
    sb.append("The analysis failed for the following reason: ").append(error.getMessage());
    sb.append(error.getLocalizedMessage());

    return sb.toString();
  }
}
