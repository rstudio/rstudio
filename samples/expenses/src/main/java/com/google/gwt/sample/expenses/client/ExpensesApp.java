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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.requestfactory.ui.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.ui.client.LoginWidget;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.HasWidgets;

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

  private final EventBus eventBus;
  private final PlaceController placeController;
  private final PlaceHistoryHandler placeHistoryHandler;
  private final ExpensesRequestFactory requestFactory;
  private final ExpensesShell shell;

  private String lastDepartment;
  private EmployeeProxy lastEmployee;

  public ExpensesApp(ExpensesRequestFactory requestFactory, EventBus eventBus,
      ExpensesShell shell, PlaceHistoryHandler placeHistoryHandler,
      PlaceController placeController) {
    this.requestFactory = requestFactory;
    this.eventBus = eventBus;
    this.shell = shell;
    this.placeHistoryHandler = placeHistoryHandler;
    this.placeController = placeController;
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
    final ExpenseList expenseList = shell.getExpenseList();
    final ExpenseDetails expenseDetails = shell.getExpenseDetails();

    root.add(shell);

    // Check for Authentication failures or mismatches
    RequestEvent.register(eventBus, new AuthenticationFailureHandler());

    // Kick off the login widget
    final LoginWidget login = shell.getLoginWidget();
    Receiver<UserInformationProxy> receiver = new Receiver<UserInformationProxy>() {
      @Override
      public void onSuccess(UserInformationProxy userInformationRecord) {
        login.setUserInformation(userInformationRecord);
      }
    };
    requestFactory.userInformationRequest().getCurrentUserInformation(
        Location.getHref()).fire(receiver);

    // Listen for requests from ExpenseTree.
    expenseTree.setListener(new ExpenseTree.Listener() {
      public void onSelection(String department, EmployeeProxy employee) {
        lastDepartment = department;
        lastEmployee = employee;
        expenseList.setEmployee(department, employee);
        shell.showExpenseDetails(false);
      }
    });

    // Listen for requests from the ExpenseList.
    expenseList.setListener(new ExpenseList.Listener() {
      public void onReportSelected(ReportProxy report) {
        expenseDetails.setReportRecord(report, lastDepartment, lastEmployee);
        shell.showExpenseDetails(true);
      }
    });

    /*
     * TODO these should be constructor arguments, and the inits should probably
     * happen onLoad
     */
    expenseList.init(requestFactory, eventBus);
    expenseDetails.init(eventBus);

    // Browser history integration
    placeHistoryHandler.register(placeController, eventBus, new Place() {
    });
    placeHistoryHandler.handleCurrentHistory();
  }
}
