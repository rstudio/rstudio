/*
 * Copyright 2007 Google Inc.
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
import java.util.Iterator;

/**
 * Suggestion picker. Each "item" represents a suggestion.
 */
public class SuggestPicker extends AbstractItemPicker {

  /**
   * Default style for the picker.
   */
  private static final String STYLENAME_DEFAULT = "gwt-SuggestPicker";

  private int startInvisible = Integer.MAX_VALUE;

  /**
   * Constructor for <code>SuggestPicker</code>.
   */
  public SuggestPicker() {
    setStyleName(STYLENAME_DEFAULT);
  }

  public boolean delegateKeyDown(char keyCode) {
    if (isAttached()) {
      switch (keyCode) {
        case KeyboardListener.KEY_DOWN:
          shiftSelection(1);
          return true;
        case KeyboardListener.KEY_UP:
          shiftSelection(-1);
          return true;
        case KeyboardListener.KEY_ENTER:
          commitSelection();
          return true;
      }
    }
    return false;
  }

  public int getItemCount() {
    if (startInvisible == Integer.MAX_VALUE) {
      return 0;
    } else {
      return startInvisible;
    }
  }

  /**
   * Sets the suggestions associated with this picker.
   * 
   * @param suggestions suggestions for this picker; the suggestions must have
   *          valid {@link String#toString()} methods
   */
  public final void setItems(Collection suggestions) {
    setItems(suggestions.iterator());
  }

  void shiftSelection(int shift) {
    int newSelect = getSelectedIndex() + shift;
    if (newSelect >= super.getItemCount() || newSelect < 0
        || newSelect >= startInvisible) {
      return;
    }
    setSelection(getItem(newSelect));
  }

  /**
   * Ensures the existence of the given item and returns it.
   * 
   * @param itemIndex item index to ensure
   * @return associated item
   */
  private Item ensureItem(int itemIndex) {
    for (int i = super.getItemCount(); i <= itemIndex; i++) {
      Item item = new Item(i);
      getLayout().setWidget(i, 0, item);
    }
    return getItem(itemIndex);
  }

  /**
   * Sets the suggestions associated with this picker.
   * 
   */
  private final void setItems(Iterator suggestions) {
    int itemCount = 0;

    // Ensure all needed items exist and set each item's html to the given
    // suggestion.
    while (suggestions.hasNext()) {
      Item item = ensureItem(itemCount);
      format(item.getElement(), suggestions.next());
      ++itemCount;
    }

    if (itemCount == 0) {
      throw new IllegalStateException(
          "Must set at least one item in a SuggestPicker");
    }

    // Render visible all needed cells.
    int min = Math.min(itemCount, super.getItemCount());
    for (int i = startInvisible; i < min; i++) {
      setVisible(i, true);
    }

    // Render invisible all useless cells.
    startInvisible = itemCount;
    for (int i = itemCount; i < super.getItemCount(); i++) {
      setVisible(i, false);
    }
  }

  /**
   * Sets whether the given item is visible.
   * 
   * @param itemIndex item index
   * @param visible visible boolean
   */
  private void setVisible(int itemIndex, boolean visible) {
    UIObject.setVisible(getLayout().getRowFormatter().getElement(itemIndex),
        visible);
  }

}