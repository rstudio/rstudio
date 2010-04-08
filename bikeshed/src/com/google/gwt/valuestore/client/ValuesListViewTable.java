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
package com.google.gwt.valuestore.client;

import com.google.gwt.bikeshed.list.client.Column;
import com.google.gwt.bikeshed.list.client.Header;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.shared.ListRegistration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;
import com.google.gwt.valuestore.shared.ValuesKey;
import com.google.gwt.valuestore.shared.ValuesListView;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A widget that displays lists of {@link com.google.gwt.valuestore.ValueStore
 * ValueStore} records in a {@link PagingTableListView}
 *
 * @param <K> the type of the ValuesKey shared by these records
 */
public abstract class ValuesListViewTable<K extends ValuesKey<K>> extends
    Composite implements ValuesListView<K> {
  interface Binder extends UiBinder<HTMLPanel, ValuesListViewTable<?>> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  public ValuesListView.Delegate delegate;
  public ListModelAdapter<K> listModel;

  @UiField(provided = true)
  PagingTableListView<Values<K>> table;
  @UiField
  HeadingElement heading;

  public ValuesListViewTable(String headingMessage,
      List<Column<Values<K>, ?, ?>> columns, List<Header<?>> headers) {
    listModel = createModel();
    table = new PagingTableListView<Values<K>>(listModel, 100);
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

  public ValuesListViewTable<K> asWidget() {
    return this;
  }

  /**
   * @return the delegate for this view, or null if there is none
   */
  public ValuesListView.Delegate getDelegate() {
    return delegate;
  }

  public abstract Collection<Property<K, ?>> getProperties();

  /**
   * @param delegate the new delegate for this view. May be null
   */
  public void setDelegate(ValuesListView.Delegate delegate) {
    this.delegate = delegate;
  }
  
  public void setValueList(List<Values<K>> newValues) {
    listModel.setValueList(newValues);
  }

  private ListModelAdapter<K> createModel() {
    return new ListModelAdapter<K>() {
      @Override
      protected void onRangeChanged(ListRegistration<Values<K>> reg, int start, int length) {
        getDelegate().onRangeChanged(start, length);
      }
    };
  }
}
