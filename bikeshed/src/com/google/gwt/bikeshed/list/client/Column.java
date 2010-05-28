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
package com.google.gwt.bikeshed.list.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.view.client.HasViewData;
import com.google.gwt.view.client.ProvidesKey;

import java.util.HashMap;
import java.util.Map;

/**
 * A representation of a column in a table. The column may maintain view data
 * for each cell on demand. New view data, if needed, is created by the cell's
 * onBrowserEvent method, stored in the Column, and passed to future calls to
 * Cell's {@link Cell#onBrowserEvent} and @link{Cell#render} methods.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
// TODO - when can we get rid of a view data object?
// TODO - should viewData implement some interface? (e.g., with
// commit/rollback/dispose)
// TODO - have a ViewDataColumn superclass / SimpleColumn subclass
public abstract class Column<T, C> implements HasViewData, HasCell<T, C> {

  protected final Cell<C> cell;

  protected FieldUpdater<T, C> fieldUpdater;

  protected Map<Object, Object> viewDataMap = new HashMap<Object, Object>();

  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  public boolean consumesEvents() {
    return cell.consumesEvents();
  }

  /**
   * Returns true if the contents of the column may depend on the current state
   * of the selection model associated with the table that is displaying this
   * column. The default implementation returns false.
   */
  public boolean dependsOnSelection() {
    return false;
  }

  public Cell<C> getCell() {
    return cell;
  }

  public FieldUpdater<T, C> getFieldUpdater() {
    return fieldUpdater;
  }

  public abstract C getValue(T object);

  public Object getViewData(Object key) {
    return viewDataMap.get(key);
  }

  /**
   * @param providesKey an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key.
   */
  public void onBrowserEvent(Element elem, final int index, final T object,
      NativeEvent event, ProvidesKey<T> providesKey) {
    Object key = providesKey == null ? object : providesKey.getKey(object);
    Object viewData = viewDataMap.get(key);
    Object newViewData = cell.onBrowserEvent(elem, getValue(object), viewData,
        event, fieldUpdater == null ? null : new ValueUpdater<C>() {
          public void update(C value) {
            fieldUpdater.update(index, object, value);
          }
        });
    if (newViewData != viewData) {
      viewDataMap.put(key, newViewData);
    }
  }

  public void render(T object, StringBuilder sb) {
    cell.render(getValue(object), viewDataMap.get(object), sb);
  }

  public void setFieldUpdater(FieldUpdater<T, C> fieldUpdater) {
    this.fieldUpdater = fieldUpdater;
  }

  public void setViewData(Object key, Object viewData) {
    viewDataMap.put(key, viewData);
  }
}
