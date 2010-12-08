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
import com.google.gwt.sample.gaerequest.client.LoginWidget;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasOneWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * UI shell for expenses sample app.
 */
public class ExpensesShell extends Composite {
  interface ShellUiBinder extends UiBinder<Widget, ExpensesShell> {
  }

  private static ShellUiBinder uiBinder = GWT.create(ShellUiBinder.class);

  @UiField(provided = true)
  final ExpenseReportList expenseList;

  @UiField(provided = true)
  final ExpenseReportDetails expenseDetails;

  @UiField(provided = true)
  final ExpenseTree expenseTree;

  @UiField(provided = true)
  final LoginWidget loginWidget;

  @UiField SlidingPanel slidingPanel;
  @UiField DockLayoutPanel dockLayout;

  public ExpensesShell(ExpenseTree expenseTree, ExpenseReportList expenseList,
      ExpenseReportDetails expenseDetails, LoginWidget loginWidget) {
    this.expenseTree = expenseTree;
    this.expenseList = expenseList;
    this.expenseDetails = expenseDetails;
    this.loginWidget = loginWidget;
    initWidget(uiBinder.createAndBindUi(this));
  }

  public ExpenseReportDetails getExpenseDetails() {
    return expenseDetails;
  }

  public ExpenseReportList getExpenseList() {
    return expenseList;
  }

  public ExpenseTree getExpenseTree() {
    return expenseTree;
  }

  public LoginWidget getLoginWidget() {
    return loginWidget;
  }

  public HasOneWidget getPanel() {
    return slidingPanel;
  }
}
