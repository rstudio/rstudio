/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.BidiFormatter;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;

/**
 * A widget that presents a list of choices to the user, either as a list box or
 * as a drop-down list.
 * 
 * <p>
 * <img class='gallery' src='doc-files/ListBox.png'/>
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
 *
 * <p>
 * <h3>Built-in Bidi Text Support</h3>
 * This widget is capable of automatically adjusting its direction according to
 * its content. This feature is controlled by {@link #setDirectionEstimator},
 * and is off by default.
 * </p>
 *
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * The items of a ListBox element are laid out in &lt;g:item> elements.
 * Each item contains text that will be added to the list of available
 * items that will be shown, either in the drop down or list. (Note that
 * the tags of the item elements are not capitalized. This is meant to
 * signal that the item is not a runtime object, and so cannot have a
 * <code>ui:field</code> attribute.) It is also possible to explicitly
 * specify item's value using value attribute as shown below.
 * <p>
 * For example:
 * 
 * <pre>
 * &lt;g:ListBox>
 *  &lt;g:item>
 *    first
 *  &lt;/g:item>
 *  &lt;g:item value='2'>
 *    second
 *  &lt;/g:item>
 * &lt;/g:ListBox>
 * </pre>
 * <p>
 * <h3>Important usage note</h3>
 * <b>Subclasses should neither read nor write option text directly from the
 * option elements created by this class, since such text may need to be wrapped
 * in Unicode bidi formatting characters. They can use the getOptionText and/or
 * setOptionText methods for this purpose instead.</b>
 */
