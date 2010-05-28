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
import com.google.gwt.sample.expenses.gwt.client.place.EmployeeScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ListScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ReportScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlace;
import com.google.gwt.sample.expenses.gwt.client.place.ScaffoldPlaceFilter;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeActivitiesMapper;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportActivitiesMapper;

/**
 * Finds the activity to run for a particular {@link ScaffoldPlace}.
 */
public final class ScaffoldMobileActivities implements
    ActivityMapper<ScaffoldPlace> {

  private final ActivityMapper<ListScaffoldPlace> listActivitiesBuilder;
  private final ActivityMapper<EmployeeScaffoldPlace> employeeActivitiesBuilder;
  private final ActivityMapper<ReportScaffoldPlace> reportActivitiesBuilder;

  /**
   * @param requestFactory
   * @param placeController
   */
  public ScaffoldMobileActivities(
      ActivityMapper<ListScaffoldPlace> listActivitiesBuilder,
      ExpensesRequestFactory requestFactory,
      PlaceController<ScaffoldPlace> placeController) {
    this.listActivitiesBuilder = listActivitiesBuilder;
    this.employeeActivitiesBuilder = new EmployeeActivitiesMapper(
        requestFactory, placeController);
    this.reportActivitiesBuilder = new ReportActivitiesMapper(requestFactory,
        placeController);
  }

  public Activity getActivity(ScaffoldPlace place) {
    return place.acceptFilter(new ScaffoldPlaceFilter<Activity>() {
      public Activity filter(EmployeeScaffoldPlace place) {
        return employeeActivitiesBuilder.getActivity(place);
      }

      public Activity filter(ListScaffoldPlace place) {
        return listActivitiesBuilder.getActivity(place);
      }

      public Activity filter(ReportScaffoldPlace place) {
        return reportActivitiesBuilder.getActivity(place);
      }
    });
  }
}
