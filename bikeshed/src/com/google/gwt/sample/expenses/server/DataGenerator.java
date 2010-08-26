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

import com.google.gwt.dev.util.Util;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.sample.expenses.server.domain.Employee;
import com.google.gwt.sample.expenses.server.domain.Expense;
import com.google.gwt.sample.expenses.server.domain.Report;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class DataGenerator {

  // add support later. currently, it can be only zero.
  private static final long ID_START = 0;

  // Must be in sync with DESCRIPTIONS
  private static final String[] CATEGORIES = {
      "Dining", "Dining", "Dining", "Lodging", "Lodging",
      "Local Transportation", "Local Transportation", "Local Transportation",
      "Air Travel", "Air Travel", "Office Supplies", "Office Supplies",
      "Office Supplies", "Office Supplies",};

  private static final String[] DEPARTMENTS = {
      "Sales", "Marketing", "Engineering", "Operations", "Finance"};

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

  private static final String[] PURPOSES1 = {
      "Visting", "Travelling to", "Having fun in", "Paid vacation in",
      "Work related visit to"};

  private static final String[] PURPOSES2 = {
      "Istanbul", "Delhi", "Honolulu", "San Francisco", "Atlanta", "Miami",
      "Los Angeles", "Sydney", "Dortmund", "Dusseldorf", "Melbourne",
      "New York", "Chicago", "Mountain View", "Zurich", "Frankfurt",
      "Bangalore"};

  public static void main(String args[]) throws JSONException {
    if (args.length < 2) {
      throw new IllegalArgumentException(
          "Requires two arguments: #Employees and json-fileName");
    }
    int numEmployees = Integer.parseInt(args[0]);
    DataGenerator generator = new DataGenerator();
    List<Object> recordList = generator.generateData(numEmployees);
    System.out.println("Generated " + recordList.size()
        + " records, #employees: " + numEmployees);
    String jsonString = generator.getJsonString(recordList);
    File output = new File(args[1]);
    Util.writeStringAsFile(output, jsonString);
    System.out.println("Wrote out the data as json into file "
        + output.getAbsolutePath());
  }

  Random rand = new Random();
  List<Object> objectList = new ArrayList<Object>();

  // TODO: hacky, assuming that id is assigned sequentially.
  long id = ID_START + 1;
  int employeeCount = 0;
  List<Long> supervisors = new ArrayList<Long>();

  List<Object> generateData(int numEmployees) {
    // Initialize the database.
    for (int i = 0; i < numEmployees; i++) {
      addEmployee();
    }
    return objectList;
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
    String department = DEPARTMENTS[employeeCount % DEPARTMENTS.length];
    abc.setDepartment(department);
    long employeeId = id++;
    abc.setId(employeeId);
    objectList.add(abc);
    employeeCount++;
    Long supervisorKey = nextValue(supervisors);
    if (supervisorKey != null) {
      abc.setSupervisorKey(supervisorKey);
    }

    if (supervisors.isEmpty() || rand.nextInt(3) == 0) {
      supervisors.add(employeeId);
    }

    addReports(abc.getId(), department, supervisorKey);
  }

  private void addExpenses(Long reportId) {
    int num = rand.nextInt(3) + 1;
    for (int i = 0; i < num; i++) {
      int index = rand.nextInt(DESCRIPTIONS.length);
      Expense detail = new Expense();
      detail.setReportId(reportId);
      detail.setDescription(DESCRIPTIONS[index]);
      detail.setCreated(getDate());
      detail.setAmount(rand.nextInt(25000) / 100.0);
      detail.setCategory(CATEGORIES[index]);
      detail.setApproval("");
      detail.setReasonDenied("");
      detail.setId(id++);
      objectList.add(detail);
    }
  }

  /**
   * Add a randomly generated report.
   * 
   * @param employeeId the id of the employee who created the report
   */
  private void addReports(Long employeeId, String department, Long supervisorKey) {
    // Add 1-20 expense reports.
    int reportCount = 1 + rand.nextInt(2);
    for (int i = 0; i < reportCount; i++) {
      Report report = new Report();
      report.setCreated(getDate());
      report.setDepartment(department);
      report.setReporterKey(employeeId);
      report.setPurpose(nextValue(PURPOSES1) + " " + nextValue(PURPOSES2));
      report.setNotes(nextValue(NOTES));
      if (supervisorKey != null) {
        report.setApprovedSupervisorKey(supervisorKey);
      }
      report.setId(id++);
      objectList.add(report);

      addExpenses(report.getId());
    }
  }

  private Date getDate() {
    long now = new Date().getTime();
    // Go back up to 90 days from the current date
    long dateOffset = rand.nextInt(60 * 60 * 24 * 90) * 1000L;
    return new Date(now - dateOffset);
  }

  private JSONObject getEmployeeAsJson(Employee employee) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", "" + employee.getId());
    jsonObject.put("userName", employee.getUserName());
    jsonObject.put("displayName", employee.getDisplayName());
    jsonObject.put("department", employee.getDepartment());
    Long supervisorKey = employee.getSupervisorKey();
    if (supervisorKey != null) {
      jsonObject.put("supervisorKey", "" + employee.getSupervisorKey());
    }
    return jsonObject;
  }

  private JSONObject getExpenseAsJson(Expense expense) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", "" + expense.getId());
    jsonObject.put("report", expense.getReportId());
    jsonObject.put("description", expense.getDescription());
    jsonObject.put("created", expense.getCreated().getTime());
    jsonObject.put("amount", expense.getAmount());
    jsonObject.put("category", expense.getCategory());
    return jsonObject;
  }

  // use reflection later.
  private String getJsonString(List<Object> objectList) throws JSONException {
    JSONObject outer = new JSONObject();
    JSONArray createList = new JSONArray();
    for (Object object : objectList) {
      JSONObject record = new JSONObject();
      if (object instanceof Employee) {
        record.put("EmployeeRecord", getEmployeeAsJson((Employee) object));
      } else if (object instanceof Report) {
        record.put("ReportRecord", getReportAsJson((Report) object));
      } else if (object instanceof Expense) {
        record.put("ExpenseRecord", getExpenseAsJson((Expense) object));
      }
      id++;
      createList.put(record);
    }
    outer.put(WriteOperation.CREATE.name(), createList);
    return outer.toString(4);
  }

  private JSONObject getReportAsJson(Report report) throws JSONException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", "" + report.getId());
    jsonObject.put("created", report.getCreated().getTime());
    jsonObject.put("department", report.getDepartment());
    jsonObject.put("reporter", report.getReporterKey());
    jsonObject.put("purpose", report.getPurpose());
    jsonObject.put("notes", report.getNotes());
    Long approvedSupervisorKey = report.getApprovedSupervisorKey();
    if (approvedSupervisorKey != null) {
      jsonObject.put("approvedSupervisor", "" + report.getApprovedSupervisorKey());
    }
    return jsonObject;
  }

  private Long nextValue(List<Long> supervisors) {
    if (supervisors.isEmpty()) {
      return null;
    }
    return supervisors.get(rand.nextInt(supervisors.size()));
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
