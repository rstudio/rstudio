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
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.requestfactory.shared.PropertyReference;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.expenses.client.request.ExpenseProxy;
import com.google.gwt.sample.expenses.client.request.ExpenseProxyChanged;
import com.google.gwt.sample.expenses.client.request.ExpensesRequestFactory;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

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
    void onEditExpense(ExpenseProxy expense);
  }

  interface Binder extends UiBinder<Widget, MobileExpenseDetails> {
  }

  private static Binder BINDER = GWT.create(Binder.class);

  @UiField
  Element approvalText, nameText, dateText, categoryText, priceText, reasonRow,
      reasonText;

  private ExpenseProxy expense;
  private final Listener listener;
  private final ExpensesRequestFactory requestFactory;

  public MobileExpenseDetails(Listener listener, HandlerManager eventBus,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;

    eventBus.addHandler(ExpenseProxyChanged.TYPE,
        new ExpenseProxyChanged.Handler() {
          public void onExpenseRecordChanged(ExpenseProxyChanged event) {
            if (expense != null) {
              ExpenseProxy newRecord = event.getProxy();
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
    PropertyReference<Long> idRef = new PropertyReference<Long>(expense,
        ExpenseProxy.id);

    requestFactory.expenseRequest().findExpense(idRef).fire(
        new Receiver<List<ExpenseProxy>>() {
          public void onSuccess(List<ExpenseProxy> response, Set<SyncResult> syncResults) {
            assert response.size() == 1;
            show(response.get(0));
          }
        });
  }

  public void show(ExpenseProxy expense) {
    this.expense = expense;

    @SuppressWarnings("deprecation")
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
