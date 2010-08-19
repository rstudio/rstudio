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
package com.google.gwt.sample.expenses.gwt.ui.employee;

import com.google.gwt.app.place.AbstractRecordEditActivity;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.RecordEditView;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.valuestore.shared.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An activity that requests all info on an employee, allows the user to edit
 * it, and persists the results.
 */
public class EmployeeEditActivity extends
    AbstractRecordEditActivity<EmployeeRecord> {
  private static RecordEditView<EmployeeRecord> defaultView;

  private static RecordEditView<EmployeeRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new EmployeeEditView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public EmployeeEditActivity(EmployeeRecord proxy,
      ExpensesRequestFactory requests, PlaceController placeController,
      boolean creating) {
    this(getDefaultView(), proxy, requests, placeController, creating);
  }

  /**
   * Creates an activity that uses the given view instance.
   */
  public EmployeeEditActivity(RecordEditView<EmployeeRecord> view,
      EmployeeRecord proxy, ExpensesRequestFactory requests,
      PlaceController placeController, boolean creating) {
    super(view, proxy, EmployeeRecord.class, creating, requests,
        placeController);
    this.requests = requests;
  }

  @Override
  public void start(Display display, EventBus eventBus) {
    getEmployeeEditView().setEmployeePickerValues(
        Collections.<EmployeeRecord> emptyList());
    
    requests.employeeRequest().findEmployeeEntries(0, 50).with(
        EmployeeRenderer.instance().getPaths()).fire(
        new Receiver<List<EmployeeRecord>>() {
          public void onSuccess(List<EmployeeRecord> response,
              Set<SyncResult> syncResults) {
            List<EmployeeRecord> values = new ArrayList<EmployeeRecord>();
            values.add(null);
            values.addAll(response);
            getEmployeeEditView().setEmployeePickerValues(values);
          }

        });
    super.start(display, eventBus);
  }

  @Override
  protected void fireFindRequest(Value<Long> id,
      Receiver<EmployeeRecord> callback) {
    requests.employeeRequest().findEmployee(id).fire(callback);
  }

  protected RequestObject<Void> getPersistRequest(EmployeeRecord record) {
    return requests.employeeRequest().persist(record);
  }

  private EmployeeEditView getEmployeeEditView() {
    return ((EmployeeEditView) getView());
  }
}
