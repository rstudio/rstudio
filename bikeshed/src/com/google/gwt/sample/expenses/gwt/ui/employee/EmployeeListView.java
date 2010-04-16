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
package com.google.gwt.sample.expenses.gwt.ui.employee;

import com.google.gwt.bikeshed.cells.client.ActionCell;
import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.IdentityColumn;
import com.google.gwt.bikeshed.list.client.TextColumn;
import com.google.gwt.bikeshed.list.client.TextHeader;
import com.google.gwt.sample.expenses.gwt.place.ExpensesPlaces;
import com.google.gwt.sample.expenses.gwt.request.EmployeeRecord;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link ValuesListViewTable} specialized to {@link EmployeeKey} values.
 * <p>
 * TODO The bulk of this should be in a <g:table> in a ui.xml file
 */
public class EmployeeListView extends ValuesListViewTable<EmployeeRecord> {
  private static final List<Property<?>> properties;
  static {
    List<Property<?>> p = new ArrayList<Property<?>>();
    p.add(EmployeeRecord.userName);
    p.add(EmployeeRecord.displayName);
    properties = Collections.unmodifiableList(p);
  }

  private static List<Column<EmployeeRecord, ?, ?>> getColumns(
      final ExpensesPlaces places) {
    List<Column<EmployeeRecord, ?, ?>> columns = new ArrayList<Column<EmployeeRecord, ?, ?>>();

    columns.add(new TextColumn<EmployeeRecord>() {
      @Override
      public String getValue(EmployeeRecord object) {
        return object.getUserName();
      }
    });

    columns.add(new TextColumn<EmployeeRecord>() {
      @Override
      public String getValue(EmployeeRecord object) {
        return object.getDisplayName();
      }
    });

    columns.add(new IdentityColumn<EmployeeRecord>(
        new ActionCell<EmployeeRecord>("Show",
            places.<EmployeeRecord> getDetailsGofer())));

//    columns.add(new IdentityColumn<EmployeeRecord>(
//        new ActionCell<EmployeeRecord>("Edit",
//            places.<EmployeeRecord> getEditorGofer())));

    return columns;
  }

  private static List<Header<?>> getHeaders() {
    List<Header<?>> headers = new ArrayList<Header<?>>();
    for (final Property<?> property : properties) {
      headers.add(new TextHeader(property.getName()));
    }
    return headers;
  }

  public EmployeeListView(String headingMessage, ExpensesPlaces places) {
    super(headingMessage, getColumns(places), getHeaders());
  }

  public List<Property<?>> getProperties() {
    return properties;
  }
}
