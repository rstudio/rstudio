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

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * A representation of a column in a table.  The column may maintain view data
 * for each cell on demand.  New view data, if needed, is created by the
 * cell's onBrowserEvent method, stored in the Column, and passed to future
 * calls to Cell's {@link Cell#onBrowserEvent} and @link{Cell#render} methods.
 * 
 * @param <T> the row type
 * @param <C> the column type
 * @param <V> the view data type
 */
// TODO - when can we get rid of a view data object?
// TODO - should viewData implement some interface? (e.g., with commit/rollback/dispose)
// TODO - have a ViewDataColumn superclass / SimpleColumn subclass
public abstract class Column<T, C, V> {

  protected final Cell<C, V> cell;

  protected FieldUpdater<T, C, V> fieldUpdater;

  /**
   * An instance of HasKey<T> that is used to provide a key for a row object
   * for the purpose of retrieving view data.  A value of null means that
   * the row object itself will be used as the key.
   */
  protected HasKey<T> hasKey;

  protected Map<Object, V> viewDataMap = new HashMap<Object, V>();

  public Column(Cell<C, V> cell, HasKey<T> hasKey) {
    this.cell = cell;
    this.hasKey = hasKey;
  }

  public Column(Cell<C, V> cell) {
    this(cell, null);
  }

  public boolean consumesEvents() {
    return cell.consumesEvents();
  }

  public abstract C getValue(T object);

  public void onBrowserEvent(Element elem, final int index, final T object,
      NativeEvent event) {
    Object key = hasKey == null ? object : hasKey.getKey(object);
    V viewData = viewDataMap.get(key);
    V newViewData = cell.onBrowserEvent(elem,
        getValue(object), viewData, event, fieldUpdater == null ? null
            : new ValueUpdater<C, V>() {
              public void update(C value, V viewData) {
                fieldUpdater.update(index, object, value, viewData);
              }
            });
    if (newViewData != viewData) {
      viewDataMap.put(key, newViewData);
    }
  }

  public void render(T object, StringBuilder sb) {
    C value = getValue(object);
    cell.render(value, viewDataMap.get(object), sb);
  }

  public void setFieldUpdater(FieldUpdater<T, C, V> fieldUpdater) {
    this.fieldUpdater = fieldUpdater;
  }

  protected Cell<C, V> getCell() {
    return cell;
  }

  protected FieldUpdater<T, C, V> getFieldUpdater() {
    return fieldUpdater;
  }
}
