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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.ProvidesKey;

/**
 * A representation of a column in a table. The column may maintain view data
 * for each cell on demand. New view data, if needed, is created by the cell's
 * onBrowserEvent method, stored in the Column, and passed to future calls to
 * Cell's {@link Cell#onBrowserEvent} and {@link Cell#render} methods.
 *
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class Column<T, C> implements HasCell<T, C> {

  /**
   * The {@link Cell} responsible for rendering items in the column.
   */
  protected final Cell<C> cell;

  /**
   * The {@link FieldUpdater} used for updating values in the column.
   */
  protected FieldUpdater<T, C> fieldUpdater;

  /**
   * Construct a new Column with a given {@link Cell}.
   *
   * @param cell the Cell used by this Column
   */
  public Column(Cell<C> cell) {
    this.cell = cell;
  }

  /**
   * Returns the {@link Cell} responsible for rendering items in the column.
   * 
   * @return a Cell
   */
  public Cell<C> getCell() {
    return cell;
  }

  /**
   * Returns the {@link FieldUpdater} used for updating values in the column.
   * 
   * @return an instance of FieldUpdater<T, C>
   * @see #setFieldUpdater(FieldUpdater)
   */
  public FieldUpdater<T, C> getFieldUpdater() {
    return fieldUpdater;
  }

  /**
   * Returns the column value from within the underlying data object.
   */
  public abstract C getValue(T object);

  /**
   * Handle a browser event that took place within the column.
   *
   * @param elem the parent Element
   * @param index the current row index of the object
   * @param object the base object to be updated
   * @param event the native browser event
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *          object should act as its own key.
   */
  public void onBrowserEvent(Element elem, final int index, final T object,
      NativeEvent event, ProvidesKey<T> keyProvider) {
    Object key = getKey(object, keyProvider);
    ValueUpdater<C> valueUpdater = (fieldUpdater == null)
        ? null : new ValueUpdater<C>() {
          public void update(C value) {
            fieldUpdater.update(index, object, value);
          }
        };
    cell.onBrowserEvent(elem, getValue(object), key, event, valueUpdater);
  }

  /**
   * Render the object into the cell.
   *
   * @param object the object to render
   * @param keyProvider the {@link ProvidesKey} for the object
   * @param sb the buffer to render into
   */
  public void render(T object, ProvidesKey<T> keyProvider, SafeHtmlBuilder sb) {
    Object key = getKey(object, keyProvider);
    cell.render(getValue(object), key, sb);
  }

  /**
   * Set the {@link FieldUpdater} used for updating values in the column.
   *
   * @param fieldUpdater the field updater
   * @see #getFieldUpdater()
   */
  public void setFieldUpdater(FieldUpdater<T, C> fieldUpdater) {
    this.fieldUpdater = fieldUpdater;
  }

  /**
   * Get the view key for the object given the {@link ProvidesKey}. If the
   * {@link ProvidesKey} is null, the object is used as the key.
   *
   * @param object the row object
   * @param keyProvider the {@link ProvidesKey}
   * @return the key for the object
   */
  private Object getKey(T object, ProvidesKey<T> keyProvider) {
    return keyProvider == null ? object : keyProvider.getKey(object);
  }
}
