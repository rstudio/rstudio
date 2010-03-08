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
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.sample.expenses.shared.Employee;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.valuestore.shared.Values;

import java.util.List;

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
    root.add(shell);

    final HasValueList<Values<Employee>> employees = new HasValueList<Values<Employee>>() {

      public void editValueList(boolean replace, int index,
          List<Values<Employee>> newValues) {
        throw new UnsupportedOperationException();
      }

      public void setValueList(List<Values<Employee>> newValues) {
        shell.users.clear();
        for (Values<Employee> values : newValues) {
          shell.users.addItem(values.get(Employee.DISPLAY_NAME),
              values.get(Employee.USER_NAME));
        }
      }

      public void setValueListSize(int size, boolean exact) {
        throw new UnsupportedOperationException();
      }
    };

    requestFactory.employeeRequest().findAllEmployees().forProperty(
        Employee.DISPLAY_NAME).forProperty(Employee.USER_NAME).to(shell).fire();

    // TODO(rjrjr) now get details
    final TextBox nameHolder = new TextBox();

    shell.users.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent event) {
        nameHolder.setText("gesundheit");
        // Remember the slots
        // requestFactory.employeeRequest().findEmployee(literal(shell.users.getValue());
      }
    });
  }
}
