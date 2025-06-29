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
  private static final Scheduler scheduler;

  static {
    try {
      scheduler = StdSchedulerFactory.getDefaultScheduler();
      scheduler.start();
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to initialize Quartz scheduler", e);
    }
  }

  private EmailScheduler() {}

  public static Scheduler getScheduler() {
    return scheduler;
  }

  public static String schedule(String to, String subject, String body, LocalDateTime dateTime) {
    try {
      String jobId = "emailJob_" + System.currentTimeMillis();
      JobDetail job =
          JobBuilder.newJob(EmailJob.class)
              .withIdentity(jobId, "emails")
              .withDescription("Send email to " + to)
              .usingJobData("to", to)
              .usingJobData("subject", subject)
              .usingJobData("body", body)
              .build();

      Trigger trigger =
          TriggerBuilder.newTrigger()
              .withIdentity("trigger_" + jobId, "emails")
              .startAt(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()))
              .withSchedule(SimpleScheduleBuilder.simpleSchedule())
              .build();

      scheduler.scheduleJob(job, trigger);
    } catch (SchedulerException e) {
      throw new RuntimeException("Failed to schedule email", e);
    }
    return to;
  }

  public static class EmailJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getMergedJobDataMap();
      String to = data.getString("to");
      String subject = data.getString("subject");
      String body = data.getString("body");
      EmailSender.send(to, subject, body);
    }
  }
}
