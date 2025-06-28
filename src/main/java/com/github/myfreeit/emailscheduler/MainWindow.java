package com.github.myfreeit.emailscheduler;

/*
 * Copyright (c) 2025, Denis Odesskiy. All rights reserved.
 *
 * This software is the confidential and proprietary information of Denis Odesskiy
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Denis Odesskiy.
 */

import java.awt.*;
import java.time.LocalDateTime;
import javax.swing.*;

public class MainWindow {
  private MainWindow() {}

  public static void showUi() {
    JFrame frame = new JFrame("Email Scheduler");
    frame.setSize(400, 300);
    frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    frame.setLayout(new GridLayout(6, 2));

    JTextField toField = new JTextField();
    JTextField subjectField = new JTextField();
    JTextArea bodyArea = new JTextArea();
    JTextField timeField = new JTextField("yyyy-MM-ddTHH:mm");

    JButton scheduleButton = new JButton("Schedule");
    scheduleButton.addActionListener(
        e -> {
          String to = toField.getText();
          String subject = subjectField.getText();
          String body = bodyArea.getText();
          LocalDateTime time = LocalDateTime.parse(timeField.getText());

          EmailScheduler.schedule(to, subject, body, time);
        });

    frame.add(new JLabel("To:"));
    frame.add(toField);
    frame.add(new JLabel("Subject:"));
    frame.add(subjectField);
    frame.add(new JLabel("Message Body:"));
    frame.add(new JScrollPane(bodyArea));
    frame.add(new JLabel("Scheduled Time (ISO):"));
    frame.add(timeField);
    frame.add(scheduleButton);

    TrayHandler.addTraySupport(frame);
    frame.setVisible(true);
  }
}
