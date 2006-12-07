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

/**
 * A widget that presents a list of choices to the user, either as a list box or
 * as a drop-down list.
 * 
 * <p>
 * <img class='gallery' src='ListBox.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-ListBox { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.ListBoxExample}
 * </p>
 */
public class ListBox extends FocusWidget implements SourcesChangeEvents,
    HasName {

  private static final int INSERT_AT_END = -1;
  private ChangeListenerCollection changeListeners;

  /**
   * Creates an empty list box.
   */
  public ListBox() {
    super(DOM.createSelect());
    sinkEvents(Event.ONCHANGE);
    setStyleName("gwt-ListBox");
  }

  public void addChangeListener(ChangeListener listener) {
    if (changeListeners == null) {
      changeListeners = new ChangeListenerCollection();
    }
    changeListeners.add(listener);
  }

  /**
   * Adds an item to the list box.
   * 
   * @param item the text of the item to be added
   */
  public void addItem(String item) {
    insertItem(item, INSERT_AT_END);
  }

  /**
   * Adds an item to the list box.
   * 
   * @param item the text of the item to be added
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}.
   */
  public void addItem(String item, String value) {
    insertItem(item, value, INSERT_AT_END);
  }

  /**
   * Removes all items from the list box.
   */
  public void clear() {
    Element h = getElement();
    while (DOM.getChildCount(h) > 0) {
      DOM.removeChild(h, DOM.getChild(h, 0));
    }
  }

  /**
   * Gets the number of items present in the list box.
   * 
   * @return the number of items
   */
  public int getItemCount() {
    return DOM.getChildCount(getElement());
  }

  /**
   * Gets the text associated with the item at the specified index.
   * 
   * @param index the index of the item whose text is to be retrieved
   * @return the text associated with the item
   */
  public String getItemText(int index) {
    Element child = DOM.getChild(getElement(), index);
    return DOM.getInnerText(child);
  }

  public String getName() {
    return DOM.getAttribute(getElement(), "name");
  }

  /**
   * Gets the currently-selected item. If multiple items are selected, this
   * method will return the first selected item ({@link #isItemSelected(int)}
   * can be used to query individual items).
   * 
   * @return the selected index, or <code>-1</code> if none is selected
   */
  public int getSelectedIndex() {
    return DOM.getIntAttribute(getElement(), "selectedIndex");
  }

  /**
   * Gets the value associated with the item at a given index.
   * 
   * @param index the index of the item to be retrieved
   * @return the item's associated value
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public String getValue(int index) {
    checkIndex(index);

    Element option = DOM.getChild(getElement(), index);
    return DOM.getAttribute(option, "value");
  }

  /**
   * Gets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @return the visible item count
   */
  public int getVisibleItemCount() {
    return DOM.getIntAttribute(getElement(), "size");
  }

  /**
   * Inserts an item into the list box.
   * 
   * @param item the text of the item to be inserted
   * @param index the index at which to insert it
   */
  public void insertItem(String item, int index) {
    insertItem(item, null, index);
  }

  /**
   * Inserts an item into the list box.
   * 
   * @param item the text of the item to be inserted
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}.
   * @param index the index at which to insert it
   */
  public void insertItem(String item, String value, int index) {
    DOM.insertListItem(getElement(), item, value, index);
  }

  /**
   * Determines whether an individual list item is selected.
   * 
   * @param index the index of the item to be tested
   * @return <code>true</code> if the item is selected
   */
  public boolean isItemSelected(int index) {
    checkIndex(index);

    Element option = DOM.getChild(getElement(), index);
    return DOM.getBooleanAttribute(option, "selected");
  }

  /**
   * Gets whether this list allows multiple selection.
   * 
   * @return <code>true</code> if multiple selection is allowed
   */
  public boolean isMultipleSelect() {
    return DOM.getBooleanAttribute(getElement(), "multiple");
  }

  public void onBrowserEvent(Event event) {
    if (DOM.eventGetType(event) == Event.ONCHANGE) {
      if (changeListeners != null) {
        changeListeners.fireChange(this);
      }
    } else {
      super.onBrowserEvent(event);
    }
  }

  public void removeChangeListener(ChangeListener listener) {
    if (changeListeners != null) {
      changeListeners.remove(listener);
    }
  }

  /**
   * Removes the item at the specified index.
   * 
   * @param index the index of the item to be removed
   */
  public void removeItem(int index) {
    Element child = DOM.getChild(getElement(), index);
    DOM.removeChild(getElement(), child);
  }

  /**
   * Sets whether an individual list item is selected.
   * 
   * @param index the index of the item to be selected or unselected
   * @param selected <code>true</code> to select the item
   */
  public void setItemSelected(int index, boolean selected) {
    checkIndex(index);

    Element option = DOM.getChild(getElement(), index);
    DOM.setBooleanAttribute(option, "selected", selected);
  }

  /**
   * Sets the text associated with the item at a given index.
   * 
   * @param index the index of the item to be set
   * @param text the item's new text
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setItemText(int index, String text) {
    checkIndex(index);
    if (text == null) {
      throw new NullPointerException("Cannot set an option to have null text");
    }
    DOM.setOptionText(getElement(), text, index);
  }

  /**
   * Sets whether this list allows multiple selections.
   * 
   * @param multiple <code>true</code> to allow multiple selections
   */
  public void setMultipleSelect(boolean multiple) {
    DOM.setBooleanAttribute(getElement(), "multiple", multiple);
  }

  public void setName(String name) {
    DOM.setAttribute(getElement(), "name", name);
  }

  /**
   * Sets the currently selected index.
   * 
   * @param index the index of the item to be selected
   */
  public void setSelectedIndex(int index) {
    DOM.setIntAttribute(getElement(), "selectedIndex", index);
  }

  /**
   * Sets the value associated with the item at a given index. This value can be
   * used for any purpose, but is also what is passed to the server when the
   * ListBox is submitted as part of a {@link FormPanel}.
   * 
   * @param index the index of the item to be set
   * @param value the item's new value
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setValue(int index, String value) {
    checkIndex(index);

    Element option = DOM.getChild(getElement(), index);
    DOM.setAttribute(option, "value", value);
  }

  /**
   * Sets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @param visibleItems the visible item count
   */
  public void setVisibleItemCount(int visibleItems) {
    DOM.setIntAttribute(getElement(), "size", visibleItems);
  }

  private void checkIndex(int index) {
    Element elem = getElement();
    if ((index < 0) || (index >= DOM.getChildCount(elem))) {
      throw new IndexOutOfBoundsException();
    }
  }
}
