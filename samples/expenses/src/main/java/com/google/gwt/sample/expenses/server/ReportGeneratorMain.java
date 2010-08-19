/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.expenses.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Command-line entry point for report generation.
 */
public class ReportGeneratorMain {

  protected static final int MAX_REPORTS = 18000000;
  protected static final int REPORTS_PER_DIR = 1000000;
  protected static final int VERSION = 1;
  
  static int index = 0;
  static PrintWriter empWriter;
  static PrintWriter repWriter;
  static PrintWriter expWriter;
  static String startDate = null;
  
  static void makeOutputDir() {
    try {
      if (startDate == null) {
        startDate = dateToString(new Date());
      }
      String dir = "/auto/gwt/io-expenses-" + MAX_REPORTS + "-" + twoDigit(index++) + "-" + startDate;
      new File(dir).mkdirs();
      if (empWriter != null) {
        empWriter.close();
      }
      if (repWriter != null) {
        repWriter.close();
      }
      if (expWriter != null) {
        expWriter.close();
      }
      empWriter = new PrintWriter(dir + "/Employee.csv");
      repWriter = new PrintWriter(dir + "/Report.csv");
      expWriter = new PrintWriter(dir + "/Expense.csv");
      empWriter.println("userName,displayName,supervisorKey,VERSION,key,department,password");
      repWriter.println("created,notes,VERSION,approvedSupervisor,key,reporter,purposeLowerCase,purpose,department");
      expWriter.println("category,description,reasonDenied,amount,VERSION,report,key,created,approval");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    try {
      makeOutputDir();

      ReportGenerator reportGenerator = new ReportGenerator() {
        long allids = 10684381L;

        @Override
        public boolean shouldContinue() {
          return getNumReports() < MAX_REPORTS;
        }

        @Override
        public long storeEmployee(EmployeeDTO employee) {
          // Start a new output directory every 1M reports
          if (getNumReports() >= REPORTS_PER_DIR * index) {
            ReportGeneratorMain.makeOutputDir();
          }
          
          long id = allids++;
          // userName,displayName,supervisorKey,VERSION,key,department,password
          empWriter.println(employee.userName + "," + employee.displayName + ","
              + employee.supervisorKey + "," + VERSION + "," + id + ","
              + employee.department + ",");
          return id;
        }

        @Override
        public long storeExpense(ExpenseDTO expense) {
          long id = allids++;
          // category,description,reasonDenied,amount,VERSION,report,key,created,approval"
          expWriter.println(expense.category + "," + expense.description + ",," + expense.amount + ","
              + VERSION + "," + expense.reportId + "," + id + "," + dateToString(expense.created)
              + ",");
          return id;
        }

        @Override
        public long storeReport(ReportDTO report) {          
          long id = allids++;
          // created,notes,VERSION,approvedSupervisor,key,reporter,purposeLowerCase,purpose,department
          repWriter.println(dateToString(report.created) + ",\"" + report.notes + "\"," + VERSION + ","
              + report.approvedSupervisorKey + "," + id + "," + report.reporterKey + ",\""
              + report.purpose.toLowerCase() + "\",\"" + report.purpose + "\"," + report.department);
          return id;
        }
      };

      reportGenerator.init("/home/rice/www/dist.all.last.txt",
          "/home/rice/www/dist.female.first.txt",
      "/home/rice/www/dist.male.first.txt");

      // Use same manager for everyone
      long supervisorId = 1;
      while (reportGenerator.shouldContinue()) {
        int department = reportGenerator.getDepartment();
        reportGenerator.makeEmployee(department, supervisorId);
      }

      empWriter.close();
      repWriter.close();
      expWriter.close();
    } catch (Exception e) {
      throw e;
    }
  }

  @SuppressWarnings("deprecation")
  private static String dateToString(Date date) {
    return (date.getYear() + 1900) + twoDigit(date.getMonth() + 1)
        + twoDigit(date.getDate()) + "T" + twoDigit(date.getHours()) + ":"
        + twoDigit(date.getMinutes());
  }
  
  private static String twoDigit(int i) {
    return i < 10 ? "0" + i : "" + i;
  }
}
