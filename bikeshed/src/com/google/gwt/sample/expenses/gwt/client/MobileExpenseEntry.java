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
import com.google.gwt.dom.client.Element;
import com.google.gwt.requestfactory.shared.DeltaValueStore;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.Date;
import java.util.Map;
import java.util.Set;

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

  private ExpenseRecord expense;
  private final ExpensesRequestFactory requestFactory;
  private final Listener listener;
  private RequestFactory.RequestObject<Void> requestObject;

  public MobileExpenseEntry(Listener listener,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    initWidget(BINDER.createAndBindUi(this));

    populateList(dateYear, 2000, 2010);
    populateList(dateMonth, 1, 12);
    populateList(dateDay, 1, 31);
  }

  public Widget asWidget() {
    return this;
  }

  public void create(String reportId) {
    requestObject = requestFactory.expenseRequest().persist();
    DeltaValueStore deltas = requestObject.getDeltaValueStore();
    expense = (ExpenseRecord) deltas.create(ExpenseRecord.class);
    deltas.set(ExpenseRecord.reportId, expense, reportId);
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
    DeltaValueStore deltas = requestObject.getDeltaValueStore();
    deltas.set(ExpenseRecord.description, expense, nameText.getText());
    deltas.set(ExpenseRecord.category, expense, categoryText.getText());

    // TODO(jgw): validate amount (in dollars -- database is in pennies)
    String amountText = priceText.getText();
    double amount = Double.parseDouble(amountText);
    deltas.set(ExpenseRecord.amount, expense, amount);

    // TODO(jgw): Use non-deprecated date methods for this.
    Date date = new Date(
        dateYear.getSelectedIndex() + 100,
        dateMonth.getSelectedIndex(),
        dateDay.getSelectedIndex() + 1
    );
    deltas.set(ExpenseRecord.created, expense, date);

    // TODO: wait throbber
    requestObject.fire(new Receiver<Void>() {
          public void onSuccess(Void ignore, Set<SyncResult> response) {
            // Check for commit errors.
            String errorMessage = "";
            for (SyncResult result : response) {
              if (result.hasViolations()) {
                Map<String, String> violations = result.getViolations();
                for (String message : violations.values()) {
                  errorMessage += message + " ";
                }
              }
            }
            if (errorMessage.length() > 0) {
              errorText.setInnerText(errorMessage);
            } else {
              listener.onExpenseUpdated();
            }
          }
        });
  }

  public void onRefresh(boolean clear) {
  }

  public void show(ExpenseRecord expense) {
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
