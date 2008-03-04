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
import com.google.gwt.user.client.ui.impl.FocusImpl;

/**
 * Abstract base class for most widgets that can receive keyboard focus.
 */
public abstract class FocusWidget extends Widget implements SourcesClickEvents,
    SourcesFocusEvents, HasFocus, SourcesKeyboardEvents {

  private static final FocusImpl impl = FocusImpl.getFocusImplForWidget();

  /**
   * Gets the FocusImpl instance.
   * 
   * @return impl
   */
  protected static FocusImpl getFocusImpl() {
    return impl;
  }

  private ClickListenerCollection clickListeners;
  private FocusListenerCollection focusListeners;
  private KeyboardListenerCollection keyboardListeners;

  /**
   * Creates a new focus widget with no element. {@link #setElement(Element)}
   * must be called before any other methods.
   */
  protected FocusWidget() {
  }

  /**
   * Creates a new focus widget that wraps the specified browser element.
   * 
   * @param elem the element to be wrapped
   */
  protected FocusWidget(Element elem) {
    setElement(elem);
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
      sinkEvents(Event.ONCLICK);
    }
    clickListeners.add(listener);
  }

  public void addFocusListener(FocusListener listener) {
    if (focusListeners == null) {
      focusListeners = new FocusListenerCollection();
      sinkEvents(Event.FOCUSEVENTS);
    }
    focusListeners.add(listener);
  }

  public void addKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners == null) {
      keyboardListeners = new KeyboardListenerCollection();
      sinkEvents(Event.KEYEVENTS);
    }
    keyboardListeners.add(listener);
  }

  public int getTabIndex() {
    return impl.getTabIndex(getElement());
  }

  /**
   * Gets whether this widget is enabled.
   * 
   * @return <code>true</code> if the widget is enabled
   */
  public boolean isEnabled() {
    return !DOM.getElementPropertyBoolean(getElement(), "disabled");
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK:
        if (clickListeners != null) {
          clickListeners.fireClick(this);
        }
        break;

      case Event.ONBLUR:
      case Event.ONFOCUS:
        if (focusListeners != null) {
          focusListeners.fireFocusEvent(this, event);
        }
        break;

      case Event.ONKEYDOWN:
      case Event.ONKEYUP:
      case Event.ONKEYPRESS:
        if (keyboardListeners != null) {
          keyboardListeners.fireKeyboardEvent(this, event);
        }
        break;
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void removeFocusListener(FocusListener listener) {
    if (focusListeners != null) {
      focusListeners.remove(listener);
    }
  }

  public void removeKeyboardListener(KeyboardListener listener) {
    if (keyboardListeners != null) {
      keyboardListeners.remove(listener);
    }
  }

  public void setAccessKey(char key) {
    DOM.setElementProperty(getElement(), "accessKey", "" + key);
  }

  /**
   * Sets whether this widget is enabled.
   * 
   * @param enabled <code>true</code> to enable the widget, <code>false</code>
   *          to disable it
   */
  public void setEnabled(boolean enabled) {
    DOM.setElementPropertyBoolean(getElement(), "disabled", !enabled);
  }

  public void setFocus(boolean focused) {
    if (focused) {
      impl.focus(getElement());
    } else {
      impl.blur(getElement());
    }
  }

  public void setTabIndex(int index) {
    impl.setTabIndex(getElement(), index);
  }

  /**
   * Fire all current {@link ClickListener}.
   */
  void fireClickListeners() {
    /*
     * Implementation note: PushButton needs to fire click listeners manually.
     * Exposing this method so it can do so.
     */
    if (clickListeners != null) {
      clickListeners.fireClick(this);
    }
  }
}
