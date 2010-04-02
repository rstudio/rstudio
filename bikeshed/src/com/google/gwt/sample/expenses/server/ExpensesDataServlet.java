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
import com.google.gwt.sample.expenses.server.domain.Report;
import com.google.gwt.sample.expenses.server.domain.Storage;
import com.google.gwt.sample.expenses.shared.ReportKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;

/**
 * Dwindling interim servlet that calls our mock storage backend directly
 * instead of reflectively. Should soon vanish completely. 
 */
public class ExpensesDataServlet extends RequestFactoryServlet {

  @Override
  protected void sync(String content, PrintWriter writer) {

    try {
      JSONArray reportArray = new JSONArray(content);
      int length = reportArray.length();
      if (length > 0) {
        JSONObject report = reportArray.getJSONObject(0);
        Report r = Report.findReport(report.getLong(ReportKey.get().getId().getName()));
        r.setPurpose(report.getString(ReportKey.get().getPurpose().getName()));
        r = Storage.INSTANCE.persist(r);
        report.put(ReportKey.get().getVersion().getName(), r.getVersion());
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
