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
package com.google.gwt.bikeshed.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A light weight representation of a renderable object.
 *
 * @param <C> the type that this Cell represents
 * @param <V> the type of view data that this cell consumes
 */
public abstract class Cell<C, V> {

  /**
   * Returns true if the cell is interested in browser events. The default
   * implementation returns false.
   */
  public boolean consumesEvents() {
    return false;
  }

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
  public V onBrowserEvent(Element parent, C value, V viewData,
      NativeEvent event, ValueUpdater<C, V> valueUpdater) {
    return null;
  }

  /**
   * Render a cell as HTML into a StringBuilder, suitable for passing to
   * {@link Element#setInnerHTML} on a container element.
   *
   * @param value the cell value to be rendered
   * @param viewData view data associated with the cell
   * @param sb the StringBuilder to be written to
   */
  // TODO: render needs a way of assuming text by default, but allowing HTML
  public abstract void render(C value, V viewData, StringBuilder sb);

  public void setValue(Element parent, C value, V viewData) {
    StringBuilder sb = new StringBuilder();
    render(value, viewData, sb);
    parent.setInnerHTML(sb.toString());
  }
}
