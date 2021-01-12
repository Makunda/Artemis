package com.castsoftware.artemis.controllers;

import com.castsoftware.artemis.config.Configuration;
import com.castsoftware.artemis.exceptions.file.MissingFileException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Arrays;
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
}
