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

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.sample.expenses.client.place.ReportListPlace;
import com.google.gwt.sample.expenses.client.place.ReportPlace;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.sample.gaerequest.client.ReloadOnAuthenticationFailure;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the Expenses app.
 */
public class ExpensesApp {
  /**
   * TODO: This belongs on the server, probably as an entity
   */
  public static final String[] DEPARTMENTS = {
      "Engineering", "Finance", "Marketing", "Operations", "Sales"};

  private static final Logger log = Logger.getLogger(ExpensesShell.class.getName());

  private final ActivityManager activityManager;
  private final EventBus eventBus;
  private final PlaceController placeController;
  private final PlaceHistoryHandler placeHistoryHandler;
  private final ExpensesShell shell;

  private EntityProxyId<EmployeeProxy> lastEmployee;
  private String lastDepartment = "";

  public ExpensesApp(ActivityManager activityManager, EventBus eventBus,
      PlaceController placeController, PlaceHistoryHandler placeHistoryHandler,
      ExpensesShell shell) {
    this.activityManager = activityManager;
    this.eventBus = eventBus;
    this.placeController = placeController;
    this.placeHistoryHandler = placeHistoryHandler;
    this.shell = shell;
  }

  /**
   * Start the app, and add its main widget to the given panel.
   */
  public void run(HasWidgets root) {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        log.log(Level.SEVERE, e.getMessage(), e);
      }
    });

    final ExpenseTree expenseTree = shell.getExpenseTree();
    final ExpenseReportList expenseList = shell.getExpenseList();
    final ExpenseReportDetails expenseDetails = shell.getExpenseDetails();

    // Handle breadcrumb events from Expense Details.
    expenseDetails.getReportsLink().addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        placeController.goTo(expenseDetails.getReportListPlace());
      }
    });

    // Check for Authentication failures or mismatches
    new ReloadOnAuthenticationFailure().register(eventBus);

    // Listen for requests from ExpenseTree.
    expenseTree.setListener(new ExpenseTree.Listener() {

      public void onSelection(String department, EntityProxyId<EmployeeProxy> employee) {
        lastEmployee = employee;
        lastDepartment = department;
        placeController.goTo(new ReportListPlace(employee, department));
      }
    });

    // Listen for requests from the ExpenseList.
    expenseList.setListener(new ExpenseReportList.Listener() {
      public void onReportSelected(ReportProxy report) {
        placeController.goTo(new ReportPlace( //
            new ReportListPlace(lastEmployee, lastDepartment), //
            report.stableId() //
        ));
      }
    });

    // Give the ActivityManager a panel to run
    activityManager.setDisplay(shell.getPanel());

    // Browser history integration
    placeHistoryHandler.register(placeController, eventBus, ReportListPlace.ALL);
    placeHistoryHandler.handleCurrentHistory();

    root.add(shell);
  }
}
