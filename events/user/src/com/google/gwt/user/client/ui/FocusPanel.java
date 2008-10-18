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

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.impl.FocusImpl;

/**
 * A simple panel that makes its contents focusable, and adds the ability to
 * catch mouse and keyboard events.
 */
public class FocusPanel extends SimplePanel implements HasFocus,
    SourcesClickEvents, SourcesMouseEvents, SourcesMouseWheelEvents,
    HasAllMouseHandlers, HasClickHandlers, HasAllKeyHandlers,
    HasAllFocusHandlers {

  static final FocusImpl impl = FocusImpl.getFocusImplForPanel();

  public FocusPanel() {
    super(impl.createFocusable());
  }

  public FocusPanel(Widget child) {
    this();
    setWidget(child);
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
    HasAllFocusHandlers.Adaptor.addHandlers(this, new L.Focus(listener));
  }

  @Deprecated
  public void addKeyboardListener(KeyboardListener listener) {
    HasAllKeyHandlers.Adaptor.addHandlers(this, new L.Keyboard(listener));
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

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(MouseDownEvent.TYPE, handler);
  }

  @Deprecated
  public void addMouseListener(MouseListener listener) {
    L.Mouse.add(this, listener);
  }

  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(MouseMoveEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(MouseOutEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(MouseOverEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(MouseUpEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(MouseWheelEvent.TYPE, handler);
  }

  @Deprecated
  public void addMouseWheelListener(MouseWheelListener listener) {
    addMouseWheelHandler(new L.MouseWheel(listener));
  }

  public int getTabIndex() {
    return impl.getTabIndex(getElement());
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

  @Deprecated
  public void removeMouseListener(MouseListener listener) {
    L.Mouse.remove(this, listener);
  }

  @Deprecated
  public void removeMouseWheelListener(MouseWheelListener listener) {
    L.MouseWheel.remove(this, listener);
  }

  public void setAccessKey(char key) {
    impl.setAccessKey(getElement(), key);
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
}
