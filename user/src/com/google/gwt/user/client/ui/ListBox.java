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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.core.client.GWT;

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

  /**
   * ListBox implementation for all browsers except Safari. This implementation
   * relies on the JavaScript Select object and its 'options' array.
   */
  private static class Impl {

    public native void clear(Element select) /*-{
      select.options.length = 0;
    }-*/;

    public native int getItemCount(Element select) /*-{
      return select.options.length;
    }-*/;

    public native String getItemText(Element select, int index) /*-{
      return select.options[index].text;
    }-*/;

    public native String getItemValue(Element select, int index) /*-{
      return select.options[index].value;
    }-*/;

    public native boolean isItemSelected(Element select, int index) /*-{
      return select.options[index].selected;
    }-*/;

    public native void removeItem(Element select, int index) /*-{
      select.options[index] = null;
    }-*/;

    public native void setItemSelected(Element select, int index,
                                       boolean selected) /*-{
      select.options[index].selected = selected;
    }-*/;

    public native void setValue(Element select, int index, String value) /*-{
      select.options[index].value = value;
    }-*/;
  }

  /**
   * ListBox implementation for Safari. The 'options' array cannot be used
   * due to a bug in the version of WebKit that ships with GWT
   * (http://bugs.webkit.org/show_bug.cgi?id=10472).
   * The 'children' array, which is common for all DOM elements in Safari,
   * does not suffer from the same problem. Ideally, the 'children'
   * array should be used in all of the traversal methods in the DOM classes.
   * Unfortunately, due to a bug in Safari 2
   * (http://bugs.webkit.org/show_bug.cgi?id=3330), this will not work.
   * However, this bug does not cause problems in the case of <SELECT>
   * elements, because their descendent elements are only one level deep.
   */
  private static class ImplSafari extends Impl {

    public native void clear(Element select) /*-{
      select.innerText = '';
    }-*/;

    public native int getItemCount(Element select) /*-{
      return select.children.length;
    }-*/;

    public native String getItemText(Element select, int index) /*-{
      return select.children[index].text;
    }-*/;

    public native String getItemValue(Element select, int index) /*-{
      return select.children[index].value;
    }-*/;

    public native boolean isItemSelected(Element select, int index) /*-{
      return select.children[index].selected;
    }-*/;

    public native void removeItem(Element select, int index) /*-{
      select.removeChild(select.children[index]);
    }-*/;

    public native void setItemSelected(Element select, int index,
                                       boolean selected) /*-{
      select.children[index].selected = selected;
    }-*/;

    public native void setValue(Element select, int index, String value) /*-{
      select.children[index].value = value;
    }-*/;
  }

  private static final int INSERT_AT_END = -1;
  private static final Impl impl = (Impl) GWT.create(Impl.class);
  private ChangeListenerCollection changeListeners;

  /**
   * Creates an empty list box in single selection mode.
   */
  public ListBox() {
    this(false);
  }

  /**
   * Creates an empty list box. The preferred way to enable multiple selections
   * is to use this constructor rather than {@link #setMultipleSelect(boolean)}.
   * 
   * @param isMultipleSelect specifies if multiple selection is enabled
   */
  public ListBox(boolean isMultipleSelect) {
    super(DOM.createSelect(isMultipleSelect));
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
   * Adds an item to the list box. This method has the same effect as
   * 
   * <pre>
   * addItem(item, item)
   * </pre>
   * 
   * @param item the text of the item to be added
   */
  public void addItem(String item) {
    insertItem(item, INSERT_AT_END);
  }

  /**
   * Adds an item to the list box, specifying an initial value for the item.
   * 
   * @param item the text of the item to be added
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}; cannot be <code>null</code>
   */
  public void addItem(String item, String value) {
    insertItem(item, value, INSERT_AT_END);
  }

  /**
   * Removes all items from the list box.
   */
  public void clear() {
    impl.clear(getElement());
  }

  /**
   * Gets the number of items present in the list box.
   * 
   * @return the number of items
   */
  public int getItemCount() {
    return impl.getItemCount(getElement());
  }

  /**
   * Gets the text associated with the item at the specified index.
   * 
   * @param index the index of the item whose text is to be retrieved
   * @return the text associated with the item
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public String getItemText(int index) {
    checkIndex(index);
    return impl.getItemText(getElement(), index);
  }

  public String getName() {
    return DOM.getElementProperty(getElement(), "name");
  }

  /**
   * Gets the currently-selected item. If multiple items are selected, this
   * method will return the first selected item ({@link #isItemSelected(int)}
   * can be used to query individual items).
   * 
   * @return the selected index, or <code>-1</code> if none is selected
   */
  public int getSelectedIndex() {
    return DOM.getElementPropertyInt(getElement(), "selectedIndex");
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
    return impl.getItemValue(getElement(), index);
  }

  /**
   * Gets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @return the visible item count
   */
  public int getVisibleItemCount() {
    return DOM.getElementPropertyInt(getElement(), "size");
  }

  /**
   * Inserts an item into the list box. Has the same effect as
   * 
   * <pre>
   * insertItem(item, item, index)
   * </pre>
   * 
   * @param item the text of the item to be inserted
   * @param index the index at which to insert it
   */
  public void insertItem(String item, int index) {
    insertItem(item, item, index);
  }

  /**
   * Inserts an item into the list box, specifying an initial value for the
   * item. If the index is less than zero, or greater than or equal to
   * the length of the list, then the item will be appended to the end of
   * the list.
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
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public boolean isItemSelected(int index) {
    checkIndex(index);
    return impl.isItemSelected(getElement(), index);
  }

  /**
   * Gets whether this list allows multiple selection.
   * 
   * @return <code>true</code> if multiple selection is allowed
   */
  public boolean isMultipleSelect() {
    return DOM.getElementPropertyBoolean(getElement(), "multiple");
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
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void removeItem(int index) {
    checkIndex(index);
    impl.removeItem(getElement(), index);
  }

  /**
   * Sets whether an individual list item is selected.
   * 
   * <p>
   * Note that setting the selection programmatically does <em>not</em> cause
   * the {@link ChangeListener#onChange(Widget)} event to be fired.
   * </p>
   * 
   * @param index the index of the item to be selected or unselected
   * @param selected <code>true</code> to select the item
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setItemSelected(int index, boolean selected) {
    checkIndex(index);
    impl.setItemSelected(getElement(), index, selected);
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
   * Sets whether this list allows multiple selections. <em>NOTE: The preferred
   * way of enabling multiple selections in a list box is by using the
   * {@link #ListBox(boolean)} constructor. Using this method can spuriously
   * fail on Internet Explorer 6.0.</em>
   * 
   * @param multiple <code>true</code> to allow multiple selections
   */
  public void setMultipleSelect(boolean multiple) {
    // TODO: we can remove the above doc admonition once we address issue 1007
    DOM.setElementPropertyBoolean(getElement(), "multiple", multiple);
  }

  public void setName(String name) {
    DOM.setElementProperty(getElement(), "name", name);
  }

  /**
   * Sets the currently selected index.
   * 
   * <p>
   * Note that setting the selected index programmatically does <em>not</em>
   * cause the {@link ChangeListener#onChange(Widget)} event to be fired.
   * </p>
   * 
   * @param index the index of the item to be selected
   */
  public void setSelectedIndex(int index) {
    DOM.setElementPropertyInt(getElement(), "selectedIndex", index);
  }

  /**
   * Sets the value associated with the item at a given index. This value can be
   * used for any purpose, but is also what is passed to the server when the
   * list box is submitted as part of a {@link FormPanel}.
   * 
   * @param index the index of the item to be set
   * @param value the item's new value; cannot be <code>null</code>
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setValue(int index, String value) {
    checkIndex(index);   
    impl.setValue(getElement(), index, value);
  }

  /**
   * Sets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @param visibleItems the visible item count
   */
  public void setVisibleItemCount(int visibleItems) {
    DOM.setElementPropertyInt(getElement(), "size", visibleItems);
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= getItemCount()) {
      throw new IndexOutOfBoundsException();
    }
  }
}
