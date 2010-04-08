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

import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.ValuesListView;

/**
 * An implementation of {@link ValuesListView.Delegate} that requests all
 * {@link EmployeeKey} records.
 */
public final class AllEmployeesRequester implements ValuesListView.Delegate {
  private final ValuesListViewTable<EmployeeKey> view;
  private final ExpensesRequestFactory requests;

  public AllEmployeesRequester(ExpensesRequestFactory requests,
      ValuesListViewTable<EmployeeKey> newView) {
    this.view = newView;
    this.requests = requests;
  }

  public void onRangeChanged(int start, int length) {
    // TODO use start and length
    requests.employeeRequest().findAllEmployees().forProperties(
        view.getProperties()).to(view).fire();
  }
}