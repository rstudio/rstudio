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
package com.google.gwt.sample.expenses.gwt.ui;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.scaffold.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeListActivity;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportListActivity;

/**
 * The class that knows what {@link Activity} to run when the user goes to a
 * particular {@link ListScaffoldPlace}.
 * <p>
 * To create a new list activity, copy one of the existing ones (and the
 * corresponding view), and register it here.
 */
public class ListActivitiesMapper implements ActivityMapper<ListScaffoldPlace> {
  private final ExpensesRequestFactory requests;
  private final PlaceController<ScaffoldPlace> placeController;

  public ListActivitiesMapper(ExpensesRequestFactory requests,
      PlaceController<ScaffoldPlace> placeController) {
    this.requests = requests;
    this.placeController = placeController;
  }

  public Activity getActivity(ListScaffoldPlace place) {
    if (place.getType().equals(EmployeeRecord.class)) {
      return new EmployeeListActivity(requests, placeController);
    }
    if (place.getType().equals(ReportRecord.class)) {
      return new ReportListActivity(requests, placeController);
    }

    throw new RuntimeException("Unable to locate a activity for " + place);
  }
}
