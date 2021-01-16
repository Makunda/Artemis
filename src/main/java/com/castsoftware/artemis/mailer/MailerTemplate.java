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
