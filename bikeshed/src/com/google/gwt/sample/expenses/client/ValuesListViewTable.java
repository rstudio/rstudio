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

import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListModel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;

import java.util.Iterator;
import java.util.List;

public class ValuesListViewTable<K extends ValuesKey<K>> extends Composite {
  interface Binder extends UiBinder<HTMLPanel, ValuesListViewTable<?>> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField(provided = true) PagingTableListView<Values<K>> table;
  @UiField HeadingElement heading;

  public ValuesListViewTable(String headingMessage, ListModel<Values<K>> model,
      List<Column<Values<K>, ?, ?>> columns, List<Header<?>> headers) {
    table = new PagingTableListView<Values<K>>(model, 100);
    final Iterator<Header<?>> nextHeader = headers.iterator();
    for (Column<Values<K>, ?, ?> column : columns) {
      if (nextHeader.hasNext()) {
        table.addColumn(column, nextHeader.next());
      } else {
        table.addColumn(column);
      }
    }
    initWidget(BINDER.createAndBindUi(this));

    heading.setInnerText(headingMessage);
  }
}
