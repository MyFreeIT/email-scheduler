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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

public class MainWindow {
  private MainWindow() {}

  public static void showUi() {
    SwingUtilities.invokeLater(
        () -> {
          Scheduler scheduler = EmailScheduler.getScheduler();

          JFrame frame = new JFrame("Email Scheduler");
          frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

          JPanel main = new JPanel(new BorderLayout(10, 10));
          main.setBorder(new EmptyBorder(12, 12, 12, 12));
          frame.setContentPane(main);

          // Inputs (To / Subject)
          JPanel top = new JPanel(new GridLayout(2, 2, 8, 8));
          JTextField toField = new JTextField();
          JTextField subjectField = new JTextField();
          top.add(new JLabel("To:"));
          top.add(toField);
          top.add(new JLabel("Subject:"));
          top.add(subjectField);
          main.add(top, BorderLayout.NORTH);

          // Body area
          JTextArea bodyArea = new JTextArea(6, 1);
          bodyArea.setLineWrap(true);
          bodyArea.setWrapStyleWord(true);
          JScrollPane bodyScroll = new JScrollPane(bodyArea);
          bodyScroll.setBorder(BorderFactory.createTitledBorder("Message Body"));
          main.add(bodyScroll, BorderLayout.CENTER);

          // Controls: DatePicker, Time, Schedule Button
          JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

          // Date picker
          UtilDateModel dateModel = new UtilDateModel();
          LocalDate today = LocalDate.now();
          dateModel.setDate(today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth());
          dateModel.setSelected(true);
          Properties dd = new Properties();
          dd.put("text.today", "Today");
          dd.put("text.month", "Month");
          dd.put("text.year", "Year");
          JDatePickerImpl datePicker =
              new JDatePickerImpl(
                  new JDatePanelImpl(dateModel, dd), new DateLabelFormatter("yyyy-MM-dd"));
          datePicker
              .getJFormattedTextField()
              .setText(new DateLabelFormatter("yyyy-MM-dd").valueToString(dateModel.getValue()));

          datePicker.addActionListener(
              e -> {
                Date d = dateModel.getValue();
                if (d != null) {
                  datePicker
                      .getJFormattedTextField()
                      .setText(new DateLabelFormatter("yyyy-MM-dd").valueToString(d));
                }
              });

          // Time spinner
          JSpinner timeSpinner =
              new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
          timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));

          JButton scheduleBtn = new JButton("Schedule");
          scheduleBtn.setPreferredSize(new Dimension(100, 28));

          controls.add(new JLabel("Date:"));
          controls.add(datePicker);
          controls.add(new JLabel("Time:"));
          controls.add(timeSpinner);
          controls.add(scheduleBtn);

          // Jobs table & Cancel Button
          String[] cols = {"Job ID", "To", "Subject", "When"};
          DefaultTableModel tm =
              new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                  return false;
                }
              };
          JTable table = new JTable(tm);
          // Hide Job ID column
          table.getColumnModel().getColumn(0).setMinWidth(0);
          table.getColumnModel().getColumn(0).setMaxWidth(0);

          JScrollPane tableScroll = new JScrollPane(table);
          tableScroll.setPreferredSize(new Dimension(680, 120));

          JButton cancelBtn = new JButton("Cancel Selected");
          cancelBtn.setPreferredSize(new Dimension(160, 28));
          JPanel jobsPanel = new JPanel(new BorderLayout(6, 6));
          jobsPanel.setBorder(BorderFactory.createTitledBorder("Queued Emails"));
          jobsPanel.add(tableScroll, BorderLayout.CENTER);
          jobsPanel.add(cancelBtn, BorderLayout.SOUTH);

          // Combine controls & jobsPanel vertically
          JPanel bottomContainer = new JPanel();
          bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
          bottomContainer.add(controls);
          bottomContainer.add(Box.createRigidArea(new Dimension(0, 8)));
          bottomContainer.add(jobsPanel);
          main.add(bottomContainer, BorderLayout.SOUTH);

          // Action: Schedule new email
          scheduleBtn.addActionListener(
              e -> {
                try {
                  String to = toField.getText().trim();
                  String sub = subjectField.getText().trim();
                  String body = bodyArea.getText().trim();

                  Date d = dateModel.getValue();
                  LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                  Date t = (Date) timeSpinner.getValue();
                  LocalTime lt =
                      t.toInstant()
                          .atZone(ZoneId.systemDefault())
                          .toLocalTime()
                          .withSecond(0)
                          .withNano(0);
                  LocalDateTime ldt = LocalDateTime.of(ld, lt);

                  String jobId = EmailScheduler.schedule(to, sub, body, ldt);
                  tm.addRow(new Object[] {jobId, to, sub, ldt.toString()});
                } catch (Exception ex) {
                  JOptionPane.showMessageDialog(
                      frame,
                      "Fill all fields & valid date/time",
                      "Input Error",
                      JOptionPane.ERROR_MESSAGE);
                }
              });

          // Action: Cancel selected task
          cancelBtn.addActionListener(
              e -> {
                int r = table.getSelectedRow();
                if (r < 0) {
                  return;
                }
                String jobId = tm.getValueAt(r, 0).toString();
                try {
                  scheduler.deleteJob(JobKey.jobKey(jobId, "emails"));
                  tm.removeRow(r);
                } catch (Exception ex) {
                  JOptionPane.showMessageDialog(
                      frame,
                      "Cannot cancel:\n" + ex.getMessage(),
                      "Error",
                      JOptionPane.ERROR_MESSAGE);
                }
              });

          // Auto-refresh queue every 5 seconds
          new Timer(
                  5_000,
                  e -> {
                    int sel = table.getSelectedRow();
                    tm.setRowCount(0);
                    try {
                      scheduler
                          .getJobGroupNames()
                          .forEach(
                              gr -> {
                                try {
                                  scheduler
                                      .getJobKeys(GroupMatcher.jobGroupEquals(gr))
                                      .forEach(
                                          jk -> {
                                            try {
                                              List<? extends Trigger> tr =
                                                  scheduler.getTriggersOfJob(jk);
                                              Date nxt =
                                                  tr.isEmpty() ? null : tr.get(0).getNextFireTime();
                                              JobDataMap map =
                                                  scheduler.getJobDetail(jk).getJobDataMap();
                                              tm.addRow(
                                                  new Object[] {
                                                    jk.getName(),
                                                    map.getString("to"),
                                                    map.getString("subject"),
                                                    nxt == null ? "-" : nxt.toString()
                                                  });
                                            } catch (SchedulerException ex) {
                                              throw new RuntimeException(ex);
                                            }
                                          });
                                } catch (SchedulerException ex) {
                                  throw new RuntimeException(ex);
                                }
                              });
                    } catch (SchedulerException ex) {
                      throw new RuntimeException(ex);
                    }
                    if (sel >= 0 && sel < tm.getRowCount()) {
                      table.setRowSelectionInterval(sel, sel);
                    }
                  })
              .start();

          frame.pack();
          frame.setLocationRelativeTo(null);
          TrayHandler.addTraySupport(frame);
          frame.setVisible(true);
        });
  }

  /** Formatter JDatePicker. */
  private static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
    private final SimpleDateFormat fmt;

    public DateLabelFormatter(String pattern) {
      this.fmt = new SimpleDateFormat(pattern);
    }

    @Override
    public Object stringToValue(String text) throws ParseException {
      return fmt.parse(text);
    }

    @Override
    public String valueToString(Object value) {
      return (value instanceof Date) ? fmt.format((Date) value) : "";
    }
  }
}
