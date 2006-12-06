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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.TextBoxImpl;

/**
 * Abstract base class for all text entry widgets.
 */
public class TextBoxBase extends FocusWidget implements SourcesKeyboardEvents,
    SourcesChangeEvents, SourcesClickEvents, HasText, HasName {

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

  private static TextBoxImpl impl = (TextBoxImpl) GWT.create(TextBoxImpl.class);

  private ChangeListenerCollection changeListeners;
  private ClickListenerCollection clickListeners;
  private Event currentEvent;
  private KeyboardListenerCollection keyboardListeners;

  /**
   * Creates a text box that wraps the given browser element handle. This is
   * only used by subclasses.
   * 
   * @param elem the browser element to wrap
   */
  protected TextBoxBase(Element elem) {
    super(elem);
    sinkEvents(Event.ONCHANGE);
  }

  public void addChangeListener(ChangeListener listener) {
    if (changeListeners == null) {
      changeListeners = new ChangeListenerCollection();
    }
    changeListeners.add(listener);
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
    }
    clickListeners.add(listener);
  }

  public void addKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners == null) {
      keyboardListeners = new KeyboardListenerCollection();
    }
    keyboardListeners.add(listener);
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
    return DOM.getAttribute(getElement(), "name");
  }

  /**
   * Gets the text currently selected within this text box.
   * 
   * @return the selected text, or an empty string if none is selected
   */
  public String getSelectedText() {
    int start = getCursorPos(), length = getSelectionLength();
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
    return DOM.getAttribute(getElement(), "value");
  }

  public void onBrowserEvent(Event event) {
    // Call the superclass' implementation first (because FocusWidget fires
    // some events itself).
    super.onBrowserEvent(event);

    int type = DOM.eventGetType(event);
    if ((keyboardListeners != null) && (type & Event.KEYEVENTS) != 0) {
      // Fire the keyboard event. Hang on to the current event object so that
      // cancelKey() and setKey() can be implemented.
      currentEvent = event;
      keyboardListeners.fireKeyboardEvent(this, event);
      currentEvent = null;
    } else if (type == Event.ONCLICK) {
      // Fire the click event.
      if (clickListeners != null) {
        clickListeners.fireClick(this);
      }
    } else if (type == Event.ONCHANGE) {
      // Fire the change event.
      if (changeListeners != null) {
        changeListeners.fireChange(this);
      }
    }
  }

  public void removeChangeListener(ChangeListener listener) {
    if (changeListeners != null) {
      changeListeners.remove(listener);
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void removeKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners != null) {
      keyboardListeners.remove(listener);
    }
  }

  /**
   * Selects all of the text in the box.
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
   */
  public void setKey(char key) {
    if (currentEvent != null) {
      DOM.eventSetKeyCode(currentEvent, key);
    }
  }

  public void setName(String name) {
    DOM.setAttribute(getElement(), "name", name);
  }

  /**
   * Sets the range of text to be selected.
   * 
   * @param pos the position of the first character to be selected
   * @param length the number of characters to be selected
   */
  public void setSelectionRange(int pos, int length) {
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
    DOM.setAttribute(getElement(), "value", text);
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

  protected TextBoxImpl getImpl() {
    return impl;
  }
}