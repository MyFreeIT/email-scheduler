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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.jdatepicker.impl.*;

public class MainWindow {
  private MainWindow() {}

  public static void showUi() {
    SwingUtilities.invokeLater(
        () -> {
          // Frame setup
          JFrame frame = new JFrame("Email Scheduler");
          frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
          frame.setResizable(false);
          frame.setPreferredSize(new Dimension(600, 400));

          // Main panel
          JPanel main = new JPanel(new BorderLayout(10, 10));
          main.setBorder(new EmptyBorder(12, 12, 12, 12));
          frame.setContentPane(main);

          // Top: To & Subject
          JPanel top = new JPanel(new GridLayout(2, 2, 8, 8));
          top.add(new JLabel("To:"));
          JTextField toField = new JTextField();
          top.add(toField);
          top.add(new JLabel("Subject:"));
          JTextField subjectField = new JTextField();
          top.add(subjectField);
          main.add(top, BorderLayout.NORTH);

          // Center: Message body
          JTextArea bodyArea = new JTextArea();
          bodyArea.setLineWrap(true);
          bodyArea.setWrapStyleWord(true);
          JScrollPane bodyScroll = new JScrollPane(bodyArea);
          bodyScroll.setBorder(BorderFactory.createTitledBorder("Message Body"));
          main.add(bodyScroll, BorderLayout.CENTER);

          // Bottom: schedule pickers + button
          JPanel bottom = new JPanel(new BorderLayout(10, 10));

          // DatePicker (JDatePicker)
          LocalDate today = LocalDate.now();
          UtilDateModel dateModel = new UtilDateModel();
          dateModel.setDate(today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
          dateModel.setSelected(true);

          Properties p = new Properties();
          p.put("text.today", "Today");
          p.put("text.month", "Month");
          p.put("text.year", "Year");

          JDatePanelImpl datePanel = new JDatePanelImpl(dateModel, p);

          DateLabelFormatter df = new DateLabelFormatter("yyyy-MM-dd");
          JDatePickerImpl datePicker = new JDatePickerImpl(datePanel, df);

          datePicker.getJFormattedTextField().setText(df.valueToString(dateModel.getValue()));

          datePicker.addActionListener(
              e -> {
                Date sel = dateModel.getValue();
                if (sel != null) {
                  datePicker.getJFormattedTextField().setText(df.valueToString(sel));
                }
              });

          // Time spinner
          SpinnerDateModel timeModel =
              new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
          JSpinner timeSpinner = new JSpinner(timeModel);
          timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));

          JPanel dtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
          dtPanel.setBorder(BorderFactory.createTitledBorder("Schedule"));
          dtPanel.add(new JLabel("Date:"));
          dtPanel.add(datePicker);
          dtPanel.add(new JLabel("Time:"));
          dtPanel.add(timeSpinner);

          bottom.add(dtPanel, BorderLayout.CENTER);

          // Schedule button
          JButton scheduleButton = new JButton("Schedule");
          scheduleButton.setPreferredSize(new Dimension(120, 30));
          scheduleButton.addActionListener(
              e -> {
                try {
                  String to = toField.getText().trim();
                  String subject = subjectField.getText().trim();
                  String body = bodyArea.getText().trim();

                  Date d = (Date) datePicker.getModel().getValue();
                  LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                  Date t = (Date) timeSpinner.getValue();
                  LocalTime time =
                      t.toInstant()
                          .atZone(ZoneId.systemDefault())
                          .toLocalTime()
                          .withSecond(0)
                          .withNano(0);

                  LocalDateTime dateTime = LocalDateTime.of(date, time);
                  EmailScheduler.schedule(to, subject, body, dateTime);
                } catch (Exception ex) {
                  JOptionPane.showMessageDialog(
                      frame,
                      "Please fill all fields and pick a valid date/time.",
                      "Input Error",
                      JOptionPane.ERROR_MESSAGE);
                }
              });

          JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
          btnPanel.add(scheduleButton);
          bottom.add(btnPanel, BorderLayout.SOUTH);

          main.add(bottom, BorderLayout.SOUTH);

          // Finalize
          frame.pack();
          frame.setLocationRelativeTo(null);
          TrayHandler.addTraySupport(frame);
          frame.setVisible(true);
        });
  }

  /** Formatter JDatePicker. */
  private static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
    private final SimpleDateFormat dateFormatter;

    public DateLabelFormatter(String pattern) {
      this.dateFormatter = new SimpleDateFormat(pattern);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
      return dateFormatter.parse(text);
    }

    @Override
    public String valueToString(Object value) {
      if (value instanceof Date) {
        return dateFormatter.format((Date) value);
      }
      return "";
    }
  }
}
