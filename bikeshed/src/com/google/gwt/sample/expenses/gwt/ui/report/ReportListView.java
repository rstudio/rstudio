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
package com.google.gwt.sample.expenses.gwt.ui.report;

import com.google.gwt.bikeshed.cells.client.ActionCell;
import com.google.gwt.bikeshed.cells.client.DateCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.IdentityColumn;
import com.google.gwt.bikeshed.list.client.SimpleColumn;
import com.google.gwt.bikeshed.list.client.TextColumn;
import com.google.gwt.bikeshed.list.client.TextHeader;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.ReportRecord;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * {@link ValuesListViewTable} specialized to {@link ReportKey} values.
 * <p>
 * TODO The bulk of this should be in a <g:table> in a ui.xml file
 */
public class ReportListView extends ValuesListViewTable<ReportRecord> {
  private static final List<Property<?>> properties;
  static {
    List<Property<?>> p = new ArrayList<Property<?>>();
    p.add(ReportRecord.created);
    p.add(ReportRecord.purpose);
    properties = Collections.unmodifiableList(p);
  }

  private static List<Column<ReportRecord, ?, ?>> getColumns(
      final ExpensesPlaces places) {
    List<Column<ReportRecord, ?, ?>> columns = new ArrayList<Column<ReportRecord, ?, ?>>();

    DateCell dateCell = new DateCell(DateTimeFormat.getShortDateFormat());
    columns.add(new SimpleColumn<ReportRecord, Date>(dateCell) {
      @Override
      public Date getValue(ReportRecord object) {
        return object.getCreated();
      }
    });

    columns.add(new TextColumn<ReportRecord>() {
      @Override
      public String getValue(ReportRecord object) {
        return object.getPurpose();
      }
    });

    columns.add(new IdentityColumn<ReportRecord>(
        new ActionCell<ReportRecord>("Show",
            places.<ReportRecord> getDetailsGofer())));

//    columns.add(new IdentityColumn<ReportRecord>(
//        new ActionCell<ReportRecord>("Edit",
//            places.<ReportRecord> getEditorGofer())));

    return columns;
  }

  private static List<Header<?>> getHeaders() {
    List<Header<?>> headers = new ArrayList<Header<?>>();
    for (final Property<?> property : properties) {
      headers.add(new TextHeader(property.getName()));
    }
    return headers;
  }

  public ReportListView(String headingMessage, ExpensesPlaces places) {
    super(headingMessage, getColumns(places), getHeaders());
  }

  public List<Property<?>> getProperties() {
    return properties;
  }

}
