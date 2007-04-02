/*
 * Copyright 2006 Google Inc.
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

package com.google.gwt.user.client.ui;

import java.util.Collection;

/**
 * Represents a pickable list of items. Each {@link ItemPicker} should be able
 * to respond to mouse and keyboard events.
 * 
 */
public interface ItemPicker extends SourcesChangeEvents {
  
  /**
   * Commits the current selection. Any relevant change listeners are fired.
   */
  public void commitSelection();

  /**
   * Allows the {@link ItemPicker} to be controlled via keyboard input. This
   * method should be hooked up to an appropriate
   * {@link KeyboardListener#onKeyDown(Widget, char, int)} method.
   * 
   * @param keyCode key code
   * @return <code>true</code> if the key code was consumed by the picker,
   *         <code>false</code> otherwise
   */
  public boolean delegateKeyDown(char keyCode);

  /**
   * Gets the number of items.
   * 
   * @return number of items
   */
  public int getItemCount();

  /**
   * Gets the currently selected index.
   * 
   * @return selected index, or -1 if no index is selected
   */
  public int getSelectedIndex();

  /**
   * Gets the value associated with the currently selected index.
   * 
   * <p>
   * The value should be convertible into a human readable {@link String} by
   * calling the {@link String#toString()} method.
   * </p>
   * 
   * @return current selected value, or null if no value is selected
   */
  public Object getSelectedValue();

  /**
   * Gets the value associated with the given index.
   * <p>
   * The value should be convertible into a human readable {@link String} by
   * calling the {@link String#toString()}.
   * </p>
   * 
   * @param index index
   * @return the value associated with <code>index</code>.
   */
  // Design note: Object is returned rather than String in order to allow the
  // user to return type-safe enumerations.
  public Object getValue(int index);

  /**
   * Sets the items to be displayed. The expected type of each item should be
   * clearly documented in each class which implements this interface.  
   * 
   * @param items items to be displayed
   */
  public void setItems(Collection items);

  /**
   * Sets the currently selected index.
   * 
   * @param index new selected index
   */
  public void setSelectedIndex(int index);

}