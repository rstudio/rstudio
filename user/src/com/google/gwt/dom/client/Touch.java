/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Class representing touches.
 *
 * See {@link <a href="http://developer.apple.com/library/safari/#documentation/UserExperience/Reference/TouchClassReference/Touch/Touch.html">Safari Touch Documentation</a>}
 */
public class Touch extends JavaScriptObject {

  protected Touch() {
  }

  /**
   * Gets the touch x-position within the browser window's client area.
   *
   * @return the touch x-position
   */
  public final int getClientX() {
    return DOMImpl.impl.touchGetClientX(this);
  }

  /**
   * Gets the touch y-position within the browser window's client area.
   *
   * @return the touch y-position
   */
  public final int getClientY() {
    return DOMImpl.impl.touchGetClientY(this);
  }

  /**
   * Gets a unique identifier for this touch.
   *
   * @return the unique identifier for this touch
   */
  public final int getIdentifier() {
    return DOMImpl.impl.touchGetIdentifier(this);
  }

  /**
   * Gets the touch x-position within the browser document.
   *
   * @return the touch x-position
   */
  public final int getPageX() {
    return DOMImpl.impl.touchGetPageX(this);
  }

  /**
   * Gets the touch y-position within the browser document.
   *
   * @return the touch y-position
   */
  public final int getPageY() {
    return DOMImpl.impl.touchGetPageY(this);
  }

  /**
   * Gets the touch x-position relative to a given element.
   *
   * @param target the element whose coordinate system is to be used
   * @return the relative x-position
   */
  public final int getRelativeX(Element target) {
    return getClientX() - target.getAbsoluteLeft() + target.getScrollLeft()
        + target.getOwnerDocument().getScrollLeft();
  }

  /**
   * Gets the touch y-position relative to a given element.
   *
   * @param target the element whose coordinate system is to be used
   * @return the relative y-position
   */
  public final int getRelativeY(Element target) {
    return getClientY() - target.getAbsoluteTop() + target.getScrollTop()
        + target.getOwnerDocument().getScrollTop();
  }

  /**
   * Gets the touch x-position on the user's display.
   *
   * @return the touch x-position
   */
  public final int getScreenX() {
    return DOMImpl.impl.touchGetScreenX(this);
  }

  /**
   * Gets the touch y-position on the user's display.
   *
   * @return the touch y-position
   */
  public final int getScreenY() {
    return DOMImpl.impl.touchGetScreenY(this);
  }

  /**
   * Gets the target element for the current touch.
   *
   * @return the target element
   */
  public final EventTarget getTarget() {
    return DOMImpl.impl.touchGetTarget(this);
  }
}
