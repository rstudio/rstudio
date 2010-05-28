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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * TODO
 */
public class ExpensesMobileShell extends Composite {

  interface ShellUiBinder extends UiBinder<Widget, ExpensesMobileShell> { }
  private static ShellUiBinder BINDER = GWT.create(ShellUiBinder.class);

  private final ExpensesRequestFactory requestFactory;

  @UiField DeckPanel deck;
  @UiField MobileReportList reportList;
  @UiField MobileExpenseList expenseList;
  @UiField MobileExpenseDetails expenseDetails;

  @UiField Button backButton, forwardButton;
  @UiField SpanElement titleSpan;

  public ExpensesMobileShell(ExpensesRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    initWidget(BINDER.createAndBindUi(this));

    deck.showWidget(0);
  }

  @UiFactory
  MobileReportList createReportList() {
    return new MobileReportList(new MobileReportList.Listener() {
      public void onReportSelected(ReportRecord report) {
        expenseList.show(report);
        deck.showWidget(1);
      }
    }, requestFactory);
  }

  @UiFactory
  MobileExpenseList createExpenseList() {
    return new MobileExpenseList(new MobileExpenseList.Listener() {
      public void onExpenseSelected(ExpenseRecord expense) {
        expenseDetails.show(expense);
        deck.showWidget(2);
      }
    }, requestFactory);
  }

  @UiHandler("backButton")
  void onBack(ClickEvent evt) {
    int idx = deck.getVisibleWidget();
    if (idx > 0) {
      deck.showWidget(idx - 1);
    }
  }

  @UiHandler("forwardButton")
  void onForward(ClickEvent evt) {
  }
}
