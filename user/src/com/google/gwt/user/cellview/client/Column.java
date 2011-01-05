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
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HasAlignment;

/**
 * A representation of a column in a table. The column may maintain view data
 * for each cell on demand. New view data, if needed, is created by the cell's
 * onBrowserEvent method, stored in the Column, and passed to future calls to
 * Cell's {@link Cell#onBrowserEvent} and {@link Cell#render} methods.
 * 
 * @param <T> the row type
 * @param <C> the column type
 */
public abstract class Column<T, C> implements HasCell<T, C>, HasAlignment {

  /**
   * The {@link Cell} responsible for rendering items in the column.
   */
  private final Cell<C> cell;

  /**
   * The {@link FieldUpdater} used for updating values in the column.
   */
  private FieldUpdater<T, C> fieldUpdater;

  private boolean isSortable = false;
  private HorizontalAlignmentConstant hAlign = null;
  private VerticalAlignmentConstant vAlign = null;

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

  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return hAlign;
  }

  /**
   * Returns the column value from within the underlying data object.
   */
  public abstract C getValue(T object);

  public VerticalAlignmentConstant getVerticalAlignment() {
    return vAlign;
  }

  /**
   * Check if the column is sortable.
   * 
   * @return true if sortable, false if not
   */
  public boolean isSortable() {
    return isSortable;
  }

  /**
   * Handle a browser event that took place within the column.
   * 
   * @param context the cell context
   * @param elem the parent Element
   * @param object the base object to be updated
   * @param event the native browser event
   */
  public void onBrowserEvent(Context context, Element elem, final T object,
      NativeEvent event) {
    final int index = context.getIndex();
    ValueUpdater<C> valueUpdater = (fieldUpdater == null) ? null
        : new ValueUpdater<C>() {
          public void update(C value) {
            fieldUpdater.update(index, object, value);
          }
        };
    cell.onBrowserEvent(context, elem, getValue(object), event, valueUpdater);
  }

  /**
   * Render the object into the cell.
   * 
   * @param context the cell context
   * @param object the object to render
   * @param sb the buffer to render into
   */
  public void render(Context context, T object, SafeHtmlBuilder sb) {
    cell.render(context, getValue(object), sb);
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
   * {@inheritDoc}
   * 
   * <p>
   * The new horizontal alignment will apply the next time the table is
   * rendered.
   * </p>
   */
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    this.hAlign = align;
  }

  /**
   * Set whether or not the column can be sorted. The change will take effect
   * the next time the table is redrawn.
   * 
   * @param sortable true to make sortable, false to make unsortable
   */
  public void setSortable(boolean sortable) {
    this.isSortable = sortable;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The new vertical alignment will apply the next time the table is rendered.
   * </p>
   */
  public void setVerticalAlignment(VerticalAlignmentConstant align) {
    this.vAlign = align;
  }
}
