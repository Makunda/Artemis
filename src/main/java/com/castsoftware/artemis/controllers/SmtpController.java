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

package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SmtpController {

  private static final String CONFIG_KEY_RECIPIENT_SMTP = "artemis.smtp.recipients";
  /**
   * Get the list of the recipients that will receive the results of the framework discovery
   *
   * @return The list of recipients
   */
  public static List<String> getMailsRecipients() {
    String recipientsString = Configuration.get(CONFIG_KEY_RECIPIENT_SMTP);
    return Arrays.asList(recipientsString.split(";"));
  }

  /**
   * Set a new list of recipients. Must be separated by a coma
   *
   * @param recipients List of mails separated by a coma
   * @return True if the operation was successful, False if the list of address provided is not
   *     valid
   */
  public static Boolean setMailsRecipients(Neo4jAL neo4jAL, String recipients) {
    // Validates the mails list
    try {
      InternetAddress[] emailAddr = InternetAddress.parse(recipients);
      for (InternetAddress ia : emailAddr) {
        ia.validate();
      }

      // All the address are valid
      UserConfiguration.set(neo4jAL, CONFIG_KEY_RECIPIENT_SMTP, recipients);

      return true;
    } catch (AddressException | MissingFileException ex) {
      return false;
    }
  }

  /**
   * Get the Parameters of the mail server
   *
   * @param neo4jAL Neo4j Access Layer
   * @return
   */
  public static List<String> getMailConfiguration(Neo4jAL neo4jAL) {
    List<String> returnList = new ArrayList<>();

    String smtpServer = UserConfiguration.get(neo4jAL, "smtp.server");

    neo4jAL.logInfo("Artemis directory loaded : " + UserConfiguration.isLoaded());

    if (UserConfiguration.isLoaded()) {
      for (Iterator<Object> it = UserConfiguration.getKeySet().iterator(); it.hasNext(); ) {
        Object o = it.next();
        String key = o.toString();

        if (key.startsWith("smtp.")) {
          returnList.add("Key in configuration : " + o.toString());
        }
      }
    }
    neo4jAL.logInfo("Address of the smtp server : " + smtpServer);
    return returnList;
  }


}
