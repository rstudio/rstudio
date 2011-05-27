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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.expenses.shared.ExpenseProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: doc.
 */
public class MobileExpenseList extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onCreateExpense(ReportProxy report);

    void onEditReport(ReportProxy report);

    void onExpenseSelected(ExpenseProxy expense);
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<div class=\"item\">{0}{1} ({2})</div>")
    SafeHtml div(SafeHtml approvalIcon, String description, String amount);
  }

  /**
   * The cell used to render {@link ExpenseProxy}s.
   */
  private class ExpenseCell extends AbstractCell<ExpenseProxy> {

    private final SafeHtml approvedHtml;
    private final String approvedText = Approval.APPROVED.getText();
    private final SafeHtml blankHtml;
    private final SafeHtml deniedHtml;
    private final String deniedText = Approval.DENIED.getText();

    public ExpenseCell() {
      if (template == null) {
        template = GWT.create(Template.class);
      }
      approvedHtml = Approval.APPROVED.getIconHtml();
      blankHtml = Approval.BLANK.getIconHtml();
      deniedHtml = Approval.DENIED.getIconHtml();
    }

    @Override
    public void render(Context context, ExpenseProxy value, SafeHtmlBuilder sb) {
      String approval = value.getApproval();
      SafeHtml approvalIcon;
      if (approvedText.equals(approval)) {
        approvalIcon = approvedHtml;
      } else if (deniedText.equals(approval)) {
        approvalIcon = deniedHtml;
      } else {
        approvalIcon = blankHtml;
      }
      sb.append(template.div(approvalIcon, value.getDescription(),
          ExpensesMobile.formatCurrency(value.getAmount())));
    }
  }

  private static Template template;

  /**
   * The auto refresh interval in milliseconds.
   */
  private static final int REFRESH_INTERVAL = 5000;

  private final ExpensesRequestFactory requestFactory;
  private final CellList<ExpenseProxy> expenseList;
  private final AsyncDataProvider<ExpenseProxy> expenseDataProvider;
  private final NoSelectionModel<ExpenseProxy> expenseSelection;

  /**
   * The set of Expense keys that we already know are denied. When a new key is
   * added, we compare it to the list of known keys to determine if it is new.
   */
  private Set<Object> knownDeniedKeys = null;

  /**
   * The receiver for the last request.
   */
  private Receiver<List<ExpenseProxy>> lastReceiver;

  private ReportProxy report;
  private final Listener listener;

  /**
   * The {@link Timer} used to periodically refresh the table.
   */
  private final Timer refreshTimer = new Timer() {
    @Override
    public void run() {
      requestExpenses();
    }
  };

  public MobileExpenseList(
      final Listener listener, final ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    expenseDataProvider = new AsyncDataProvider<ExpenseProxy>(new EntityProxyKeyProvider<ExpenseProxy>()) {
      @Override
      protected void onRangeChanged(HasData<ExpenseProxy> view) {
        requestExpenses();
      }
    };

    expenseList = new CellList<ExpenseProxy>(new ExpenseCell());
    expenseList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    expenseSelection = new NoSelectionModel<ExpenseProxy>();
    expenseList.setSelectionModel(expenseSelection);
    expenseSelection.addSelectionChangeHandler(
        new SelectionChangeEvent.Handler() {
          public void onSelectionChange(SelectionChangeEvent event) {
            listener.onExpenseSelected(
                expenseSelection.getLastSelectedObject());
          }
        });

    expenseDataProvider.addDataDisplay(expenseList);
    initWidget(expenseList);
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  public String getPageTitle() {
    return report != null ? report.getPurpose() : "";
  }

  public boolean needsAddButton() {
    return true;
  }

  public String needsCustomButton() {
    return "Edit";
  }

  public boolean needsRefreshButton() {
    return true;
  }

  public void onAdd() {
    listener.onCreateExpense(report);
  }

  public void onCustom() {
    listener.onEditReport(report);
  }

  public void onRefresh(boolean clear) {
    if (clear) {
      expenseDataProvider.updateRowCount(0, true);
    }
    requestExpenses();
  }

  public void show(ReportProxy report) {
    this.report = report;
    knownDeniedKeys = null;

    onRefresh(true);
  }

  private String[] getExpenseColumns() {
    return new String[]{"description", "amount"};
  }

  /**
   * Request the expenses.
   */
  private void requestExpenses() {
    refreshTimer.cancel();
    if (requestFactory == null || report == null) {
      return;
    }
    lastReceiver = new Receiver<List<ExpenseProxy>>() {
      @Override
      public void onSuccess(List<ExpenseProxy> newValues) {
        if (this == lastReceiver) {
          int size = newValues.size();
          expenseDataProvider.updateRowCount(size, true);
          expenseDataProvider.updateRowData(0, newValues);

          // Add the new keys to the known keys.
          boolean isInitialData = knownDeniedKeys == null;
          if (knownDeniedKeys == null) {
            knownDeniedKeys = new HashSet<Object>();
          }
          for (ExpenseProxy value : newValues) {
            Object key = expenseDataProvider.getKey(value);
            String approval = value.getApproval();
            if (Approval.DENIED.getText().equals(approval)) {
              if (!isInitialData && !knownDeniedKeys.contains(key)) {
                (new PhaseAnimation.CellListPhaseAnimation<ExpenseProxy>(
                    expenseList, value, expenseDataProvider)).run();
              }
              knownDeniedKeys.add(key);
            } else {
              knownDeniedKeys.remove(key);
            }
          }

          refreshTimer.schedule(REFRESH_INTERVAL);
        }
      }
    };
    requestFactory.expenseRequest().findExpensesByReport(
        report.getId()).with(getExpenseColumns()).fire(
        lastReceiver);
  }
}
