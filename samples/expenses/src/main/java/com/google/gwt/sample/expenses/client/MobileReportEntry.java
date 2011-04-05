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
import com.google.gwt.sample.expenses.shared.EmployeeProxy;
import com.google.gwt.sample.expenses.shared.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.shared.ReportProxy;
import com.google.gwt.sample.expenses.shared.ReportRequest;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.shared.Receiver;

import java.util.Date;

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
  @SuppressWarnings("unused")
  private final Listener listener;
  private ReportRequest request;

  public MobileReportEntry(Listener listener,
      ExpensesRequestFactory requestFactory) {
    this.listener = listener;
    this.requestFactory = requestFactory;
    initWidget(BINDER.createAndBindUi(this));

    for (String department : ExpensesApp.DEPARTMENTS) {
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
    request = requestFactory.reportRequest();
    report = request.create(ReportProxy.class);
    request.persist().using(report);
    report.setReporter(reporter);
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
    ReportProxy editableReport = request.edit(report);
    editableReport.setPurpose(purposeText.getText());
    editableReport.setNotes(notesText.getText());
    editableReport.setDepartment(departmentList.getValue(departmentList.getSelectedIndex()));

    // TODO(jgw): Use non-deprecated date methods for this.
    Date date = new Date(dateYear.getSelectedIndex() + 100,
        dateMonth.getSelectedIndex(), dateDay.getSelectedIndex() + 1);
    editableReport.setCreated(date);

    // TODO: wait throbber
    request.fire(new Receiver<Void>() {
      @Override
      public void onSuccess(Void ignore) {
      }
      
      // use onViolations to check for ConstraintViolations.
    });
  }

  public void onRefresh(boolean clear) {
  }

  public void show(ReportProxy report) {
    this.report = report;
    displayReport();
  }

  private void displayReport() {
    errorText.setInnerText("");
    purposeText.setText(report.getPurpose());
    notesText.setText(report.getNotes());
    String department = report.getDepartment();
    departmentList.setSelectedIndex(0);
    for (int i = 0; i < ExpensesApp.DEPARTMENTS.length; i++) {
      if (ExpensesApp.DEPARTMENTS[i].equals(department)) {
        departmentList.setSelectedIndex(i);
      }
    }

    Date d = report.getCreated();
    showCreationDate(d);
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

  @SuppressWarnings("deprecation")
  private void showCreationDate(Date d) {
    if (d != null) {
      // TODO(jgw): Use non-deprecated date methods for this.
      dateYear.setSelectedIndex(d.getYear() - 100);
      dateMonth.setSelectedIndex(d.getMonth());
      dateDay.setSelectedIndex(d.getDate() - 1);
    } else {
      dateYear.setSelectedIndex(0);
      dateMonth.setSelectedIndex(0);
      dateDay.setSelectedIndex(0);
    }
  }
}
