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

package com.google.gwt.user.client.ui.impl;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helpful base implementation of the {@link ItemPicker} interface.
 */
abstract class AbstractItemPickerImpl extends Widget {
  /**
   * Selectable item.
   */
  class Item extends UIObject {
    private int index;
    private Object value;

    /**
     * Constructor for <code>Item</code>.
     * 
     * @param index index associated with item
     */
    Item(int index) {
      setElement(DOM.createTD());
      this.index = index;
      this.setStyleName(STYLENAME_ITEM);
      items.add(index, this);
    }

    public String toString() {
      return "value: " + this.getValue() + " index: " + this.getIndex();
    }

    /**
     * Gets the index of the item.
     * 
     * @return the item's index
     */
    int getIndex() {
      return index;
    }

    AbstractItemPickerImpl getOwner() {
      return AbstractItemPickerImpl.this;
    }

    Object getValue() {
      return value;
    }

    void setValue(Object value) {
      this.value = value;
    }
  }

  private static final String STYLENAME_SELECTED_ITEM = "selected";
  private static final String STYLENAME_ITEM = "item";

  final Element body;
  private Element currentTR;
  private final ArrayList items;
  private ChangeListenerCollection changeListeners = new ChangeListenerCollection();
  private Item selectedItem;

  public AbstractItemPickerImpl() {
    items = new ArrayList();
    Element table = DOM.createTable();
    body = DOM.createTBody();
    DOM.appendChild(table, body);
    setElement(table);
    sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT);
    DOM.setIntAttribute(table, "cellPadding", 0);
    DOM.setIntAttribute(table, "cellSpacing", 0);
  }

  public final void addChangeListener(ChangeListener listener) {
    if (changeListeners == null) {
      changeListeners = new ChangeListenerCollection();
    }
    changeListeners.add(listener);
  }

  public void commitSelection() {
    if (selectedItem == null) {
      throw new IllegalStateException("No element is selected");
    }
    changeListeners.fireChange(this);
  }

  public abstract boolean delegateKeyDown(char keyCode);

  public int getItemCount() {
    return items.size();
  }

  public final int getSelectedIndex() {
    Item item = getSelectedItem();
    if (item == null) {
      return -1;
    }
    return item.getIndex();
  }

  public final Object getSelectedValue() {
    Item item = getSelectedItem();
    if (item == null) {
      return null;
    } else {
      return item.getValue();
    }
  }

  public void onBrowserEvent(Event event) {
    super.onBrowserEvent(event);
    Item item = findItem(DOM.eventGetTarget(event));
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        commitSelection();
        break;
      }
      case Event.ONMOUSEOVER: {
        if (item != null) {
          setSelection(item);
        }
        break;
      }
      case Event.ONMOUSEOUT: {
        if (item != null) {
          setSelection(null);
        }
        break;
      }
    }
  }

  public final void removeChangeListener(ChangeListener listener) {
    this.changeListeners.remove(listener);
  }

  public abstract void setItems(Collection items);

  public final void setSelectedIndex(int index) {
    Item item = getItem(index);
    setSelection(item);
  }

  void addItem(Item item, boolean vertical) {
    if (vertical) {
      currentTR = DOM.createTR();
      DOM.appendChild(body, currentTR);
    }
    DOM.appendChild(currentTR, item.getElement());
  }

  void clearItems() {
    items.clear();
  }

  /**
   * Gets the ith item.
   * 
   * @param index index of item
   * @return the ith item
   */
  Item getItem(int index) {
    return (Item) items.get(index);
  }

  /**
   * Gets the currently selected item.
   * 
   * @return selected item
   */
  Item getSelectedItem() {
    return selectedItem;
  }

  /**
   * Sets the current selection.
   * 
   * @param item item to set
   */
  void setSelection(Item item) {
    if (selectedItem == item) {
      return;
    }

    // Remove old selected item.
    if (selectedItem != null) {
      selectedItem.removeStyleName(STYLENAME_SELECTED_ITEM);
      selectedItem.addStyleName(STYLENAME_ITEM);
    }

    // Add new selected item.
    selectedItem = item;
    if (selectedItem != null) {
      selectedItem.removeStyleName(STYLENAME_ITEM);
      selectedItem.addStyleName(STYLENAME_SELECTED_ITEM);
    }
  }

  /**
   * Shifts the current selection by the given amount, unless that would make
   * the selection invalid.
   * 
   * @param shift the amount to shift the current selection by
   */
  void shiftSelection(int shift) {
    int newIndex = getSelectedIndex() + shift;
    if (newIndex < 0 || newIndex >= getItemCount()) {
      return;
    } else {
      Item item = getItem(newIndex);
      setSelection(item);
    }
  }

  private Item findItem(Element hItem) {
    for (int i = 0; i < items.size(); ++i) {
      Item item = (Item) items.get(i);
      if (DOM.isOrHasChild(item.getElement(), hItem)) {
        return item;
      }
    }

    return null;
  }
}
