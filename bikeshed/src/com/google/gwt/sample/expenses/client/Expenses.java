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
import com.google.gwt.sample.expenses.gen.ExpenseRequestFactoryImpl;
import com.google.gwt.sample.expenses.shared.EmployeeKey;
import com.google.gwt.sample.expenses.shared.ReportKey;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.valuestore.client.ValuesImpl;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Expenses implements EntryPoint {
//  /**
//   * The message displayed to the user when the server cannot be reached or
//   * returns an error.
//   */
//  private static final String SERVER_ERROR = "An error occurred while "
//      + "attempting to contact the server. Please check your network "
//      + "connection and try again.";

  private final ExpenseRequestFactoryImpl requestFactory = GWT.create(ExpenseRequestFactoryImpl.class);

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    RootLayoutPanel root = RootLayoutPanel.get();

    final Shell shell = new Shell();
    final EmployeeList employees = new EmployeeList(shell.users);

    root.add(shell);
    
    shell.setListener(new Shell.Listener() {
      public void setFirstPurpose(String purpose) {
        ValuesImpl<ReportKey> reportValues = (ValuesImpl<ReportKey>) shell.getValues().get(0);
        reportValues.setString(ReportKey.get().getPurpose(), purpose);
        List<Values<?>> deltaValueStore = new ArrayList<Values<?>>();
        deltaValueStore.add(reportValues);
        
        requestFactory.syncRequest(deltaValueStore).fire();
      }
    });

    employees.setListener(new EmployeeList.Listener() {
      public void onEmployeeSelected(Values<EmployeeKey> e) {
        requestFactory.reportRequest().//
        findReportsByEmployee(e.getRef(EmployeeKey.get().getId())).//
        forProperty(ReportKey.get().getCreated()).//
        forProperty(ReportKey.get().getPurpose()).//
        to(shell).//
        fire();
      }
    });

    requestFactory.employeeRequest().findAllEmployees().forProperty(
        EmployeeKey.get().getDisplayName()).forProperty(EmployeeKey.get().getUserName()).to(employees).fire();
  }
}
