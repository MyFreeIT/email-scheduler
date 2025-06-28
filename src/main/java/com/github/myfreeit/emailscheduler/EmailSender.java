package com.github.myfreeit.emailscheduler;

/*
 * Copyright (c) 2025, Denis Odesskiy. All rights reserved.
 *
 * This software is the confidential and proprietary information of Denis Odesskiy
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Denis Odesskiy.
 */

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmailSender {

  private static final Properties config = new Properties();

  static {
    try (InputStream input =
        EmailSender.class.getClassLoader().getResourceAsStream("config.properties")) {
      if (input != null) {
        config.load(input);
      } else {
        throw new RuntimeException("Could not find config.properties");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void send(String to, String subject, String body) {
    final String username = config.getProperty("smtp.username");
    final String password = config.getProperty("smtp.password");

    Properties props = new Properties();
    props.put("mail.smtp.auth", config.getProperty("smtp.auth", "true"));
    props.put("mail.smtp.starttls.enable", config.getProperty("smtp.starttls", "true"));
    props.put("mail.smtp.host", config.getProperty("smtp.host"));
    props.put("mail.smtp.port", config.getProperty("smtp.port"));

    Session session =
        Session.getInstance(
            props,
            new Authenticator() {
              @Override
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
              }
            });

    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
      message.setSubject(subject);
      message.setText(body);
      Transport.send(message);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }
}
