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
package com.google.gwt.event.dom.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

/**
 * Abstract class representing mouse events.
 * 
 * @param <H> handler type
 * 
 */
public abstract class MouseEvent<H extends EventHandler> extends DomEvent<H> {
  /**
   * Gets the x coordinate relative to the given element.
   * 
   * @param nativeEvent the native event
   * @param relativeTo the relative element
   * @return the relative x
   */
  public static int getRelativeX(Event nativeEvent, Element relativeTo) {
    return nativeEvent.getClientX() - relativeTo.getAbsoluteLeft()
        + relativeTo.getScrollLeft() + Window.getScrollLeft();
  }

  /**
   * Gets the y coordinate relative to the given element.
   * 
   * @param nativeEvent the native event
   * @param relativeTo the relative element
   * @return the relative y
   */
  public static int getRelativeY(Event nativeEvent, Element relativeTo) {
    return nativeEvent.getClientY() - relativeTo.getAbsoluteTop()
        + relativeTo.getScrollTop() + Window.getScrollTop();
  }

  /**
   * Gets the mouse x-position within the browser window's client area.
   * 
   * @return the mouse x-position
   */
  public int getClientX() {
    return getNativeEvent().getClientX();
  }

  /**
   * Gets the mouse y-position within the browser window's client area.
   * 
   * @return the mouse y-position
   */
  public int getClientY() {
    return getNativeEvent().getClientY();
  }

  /**
   * Gets the button value. Compare it to {@link Event#BUTTON_LEFT},
   * {@link Event#BUTTON_RIGHT}, {@link Event#BUTTON_MIDDLE}
   * 
   * @return the button value
   */
  public int getNativeButton() {
    return getNativeEvent().getButton();
  }

  /**
   * Gets the x coordinate relative to the given element.
   * 
   * @param relativeTo the relative element
   * @return the relative x
   */
  public int getRelativeX(Element relativeTo) {
    return getRelativeX(getNativeEvent(), relativeTo);
  }

  /**
   * Gets the y coordinate relative to the given element.
   * 
   * 
   * @param relativeTo the relative element
   * @return the relative y
   */
  public int getRelativeY(Element relativeTo) {
    return getRelativeY(getNativeEvent(), relativeTo);
  }

  /**
   * Gets the mouse x-position on the user's display.
   * 
   * @return the mouse x-position
   */
  public int getScreenX() {
    return getNativeEvent().getScreenX();
  }

  /**
   * Gets the mouse y-position on the user's display.
   * 
   * @return the mouse y-position
   */
  public int getScreenY() {
    return getNativeEvent().getScreenY();
  }

  /**
   * Is <code>alt</code> key down.
   * 
   * @return whether the alt key is down
   */
  public boolean isAltKeyDown() {
    return getNativeEvent().getAltKey();
  }

  /**
   * Is <code>control</code> key down.
   * 
   * @return whether the control key is down
   */
  public boolean isControlKeyDown() {
    return getNativeEvent().getCtrlKey();
  }

  /**
   * Is <code>meta</code> key down.
   * 
   * @return whether the meta key is down
   */
  public boolean isMetaKeyDown() {
    return getNativeEvent().getMetaKey();
  }

  /**
   * Is <code>shift</code> key down.
   * 
   * @return whether the shift key is down
   */
  public boolean isShiftKeyDown() {
    return getNativeEvent().getShiftKey();
  }
}