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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.SyncResult;
import com.google.gwt.view.client.AsyncListViewAdapter;
import com.google.gwt.view.client.ListView;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * TODO: doc.
 */
public class MobileReportList extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onCreateReport(Long reporterId);

    void onReportSelected(ReportRecord report);
  }

  /**
   * The receiver for the last request.
   */
  private Receiver<List<ReportRecord>> lastReceiver;

  private final Long employeeId;
  private final Listener listener;
  private final CellList<ReportRecord> reportList;
  private final AsyncListViewAdapter<ReportRecord> reportAdapter;
  private final NoSelectionModel<ReportRecord> reportSelection;
  private final ExpensesRequestFactory requestFactory;

  public MobileReportList(final Listener listener,
      final ExpensesRequestFactory requestFactory, long employeeId) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    this.employeeId = new Long(employeeId);
    reportAdapter = new AsyncListViewAdapter<ReportRecord>() {
      @Override
      protected void onRangeChanged(ListView<ReportRecord> view) {
        requestReports();
      }
    };
    reportAdapter.setKeyProvider(Expenses.REPORT_RECORD_KEY_PROVIDER);

    reportList = new CellList<ReportRecord>(new AbstractCell<ReportRecord>() {
      @Override
      public void render(ReportRecord value, Object viewData, StringBuilder sb) {
        sb.append("<div class='item'>" + value.getPurpose() + "</div>");
      }
    });

    reportSelection = new NoSelectionModel<ReportRecord>();
    reportSelection.setKeyProvider(Expenses.REPORT_RECORD_KEY_PROVIDER);
    reportSelection.addSelectionChangeHandler(new SelectionModel.SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        listener.onReportSelected(reportSelection.getLastSelectedObject());
      }
    });

    reportList.setSelectionModel(reportSelection);
    reportAdapter.addView(reportList);

    initWidget(reportList);
    onRefresh(false);
  }

  public Widget asWidget() {
    return this;
  }

  public String getPageTitle() {
    return "Expense Reports";
  }

  public boolean needsAddButton() {
    return true;
  }

  public String needsCustomButton() {
    return null;
  }

  public boolean needsRefreshButton() {
    return true;
  }

  public void onAdd() {
    listener.onCreateReport(employeeId);
  }

  public void onCustom() {
  }

  public void onRefresh(boolean clear) {
    if (clear) {
      reportAdapter.updateDataSize(0, true);
    }
    requestReports();
  }

  private Collection<Property<?>> getReportColumns() {
    List<Property<?>> columns = new ArrayList<Property<?>>();
    columns.add(ReportRecord.created);
    columns.add(ReportRecord.purpose);
    return columns;
  }

  private void requestReports() {
    if (requestFactory == null) {
      return;
    }
    lastReceiver = new Receiver<List<ReportRecord>>() {
      public void onSuccess(List<ReportRecord> newValues, Set<SyncResult> syncResults) {
        int size = newValues.size();
        reportAdapter.updateDataSize(size, true);
        reportAdapter.updateViewData(0, size, newValues);
      }
    };
    requestFactory.reportRequest().findReportEntriesBySearch(employeeId, "",
        "", ReportRecord.created.getName() + " DESC", 0, 25).forProperties(
        getReportColumns()).fire(lastReceiver);
  }
}
