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

import com.google.gwt.bikeshed.list.client.CellList;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.request.ExpenseRecord;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO
 */
public class MobileExpenseList extends Composite implements
    Receiver<List<ExpenseRecord>> {

  /**
   * TODO
   */
  public interface Listener {
    void onExpenseSelected(ExpenseRecord expense);
  }

  private final ExpensesRequestFactory requestFactory;
  private final CellList<ExpenseRecord> expenseList;
  private final ListViewAdapter<ExpenseRecord> expenseAdapter;
  private final SingleSelectionModel<ExpenseRecord> expenseSelection;

  public MobileExpenseList(final Listener listener,
      final ExpensesRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
    expenseAdapter = new ListViewAdapter<ExpenseRecord>();

    expenseList = new CellList<ExpenseRecord>(
        new AbstractCell<ExpenseRecord>() {
          @Override
          public void render(ExpenseRecord value, Object viewData,
              StringBuilder sb) {
            sb.append("<div onclick='' class='item'>" + value.getDescription() + " " +
                ExpensesMobile.formatCurrency(value.getAmount().intValue())
                + "</div>");
          }
        });

    expenseSelection = new SingleSelectionModel<ExpenseRecord>();
    expenseList.setSelectionModel(expenseSelection);
    expenseSelection.addSelectionChangeHandler(new SelectionModel.SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        listener.onExpenseSelected(expenseSelection.getSelectedObject());
      }
    });

    expenseAdapter.addView(expenseList);
    initWidget(expenseList);
  }

  public void onSuccess(List<ExpenseRecord> newValues) {
    expenseAdapter.setList(newValues);
  }

  public void show(ReportRecord report) {
    requestFactory.expenseRequest().findExpensesByReport(
        report.getRef(ReportRecord.id)).forProperties(getExpenseColumns()).to(
        this).fire();
  }

  private Collection<Property<?>> getExpenseColumns() {
    List<Property<?>> columns = new ArrayList<Property<?>>();
    columns.add(ExpenseRecord.description);
    columns.add(ExpenseRecord.amount);
    return columns;
  }
}
