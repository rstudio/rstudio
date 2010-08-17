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
import com.google.gwt.app.place.ProxyListPlace;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ExpensesEntityTypesProcessor;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.ui.employee.EmployeeListActivity;
import com.google.gwt.sample.expenses.gwt.ui.report.ReportListActivity;

/**
 * Instantiates master activities.
 */
public final class ExpensesMasterActivities implements ActivityMapper {
  private final ExpensesRequestFactory requests;
  private final PlaceController placeController;

  public ExpensesMasterActivities(ExpensesRequestFactory requests,
      PlaceController placeController) {
    this.requests = requests;
    this.placeController = placeController;
  }

  public Activity getActivity(Place place) {
    if (!(place instanceof ProxyListPlace)) {
      return null;
    }

    ProxyListPlace listPlace = (ProxyListPlace) place;

    return new ExpensesEntityTypesProcessor<Activity>() {
      @Override
      public void handleEmployee(EmployeeRecord isNull) {
        setResult(new EmployeeListActivity(requests, placeController));
      }

      @Override
      public void handleReport(ReportRecord isNull) {
        setResult(new ReportListActivity(requests, placeController));
      }
    }.process(listPlace.getProxyClass());
  }
}
