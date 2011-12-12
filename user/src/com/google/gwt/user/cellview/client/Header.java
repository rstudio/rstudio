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
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * A table column header or footer.
 * 
 * @param <H> the {@link Cell} type
 */
public abstract class Header<H> {

  private final Cell<H> cell;

  private String headerStyleNames = null;
  private ValueUpdater<H> updater;

  /**
   * Construct a Header with a given {@link Cell}.
   * 
   * @param cell the {@link Cell} responsible for rendering items in the header
   */
  public Header(Cell<H> cell) {
    this.cell = cell;
  }

  /**
   * Return the {@link Cell} responsible for rendering items in the header.
   * 
   * @return the header Cell
   */
  public Cell<H> getCell() {
    return cell;
  }

  /**
   * Get extra style names that should be applied to a cell in this header. May be overriden to
   * get value dependent styles by calling {@link #getValue}.
   * 
   * @return the extra styles of the given row in a space-separated list, or
   *         {@code null} if there are no extra styles for the cells in this
   *         header
   */
  public String getHeaderStyleNames() {
    return headerStyleNames;
  }
  
  /**
   * Get the key for the header value. By default, the key is the same as the
   * value. Override this method to return a custom key.
   * 
   * @return the key associated with the value
   */
  public Object getKey() {
    return getValue();
  }

  /**
   * Return the header value.
   * 
   * @return the header value
   */
  public abstract H getValue();

  /**
   * Handle a browser event that took place within the header.
   * 
   * @param context the context of the header
   * @param elem the parent Element
   * @param event the native browser event
   */
  public void onBrowserEvent(Context context, Element elem, NativeEvent event) {
    cell.onBrowserEvent(context, elem, getValue(), event, updater);
  }

  /**
   * Preview a browser event that may trigger a column sort event. Return true if the
   * {@link CellTable} should proceed with sorting the column. Subclasses can override this method
   * to disable column sort for some click events, or particular header/footer sections.
   * <p>
   * This method will be invoked even if the header's cell does not consume a click event.
   * </p>
   * 
   * @param context the context of the header
   * @param elem the parent Element
   * @param event the native browser event
   * @return true if the {@link CellTable} should continue respond to the event (i.e., if this is
   *         a click event on a sortable column's header, fire {@link ColumnSortEvent}). False if
   *         the {@link CellTable} should stop respond to the event. 
   */
  public boolean onPreviewColumnSortEvent(Context context, Element elem, NativeEvent event) {
    return true;
  }

  /**
   * Render the header.
   * 
   * @param context the context of the header
   * @param sb a {@link SafeHtmlBuilder} to render into
   */
  public void render(Context context, SafeHtmlBuilder sb) {
    cell.render(context, getValue(), sb);
  }

  /**
   * Set extra style names that should be applied to every cell in this header.
   * 
   * <p>
   * If you want to apply style names based on the header value, override
   * {@link #getHeaderStyleNames(Object)} directly.
   * </p>
   * 
   * @param styleNames the extra style names to apply in a space-separated list,
   *          or {@code null} if there are no extra styles for this cell
   */
  public void setHeaderStyleNames(String styleNames) {
    this.headerStyleNames = styleNames;
  }
    
  /**
   * Set the {@link ValueUpdater}.
   * 
   * @param updater the value updater to use
   */
  public void setUpdater(ValueUpdater<H> updater) {
    this.updater = updater;
  }
}
