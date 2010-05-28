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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecordChanged;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecordChanged;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.view.client.ProvidesKey;

/**
 * Entry point for the Expenses app.
 */
public class Expenses implements EntryPoint {

  /**
   * The key provider for {@link EmployeeRecord}s.
   */
  public static final ProvidesKey<EmployeeRecord> EMPLOYEE_RECORD_KEY_PROVIDER = new ProvidesKey<EmployeeRecord>() {
    public Object getKey(EmployeeRecord item) {
      return item == null ? null : item.getId();
    }
  };

  private String lastDepartment;
  private EmployeeRecord lastEmployee;
  private ExpensesRequestFactory requestFactory;
  private ExpensesShell shell;

  public void onModuleLoad() {
    final HandlerManager eventBus = new HandlerManager(null);
    requestFactory = GWT.create(ExpensesRequestFactory.class);
    requestFactory.init(eventBus);

    RootLayoutPanel root = RootLayoutPanel.get();

    shell = new ExpensesShell();
    final ExpenseTree expenseTree = shell.getExpenseTree();
    final ExpenseList expenseList = shell.getExpenseList();
    final ExpenseDetails expenseDetails = shell.getExpenseDetails();

    root.add(shell);

    // Listen for requests from ExpenseTree.
    expenseTree.setListener(new ExpenseTree.Listener() {
      public void onSelection(String department, EmployeeRecord employee) {
        lastDepartment = department;
        lastEmployee = employee;
        expenseList.setEmployee(department, employee);
        shell.showExpenseDetails(false);
      }
    });
    expenseTree.setRequestFactory(requestFactory);

    // Listen for requests from the ExpenseList.
    expenseList.setListener(new ExpenseList.Listener() {
      public void onReportSelected(ReportRecord report) {
        expenseDetails.setExpensesRequestFactory(requestFactory);
        expenseDetails.setReportRecord(report, lastDepartment, lastEmployee);
        shell.showExpenseDetails(true);
      }
    });
    expenseList.setRequestFactory(requestFactory);
    eventBus.addHandler(ReportRecordChanged.TYPE, expenseList);

    eventBus.addHandler(ExpenseRecordChanged.TYPE, expenseDetails);
  }
}
