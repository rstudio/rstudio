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
package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import java.util.Collection;
import java.util.Iterator;

/**
 * Suggestion picker, used in the implementation of
 * {@link com.google.gwt.user.client.ui.SuggestBox}.
 */
public class SuggestPickerImpl extends AbstractItemPickerImpl {

  /**
   * Default style for the picker.
   */
  private static final String STYLENAME_DEFAULT = "gwt-SuggestBoxPopup";
  private final boolean asHTML;
  private int startInvisible = Integer.MAX_VALUE;

  /**
   * Constructor for <code>SuggestPickerImpl</code>.
   * 
   * @param asHTML flag used to indicate how to treat {@link Suggestion} display
   *          strings
   */
  public SuggestPickerImpl(boolean asHTML) {
    this.asHTML = asHTML;
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
   * @param suggestions suggestions for this picker
   */
  public final void setItems(Collection suggestions) {
    setItems(suggestions.iterator());
  }

  protected native Element getRow(Element elem, int row)/*-{
   return elem.rows[row];
   }-*/;

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
      addItem(item, true);
    }
    return getItem(itemIndex);
  }

  /**
   * Sets the suggestions associated with this picker.
   */
  private final void setItems(Iterator suggestions) {
    int itemCount = 0;

    // Ensure all needed items exist and set each item's html to the given
    // suggestion.
    while (suggestions.hasNext()) {
      Item item = ensureItem(itemCount);
      Suggestion suggestion = (Suggestion) suggestions.next();
      String display = suggestion.getDisplayString();
      if (asHTML) {
        DOM.setInnerHTML(item.getElement(), display);
      } else {
        DOM.setInnerText(item.getElement(), display);
      }
      item.setValue(suggestion.getValue());
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
    UIObject.setVisible(getRow(body, itemIndex), visible);
  }
}
