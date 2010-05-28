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

import com.google.gwt.requestfactory.server.RequestFactoryServlet;
import com.google.gwt.sample.expenses.server.domain.Employee;
import com.google.gwt.sample.expenses.server.domain.Expense;
import com.google.gwt.sample.expenses.server.domain.Report;

import java.util.Date;
import java.util.Random;

/**
 * Dwindling interim servlet that calls our mock storage backend directly
 * instead of reflectively. Should soon vanish completely.
 */
@SuppressWarnings("serial")
public class ExpensesDataServlet extends RequestFactoryServlet {

  // Must be in sync with DESCRIPTIONS
  private static final String[] CATEGORIES = {
      "Dining", "Dining", "Dining", "Lodging", "Lodging",
      "Local Transportation", "Local Transportation", "Local Transportation",
      "Air Travel", "Air Travel", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies",};

  private static final String[] DEPARTMENTS = {
      "Sales", "Marketing", "Engineering", "Operations"};

  // Must be in sync with CATEGORIES
  private static final String[] DESCRIPTIONS = {
      "Breakfast", "Lunch", "Dinner", "Hotel", "Bed & Breakfast", "Train fare",
      "Taxi fare", "Bus ticket", "Flight from ATL to SFO",
      "Flight from SFO to ATL", "Paperclips", "Stapler", "Scissors", "Paste",};

  private static final String[] FIRST_NAMES = {
      "Amy", "Bob", "Catherine", "Dave", "Earl", "Flin", "George", "Harriot",
      "Ingrid", "John", "Katy", "Leo", "Mike", "Nancy", "Owen", "Paul",
      "Reece", "Sally", "Terry", "Val", "Wes", "Xavier", "Zack"};

  private static final String[] LAST_NAMES = {
      "Awesome", "Bravo", "Cool", "Fantastic", "Great", "Happy",
      "Ignoranomous", "Krazy", "Luminous", "Magnanimous", "Outstanding",
      "Perfect", "Radical", "Stellar", "Terrific", "Wonderful"};

  private static final String[] NOTES = {
      // Some entries do not have notes.
      "", "Need approval by Monday", "Show me the money",
      "Please bill to the Widgets project", "High priority", "Review A.S.A.P."};

  private static final String[] PURPOSES = {
      "Spending lots of money", "Team building diamond cutting offsite",
      "Visit to Istanbul", "ISDN modem for telecommuting", "Sushi offsite",
      "Baseball card research", "Potato chip cooking offsite",
      "Money laundering", "Donut day"};

  Random rand = new Random();

  @Override
  protected void initDb() {
    long size = Employee.countEmployees();
    if (size > 1) {
      return;
    }

    // Initialize the database.
    for (int i = 0; i < 100; i++) {
      addEmployee();
    }
  }

  /**
   * Add a randomly generated employee.
   */
  private void addEmployee() {
    Employee abc = new Employee();
    String firstName = nextValue(FIRST_NAMES);
    String lastName = nextValue(LAST_NAMES);
    String username = (firstName.charAt(0) + lastName).toLowerCase();
    abc.setUserName(username);
    abc.setDisplayName(firstName + " " + lastName);
    abc.setDepartment(nextValue(DEPARTMENTS));
    abc.persist();

    addReports(abc.getId());
  }

  private void addExpenses(Long reportId) {
    int num = rand.nextInt(5) + 1;
    for (int i = 0; i < num; i++) {
      int index = rand.nextInt(DESCRIPTIONS.length);
      Expense detail = new Expense();
      detail.setReportId(reportId);
      detail.setDescription(DESCRIPTIONS[index]);
      detail.setDate(getDate());
      detail.setAmount(rand.nextInt(25000) / 100.0);
      detail.setCategory(CATEGORIES[index]);
      detail.setApproval("");
      detail.setReasonDenied("");
      detail.persist();
    }
  }

  /**
   * Add a randomly generated report.
   * 
   * @param employeeId the id of the employee who created the report
   */
  private void addReports(Long employeeId) {
    // Add 1-20 expense reports.
    int reportCount = 1 + rand.nextInt(20);
    for (int i = 0; i < reportCount; i++) {
      Report report = new Report();
      report.setCreated(getDate());
      report.setReporterKey(employeeId);
      report.setPurpose(nextValue(PURPOSES));
      report.setNotes(nextValue(NOTES));
      report.persist();

      addExpenses(report.getId());
    }
  }

  private Date getDate() {
    long now = new Date().getTime();
    // Go back up to 90 days from the current date
    long dateOffset = rand.nextInt(60 * 60 * 24 * 90) * 1000L;
    return new Date(now - dateOffset);
  }

  /**
   * Get the next random value from an array.
   * 
   * @param array the array
   * @return a random value from the array
   */
  private String nextValue(String[] array) {
    return array[rand.nextInt(array.length)];
  }
}
