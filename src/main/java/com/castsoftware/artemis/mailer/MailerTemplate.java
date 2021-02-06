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

import com.castsoftware.artemis.results.FrameworkResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

  public static String generateResultMail(String application, List<FrameworkResult> nodes) {

    StringBuilder sb = new StringBuilder();

    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    Date date = new Date();

    sb.append("The analysis for application : ")
        .append(application)
        .append(" returned the following results :")
        .append("<br>");
    sb.append("<i>Analysis completed on ").append(formatter.format(date)).append(" </i>");
    sb.append("<br");
    sb.append("Please find the results of the analysis below: <br>");

    // Build the table
    sb.append(
        "<div class=\"divTable paleBlueRows\">\n"
            + "        <div class=\"divTableHeading\">\n"
            + "          <div class=\"divTableRow\">\n"
            + "            <div class=\"divTableHead\">Name</div>\n"
            + "            <div class=\"divTableHead\">Category</div>\n"
            + "            <div class=\"divTableHead\">Description</div>\n"
            + "            <div class=\"divTableHead\">Detected  as</div>\n"
            + "          </div>\n"
            + "          </div>\n"
            + "          <div class=\"divTableBody\">");

    // Append the rows containing the values in the table
    for (FrameworkResult fn : nodes) {

      sb.append(
          "<div class=\"divTableRow\">\n"
              + "            <div class=\"divTableCell\">"
              + fn.name
              + "</div>\n"
              + "            <div class=\"divTableCell\">"
              + fn.category
              + "</div>\n"
              + "            <div class=\"divTableCell\">"
              + fn.description
              + "</div>\n"
              + "            <div class=\"divTableCell\">"
              + fn.type
              + "</div>\n"
              + "          </div>");
    }

    sb.append("</div>\n" + "      </div>");

    return sb.toString();
  }
}
