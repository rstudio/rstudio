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
package com.google.gwt.sample.expenses.gwt.client;

import com.google.gwt.app.place.Activity;
import com.google.gwt.app.place.ActivityMapper;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.sample.expenses.gwt.client.place.BaseScaffoldPlaceFilter;
import com.google.gwt.sample.expenses.gwt.client.place.EmployeeScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ReportScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeActivitiesMapper;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportActivitiesMapper;

/**
 * Finds the activity to run for a particular {@link ScaffoldPlace} in the bottom
 * half of the {@link ScaffoldShell}.
 */
public final class ScaffoldDetailsActivities implements
    ActivityMapper<ScaffoldPlace> {

  private final ActivityMapper<EmployeeScaffoldPlace> employeeActivities;
  private final ActivityMapper<ReportScaffoldPlace> reportActivities;
  
  public ScaffoldDetailsActivities(ExpensesRequestFactory requestFactory,
      PlaceController<ScaffoldPlace> placeController) {
    this.employeeActivities = new EmployeeActivitiesMapper(
        requestFactory, placeController);
    this.reportActivities = new ReportActivitiesMapper(requestFactory,
        placeController);
  }

  public Activity getActivity(ScaffoldPlace place) {
    return place.acceptFilter(new BaseScaffoldPlaceFilter<Activity>(null) {
      @Override
      public Activity filter(EmployeeScaffoldPlace place) {
        return employeeActivities.getActivity(place);
      }

      @Override
      public Activity filter(ReportScaffoldPlace place) {
        return reportActivities.getActivity(place);
      }
    });
  }
}
