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
import com.google.gwt.requestfactory.shared.Receiver;
import com.google.gwt.requestfactory.shared.RequestObject;
import com.google.gwt.requestfactory.shared.SyncResult;
import com.google.gwt.sample.expenses.client.request.EmployeeProxy;
import com.google.gwt.sample.expenses.client.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.client.request.ReportProxy;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;
import java.util.Set;

/**
 * Form to create a new ReportRecord.
 */
public class MobileReportEntry extends Composite implements MobilePage {

  /**
   * TODO: doc.
   */
  public interface Listener {
    void onReportUpdated();
  }

  interface Binder extends UiBinder<Widget, MobileReportEntry> {
  }

  private static Binder BINDER = GWT.create(Binder.class);

  @UiField
  TextBox purposeText, notesText;
  @UiField
  ListBox dateYear, dateMonth, dateDay, departmentList;
  @UiField
  Element errorText;

  private ReportProxy report;
  private final ExpensesRequestFactory requestFactory;
  private final Listener listener;
  private RequestObject<Void> requestObject;

  public MobileReportEntry(Listener listener,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    initWidget(BINDER.createAndBindUi(this));

    for (String department : Expenses.DEPARTMENTS) {
      departmentList.addItem(department);
    }

    populateList(dateYear, 2000, 2010);
    populateList(dateMonth, 1, 12);
    populateList(dateDay, 1, 31);
  }

  @Override
  public Widget asWidget() {
    return this;
  }

  public void create(EmployeeProxy reporter) {
    report = requestFactory.create(ReportProxy.class);
    requestObject = requestFactory.reportRequest().persist(report);
    ReportProxy editableReport = requestObject.edit(report);
    editableReport.setReporter(reporter);
    displayReport();
  }

  public String getPageTitle() {
    return report != null ? report.getPurpose() : "";
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
    ReportProxy editableReport = requestObject.edit(report);
    editableReport.setPurpose(purposeText.getText());
    editableReport.setNotes(notesText.getText());
    editableReport.setDepartment(departmentList.getValue(departmentList.getSelectedIndex()));

    // TODO(jgw): Use non-deprecated date methods for this.
    Date date = new Date(dateYear.getSelectedIndex() + 100,
        dateMonth.getSelectedIndex(), dateDay.getSelectedIndex() + 1);
    editableReport.setCreated(date);

    // TODO: wait throbber
    requestObject.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore, Set<SyncResult> response) {
        // Check for commit errors.
        String errorMessage = "";
        // FIXME: Error handling disabled
        if (errorMessage.length() > 0) {
          errorText.setInnerText(errorMessage);
        } else {
          listener.onReportUpdated();
        }
      }
    });
  }

  public void onRefresh(boolean clear) {
  }

  public void show(ReportProxy report) {
    this.report = report;
    displayReport();
  }

  @SuppressWarnings("deprecation")
  private void displayReport() {
    errorText.setInnerText("");
    purposeText.setText(report.getPurpose());
    notesText.setText(report.getNotes());
    String department = report.getDepartment();
    departmentList.setSelectedIndex(0);
    for (int i = 0; i < Expenses.DEPARTMENTS.length; i++) {
      if (Expenses.DEPARTMENTS[i].equals(department)) {
        departmentList.setSelectedIndex(i);
      }
    }

    // TODO(jgw): Use non-deprecated date methods for this.
    Date d = report.getCreated();
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
