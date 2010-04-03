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
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.ExpensesRequestFactory;
import com.google.gwt.sample.expenses.gwt.request.ReportKey;
import com.google.gwt.valuestore.client.ListModelAdapter;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@ValuesListViewTable} specialized to {@link ReportKey} values.
 * <p>
 * TODO The bulk of this should be in a <g:table> in a ui.xml file
 */
public class ReportListView extends ValuesListViewTable<ReportKey> {

  private static List<Column<Values<ReportKey>, ?, ?>> getColumns(
      final ExpensesPlaces places) {
    List<Column<Values<ReportKey>, ?, ?>> columns = new ArrayList<Column<Values<ReportKey>, ?, ?>>();

    DateCell dateCell = new DateCell(DateTimeFormat.getShortDateFormat());
    columns.add(new SimpleColumn<Values<ReportKey>, Date>(dateCell) {
      @Override
      public Date getValue(Values<ReportKey> object) {
        return object.get(ReportKey.get().getCreated());
      }
    });

    columns.add(new TextColumn<Values<ReportKey>>() {
      @Override
      public String getValue(Values<ReportKey> object) {
        return object.get(ReportKey.get().getPurpose());
      }
    });

    columns.add(new IdentityColumn<Values<ReportKey>>(
        new ActionCell<Values<ReportKey>>("Show",
            places.<ReportKey> getDetailsGofer())));

//    columns.add(new IdentityColumn<Values<ReportKey>>(
//        new ActionCell<Values<ReportKey>>("Edit",
//            places.<ReportKey> getEditorGofer())));

    return columns;
  }

  private static List<Header<?>> getHeaders() {
    List<Header<?>> headers = new ArrayList<Header<?>>();
    for (final Property<ReportKey, ?> property : getProperties()) {
      headers.add(new TextHeader(property.getName()));
    }
    return headers;
  }

  private static ListModel<Values<ReportKey>> getModel(
      final ExpensesRequestFactory requests) {
    return new ListModelAdapter<ReportKey>() {

      @Override
      protected void onRangeChanged(int start, int length) {
        requests.reportRequest().findAllReports().forProperties(getProperties()).to(
            this).fire();
      }
    };
  }

  private static List<Property<ReportKey, ?>> getProperties() {
    List<Property<ReportKey, ?>> properties = new ArrayList<Property<ReportKey, ?>>();
    properties.add(ReportKey.get().getCreated());
    properties.add(ReportKey.get().getPurpose());
    return properties;
  }

  public ReportListView(String headingMessage, ExpensesPlaces places,
      ExpensesRequestFactory requests) {
    super(headingMessage, getModel(requests), getColumns(places), getHeaders());
  }

}
