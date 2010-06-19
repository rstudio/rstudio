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
package com.google.gwt.app.place;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.view.client.PagingListView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
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
  private Delegate<R> delegate;

  public PagingListView<R> asPagingListView() {
    return table;
  }

  public AbstractRecordListView<R> asWidget() {
    return this;
  }

  public Set<Property<?>> getProperties() {
    return properties;
  }

  public void setDelegate(final Delegate<R> delegate) {
    this.delegate = delegate;
  }

  protected void init(Widget root, CellTable<R> table, Button newButton,
      List<PropertyColumn<R, ?>> columns) {
    super.initWidget(root);
    this.table = table;
    table.setSelectionEnabled(true);

    for (PropertyColumn<R, ?> column : columns) {
      table.addColumn(column, column.getProperty().getDisplayName());
      properties.add(column.getProperty());
    }

    newButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        delegate.createClicked();
      }
    });
  }

  @Override
  protected void initWidget(Widget widget) {
    throw new UnsupportedOperationException(
        "AbstractRecordListView must be initialized via "
            + "init(Widget CellTable<R> List<PropertyColumn<R, ?>> ) ");
  }
}
