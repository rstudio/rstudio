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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.app.place.Place;
import com.google.gwt.app.place.PlaceChanged;
import com.google.gwt.sample.expenses.client.place.EntityListPlace;
import com.google.gwt.sample.expenses.client.place.Places;
import com.google.gwt.sample.expenses.shared.EmployeeRef;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportRef;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.valuestore.shared.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * In charge of requesting and displaying the appropriate entity lists
 * when the user goes to an {@link EntityListPlace}.
 */
public final class ListRequester implements PlaceChanged.Handler {

  private final SimplePanel panel;
  private final TableEntityListView entitiesView;
  private final List<Property<ReportRef, ?>> reportColumns;
  private final ExpenseRequestFactory requests;
  private final List<Property<EmployeeRef, ?>> employeeColumns;

  /**
   * @param shell
   * @param entitiesView
   * @param requests
   */
  public ListRequester(SimplePanel panel, TableEntityListView entitiesView,
      ExpenseRequestFactory requests) {
    this.panel = panel;
    this.entitiesView = entitiesView;
    this.requests = requests;

    employeeColumns = new ArrayList<Property<EmployeeRef, ?>>();
    employeeColumns.add(EmployeeRef.USER_NAME);
    employeeColumns.add(EmployeeRef.DISPLAY_NAME);

    reportColumns = new ArrayList<Property<ReportRef, ?>>();
    reportColumns.add(ReportRef.CREATED);
    reportColumns.add(ReportRef.PURPOSE);
  }

  public void onPlaceChanged(PlaceChanged event) {
    // TODO all this "instanceof" and "if else" stuff is not so great

    Place newPlace = event.getNewPlace();
    if (!(newPlace instanceof EntityListPlace)) {
      return;
    }

    if (entitiesView.getParent() == null) {
      panel.clear();
      panel.add(entitiesView);
    }

    if (newPlace == Places.EMPLOYEE_LIST) {
      EntityList<EmployeeRef> list = new EntityList<EmployeeRef>("Employees",
          entitiesView, employeeColumns);
      requests.employeeRequest().findAllEmployees().forProperties(
          employeeColumns).to(list).fire();
    } else if (newPlace == Places.REPORT_LIST) {
      EntityList<ReportRef> list = new EntityList<ReportRef>("Reports",
          entitiesView, reportColumns);
      requests.reportRequest().findAllReports().forProperties(reportColumns).to(
          list).fire();
    }
  }
}