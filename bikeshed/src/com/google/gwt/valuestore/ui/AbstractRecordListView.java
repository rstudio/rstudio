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
package com.google.gwt.valuestore.ui;

import com.google.gwt.bikeshed.list.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract implementation of RecordListView. Subclasses must call {@link #init}
 * with the root widget, its {@link CellTable}, and a list of
 * {@link PropertyColumn}.
 * 
 * @param <R> the type of the records
 */
public abstract class AbstractRecordListView<R extends Record> extends
    Composite implements RecordListView<R> {

  private CellTable<R> table;
  private Set<Property<?>> properties = new HashSet<Property<?>>();

  public AbstractRecordListView<R> asWidget() {
    return this;
  }

  public Set<Property<?>> getProperties() {
    return properties;
  }

  public Range getRange() {
    return table.getRange();
  }

  public void setData(int start, int length, List<R> values) {
    table.setData(start, length, values);
  }

  public void setDataSize(int size, boolean isExact) {
    table.setDataSize(size, isExact);
  }
  
  public void setDelegate(
      com.google.gwt.view.client.ListView.Delegate<R> delegate) {
    throw new UnsupportedOperationException(
        "A RecordListView requires a RecordListView.Delegate");
  }

  public void setDelegate(final Delegate<R> delegate) {
    table.setDelegate(delegate);
    
    table.setSelectionModel(new SingleSelectionModel<R>() {
      @Override
      public void setSelected(R object, boolean selected) {
        super.setSelected(object, selected);
        delegate.showDetails(object);
      }
    });
  }

  public void setSelectionModel(SelectionModel<? super R> selectionModel) {
    table.setSelectionModel(selectionModel);
  }

  protected void init(Widget root, CellTable<R> table,
      List<PropertyColumn<R, ?>> columns) {
    super.initWidget(root);
    this.table = table;
    table.setSelectionEnabled(true);
    
    for (PropertyColumn<R, ?> column : columns) {
      table.addColumn(column, column.getProperty().getName());
      properties.add(column.getProperty());
    }
  }

  @Override
  protected void initWidget(Widget widget) {
    throw new UnsupportedOperationException(
        "AbstractRecordListView must be initialized via "
            + "init(Widget CellTable<R> List<PropertyColumn<R, ?>> ) ");
  }
}
