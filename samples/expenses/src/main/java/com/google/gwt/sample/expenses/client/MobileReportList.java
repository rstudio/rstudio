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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.List;

/**
 * TODO: doc.
 */
public class MobileReportList extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onCreateReport(EmployeeProxy reporter);

    void onReportSelected(ReportProxy report);
  }

  /**
   * The receiver for the last request.
   */
  private Receiver<List<ReportProxy>> lastReceiver;

  private final EmployeeProxy employee;
  private final Listener listener;
  private final CellList<ReportProxy> reportList;
  private final AsyncDataProvider<ReportProxy> reportDataProvider;
  private final NoSelectionModel<ReportProxy> reportSelection;
  private final ExpensesRequestFactory requestFactory;

  public MobileReportList(final Listener listener,
      final ExpensesRequestFactory requestFactory, EmployeeProxy employee) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    this.employee = employee;
    
    EntityProxyKeyProvider<ReportProxy> keyProvider = new EntityProxyKeyProvider<ReportProxy>();

    reportDataProvider = new AsyncDataProvider<ReportProxy>(keyProvider) {
      @Override
      protected void onRangeChanged(HasData<ReportProxy> view) {
        requestReports();
      }
    };

    reportList = new CellList<ReportProxy>(new AbstractCell<ReportProxy>() {
      @Override
      public void render(Context context, ReportProxy value, SafeHtmlBuilder sb) {
        sb.appendHtmlConstant("<div class='item'>");
        sb.appendEscaped(value.getPurpose());
        sb.appendHtmlConstant("</div>");
      }
    });
    reportList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);

    reportSelection = new NoSelectionModel<ReportProxy>(keyProvider);
    reportSelection.addSelectionChangeHandler(
        new SelectionChangeEvent.Handler() {
          public void onSelectionChange(SelectionChangeEvent event) {
            listener.onReportSelected(reportSelection.getLastSelectedObject());
          }
        });

    reportList.setSelectionModel(reportSelection);
    reportDataProvider.addDataDisplay(reportList);

    initWidget(reportList);
    onRefresh(false);
  }

  @Override
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
    listener.onCreateReport(employee);
  }

  public void onCustom() {
  }

  public void onRefresh(boolean clear) {
    if (clear) {
      reportDataProvider.updateRowCount(0, true);
    }
    requestReports();
  }

  private String[] getReportColumns() {
    return new String[]{"created", "purpose"};
  }

  private void requestReports() {
    if (requestFactory == null) {
      return;
    }
    lastReceiver = new Receiver<List<ReportProxy>>() {
      @Override
      public void onSuccess(
          List<ReportProxy> newValues) {
        int size = newValues.size();
        reportDataProvider.updateRowCount(size, true);
        reportDataProvider.updateRowData(0, newValues);
      }
    };
    requestFactory.reportRequest().findReportEntriesBySearch(employee.getId(),
        "", "", "created" + " DESC", 0, 25).with(
        getReportColumns()).fire(lastReceiver);
  }
}
