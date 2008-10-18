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

import com.google.gwt.event.dom.client.HasAllMouseHandlers;
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

/**
 * Helper class to reduce code size in core widgets.
 */
class MousableWidget extends Widget implements SourcesMouseEvents,
    HasAllMouseHandlers, SourcesMouseWheelEvents {

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

  @Deprecated
  public void removeMouseListener(MouseListener listener) {
    L.Mouse.remove(this, listener);
  }

  @Deprecated
  public void removeMouseWheelListener(MouseWheelListener listener) {
    L.MouseWheel.remove(this, listener);
  }
}
