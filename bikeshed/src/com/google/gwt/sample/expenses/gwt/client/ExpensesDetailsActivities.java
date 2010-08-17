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
import com.google.gwt.app.place.Place;
import com.google.gwt.app.place.PlaceController;
import com.google.gwt.app.place.ProxyPlace;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesEntityTypesProcessor;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeActivitiesMapper;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportActivitiesMapper;

/**
 * Instantiates detail activities.
 */
public class ExpensesDetailsActivities implements ActivityMapper {
  private final ExpensesRequestFactory requests;
  private final PlaceController placeController;

  public ExpensesDetailsActivities(ExpensesRequestFactory requestFactory,
      PlaceController placeController) {
    this.requests = requestFactory;
    this.placeController = placeController;
  }

  public Activity getActivity(Place place) {
    if (!(place instanceof ProxyPlace)) {
      return null;
    }

    final ProxyPlace proxyPlace = (ProxyPlace) place;

    return new ExpensesEntityTypesProcessor<Activity>() {
      @Override
      public void handleEmployee(EmployeeRecord proxy) {
        setResult(new EmployeeActivitiesMapper(requests, placeController).getActivity(proxyPlace));
      }

      @Override
      public void handleReport(ReportRecord proxy) {
        setResult(new ReportActivitiesMapper(requests, placeController).getActivity(proxyPlace));
      }
    }.process(proxyPlace.getProxy());
  }
}
