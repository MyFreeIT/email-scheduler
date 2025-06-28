package com.github.myfreeit.emailscheduler;

/*
 * Copyright (c) 2025, Denis Odesskiy. All rights reserved.
 *
 * This software is the confidential and proprietary information of Denis Odesskiy
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Denis Odesskiy.
 */

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class EmailScheduler {
  private EmailScheduler() {}

  public static void schedule(String to, String subject, String body, LocalDateTime dateTime) {
    try {
      JobDetail job =
          JobBuilder.newJob(EmailJob.class)
              .usingJobData("to", to)
              .usingJobData("subject", subject)
              .usingJobData("body", body)
              .withIdentity("emailJob" + System.currentTimeMillis(), "group1")
              .build();

      Trigger trigger =
          TriggerBuilder.newTrigger()
              .startAt(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()))
              .build();

      Scheduler scheduler = new StdSchedulerFactory().getScheduler();
      scheduler.start();
      scheduler.scheduleJob(job, trigger);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static class EmailJob implements Job {
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getMergedJobDataMap();
      EmailSender.send(data.getString("to"), data.getString("subject"), data.getString("body"));
    }
  }
}
