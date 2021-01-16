package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.config.UserConfiguration;
import com.castsoftware.artemis.database.Neo4jAL;
import com.castsoftware.artemis.exceptions.file.MissingFileException;
import com.castsoftware.artemis.mailer.Mailer;
import org.neo4j.kernel.impl.security.User;

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
     * @return The list of recipients
     */
    public static List<String> getMailsRecipients() {
        String recipientsString = Configuration.get(CONFIG_KEY_RECIPIENT_SMTP);
        return Arrays.asList(recipientsString.split(";"));
    }

    /**
     * Set a new list of recipients. Must be separated by a coma
     * @param recipients List of mails separated by a coma
     * @return True if the operation was successful, False if the list of address provided is not valid
     */
    public static Boolean setMailsRecipients(String recipients) {
        // Validates the mails list
        try {
            InternetAddress[] emailAddr = InternetAddress.parse(recipients);
            for(InternetAddress ia : emailAddr) {
                ia.validate();
            }

            // All the address are valid
            Configuration.set(CONFIG_KEY_RECIPIENT_SMTP, recipients);
            Configuration.saveAndReload();

            return true;
        } catch (AddressException | MissingFileException ex) {
            return false;
        }
    }

    /**
     * Get the Parameters of the mail server
     * @param neo4jAL Neo4j Access Layer
     * @return
     */
    public static List<String> getMailConfiguration(Neo4jAL neo4jAL) {
        List<String> returnList = new ArrayList<>();

        String smtpServer= UserConfiguration.get("smtp.server");

        neo4jAL.logInfo("Artemis directory loaded : " + UserConfiguration.isLoaded());

        if(UserConfiguration.isLoaded()) {
            for (Iterator<Object> it = UserConfiguration.getKeySet().iterator(); it.hasNext(); ) {
                Object o = it.next();
                String key = o.toString();

                if(key.startsWith("smtp.")) {
                    returnList.add("Key in configuration : " + o.toString());
                }
            }
        }
        neo4jAL.logInfo("Address of the smtp server : "+smtpServer);
        return returnList;
    }

    public static void testMailCampaign() throws Exception {
        try {
            Mailer.bulkMail();
        } catch (NullPointerException e) {
            System.err.println("An error happened :");
            System.err.println(e.getLocalizedMessage());
            System.err.println(e.getMessage());
        }
    }
}
