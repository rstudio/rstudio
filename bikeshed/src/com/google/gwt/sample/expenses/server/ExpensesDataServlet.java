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
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.server.domain.Report;
import com.google.gwt.sample.expenses.server.domain.Employee;
import com.google.gwt.valuestore.shared.Record;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.util.Date;

/**
 * Dwindling interim servlet that calls our mock storage backend directly
 * instead of reflectively. Should soon vanish completely.
 */
@SuppressWarnings("serial")
public class ExpensesDataServlet extends RequestFactoryServlet {

  @Override
  protected void initDb() {
    long size = Employee.countEmployees();
    if (size > 1) {
      return;
    }
    // initialize
    Employee abc = new Employee();
    abc.setUserName("abc");
    abc.setDisplayName("Able B. Charlie");
    abc.persist();

    Employee def = new Employee();
    def.setUserName("def");
    def.setDisplayName("Delta E. Foxtrot");
    def.setSupervisorKey(abc.getId());
    def.persist();

    Employee ghi = new Employee();
    ghi.setUserName("ghi");
    ghi.setDisplayName("George H. Indigo");
    ghi.setSupervisorKey(abc.getId());
    ghi.persist();

    for (String purpose : new String[] {
        "Spending lots of money", "Team building diamond cutting offsite",
        "Visit to Istanbul"}) {
      Report report = new Report();
      report.setReporterKey(abc.getId());
      report.setCreated(new Date());
      report.setPurpose(purpose);
      report.persist();
    }

    for (String purpose : new String[] {"Money laundering", "Donut day"}) {
      Report report = new Report();
      report.setCreated(new Date());
      report.setReporterKey(def.getId());
      report.setPurpose(purpose);
      report.persist();
    }

    for (String purpose : new String[] {
        "ISDN modem for telecommuting", "Sushi offsite",
        "Baseball card research", "Potato chip cooking offsite"}) {
      Report report = new Report();
      report.setCreated(new Date());
      report.setReporterKey(ghi.getId());
      report.setPurpose(purpose);
      report.persist();
    }
  }

  @Override
  protected void sync(String content, PrintWriter writer) {

    try {
      JSONArray reportArray = new JSONArray(content);
      int length = reportArray.length();
      if (length > 0) {
        JSONObject report = reportArray.getJSONObject(0);
        Report r = Report.findReport(report.getLong(Record.id.getName()));
        r.setPurpose(report.getString(ReportRecord.purpose.getName()));
        r.persist();
        report.put(Record.version.getName(), r.getVersion());
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
