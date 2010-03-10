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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.sample.expenses.shared.Employee;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.sample.expenses.shared.Report;
import com.google.gwt.user.client.ui.RootLayoutPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Expenses implements EntryPoint {
  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  private final ExpenseRequestFactory requestFactory = GWT.create(ExpenseRequestFactory.class);

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    RootLayoutPanel root = RootLayoutPanel.get();

    final Shell shell = new Shell();
    final EmployeeList employees = new EmployeeList(shell.users);

    root.add(shell);

    employees.setListener(new EmployeeList.Listener() {
      public void onEmployeeSelected(Employee e) {
        requestFactory.reportRequest().//
        findReportsByEmployee(e.slot(Employee.ID)).//
        forProperty(Report.CREATED).//
        forProperty(Report.PURPOSE).//
        to(shell).//
        fire();
      }
    });

    requestFactory.employeeRequest().findAllEmployees().forProperty(
        Employee.DISPLAY_NAME).forProperty(Employee.USER_NAME).to(employees).fire();
  }
}
