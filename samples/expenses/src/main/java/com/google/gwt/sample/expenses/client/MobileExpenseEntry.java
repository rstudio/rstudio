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
import com.google.gwt.dom.client.Element;
import com.google.gwt.sample.expenses.shared.ExpenseProxy;
import com.google.gwt.sample.expenses.shared.ExpenseRequest;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.Date;

/**
 * TODO: doc.
 */
public class MobileExpenseEntry extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onExpenseUpdated();
  }

  interface Binder extends UiBinder<Widget, MobileExpenseEntry> { }
  private static Binder BINDER = GWT.create(Binder.class);

  @UiField TextBox nameText, categoryText, priceText;
  @UiField ListBox dateYear, dateMonth, dateDay;
  @UiField Element errorText;

  private ExpenseProxy expense;
  private final ExpensesRequestFactory requestFactory;
  @SuppressWarnings("unused")
  private final Listener listener;
  private ExpenseRequest request;

  public MobileExpenseEntry(Listener listener,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    initWidget(BINDER.createAndBindUi(this));

    populateList(dateYear, 2000, 2010);
    populateList(dateMonth, 1, 12);
    populateList(dateDay, 1, 31);
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  public void create(ReportProxy report) {
    request = requestFactory.expenseRequest();
    expense = request.create(ExpenseProxy.class);
    expense.setReport(report);
    request.persist().using(expense);
    displayExpense();
  }

  public String getPageTitle() {
    return expense != null ? expense.getDescription() : "";
  }

  public boolean needsAddButton() {
    return false;
  }

  public String needsCustomButton() {
    return "Done";
  }

  public boolean needsRefreshButton() {
    return false;
  }

  public void onAdd() {
  }

  @SuppressWarnings("deprecation")
  public void onCustom() {
    request = requestFactory.expenseRequest();
    ExpenseProxy editableExpense = request.edit(expense);
    editableExpense.setDescription(nameText.getText());
    editableExpense.setCategory(categoryText.getText());
    request.persist().using(editableExpense);

    // TODO(jgw): validate amount (in dollars -- database is in pennies)
    String amountText = priceText.getText();
    double amount = Double.parseDouble(amountText);
    editableExpense.setAmount(amount);

    // TODO(jgw): Use non-deprecated date methods for this.
    Date date = new Date(
        dateYear.getSelectedIndex() + 100,
        dateMonth.getSelectedIndex(),
        dateDay.getSelectedIndex() + 1
    );
    editableExpense.setCreated(date);

    // TODO: wait throbber
    request.fire(new Receiver<Void>() {
          @Override
          public void onSuccess(Void ignore) {
          }

          // TODO: use onViolations to check for constraint violations.
        });
  }

  public void onRefresh(boolean clear) {
  }

  public void show(ExpenseProxy expense) {
    this.expense = expense;
    displayExpense();
  }
  
  @SuppressWarnings("deprecation")
  private void displayExpense() {
    errorText.setInnerText("");
    nameText.setText(expense.getDescription());
    categoryText.setText(expense.getCategory());
    priceText.setText(ExpensesMobile.formatCurrency(expense.getAmount()));

    // TODO(jgw): Use non-deprecated date methods for this.
    Date d = expense.getCreated();
    dateYear.setSelectedIndex(d.getYear() - 100);
    dateMonth.setSelectedIndex(d.getMonth());
    dateDay.setSelectedIndex(d.getDate() - 1);
  }

  private void populateList(ListBox list, int start, int end) {
    for (int i = start; i <= end; ++i) {
      if (i < 10) {
        list.addItem("0" + i);
      } else {
        list.addItem("" + i);
      }
    }
  }
}
