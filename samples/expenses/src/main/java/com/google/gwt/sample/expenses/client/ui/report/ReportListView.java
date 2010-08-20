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
package com.google.gwt.sample.expenses.client.ui.report;

import com.google.gwt.app.place.AbstractRecordListView;
import com.google.gwt.app.place.PropertyColumn;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormatRenderer;
import com.google.gwt.sample.expenses.client.request.EmployeeRecord;
import com.google.gwt.sample.expenses.client.request.ReportRecord;
import com.google.gwt.sample.expenses.client.ui.employee.EmployeeRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTMLPanel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@link AbstractRecordListView} specialized to {@link EmployeeKey} values.
 * <p>
 * TODO This should be a <g:table> in a ui.xml file
 */
public class ReportListView extends AbstractRecordListView<ReportRecord> {
  interface Binder extends UiBinder<HTMLPanel, ReportListView> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField CellTable<ReportRecord> table;
  @UiField Button newButton;

  public ReportListView() {
    init(BINDER.createAndBindUi(this), table, newButton, getColumns());
  }

  protected List<PropertyColumn<ReportRecord, ?>> getColumns() {
    // TODO These should be <g:col> elements in a <g:table> in the ui.xml file

    List<PropertyColumn<ReportRecord, ?>> columns = new ArrayList<PropertyColumn<ReportRecord, ?>>();

    columns.add(new PropertyColumn<ReportRecord, Date>(ReportRecord.created,
        new DateTimeFormatRenderer(DateTimeFormat.getShortDateFormat())));

    columns.add(PropertyColumn.<ReportRecord> getStringPropertyColumn(ReportRecord.purpose));

    columns.add(new PropertyColumn<ReportRecord, EmployeeRecord>(
        ReportRecord.reporter, EmployeeRenderer.instance()));

    columns.add(new PropertyColumn<ReportRecord, EmployeeRecord>(
        ReportRecord.approvedSupervisor, EmployeeRenderer.instance()));

    return columns;
  }
}
