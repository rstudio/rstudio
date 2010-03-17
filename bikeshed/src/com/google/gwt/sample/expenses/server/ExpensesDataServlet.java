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

import com.google.gwt.sample.expenses.gen.MethodName;
import com.google.gwt.sample.expenses.server.domain.Employee;
import com.google.gwt.sample.expenses.server.domain.Report;
import com.google.gwt.sample.expenses.server.domain.Storage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ExpensesDataServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);

    MethodName methodName = getMethodName(request.getParameter("methodName"));
    PrintWriter writer = response.getWriter();
    switch (methodName) {
      case FIND_ALL_EMPLOYEES:
        findAllEmployees(writer);
        break;
      case FIND_ALL_REPORTS:
        findAllReports(writer);
        break;
      case FIND_EMPLOYEE:
        // TODO
        break;
      case FIND_REPORTS_BY_EMPLOYEE:
        findReportsByEmployee(request, writer);
        break;
      default:
        System.err.println("Unknown method " + methodName);
        break;
    }
    writer.flush();
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    MethodName methodName = getMethodName(request.getParameter("methodName"));
    PrintWriter writer = response.getWriter();
    switch (methodName) {
      case SYNC:
        sync(request, writer);
        break;
      default:
        System.err.println("POST: unknown method " + methodName);
        break;
    }
    writer.flush();
  }

  private void findAllEmployees(PrintWriter writer) {
    JSONArray jsonArray = new JSONArray();
    for (Employee e : Employee.findAllEmployees()) {
      try {
        // TODO should only be getting requested properties
        // TODO clearly there should be centralized code for these conversions
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(
            com.google.gwt.sample.expenses.shared.EmployeeRef.ID.getName(),
            Long.toString(e.getId()));
        jsonObject.put(
            com.google.gwt.sample.expenses.shared.EmployeeRef.VERSION.getName(),
            e.getVersion().intValue());
        jsonObject.put(
            com.google.gwt.sample.expenses.shared.EmployeeRef.USER_NAME.getName(),
            e.getUserName());
        jsonObject.put(
            com.google.gwt.sample.expenses.shared.EmployeeRef.DISPLAY_NAME.getName(),
            e.getDisplayName());
        jsonArray.put(jsonObject);
      } catch (JSONException ex) {
        System.err.println("Unable to create a JSON object " + ex);
      }
    }
    writer.print(jsonArray.toString());
  }

  private void findAllReports(PrintWriter writer) {
    JSONArray jsonArray = new JSONArray();
    for (Report r : Report.findAllReports()) {
      reportToJson(jsonArray, r);
    }
    writer.print(jsonArray.toString());
  }

  private void findReportsByEmployee(HttpServletRequest request,
      PrintWriter writer) {
    JSONArray jsonArray = new JSONArray();
    Long id = Long.valueOf(request.getParameter("id"));
    for (Report r : Report.findReportsByEmployee(id)) {
      reportToJson(jsonArray, r);
    }
    writer.print(jsonArray.toString());
  }

  /**
   * @param request
   * @return
   */
  private MethodName getMethodName(String methodString) {
    for (MethodName method : MethodName.values()) {
      if (method.name().equals(methodString)) {
        return method;
      }
    }
    throw new IllegalArgumentException("unknown methodName: " + methodString);
  }

  /**
   * @param jsonArray
   * @param r
   */
  private void reportToJson(JSONArray jsonArray, Report r) {
    try {
      // TODO should only be getting requested properties
      // TODO clearly there should be centralized code for these conversions
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(
          com.google.gwt.sample.expenses.shared.EmployeeRef.ID.getName(),
          Long.toString(r.getId()));
      jsonObject.put(
          com.google.gwt.sample.expenses.shared.ReportRef.VERSION.getName(),
          r.getVersion().intValue());
      jsonObject.put(
          com.google.gwt.sample.expenses.shared.ReportRef.CREATED.getName(),
          Double.valueOf(r.getCreated().getTime()));
      jsonObject.put(
          com.google.gwt.sample.expenses.shared.ReportRef.PURPOSE.getName(),
          r.getPurpose());
      jsonArray.put(jsonObject);
    } catch (JSONException ex) {
      System.err.println("Unable to create a JSON object " + ex);
    }
  }

  /**
   * @param request
   * @param writer
   * @throws IOException
   */
  private void sync(HttpServletRequest request, PrintWriter writer)
      throws IOException {
    int contentLength = request.getContentLength();
    byte contentBytes[] = new byte[contentLength];
    BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
    int readBytes = 0;
    while (bis.read(contentBytes, readBytes, contentLength - readBytes) > 0) {
      // read the contents
    }
    // TODO: encoding issues?
    String content = new String(contentBytes);
    try {
      JSONArray reportArray = new JSONArray(content);
      int length = reportArray.length();
      if (length > 0) {
        JSONObject report = reportArray.getJSONObject(0);
        Report r = Report.findReport(report.getLong(com.google.gwt.sample.expenses.shared.ReportRef.ID.getName()));
        r.setPurpose(report.getString(com.google.gwt.sample.expenses.shared.ReportRef.PURPOSE.getName()));
        r = Storage.INSTANCE.persist(r);
        report.put(
            com.google.gwt.sample.expenses.shared.ReportRef.VERSION.getName(),
            r.getVersion());
        JSONArray returnArray = new JSONArray();
        // TODO: don't echo back everything.
        returnArray.put(report);
        writer.print(returnArray.toString());
      }
    } catch (JSONException e) {
      e.printStackTrace();
      // TODO: return an error.
    }
    return;
  }
}
