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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;

/**
 * TODO.
 */
public class ExpensesMobileShell extends Composite {

  interface ShellUiBinder extends UiBinder<Widget, ExpensesMobileShell> { }
  private static ShellUiBinder BINDER = GWT.create(ShellUiBinder.class);

  @UiField SimplePanel container;
  @UiField HTML backButton, addButton, refreshButton, customButton;
  @UiField Element titleSpan;

  private MobileReportList reportList;
  private MobileExpenseList expenseList;
  private MobileExpenseDetails expenseDetails;
  private MobileExpenseEntry expenseEntry;
  private MobileReportEntry reportEntry;

  private final long employeeId;
  private final HandlerManager eventBus;
  private final ExpensesRequestFactory requestFactory;
  private ArrayList<MobilePage> pages = new ArrayList<MobilePage>();

  public ExpensesMobileShell(HandlerManager eventBus,
      ExpensesRequestFactory requestFactory, long employeeId) {
    this.eventBus = eventBus;
    this.requestFactory = requestFactory;
    this.employeeId = employeeId;

    initWidget(BINDER.createAndBindUi(this));
    showReportList();
  }

  @UiHandler("addButton")
  @SuppressWarnings("unused")
  void onAdd(ClickEvent evt) {
    topPage().onAdd();
  }

  @UiHandler("backButton")
  @SuppressWarnings("unused")
  void onBack(ClickEvent evt) {
    popPage();
  }

  @UiHandler("customButton")
  @SuppressWarnings("unused")
  void onCustom(ClickEvent evt) {
    topPage().onCustom();
  }

  @UiHandler("refreshButton")
  @SuppressWarnings("unused")
  void onRefresh(ClickEvent evt) {
    topPage().onRefresh(true);
  }

  private void popPage() {
    assert pages.size() > 1;
    pages.remove(topPage());
    MobilePage topPage = topPage();
    showPage(topPage);
    topPage.onRefresh(false);
  }

  private void pushPage(MobilePage page) {
    pages.add(page);
    showPage(page);
  }

  private void showExpenseDetails(ExpenseRecord expense) {
    if (expenseDetails == null) {
      expenseDetails = new MobileExpenseDetails(
          new MobileExpenseDetails.Listener() {
            public void onEditExpense(ExpenseRecord expense) {
              showExpenseEntry(expense);
            }
          }, eventBus, requestFactory);
    }

    expenseDetails.show(expense);
    pushPage(expenseDetails);
  }

  private void showExpenseEntry(ExpenseRecord expense) {
    if (expenseEntry == null) {
      expenseEntry = new MobileExpenseEntry(new MobileExpenseEntry.Listener() {
        public void onExpenseUpdated() {
          popPage();
        }
      }, requestFactory);
    }

    expenseEntry.show(expense);
    pushPage(expenseEntry);
  }

  private void showReportEntry(ReportRecord report) {
    if (reportEntry == null) {
      reportEntry = new MobileReportEntry(new MobileReportEntry.Listener() {
        public void onReportUpdated() {
          popPage();
        }
      }, requestFactory);
    }

    reportEntry.show(report);
    pushPage(reportEntry);
  }

  private void showExpenseList(ReportRecord report) {
    if (expenseList == null) {
      expenseList = new MobileExpenseList(new MobileExpenseList.Listener() {
        public void onCreateExpense(Long reportId) {
          showNewExpenseEntry(reportId);
        }

        public void onEditReport(ReportRecord report) {
          showReportEntry(report);
        }

        public void onExpenseSelected(ExpenseRecord expense) {
          showExpenseDetails(expense);
        }
      }, requestFactory);
    }

    expenseList.show(report);
    pushPage(expenseList);
  }

  private void showNewExpenseEntry(Long reportId) {
    if (expenseEntry == null) {
      expenseEntry = new MobileExpenseEntry(new MobileExpenseEntry.Listener() {
        public void onExpenseUpdated() {
          popPage();
        }
      }, requestFactory);
    }

    expenseEntry.create(reportId);
    pushPage(expenseEntry);
  }

  private void showNewReportEntry(Long reporterId) {
    if (reportEntry == null) {
      reportEntry = new MobileReportEntry(new MobileReportEntry.Listener() {
        public void onReportUpdated() {
          popPage();
        }
      }, requestFactory);
    }

    reportEntry.create(reporterId);
    pushPage(reportEntry);
  }

  private void showPage(MobilePage page) {
    Widget oldPage = container.getWidget();
    if (oldPage != null) {
      container.remove(oldPage);
    }

    container.add(page.asWidget());

    titleSpan.setInnerText(page.getPageTitle());
    backButton.setVisible(pages.size() > 1);
    refreshButton.setVisible(page.needsRefreshButton());
    addButton.setVisible(page.needsAddButton());

    String custom = page.needsCustomButton();
    if (custom != null) {
      customButton.setText(custom);
      customButton.setVisible(true);
    } else {
      customButton.setVisible(false);
    }
  }

  private void showReportList() {
    if (reportList == null) {
      reportList = new MobileReportList(new MobileReportList.Listener() {
        public void onCreateReport(Long reporterId) {
          showNewReportEntry(reporterId);
        }

        public void onReportSelected(ReportRecord report) {
          showExpenseList(report);
        }
      }, requestFactory, employeeId);
    }

    pushPage(reportList);
  }

  private MobilePage topPage() {
    return pages.get(pages.size() - 1);
  }
}
