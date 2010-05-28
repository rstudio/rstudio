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
import com.google.gwt.requestfactory.shared.EntityListRequest;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.scaffold.place.EmployeeScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ScaffoldRecordPlace.Operation;
import com.google.gwt.valuestore.ui.AbstractRecordListActivity;
import com.google.gwt.valuestore.ui.RecordListView;

/**
 * Activity that requests and displays all {@EmployeeRecord}
 * records.
 */
public class EmployeeListActivity extends
    AbstractRecordListActivity<EmployeeRecord> {

  private static RecordListView<EmployeeRecord> defaultView;

  private static RecordListView<EmployeeRecord> getDefaultView() {
    if (defaultView == null) {
      defaultView = new EmployeeListView();
    }
    return defaultView;
  }

  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;

  /**
   * Creates an activity that uses the default singleton view instance.
   */
  public EmployeeListActivity(ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this(requests, getDefaultView(), placeController);
  }

  /**
   * Creates an activity that uses its own view instance.
   */
  public EmployeeListActivity(ExpensesRequestFactory requests,
      RecordListView<EmployeeRecord> view,
      PlaceController<ScaffoldPlace> placeController) {
    super(view);
    this.requests = requests;
    this.placeController = placeController;
  }

  public void edit(EmployeeRecord record) {
    placeController.goTo(new EmployeeScaffoldPlace(record, Operation.EDIT));
  }

  public void showDetails(EmployeeRecord record) {
    placeController.goTo(new EmployeeScaffoldPlace(record, Operation.DETAILS));
  }

  protected EntityListRequest<EmployeeRecord> createRequest() {
    return requests.employeeRequest().findAllEmployees();
  }
}
