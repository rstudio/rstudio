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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helpful base implementation of the {@link ItemPicker} interface.
 */
abstract class AbstractItemPicker extends Composite implements ItemPicker {
  /*
   * Implementation note:AbstractItemPicker is package protected because we are
   * hoping we might eventually be able to slip in a more efficient
   * implementation of this class, so do not want to be bound by this
   * implementation.
   */

  /**
   * Selectable item.
   */
  class Item extends Widget {
    private int index;

    /**
     * 
     * Constructor for <code>Item</code>.
     * 
     * @param index index associated with item
     */
    Item(int index) {
      setElement(DOM.createDiv());
      sinkEvents(Event.ONMOUSEDOWN | Event.ONMOUSEOVER);
      this.index = index;
      this.setStyleName(itemStyleName);
      items.add(index, this);
    }

    public void onBrowserEvent(Event event) {
      switch (DOM.eventGetType(event)) {
        case Event.ONMOUSEOVER:
          setSelection(this);
          break;
        case Event.ONMOUSEDOWN:
          commitSelection();
          break;
      }
    }

    public String toString() {
      return "value: " + getValue(getIndex()) + " index: " + this.getIndex();
    }

    /**
     * Gets the index of the item.
     * 
     * @return the item's index
     */
    int getIndex() {
      return index;
    }

    AbstractItemPicker getOwner() {
      return AbstractItemPicker.this;
    }
  }

  private static final String STYLENAME_SELECTED_ITEM = "selected";
  private static final String STYLENAME_ITEM = "item";

  private ChangeListenerCollection changeListeners = new ChangeListenerCollection();
  private Item selectedItem;
  private final String selectedStyleName;
  private final String itemStyleName;
  private final ArrayList items;

  /**
   * Constructor for <code>ItemPicker</code>. Provides "item" as the default
   * item style name, and "selected" as the default selected item style name.
   */
  public AbstractItemPicker() {
    this(STYLENAME_ITEM, STYLENAME_SELECTED_ITEM);
  }

  /**
   * 
   * Constructor for <code>ItemPicker</code>.
   * 
   * @param itemStyleName CSS style name for default items
   * @param selectedItemStyleName CSS style name for the currently selected item
   */
  public AbstractItemPicker(String itemStyleName, String selectedItemStyleName) {
    initWidget(new FlexTable());
    this.selectedStyleName = selectedItemStyleName;
    this.itemStyleName = itemStyleName;

    // CSS does not effect padding and spacing correctly. So setting to 0
    // here.
    getLayout().setCellPadding(0);
    getLayout().setCellSpacing(0);
    items = new ArrayList();
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
    int index = getSelectedIndex();
    if (index == -1) {
      return null;
    }
    return getValue(index);
  }

  public Object getValue(int index) {
    return getValue(getItem(index).getElement());
  }

  public final void removeChangeListener(ChangeListener listener) {
    this.changeListeners.remove(listener);
  }

  public abstract void setItems(Collection items);

  public final void setSelectedIndex(int index) {
    Item item = getItem(index);
    setSelection(item);
  }

  /**
   * Formats the displayed element using the information given by the user
   * supplied item.
   * 
   * @param displayedElement the element used to the display the item
   * @param item the user supplied item information
   */
  protected void format(Element displayedElement, Object item) {
    DOM.setInnerHTML(displayedElement, item.toString());
  }

  /**
   * Gets the value from a given element. By default this method is used by
   * {@link AbstractItemPicker#getValue(int)} to compute the value that should
   * be returned to the user.
   * 
   * @param displayedElement displayed element
   * @return the value associated with the given displayed element
   */
  protected Object getValue(Element displayedElement) {
    return DOM.getInnerText(displayedElement);
  }

  void clearItems() {
    items.clear();
    getLayout().clear();
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

  FlexTable getLayout() {
    return (FlexTable) getWidget();
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
      selectedItem.removeStyleName(selectedStyleName);
      selectedItem.addStyleName(itemStyleName);
    }

    // Add new selected item.
    selectedItem = item;
    if (selectedItem != null) {
      selectedItem.removeStyleName(itemStyleName);
      selectedItem.addStyleName(selectedStyleName);
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

}
