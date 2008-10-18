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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.impl.FocusImpl;

/**
 * Abstract base class for most widgets that can receive keyboard focus.
 */
public abstract class FocusWidget extends MousableWidget implements
    SourcesClickEvents, HasClickHandlers, HasFocus, HasAllFocusHandlers,
    HasAllKeyHandlers {

  private static final FocusImpl impl = FocusImpl.getFocusImplForWidget();

  /**
   * Gets the FocusImpl instance.
   * 
   * @return impl
   */
  protected static FocusImpl getFocusImpl() {
    return impl;
  }

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

  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(BlurEvent.TYPE, handler);
  }

  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(ClickEvent.TYPE, handler);
  }

  @Deprecated
  public void addClickListener(ClickListener listener) {
    addClickHandler(new L.Click(listener));
  }

  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(FocusEvent.TYPE, handler);
  }

  @Deprecated
  public void addFocusListener(FocusListener listener) {
    L.Focus.add(this, listener);
  }

  @Deprecated
  public void addKeyboardListener(KeyboardListener listener) {
    L.Keyboard.add(this, listener);
  }

  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(KeyDownEvent.TYPE, handler);
  }

  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(KeyPressEvent.TYPE, handler);
  }

  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(KeyUpEvent.TYPE, handler);
  }

  /**
   * Gets the tab index.
   * 
   * @return the tab index
   */
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

  @Deprecated
  public void removeClickListener(ClickListener listener) {
    L.Click.remove(this, listener);
  }

  @Deprecated
  public void removeFocusListener(FocusListener listener) {
    L.Focus.remove(this, listener);
  }

  @Deprecated
  public void removeKeyboardListener(KeyboardListener listener) {
    L.Keyboard.remove(this, listener);
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

  @Override
  protected void setElement(com.google.gwt.user.client.Element elem) {
    super.setElement(elem);

    // Accessibility: setting tab index to be 0 by default, ensuring element
    // appears in tab sequence. Note that this call will not interfere with
    // any calls made to FocusWidget.setTabIndex(int) by user code, because
    // FocusWidget.setTabIndex(int) cannot be called until setElement(elem)
    // has been called.
    setTabIndex(0);
  }

}