@SuppressWarnings("deprecation")
public class ListBox extends FocusWidget implements SourcesChangeEvents,
    HasChangeHandlers, HasName, HasDirectionEstimator {

  public static final DirectionEstimator DEFAULT_DIRECTION_ESTIMATOR =
    WordCountDirectionEstimator.get();

  private static final String BIDI_ATTR_NAME = "bidiwrapped";
  
  private static final int INSERT_AT_END = -1;

  /**
   * Creates a ListBox widget that wraps an existing &lt;select&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   * @return list box
   */
  public static ListBox wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    ListBox listBox = new ListBox(element);

    // Mark it attached and remember it for cleanup.
    listBox.onAttach();
    RootPanel.detachOnWindowClose(listBox);

    return listBox;
  }

  private DirectionEstimator estimator;

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
    super(Document.get().createSelectElement(isMultipleSelect));
    setStyleName("gwt-ListBox");
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;select&gt; element.
   * 
   * @param element the element to be used
   */
  protected ListBox(Element element) {
    super(element);
    SelectElement.as(element);
  }

  public HandlerRegistration addChangeHandler(ChangeHandler handler) {
    return addDomHandler(handler, ChangeEvent.getType());
  }

  /**
   * @deprecated Use {@link #addChangeHandler} instead
   */
  @Deprecated
  public void addChangeListener(ChangeListener listener) {
    ListenerWrapper.WrappedChangeListener.add(this, listener);
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
   * Adds an item to the list box, specifying its direction. This method has the
   * same effect as
   * 
   * <pre>
   * addItem(item, dir, item)
   * </pre>
   * 
   * @param item the text of the item to be added
   * @param dir the item's direction
   */
  public void addItem(String item, Direction dir) {
    insertItem(item, dir, INSERT_AT_END);
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
   * Adds an item to the list box, specifying its direction and an initial value
   * for the item.
   * 
   * @param item the text of the item to be added
   * @param dir the item's direction
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}; cannot be <code>null</code>
   */
  public void addItem(String item, Direction dir, String value) {
    insertItem(item, dir, value, INSERT_AT_END);
  }

  /**
   * Removes all items from the list box.
   */
  public void clear() {
    getSelectElement().clear();
  }

  public DirectionEstimator getDirectionEstimator() {
    return estimator;
  }

  /**
   * Gets the number of items present in the list box.
   * 
   * @return the number of items
   */
  public int getItemCount() {
    return getSelectElement().getOptions().getLength();
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
    return getOptionText(getSelectElement().getOptions().getItem(index));
  }

  public String getName() {
    return getSelectElement().getName();
  }

  /**
   * Gets the currently-selected item. If multiple items are selected, this
   * method will return the first selected item ({@link #isItemSelected(int)}
   * can be used to query individual items).
   * 
   * @return the selected index, or <code>-1</code> if none is selected
   */
  public int getSelectedIndex() {
    return getSelectElement().getSelectedIndex();
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
    return getSelectElement().getOptions().getItem(index).getValue();
  }

  /**
   * Gets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @return the visible item count
   */
  public int getVisibleItemCount() {
    return getSelectElement().getSize();
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
   * Inserts an item into the list box, specifying its direction. Has the same
   * effect as
   * 
   * <pre>
   * insertItem(item, dir, item, index)
   * </pre>
   * 
   * @param item the text of the item to be inserted
   * @param dir the item's direction
   * @param index the index at which to insert it
   */
  public void insertItem(String item, Direction dir, int index) {
    insertItem(item, dir, item, index);
  }

  /**
   * Inserts an item into the list box, specifying an initial value for the
   * item. Has the same effect as
   *
   * <pre>
   * insertItem(item, null, value, index)
   * </pre>
   * 
   * @param item the text of the item to be inserted
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}.
   * @param index the index at which to insert it
   */
  public void insertItem(String item, String value, int index) {
    insertItem(item, null, value, index);
  }

  /**
   * Inserts an item into the list box, specifying its direction and an initial
   * value for the item. If the index is less than zero, or greater than or
   * equal to the length of the list, then the item will be appended to the end
   * of the list.
   * 
   * @param item the text of the item to be inserted
   * @param dir the item's direction. If {@code null}, the item is displayed in
   *          the widget's overall direction, or, if a direction estimator has
   *          been set, in the item's estimated direction.
   * @param value the item's value, to be submitted if it is part of a
   *          {@link FormPanel}.
   * @param index the index at which to insert it
   */
  public void insertItem(String item, Direction dir, String value, int index) {
    SelectElement select = getSelectElement();
    OptionElement option = Document.get().createOptionElement();
    setOptionText(option, item, dir);
    option.setValue(value);

    int itemCount = select.getLength();
    if (index < 0 || index > itemCount) {
      index = itemCount;
    }
    if (index == itemCount) {
      select.add(option, null);
    } else {
      OptionElement before = select.getOptions().getItem(index);
      select.add(option, before);
    }
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
    return getSelectElement().getOptions().getItem(index).isSelected();
  }

  /**
   * Gets whether this list allows multiple selection.
   * 
   * @return <code>true</code> if multiple selection is allowed
   */
  public boolean isMultipleSelect() {
    return getSelectElement().isMultiple();
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler}
   * method on the object returned by {@link #addChangeHandler} instead
   */
  @Deprecated
  public void removeChangeListener(ChangeListener listener) {
    ListenerWrapper.WrappedChangeListener.remove(this, listener);
  }

  /**
   * Removes the item at the specified index.
   *
   * @param index the index of the item to be removed
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void removeItem(int index) {
    checkIndex(index);
    getSelectElement().remove(index);
  }

  /**
   * {@inheritDoc}
   * See note at
   * {@link #setDirectionEstimator(com.google.gwt.i18n.shared.DirectionEstimator)}.
   */
  public void setDirectionEstimator(boolean enabled) {
    setDirectionEstimator(enabled ? DEFAULT_DIRECTION_ESTIMATOR : null);
  }

  /**
   * {@inheritDoc}
   * Note: this does not affect the direction of already-existing content.
   */
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    estimator = directionEstimator;
  }

  /**
   * Sets whether an individual list item is selected.
   * 
   * <p>
   * Note that setting the selection programmatically does <em>not</em> cause
   * the {@link ChangeHandler#onChange(ChangeEvent)} event to be fired.
   * </p>
   * 
   * @param index the index of the item to be selected or unselected
   * @param selected <code>true</code> to select the item
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setItemSelected(int index, boolean selected) {
    checkIndex(index);
    getSelectElement().getOptions().getItem(index).setSelected(selected);
  }

  /**
   * Sets the text associated with the item at a given index.
   * 
   * @param index the index of the item to be set
   * @param text the item's new text
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setItemText(int index, String text) {
    setItemText(index, text, null);
  }

  /**
   * Sets the text associated with the item at a given index.
   * 
   * @param index the index of the item to be set
   * @param text the item's new text
   * @param dir the item's direction.
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void setItemText(int index, String text, Direction dir) {
    checkIndex(index);
    if (text == null) {
      throw new NullPointerException("Cannot set an option to have null text");
    }
    setOptionText(getSelectElement().getOptions().getItem(index), text, dir);
  }

  /**
   * Sets whether this list allows multiple selections. <em>NOTE: The preferred
   * way of enabling multiple selections in a list box is by using the
   * {@link #ListBox(boolean)} constructor. Using this method can spuriously
   * fail on Internet Explorer 6.0.</em>
   * 
   * @param multiple <code>true</code> to allow multiple selections
   * @deprecated use {@link #ListBox(boolean)} instead
   */
  @Deprecated
  public void setMultipleSelect(boolean multiple) {
    getSelectElement().setMultiple(multiple);
  }

  public void setName(String name) {
    getSelectElement().setName(name);
  }

  /**
   * Sets the currently selected index.
   * 
   * After calling this method, only the specified item in the list will remain
   * selected. For a ListBox with multiple selection enabled, see
   * {@link #setItemSelected(int, boolean)} to select multiple items at a time.
   * 
   * <p>
   * Note that setting the selected index programmatically does <em>not</em>
   * cause the {@link ChangeHandler#onChange(ChangeEvent)} event to be fired.
   * </p>
   * 
   * @param index the index of the item to be selected
   */
  public void setSelectedIndex(int index) {
    getSelectElement().setSelectedIndex(index);
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
    getSelectElement().getOptions().getItem(index).setValue(value);
  }

  /**
   * Sets the number of items that are visible. If only one item is visible,
   * then the box will be displayed as a drop-down list.
   * 
   * @param visibleItems the visible item count
   */
  public void setVisibleItemCount(int visibleItems) {
    getSelectElement().setSize(visibleItems);
  }

  /**
   * Retrieves the text of an option element. If the text was set by
   * {@link #setOptionText} and was wrapped with Unicode bidi formatting
   * characters, also removes those additional formatting characters.
   *  
   * @param option an option element
   * @return the element's text
   */
  protected String getOptionText(OptionElement option) {
    String text = option.getText();
    if (option.hasAttribute(BIDI_ATTR_NAME) && text.length() > 1) {
      text = text.substring(1, text.length() - 1);
    }
    return text;
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-item# = the option at the specified index.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);

    // Set the id of each option
    int numItems = getItemCount();
    for (int i = 0; i < numItems; i++) {
      ensureDebugId(getSelectElement().getOptions().getItem(i), baseID, "item"
          + i);
    }
  }

  /**
   * Sets the text of an option element. If the direction of the text is
   * opposite to the page's direction, also wraps it with Unicode bidi
   * formatting characters to prevent garbling, and indicates that this was done
   * by setting the option's <code>BIDI_ATTR_NAME</code> custom attribute.
   * 
   * @param option an option element
   * @param text text to be set to the element
   * @param dir the text's direction. If {@code null} and direction estimation
   *          is turned off, direction is ignored.
   */
  protected void setOptionText(OptionElement option, String text,
      Direction dir) {
    if (dir == null && estimator != null) {
      dir = estimator.estimateDirection(text);
    }
    if (dir == null) {
      option.setText(text);
      option.removeAttribute(BIDI_ATTR_NAME);
    } else {
      String formattedText =
          BidiFormatter.getInstanceForCurrentLocale().unicodeWrapWithKnownDir(
          dir, text, false /* isHtml */, false /* dirReset */);
      option.setText(formattedText);
      if (formattedText.length() > text.length()) {
        option.setAttribute(BIDI_ATTR_NAME, "");
      } else {
        option.removeAttribute(BIDI_ATTR_NAME);
      }
    }
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= getItemCount()) {
      throw new IndexOutOfBoundsException();
    }
  }

  private SelectElement getSelectElement() {
    return getElement().cast();
  }
}
