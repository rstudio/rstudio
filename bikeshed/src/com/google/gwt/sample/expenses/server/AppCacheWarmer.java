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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Warm an App-Engine App.
 */
public class AppCacheWarmer {

  static class CountEmployees extends RequestObject {
    CountEmployees() {
      super("COUNT_EMPLOYEES", new String[0]);
    }
  }

  static class CountEmployeesByDepartment extends RequestObject {
    CountEmployeesByDepartment(String department) {
      super("COUNT_EMPLOYEES_BY_DEPARTMENT", new String[] {department});
    }
  }

  static class CountReports extends RequestObject {
    CountReports() {
      super("COUNT_REPORTS", new String[0]);
    }
  }

  static class CountReportsBySearch extends RequestObject {
    CountReportsBySearch(long reporterKey, String department, String startsWith) {
      super("COUNT_REPORTS_BY_SEARCH", new String[] {
          reporterKey + "", department, startsWith});
    }
  }

  static class FindAllEmployees extends RequestObject {
    FindAllEmployees() {
      super("FIND_ALL_EMPLOYEES", new String[0]);
    }
  }

  static class FindAllExpenses extends RequestObject {
    FindAllExpenses() {
      super("FIND_ALL_EXPENSES", new String[0]);
    }
  }

  static class FindAllReports extends RequestObject {
    FindAllReports() {
      super("FIND_ALL_REPORTS", new String[0]);
    }
  }

  static class FindEmployeeEntries extends RequestObject {
    FindEmployeeEntries(int firstResult, int maxResults) {
      super("FIND_EMPLOYEE_ENTRIES", new String[] {
          "" + firstResult, "" + maxResults});
    }
  }

  static class FindEmployeeEntriesByDepartment extends RequestObject {
    FindEmployeeEntriesByDepartment(String department, int firstResult,
        int maxResults) {
      super("FIND_EMPLOYEE_ENTRIES_BY_DEPARTMENT", new String[] {
          department, "" + firstResult, "" + maxResults});
    }
  }

  static class FindExpense extends RequestObject {
    FindExpense(String id) {
      super("FIND_EXPENSE", new String[] {id});
    }
  }

  static class FindExpensesByReport extends RequestObject {
    FindExpensesByReport(String reportId) {
      super("FIND_EXPENSES_BY_REPORT", new String[] {reportId});
    }
  }

  static class FindReport extends RequestObject {
    FindReport(String id) {
      super("FIND_REPORT", new String[] {id});
    }
  }

  static class FindReportEntries extends RequestObject {
    FindReportEntries(int firstResult, int maxResults) {
      super("FIND_REPORT_ENTRIES", new String[] {
          "" + firstResult, "" + maxResults});
    }
  }

  static class FindReportEntriesBySearch extends RequestObject {
    FindReportEntriesBySearch(Long employeeId, String department,
        String startsWith, String orderBy, int firstResult, int maxResults) {
      super("FIND_REPORT_ENTRIES_BY_SEARCH", new String[] {
          "" + employeeId, department, startsWith, orderBy, "" + firstResult,
          "" + maxResults});
    }
  }

  static class FindReportsByEmployee extends RequestObject {
    FindReportsByEmployee(Long id) {
      super("FIND_REPORTS_BY_EMPLOYEE", new String[] {"" + id});
    }
  }

  /*
   * Each subclass of the RequestObject represents a type of post request.
   */
  static class RequestObject {
    static int fetchedRequests = 0;
    final String operation;
    final String parameters[];

    RequestObject(String operation, String parameters[]) {
      this.operation = operation;
      this.parameters = parameters;
    }

    @Override
    public String toString() {
      String returnStr = "[" + operation;
      returnStr += "{";
      for (String parameter : parameters) {
        returnStr = returnStr + parameter + ",";
      }
      return returnStr + "}" + "]";
    }

