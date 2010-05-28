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
package com.google.gwt.sample.expenses.gwt.ui.report;

import com.google.gwt.app.place.AbstractActivity;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.valuestore.shared.Value;

import java.util.List;

/**
 * An {@link com.google.gwt.app.place.Activity Activity} that requests and
 * displays detailed information on a given report.
 */
public class ReportDetailsActivity extends AbstractActivity {
  class RequestCallBack implements TakesValueList<ReportRecord> {
    public void setValueList(List<ReportRecord> listOfOne) {
      ReportRecord record = listOfOne.get(0);

      StringBuilder list = new StringBuilder("<h3>Employee " + record.getId()
          + "</h3>");

      String purpose = record.getPurpose();
      list.append("<div>");
      list.append("<label>").append("Purpose: ").append("</label>");
      list.append("<span>").append(purpose).append("</span>");
      list.append("</div>");

      list.append("<div>");
      String created = record.getCreated().toString();
      list.append("<label>").append("Created: ").append("</label>");
      list.append("<span>").append(created).append("</span>");
      list.append("</div>");

      callback.onStarted(new HTML(list.toString()));
    }
  }

  private final ExpensesRequestFactory requests;

  private String id;

  private Callback callback;

  public ReportDetailsActivity(String id, ExpensesRequestFactory requests) {
    this.requests = requests;
    this.id = id;
  }

  public void start(Callback callback) {
    this.callback = callback;
    requests.reportRequest().findReport(Value.of(id)).to(new RequestCallBack()).fire();
  }
}
