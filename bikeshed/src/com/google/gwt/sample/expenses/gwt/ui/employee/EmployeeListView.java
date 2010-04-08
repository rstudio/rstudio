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
import com.google.gwt.sample.expenses.gwt.request.EmployeeKey;
import com.google.gwt.valuestore.client.ValuesListViewTable;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@ValuesListViewTable} specialized to {@link EmployeeKey} values.
 * <p>
 * TODO The bulk of this should be in a <g:table> in a ui.xml file
 */
public class EmployeeListView extends ValuesListViewTable<EmployeeKey> {
  private static final List<Property<EmployeeKey, ?>> properties;
  static {
    List<Property<EmployeeKey, ?>> p = new ArrayList<Property<EmployeeKey, ?>>();
    p.add(EmployeeKey.get().getUserName());
    p.add(EmployeeKey.get().getDisplayName());
    properties = Collections.unmodifiableList(p);
  }

  private static List<Column<Values<EmployeeKey>, ?, ?>> getColumns(
      final ExpensesPlaces places) {
    List<Column<Values<EmployeeKey>, ?, ?>> columns = new ArrayList<Column<Values<EmployeeKey>, ?, ?>>();

    columns.add(new TextColumn<Values<EmployeeKey>>() {
      @Override
      public String getValue(Values<EmployeeKey> object) {
        return object.get(EmployeeKey.get().getUserName());
      }
    });

    columns.add(new TextColumn<Values<EmployeeKey>>() {
      @Override
      public String getValue(Values<EmployeeKey> object) {
        return object.get(EmployeeKey.get().getDisplayName());
      }
    });

    columns.add(new IdentityColumn<Values<EmployeeKey>>(
        new ActionCell<Values<EmployeeKey>>("Show",
            places.<EmployeeKey> getDetailsGofer())));

    // columns.add(new IdentityColumn<Values<EmployeeKey>>(
    // new ActionCell<Values<EmployeeKey>>("Edit",
    // places.<EmployeeKey> getEditorGofer())));

    return columns;
  }

  private static List<Header<?>> getHeaders() {
    List<Header<?>> headers = new ArrayList<Header<?>>();
    for (final Property<EmployeeKey, ?> property : properties) {
      headers.add(new TextHeader(property.getName()));
    }
    return headers;
  }

  public EmployeeListView(String headingMessage, ExpensesPlaces places) {
    super(headingMessage, getColumns(places), getHeaders());
  }

  public List<Property<EmployeeKey, ?>> getProperties() {
    return properties;
  }
}
