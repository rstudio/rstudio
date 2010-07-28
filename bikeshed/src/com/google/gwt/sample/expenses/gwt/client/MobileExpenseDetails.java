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
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecordChanged;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.PropertyReference;
import com.google.gwt.valuestore.shared.SyncResult;

import java.util.List;
import java.util.Set;

/**
 * TODO: doc.
 */
public class MobileExpenseDetails extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onEditExpense(ExpenseRecord expense);
  }

  interface Binder extends UiBinder<Widget, MobileExpenseDetails> {
  }

  private static Binder BINDER = GWT.create(Binder.class);

  @UiField
  Element approvalText, nameText, dateText, categoryText, priceText, reasonRow,
      reasonText;

  private ExpenseRecord expense;
  private final Listener listener;
  private final ExpensesRequestFactory requestFactory;

  public MobileExpenseDetails(Listener listener, HandlerManager eventBus,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;

    eventBus.addHandler(ExpenseRecordChanged.TYPE,
        new ExpenseRecordChanged.Handler() {
          public void onExpenseRecordChanged(ExpenseRecordChanged event) {
            if (expense != null) {
              ExpenseRecord newRecord = event.getRecord();
              if (newRecord.getId().equals(expense.getId())) {
                show(newRecord);
              }
            }
          }
        });

    initWidget(BINDER.createAndBindUi(this));
  }

  public Widget asWidget() {
    return this;
  }

  public String getPageTitle() {
    return expense != null ? expense.getDescription() : "";
  }

  public boolean needsAddButton() {
    return false;
  }

  public String needsCustomButton() {
    return "Edit";
  }

  public boolean needsRefreshButton() {
    return true;
  }

  public void onAdd() {
  }

  public void onCustom() {
    listener.onEditExpense(expense);
  }

  public void onRefresh(boolean clear) {
    PropertyReference<String> idRef = new PropertyReference<String>(expense,
        ExpenseRecord.id);

    requestFactory.expenseRequest().findExpense(idRef).fire(
        new Receiver<List<ExpenseRecord>>() {
          public void onSuccess(List<ExpenseRecord> response, Set<SyncResult> syncResults) {
            assert response.size() == 1;
            show(response.get(0));
          }
        });
  }

  public void show(ExpenseRecord expense) {
    this.expense = expense;

    DateTimeFormat formatter = DateTimeFormat.getMediumDateFormat();

    Expenses.Approval approval = Expenses.Approval.from(expense.getApproval());
    nameText.setInnerText(expense.getDescription());
    dateText.setInnerText(formatter.format(expense.getCreated()));
    categoryText.setInnerText(expense.getCategory());
    priceText.setInnerText(ExpensesMobile.formatCurrency(expense.getAmount()));
    approvalText.setInnerHTML(Expenses.Approval.BLANK.equals(approval)
        ? "Awaiting Review" : approval.getText());
    approvalText.getStyle().setColor(approval.getColor());

    reasonText.setInnerText(expense.getReasonDenied());
    if (Expenses.Approval.DENIED.equals(approval)) {
      // Show the reason denied.
      reasonRow.getStyle().clearDisplay();
    } else {
      // Hide the reason denied.
      reasonRow.getStyle().setDisplay(Display.NONE);
    }
  }
}