    String getResponseString(String urlString) throws JSONException,
        IOException {
      fetchedRequests++;
      System.out.println("fetching " + this);
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");

      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject.put("operation", operation);
        int count = 0;
        for (String param : parameters) {
          jsonObject.put("param" + count, param);
          count++;
        }
      } catch (JSONException ex) {
        System.err.println("AppCacheWarmer ERROR: caught a JSON exception trying to construct a "
            + this + " request, quitting");
        throw ex;
      }
      OutputStreamWriter writer = new OutputStreamWriter(
          connection.getOutputStream());
      writer.write(jsonObject.toString());
      writer.close();

      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        // read the response
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            connection.getInputStream()));
        String responseString = "";
        String line = null;
        while ((line = reader.readLine()) != null) {
          responseString = responseString + line;
        }
        reader.close();
        try {
          Thread.currentThread().sleep(SLEEP_INTERVAL);
        } catch (InterruptedException e) {
          // ignore
        }

        return responseString;
      }
      throw new IOException("AppCacheWarmer ERROR: returned "
          + connection.getResponseCode() + " from " + this + " request");
    }
  }

  static final boolean GET_REPORTS_FOR_SORTED_LISTS = false; // CONFIGURABLEì

  static final long SLEEP_INTERVAL = 0L;

  static final String BASE_URL = "http://gwt-bikeshed.appspot.com";

  static final String POST_URL = BASE_URL + "/gwtRequest";

  public static void exhaustiveRun() throws IOException, JSONException {

    readStaticResources(BASE_URL);

    String departments[] = new String[] {
        "Engineering", "Finance", "Marketing", "Operations", "Sales"};
    List<RequestObject> queuedRequestObjects = new ArrayList<RequestObject>();

    // get "ALL"
    queuedRequestObjects.add(new CountReportsBySearch(-1, "", ""));
    for (String department : departments) {
      // click on the department
      for (int lastIndex : new int[] {25, 50, 75}) { // CONFIGURABLE
        RequestObject employeeEntries = new FindEmployeeEntriesByDepartment(
            department, 0, lastIndex);
        JSONArray employees = new JSONArray(
            employeeEntries.getResponseString(POST_URL));
        for (int i = 0; i < employees.length(); i++) {
          // click on the employee -- get Reports for each employee..
          JSONObject employee = (JSONObject) employees.get(i);
          RequestObject reportsForAnEmployee = new FindReportEntriesBySearch(
              Long.parseLong(employee.getString("id")), "", "", "created DESC",
              0, 20);
          JSONArray expenses = new JSONArray(
              reportsForAnEmployee.getResponseString(POST_URL));
          if (GET_REPORTS_FOR_SORTED_LISTS) {
            for (int j = 0; j < expenses.length(); j++) {
              // click on the report -- get expenses for report
              JSONObject expense = (JSONObject) expenses.get(j);
              RequestObject expensesForReport = new FindExpensesByReport(
                  expense.getString("id"));
              expensesForReport.getResponseString(POST_URL);
            }
          }
        }
      }
      queuedRequestObjects.add(new CountEmployeesByDepartment(department));
      queuedRequestObjects.add(new CountReportsBySearch(-1, department, ""));
    }

    String searchText = "";
    // count_reports_by_search
    for (String field : new String[] {
        "purpose", "notes", "department", "created"}) {
      for (String sortOrder : new String[] {"asc", "desc"}) {
        // first 2 pages
        for (int startIndex : new int[] {0, 20}) { // CONFIGURABLE
          RequestObject reportsBySearch = new FindReportEntriesBySearch(-1L,
              "", searchText, field + " " + sortOrder.toUpperCase(),
              startIndex, 20);
          JSONArray expenses = new JSONArray(
              reportsBySearch.getResponseString(POST_URL));
          if (GET_REPORTS_FOR_SORTED_LISTS) {
            for (int j = 0; j < expenses.length(); j++) {
              // click on the report -- get expenses for report
              JSONObject expense = (JSONObject) expenses.get(j);
              RequestObject expensesForReport = new FindExpensesByReport(
                  expense.getString("id"));
              expensesForReport.getResponseString(POST_URL);
            }
          }
        }
      }
    }

    for (RequestObject requestObject : queuedRequestObjects) {
      requestObject.getResponseString(POST_URL);
    }
  }

  public static void main(String args[]) {
    long startTime = System.currentTimeMillis();

    try {
      testRun();
      // exhaustiveRun();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println("Exception message: " + ex.getMessage());
    }
    System.out.println("AppCacheWarmer: ran at " + new Date() + " for "
        + (System.currentTimeMillis() - startTime) + "ms, #Requests fetched: "
        + RequestObject.fetchedRequests);
  }

  public static void testRun() throws IOException, JSONException {
    // get "ALL"
    RequestObject allReportsCount = new CountReportsBySearch(-1, "", "");
    allReportsCount.getResponseString(POST_URL);
    RequestObject allReports = new FindReportEntriesBySearch(-1L, "", "",
        "created DESC", 0, 20);
    allReports.getResponseString(POST_URL);

    String engineerList = null;
    // Expand the Engineering and Finance departments
    for (String department : new String[] {"Engineering", "Finance"}) {
      RequestObject countEmployeeEntries = new CountEmployeesByDepartment(
          department);
      countEmployeeEntries.getResponseString(POST_URL);
      for (int lastIndex : new int[] {25}) {
        RequestObject employeeEntries = new FindEmployeeEntriesByDepartment(
            department, 0, lastIndex);
        String tempList = employeeEntries.getResponseString(POST_URL);
        if (department.equals("Engineering")) {
          engineerList = tempList;
        }
      }
    }

    assert engineerList != null;
    JSONArray engineers = new JSONArray(engineerList);
    JSONObject firstEngineer = (JSONObject) engineers.get(0);
    RequestObject countReportsForAnEmployee = new CountReportsBySearch(
        Long.parseLong(firstEngineer.getString("id")), "", "");
    countReportsForAnEmployee.getResponseString(POST_URL);

    RequestObject reportsForAnEmployee = new FindReportEntriesBySearch(
        Long.parseLong(firstEngineer.getString("id")), "", "", "created DESC",
        0, 20);
    JSONArray expenses = new JSONArray(
        reportsForAnEmployee.getResponseString(POST_URL));
    // open first 5 reports
    int range = Math.min(5, expenses.length());
    for (int j = 0; j < range; j++) {
      // click on the report -- get expenses for report
      JSONObject expense = (JSONObject) expenses.get(j);
      RequestObject expensesForReport = new FindExpensesByReport(
          expense.getString("id"));
      expensesForReport.getResponseString(POST_URL);
    }

    // get count for "ALL", get "ALL", page next, page next
    allReportsCount = new CountReportsBySearch(-1, "", "");
    allReportsCount.getResponseString(POST_URL);
    for (int startIndex : new int[] {0, 20, 40}) {
      allReports = new FindReportEntriesBySearch(-1L, "", "",
          "created DESC", startIndex, 20);
      allReports.getResponseString(POST_URL);
    }

    // select "ALL" i.e. "created DESC", sort by "purpose DESC", sort by
    // "created ASC", sort by "created DESC"
    allReportsCount = new CountReportsBySearch(-1, "", "");
    allReportsCount.getResponseString(POST_URL);
    for (String sortOrder : new String[] {
        "created DESC", "purpose DESC", "created ASC", "created DESC"}) {
      RequestObject sortOrderReports = new FindReportEntriesBySearch(-1L, "", "", sortOrder, 0, 20);
      sortOrderReports.getResponseString(POST_URL);
    }
    
    // search for "..."
    String searchString = "Seattle dinner";
    RequestObject searchReportsCount = new CountReportsBySearch(-1, "", searchString);
    searchReportsCount.getResponseString(POST_URL);
    RequestObject searchReports = new FindReportEntriesBySearch(-1L, "", searchString, "created DESC", 0, 20);
    searchReports.getResponseString(POST_URL);
  }

  private static void readStaticResources(String baseUrl) throws IOException {
    URL url = new URL((baseUrl + "/Expenses.html"));
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        url.openStream()));
    String line = null;
    while ((line = reader.readLine()) != null) {
      // ...
    }
    reader.close();
  }
}
