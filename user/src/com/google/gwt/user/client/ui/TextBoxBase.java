/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

/**
 * Abstract base class for all text entry widgets.
 */
@SuppressWarnings("deprecation")
public class TextBoxBase extends FocusWidget implements SourcesChangeEvents,
    HasText, HasName, HasValue<String> {

  /**
   * Text alignment constant, used in
   * {@link TextBoxBase#setTextAlignment(TextBoxBase.TextAlignConstant)}.
   */
  public static class TextAlignConstant {
    private String textAlignString;

    private TextAlignConstant(String textAlignString) {
      this.textAlignString = textAlignString;
    }

    private String getTextAlignString() {
      return textAlignString;
    }
  }

  /**
   * Center the text.
   */
  public static final TextAlignConstant ALIGN_CENTER = new TextAlignConstant(
      "center");

  /**
   * Justify the text.
   */
  public static final TextAlignConstant ALIGN_JUSTIFY = new TextAlignConstant(
      "justify");

  /**
   * Align the text to the left edge.
   */
  public static final TextAlignConstant ALIGN_LEFT = new TextAlignConstant(
      "left");

  /**
   * Align the text to the right.
   */
  public static final TextAlignConstant ALIGN_RIGHT = new TextAlignConstant(
      "right");

  private static TextBoxImpl impl = GWT.create(TextBoxImpl.class);

  private Event currentEvent;
  private boolean valueChangeHandlerInitialized;

  /**
   * Creates a text box that wraps the given browser element handle. This is
   * only used by subclasses.
   * 
   * @param elem the browser element to wrap
   */
  protected TextBoxBase(Element elem) {
    super(elem);
  }

  @Deprecated
  public void addChangeListener(ChangeListener listener) {
    addDomHandler(new ListenerWrapper.Change(listener), ChangeEvent.getType());
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<String> handler) {
    // Initialization code
    if (!valueChangeHandlerInitialized) {
      valueChangeHandlerInitialized = true;
      addDomHandler(new ChangeHandler() {
        public void onChange(ChangeEvent event) {
          ValueChangeEvent.fire(TextBoxBase.this, getText());
        }
      }, ChangeEvent.getType());
    }
    return addHandler(handler, ValueChangeEvent.getType());
  }

  /**
   * If a keyboard event is currently being handled on this text box, calling
   * this method will suppress it. This allows listeners to easily filter
   * keyboard input.
   */
  public void cancelKey() {
    if (currentEvent != null) {
      DOM.eventPreventDefault(currentEvent);
    }
  }

  /**
   * Gets the current position of the cursor (this also serves as the beginning
   * of the text selection).
   * 
   * @return the cursor's position
   */
  public int getCursorPos() {
    return impl.getCursorPos(getElement());
  }

  public String getName() {
    return DOM.getElementProperty(getElement(), "name");
  }

  /**
   * Gets the text currently selected within this text box.
   * 
   * @return the selected text, or an empty string if none is selected
   */
  public String getSelectedText() {
    int start = getCursorPos();
    if (start < 0) {
      return "";
    }
    int length = getSelectionLength();
    return getText().substring(start, start + length);
  }

  /**
   * Gets the length of the current text selection.
   * 
   * @return the text selection length
   */
  public int getSelectionLength() {
    return impl.getSelectionLength(getElement());
  }

  public String getText() {
    return DOM.getElementProperty(getElement(), "value");
  }

  public String getValue() {
    return getText();
  }

  /**
   * Determines whether or not the widget is read-only.
   * 
   * @return <code>true</code> if the widget is currently read-only,
   *         <code>false</code> if the widget is currently editable
   */
  public boolean isReadOnly() {
    return DOM.getElementPropertyBoolean(getElement(), "readOnly");
  }

  @Override
  public void onBrowserEvent(Event event) {
    int type = DOM.eventGetType(event);
    if ((type & Event.KEYEVENTS) != 0) {
      // Fire the keyboard event. Hang on to the current event object so that
      // cancelKey() and setKey() can be implemented.
      currentEvent = event;
      // Call the superclass' onBrowserEvent as that includes the key event
      // handlers.
      super.onBrowserEvent(event);
      currentEvent = null;
    } else {
      // Handles Focus and Click events.
      super.onBrowserEvent(event);
    }
  }

  @Deprecated
  public void removeChangeListener(ChangeListener listener) {
    ListenerWrapper.Change.remove(this, listener);
  }

  /**
   * Selects all of the text in the box.
   * 
   * This will only work when the widget is attached to the document and not
   * hidden.
   */
  public void selectAll() {
    int length = getText().length();
    if (length > 0) {
      setSelectionRange(0, length);
    }
  }

  /**
   * Sets the cursor position.
   * 
   * This will only work when the widget is attached to the document and not
   * hidden.
   * 
   * @param pos the new cursor position
   */
  public void setCursorPos(int pos) {
    setSelectionRange(pos, 0);
  }

  /**
   * If a keyboard event is currently being handled by the text box, this method
   * replaces the unicode character or key code associated with it. This allows
   * listeners to easily filter keyboard input.
   * 
   * @param key the new key value
   * @deprecated this method only works in IE and should not have been added to
   *             the API
   */
  @Deprecated
  public void setKey(char key) {
    if (currentEvent != null) {
      DOM.eventSetKeyCode(currentEvent, key);
    }
  }

  public void setName(String name) {
    DOM.setElementProperty(getElement(), "name", name);
  }

  /**
   * Turns read-only mode on or off.
   * 
   * @param readOnly if <code>true</code>, the widget becomes read-only; if
   *          <code>false</code> the widget becomes editable
   */
  public void setReadOnly(boolean readOnly) {
    DOM.setElementPropertyBoolean(getElement(), "readOnly", readOnly);
    String readOnlyStyle = "readonly";
    if (readOnly) {
      addStyleDependentName(readOnlyStyle);
    } else {
      removeStyleDependentName(readOnlyStyle);
    }
  }

  /**
   * Sets the range of text to be selected.
   * 
   * This will only work when the widget is attached to the document and not
   * hidden.
   * 
   * @param pos the position of the first character to be selected
   * @param length the number of characters to be selected
   */
  public void setSelectionRange(int pos, int length) {
    // Setting the selection range will not work for unattached elements.
    if (!isAttached()) {
      return;
    }

    if (length < 0) {
      throw new IndexOutOfBoundsException(
          "Length must be a positive integer. Length: " + length);
    }
    if ((pos < 0) || (length + pos > getText().length())) {
      throw new IndexOutOfBoundsException("From Index: " + pos + "  To Index: "
          + (pos + length) + "  Text Length: " + getText().length());
    }
    impl.setSelectionRange(getElement(), pos, length);
  }

  public void setText(String text) {
    DOM.setElementProperty(getElement(), "value", text != null ? text : "");
  }

  /**
   * Sets the alignment of the text in the text box.
   * 
   * @param align the text alignment (as specified by {@link #ALIGN_CENTER},
   *          {@link #ALIGN_JUSTIFY}, {@link #ALIGN_LEFT}, and
   *          {@link #ALIGN_RIGHT})
   */
  public void setTextAlignment(TextAlignConstant align) {
    DOM.setStyleAttribute(getElement(), "textAlign", align.getTextAlignString());
  }

  public void setValue(String value) {
    setValue(value, false);
  }

  public void setValue(String value, boolean fireEvents) {
    String oldValue = getText();
    setText(value);
    if (fireEvents) {
      ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
    }
  }

  protected TextBoxImpl getImpl() {
    return impl;
  }
}
