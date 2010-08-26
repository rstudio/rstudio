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

import com.google.gwt.app.place.AbstractRecordEditActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.RecordEditView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RecordRequest;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.Value;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An activity that requests all info on a report, allows the user to edit it,
 * and persists the results.
 */
public class ReportEditActivity extends
    AbstractRecordEditActivity<ReportRecord> {
  private static RecordEditView<ReportRecord> defaultView;

  private static RecordEditView<ReportRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new ReportEditView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public ReportEditActivity(ReportRecord proxy,
      ExpensesRequestFactory requests, PlaceController placeController,
      boolean creating) {
    this(proxy, getDefaultView(), requests, placeController, creating);
  }

  /**
   * Creates an activity that uses the given view instance.
   */
  public ReportEditActivity(ReportRecord proxy,
      RecordEditView<ReportRecord> view, ExpensesRequestFactory requests,
      PlaceController placeController, boolean creating) {
    super(view, proxy, ReportRecord.class, creating, requests, placeController);
    this.requests = requests;
  }
  @Override
  public void start(Display display, EventBus eventBus) {
    getReportEditView().setEmployeePickerValues(
        Collections.<EmployeeRecord> emptyList());
    
    requests.employeeRequest().findEmployeeEntries(0, 50).with(
        EmployeeRenderer.instance().getPaths()).fire(
        new Receiver<List<EmployeeRecord>>() {
          public void onSuccess(List<EmployeeRecord> response,
              Set<SyncResult> syncResults) {
            List<EmployeeRecord> values = new ArrayList<EmployeeRecord>();
            values.add(null);
            values.addAll(response);
            getReportEditView().setEmployeePickerValues(values);
          }

        });
    super.start(display, eventBus);
  }
  
  @Override
  protected RecordRequest<ReportRecord> getFindRequest(Value<Long> id) {
    return requests.reportRequest().findReport(id);
  }

  protected RequestObject<Void> getPersistRequest(ReportRecord record) {
    return requests.reportRequest().persist(record);
  }

  private ReportEditView getReportEditView() {
    return ((ReportEditView) getView());
  }
}
