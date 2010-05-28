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
package com.google.gwt.cell.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A light weight representation of a renderable object.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * @param <C> the type that this Cell represents
 */
public interface Cell<C> {

  /**
   * Returns true if the cell is interested in browser events. The default
   * implementation returns false.
   */
  public abstract boolean consumesEvents();

  /**
   * Check if this cell depends on the selection state.
   * 
   * @return true if dependant on selection, false if not
   */
  public abstract boolean dependsOnSelection();

  /**
   * Handle a browser event that took place within the cell. The default
   * implementation returns null.
   * 
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param viewData the view data associated with the cell, or null
   * @param event the native browser event
   * @param valueUpdater a {@link ValueUpdater}, or null
   * @return a view data object which may be the one passed in or a new object
   */
  public abstract Object onBrowserEvent(Element parent, C value,
      Object viewData, NativeEvent event, ValueUpdater<C> valueUpdater);

  /**
   * Render a cell as HTML into a StringBuilder, suitable for passing to
   * {@link Element#setInnerHTML} on a container element.
   * 
   * @param value the cell value to be rendered
   * @param viewData view data associated with the cell
   * @param sb the StringBuilder to be written to
   */
  public abstract void render(C value, Object viewData, StringBuilder sb);

  /**
   * This method may be used by cell containers to set the value on a single
   * cell directly, rather than using {@link Element#setInnerHTML(String)}. See
   * {@link AbstractCell#setValue(Element, Object, Object)} for a default
   * implementation that uses {@link #render(Object, Object, StringBuilder)}.
   * 
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param viewData the view data associated with the cell, or null
   */
  public abstract void setValue(Element parent, C value, Object viewData);
}
