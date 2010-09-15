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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.requestfactory.client.AuthenticationFailureHandler;
import com.google.gwt.requestfactory.client.LoginWidget;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestEvent;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.requestfactory.shared.UserInformationProxy;
import com.google.gwt.sample.expenses.client.request.EmployeeProxy;
import com.google.gwt.sample.expenses.client.request.ExpensesRequestFactory;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Set;

/**
 * Entry point for the mobile version of the Expenses app.
 */
public class ExpensesMobile implements EntryPoint {

  /**
   * The url parameter that specifies the employee id.
   */
  private static final String EMPLOYEE_ID_PARAM = "employeeId";

  /**
   * TODO(jgw): Put this some place more sensible.
   * 
   * @param amount the amount in dollars
   */
  public static String formatCurrency(double amount) {
    StringBuilder sb = new StringBuilder();

    int price = (int) (amount * 100);
    boolean negative = price < 0;
    if (negative) {
      price = -price;
    }
    int dollars = price / 100;
    int cents = price % 100;

    if (negative) {
      sb.append("-");
    }

    sb.append(dollars);
    sb.append('.');
    if (cents < 10) {
      sb.append('0');
    }
    sb.append(cents);

    return sb.toString();
  }

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
      public void onUncaughtException(Throwable e) {
        Window.alert("Error: " + e.getMessage());
        // placeController.goTo(Place.NOWHERE);
      }
    });

    // Get the employee ID from the URL.
    long employeeId = 1;
    try {
      String value = Window.Location.getParameter(EMPLOYEE_ID_PARAM);
      if (value != null && value.length() > 0) {
        employeeId = Long.parseLong(value);
      }
    } catch (NumberFormatException e) {
      RootPanel.get().add(new Label("employeeId is invalid"));
      return;
    }

    final EventBus eventBus = new SimpleEventBus();
    final ExpensesRequestFactory requestFactory = GWT.create(ExpensesRequestFactory.class);
    requestFactory.init(eventBus);

    requestFactory.employeeRequest().findEmployee(employeeId).fire(
        new Receiver<EmployeeProxy>() {
          public void onSuccess(EmployeeProxy employee,
              Set<SyncResult> syncResults) {
            final ExpensesMobileShell shell = new ExpensesMobileShell(eventBus,
                requestFactory, employee);
            RootPanel.get().add(shell);

            // Check for Authentication failures or mismatches
            RequestEvent.register(eventBus, new AuthenticationFailureHandler());

            // Add a login widget to the page
            final LoginWidget login = shell.getLoginWidget();
            Receiver<UserInformationProxy> receiver = new Receiver<UserInformationProxy>() {
              public void onSuccess(UserInformationProxy userInformationRecord,
                  Set<SyncResult> syncResults) {
                login.setUserInformation(userInformationRecord);
              }
            };
            requestFactory.userInformationRequest().getCurrentUserInformation(
                Location.getHref()).fire(receiver);
          }
        });
  }
}
