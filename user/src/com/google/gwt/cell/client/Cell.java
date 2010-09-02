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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.Set;

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
   * Check if this cell depends on the selection state.
   *
   * @return true if dependent on selection, false if not
   */
  boolean dependsOnSelection();

  /**
   * <p>
   * Get the set of events that this cell consumes. The container that uses this
   * cell should only pass these events to {@link #onBrowserEvent(Element,
   * Object, Object, NativeEvent, ValueUpdater)}.
   * </p>
   * <p>
   * The returned value should not be modified, and may be an unmodifiable set.
   * Changes to the return value may not be reflected in the cell.
   * </p>
   *
   * @return the consumed events, or null if no events are consumed
   */
  Set<String> getConsumedEvents();

  /**
   * Check if this cell handles selection. If the cell handles selection, then
   * its container should not automatically handle selection.
   *
   * @return true if the cell handles selection, false if not
   */
  boolean handlesSelection();

  /**
   * Returns true if the cell is currently editing the data identified by the
   * given element and key. While a cell is editing, widgets containing the cell
   * may chooses to pass keystrokes directly to the cell rather than using them
   * for navigation purposes.
   *
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param key the unique key associated with the row object
   */
  boolean isEditing(Element parent, C value, Object key);

  /**
   * Handle a browser event that took place within the cell. The default
   * implementation returns null.
   *
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param key the unique key associated with the row object
   * @param event the native browser event
   * @param valueUpdater a {@link ValueUpdater}, or null if not specified
   */
  void onBrowserEvent(Element parent, C value, Object key, NativeEvent event,
      ValueUpdater<C> valueUpdater);

  /**
   * Render a cell as HTML into a StringBuilder, suitable for passing to
   * {@link Element#setInnerHTML} on a container element.
   * @param value the cell value to be rendered
   * @param key the unique key associated with the row object
   * @param sb the {@link SafeHtmlBuilder} to be written to
   */
  void render(C value, Object key, SafeHtmlBuilder sb);

  /**
   * This method may be used by cell containers to set the value on a single
   * cell directly, rather than using {@link Element#setInnerHTML(String)}. See
   * {@link AbstractCell#setValue(Element, Object, Object)} for a default
   * implementation that uses {@link #render(Object, Object, SafeHtmlBuilder)}.
   *
   * @param parent the parent Element
   * @param value the value associated with the cell
   * @param key the unique key associated with the row object
   */
  void setValue(Element parent, C value, Object key);
}
