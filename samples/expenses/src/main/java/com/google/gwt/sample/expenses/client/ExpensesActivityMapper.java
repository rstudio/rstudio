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

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.sample.expenses.client.place.ReportListPlace;
import com.google.gwt.sample.expenses.client.place.ReportPlace;

/**
 * ActivityMapper for the Expenses app.
 */
public class ExpensesActivityMapper implements ActivityMapper {

  private final ExpenseReportDetails expenseDetails;
  private final ExpenseReportList expenseList;

  public ExpensesActivityMapper(ExpenseReportDetails expenseDetails,
      ExpenseReportList expenseList) {
    this.expenseDetails = expenseDetails;
    this.expenseList = expenseList;
  }

  public Activity getActivity(Place place) {
    if (place instanceof ReportListPlace) {
      expenseList.updateForPlace((ReportListPlace) place);
      return expenseList;
    }

    if (place instanceof ReportPlace) {
      expenseDetails.updateForPlace((ReportPlace) place);
      return expenseDetails;
    }

    return null;
  }
}
