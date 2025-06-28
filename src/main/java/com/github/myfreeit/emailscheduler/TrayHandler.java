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
import javax.swing.*;

public class TrayHandler {
  private TrayHandler() {}

  public static void addTraySupport(JFrame frame) {

    if (!SystemTray.isSupported()) {
      return;
    }

    Image icon = Toolkit.getDefaultToolkit().getImage("src/main/resources/trayicon.png");
    TrayIcon trayIcon = new TrayIcon(icon, "Email Scheduler");

    trayIcon.setImageAutoSize(true);
    trayIcon.addActionListener(e -> frame.setVisible(true));

    PopupMenu menu = new PopupMenu();
    MenuItem exitItem = new MenuItem("Exit");
    exitItem.addActionListener(e -> System.exit(0));
    menu.add(exitItem);
    trayIcon.setPopupMenu(menu);

    try {
      SystemTray tray = SystemTray.getSystemTray();
      tray.add(trayIcon);
    } catch (AWTException e) {
      e.printStackTrace();
    }

    frame.addWindowListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosing(java.awt.event.WindowEvent e) {
            frame.setVisible(false);
          }
        });
  }
}
