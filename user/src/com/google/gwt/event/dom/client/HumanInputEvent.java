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
package com.google.gwt.event.dom.client;

import com.google.gwt.event.shared.EventHandler;

/**
 * Abstract class representing position events such as mouse or touch events.
 *
 * @param <H> handler type
 *
 */
public abstract class HumanInputEvent<H extends EventHandler> extends DomEvent<
    H> {

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
