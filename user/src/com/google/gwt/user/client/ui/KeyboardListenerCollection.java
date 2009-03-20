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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

import java.util.ArrayList;

/**
 * A helper class for implementers of the SourcesKeyboardEvents interface. This
 * subclass of {@link ArrayList} assumes that all objects added to it will be of
 * type {@link com.google.gwt.user.client.ui.KeyboardListener}.
 * 
 * @deprecated Widgets should now manage their own handlers via {@link Widget#addDomHandler}
 */
@Deprecated
public class KeyboardListenerCollection extends ArrayList<KeyboardListener> {

  /**
   * Gets the keyboard modifiers associated with a DOMEvent.
   * 
   * @param event the event.
   * @return the modifiers as defined in {@link KeyboardListener}.
   */
  public static int getKeyboardModifiers(Event event) {
    return (DOM.eventGetShiftKey(event) ? KeyboardListener.MODIFIER_SHIFT : 0)
        | (DOM.eventGetMetaKey(event) ? KeyboardListener.MODIFIER_META : 0)
        | (DOM.eventGetCtrlKey(event) ? KeyboardListener.MODIFIER_CTRL : 0)
        | (DOM.eventGetAltKey(event) ? KeyboardListener.MODIFIER_ALT : 0);
  }

  /**
   * Automatically fires the appropriate keyboard event to all listeners. If the
   * given event is not a keyboard event, no action will be performed.
   * 
   * @param sender the widget sending the event.
   * @param event the Event received by the widget.
   */
  public void fireKeyboardEvent(Widget sender, Event event) {
    int modifiers = getKeyboardModifiers(event);

    switch (DOM.eventGetType(event)) {
      case Event.ONKEYDOWN:
        fireKeyDown(sender, (char) DOM.eventGetKeyCode(event), modifiers);
        break;

      case Event.ONKEYUP:
        fireKeyUp(sender, (char) DOM.eventGetKeyCode(event), modifiers);
        break;

      case Event.ONKEYPRESS:
        fireKeyPress(sender, (char) DOM.eventGetKeyCode(event), modifiers);
        break;
    }
  }

  /**
   * Fires a keyDown event to all listeners.
   * 
   * @param sender the widget sending the event.
   * @param keyCode the keyCode to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void fireKeyDown(Widget sender, char keyCode, int modifiers) {
    for (KeyboardListener listener : this) {
      listener.onKeyDown(sender, keyCode, modifiers);
    }
  }

  /**
   * Fires a keyDown event to all listeners.
   * 
   * @param sender the widget sending the event.
   * @param key the key to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void fireKeyPress(Widget sender, char key, int modifiers) {
    for (KeyboardListener listener : this) {
      listener.onKeyPress(sender, key, modifiers);
    }
  }

  /**
   * Fires a keyDown event to all listeners.
   * 
   * @param sender the widget sending the event.
   * @param keyCode the keyCode to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void fireKeyUp(Widget sender, char keyCode, int modifiers) {
    for (KeyboardListener listener : this) {
      listener.onKeyUp(sender, keyCode, modifiers);
    }
  }
}
