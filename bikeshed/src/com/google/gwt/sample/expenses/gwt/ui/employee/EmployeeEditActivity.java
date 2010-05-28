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

import com.google.gwt.app.place.PlaceController;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.client.place.EmployeeScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldRecordPlace.Operation;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.valuestore.shared.Value;
import com.google.gwt.valuestore.ui.AbstractRecordEditActivity;
import com.google.gwt.valuestore.ui.RecordEditView;

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
  private final PlaceController<ScaffoldPlace> placeController;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public EmployeeEditActivity(String id, ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this(id, getDefaultView(), requests, placeController);
  }

  /**
   * Creates an activity that uses its own view instance.
   */
  public EmployeeEditActivity(String id, RecordEditView<EmployeeRecord> view,
      ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    super(view, id, requests);
    this.requests = requests;
    this.placeController = placeController;
  }

  @Override
  protected void fireFindRequest(Value<String> id,
      Receiver<EmployeeRecord> callback) {
    requests.employeeRequest().findEmployee(id).to(callback).fire();
  }

  @Override
  protected void exit() {
    placeController.goTo(new EmployeeScaffoldPlace(getId(), Operation.DETAILS));
  }
}
