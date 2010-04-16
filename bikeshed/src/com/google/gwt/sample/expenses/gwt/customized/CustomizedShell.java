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
package com.google.gwt.sample.expenses.gwt.customized;

import com.google.gwt.bikeshed.cells.client.DateCell;
import com.google.gwt.bikeshed.cells.client.EditTextCell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.TextCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListViewAdapter;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.sample.expenses.gwt.request.ReportRecordChanged;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TakesValueList;
import com.google.gwt.user.client.ui.Widget;

import java.util.Date;
import java.util.List;

/**
 * UI shell for expenses sample app. A horrible clump of stuff that should be
 * refactored into proper MVP pieces.
 */
public class CustomizedShell extends Composite implements
    TakesValueList<ReportRecord>, ReportRecordChanged.Handler {
  interface Listener {
    void setPurpose(ReportRecord report, String purpose);
  }

  interface ShellUiBinder extends UiBinder<Widget, CustomizedShell> {
  }

  private static ShellUiBinder uiBinder = GWT.create(ShellUiBinder.class);

  @UiField Element error;
  @UiField PagingTableListView<ReportRecord> listView;
  @UiField ListBox users;

  private Column<ReportRecord, Date, Void> createdCol = new Column<ReportRecord, Date, Void>(
      new DateCell()) {
    @Override
    public Date getValue(ReportRecord object) {
      return object.getCreated();
    }
  };
  private Listener listener;
  private final ListViewAdapter<ReportRecord> adapter;

  private Column<ReportRecord, String, String> purposeCol = new Column<ReportRecord, String, String>(
      new EditTextCell()) {
    @Override
    public String getValue(ReportRecord object) {
      return object.getPurpose();
    }
  };

  private Column<ReportRecord, String, Void> statusCol = new Column<ReportRecord, String, Void>(
      TextCell.getInstance()) {
    @Override
    public String getValue(ReportRecord object) {
      return "...";
    }
  };

  private List<ReportRecord> values;

  public CustomizedShell() {
    adapter = new ListViewAdapter<ReportRecord>();
    initWidget(uiBinder.createAndBindUi(this));

    listView.addColumn(createdCol, "Created");
    listView.addColumn(statusCol, "Status (tbd)");
    listView.addColumn(purposeCol, "Purpose");

    purposeCol.setFieldUpdater(new FieldUpdater<ReportRecord, String, String>() {
      public void update(int index, ReportRecord object, String value,
          String viewData) {
        adapter.getList().set(index, object);
        listener.setPurpose(object, value);
      }
    });
  }

  public List<ReportRecord> getValues() {
    return values;
  }

  public void onReportChanged(ReportRecordChanged event) {
    refresh();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setValueList(List<ReportRecord> newValues) {
    this.values = newValues;
    refresh();
  }

  @UiFactory
  PagingTableListView<ReportRecord> createListView() {
    PagingTableListView<ReportRecord> table = new PagingTableListView<ReportRecord>(
        adapter, 10);
    adapter.addView(table);
    return table;
  }

  private void refresh() {
    adapter.setList(values);
  }
}
