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

/**
 * A representation of a column in a table.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class Column<T, C> {
  private final Cell<C> cell;
  private FieldUpdater<T, C> fieldUpdater;

  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  public void onBrowserEvent(Element elem, final int index, final T object,
      NativeEvent event) {
    cell.onBrowserEvent(elem, getValue(object), event,
        fieldUpdater == null ? null : new ValueUpdater<C>() {
      public void update(C value) {
        fieldUpdater.update(index, object, value);
      }
    });
  }

  public void render(T object, StringBuilder sb) {
    C value = getValue(object);
    cell.render(value, sb);
  }

  public void setFieldUpdater(FieldUpdater<T, C> fieldUpdater) {
    this.fieldUpdater = fieldUpdater;
  }

  protected Cell<C> getCell() {
    return cell;
  }

  protected FieldUpdater<T, C> getFieldUpdater() {
    return fieldUpdater;
  }

  protected abstract C getValue(T object);
}
