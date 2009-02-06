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

import com.google.gwt.event.shared.EventHandler;

/**
 * Base class for Key events. The native keyboard events are somewhat a mess
 * (http://www.quirksmode.org/js/keys.html), we do some trivial normalization
 * here, but do not attempt any complex patching, so user be warned.
 * 
 * @param <H> The event handler type
 */
public abstract class KeyEvent<H extends EventHandler> extends DomEvent<H> {

  /**
   * Is the <code>alt</code> key down?
   * 
   * @return whether the alt key is down
   */
  public boolean isAltKeyDown() {
    return getNativeEvent().getAltKey();
  }

  /**
   * Does this event have any modifier keys down? Specifically. is the control,
   * meta, shift, or alt key currently pressed?
   * 
   * @return whether this event have any modifier key down
   */
  public boolean isAnyModifierKeyDown() {
    return isControlKeyDown() || isShiftKeyDown() || isMetaKeyDown()
        || isAltKeyDown();
  }

  /**
   * Is the <code>control</code> key down?
   * 
   * @return whether the control key is down
   */
  public boolean isControlKeyDown() {
    return getNativeEvent().getCtrlKey();
  }

  /**
   * Is the <code>meta</code> key down?
   * 
   * @return whether the meta key is down
   */
  public boolean isMetaKeyDown() {
    return getNativeEvent().getMetaKey();
  }

  /**
   * Is the <code>shift</code> key down?
   * 
   * @return whether the shift key is down
   */
  public boolean isShiftKeyDown() {
    return getNativeEvent().getShiftKey();
  }
}
