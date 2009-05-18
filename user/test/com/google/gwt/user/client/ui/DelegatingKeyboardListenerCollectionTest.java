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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests keyboard events in the {@link DelegatingKeyboardListenerCollection}
 * class.
 */
public class DelegatingKeyboardListenerCollectionTest extends GWTTestCase
    implements KeyboardListener {

  /**
   * A {@link Widget} that uses the {@link DelegatingKeyboardListenerCollection}
   * to save its list of keyboard events.
   */
  public static class DelegatingWidget extends Widget {
    // The delegating collection of keyboard listeners
    private DelegatingKeyboardListenerCollection keyboardListeners;

    // The owner of all events
    private Widget eventOwner;

    /**
     * Adds a listener interface to receive keyboard events.
     * 
     * @param listener the listener interface to add
     */
    public void addKeyboardListener(KeyboardListener listener) {
      if (keyboardListeners == null) {
        this.eventOwner = new Widget();
        keyboardListeners = new DelegatingKeyboardListenerCollection(
            this.eventOwner, new TextBox());
      }
      keyboardListeners.add(listener);
    }

    /**
     * Get the event owner.
     * 
     * @return the event owner
     */
    public Widget getEventOwner() {
      return this.eventOwner;
    }

    /**
     * Get the keyboardListener.
     * 
     * @return the keyboardListener
     */
    public DelegatingKeyboardListenerCollection getKeyboardListeners() {
      return this.keyboardListeners;
    }

    /**
     * Removes a previously added listener interface.
     * 
     * @param listener the listener interface to remove
     */
    public void removeKeyboardListener(KeyboardListener listener) {
      /*
       * This method is not used in the test or in the
       * DelegatingKeyboardListenerCollection constructor
       */
    }
  }

  // The owner of the events
  private DelegatingWidget delegatingWidget;

  // A bit used to verify that some event handler was called
  private boolean eventHandled;

  // The name of the last event fired
  private String lastEventName;

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Handle the key down event from the ownerOfEvents.
   * 
   * @param sender the widget sending the event.
   * @param keyCode the key to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void onKeyDown(Widget sender, char keyCode, int modifiers) {
    this.handleKeyEvent(sender, "onKeyDown");
  }

  /**
   * Handle the key press event from the ownerOfEvents.
   * 
   * @param sender the widget sending the event.
   * @param keyCode the key to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void onKeyPress(Widget sender, char keyCode, int modifiers) {
    this.handleKeyEvent(sender, "onKeyPress");
  }

  /**
   * Handle the key up event from the ownerOfEvents.
   * 
   * @param sender the widget sending the event.
   * @param keyCode the key to send with the event.
   * @param modifiers the modifier keys pressed at when the event occurred. This
   *          value is a combination of the bits defined by
   *          {@link KeyboardListener#MODIFIER_SHIFT},
   *          {@link KeyboardListener#MODIFIER_CTRL}, and
   *          {@link KeyboardListener#MODIFIER_ALT}.
   */
  public void onKeyUp(Widget sender, char keyCode, int modifiers) {
    this.handleKeyEvent(sender, "onKeyUp");
  }

  /**
   * Tests that the key event handlers re-fire the correct key events to the
   * correct owner. The owner of the events is a {@link TextBox}, which
   * implements {@link SourcesKeyboardEvents} and allows us to check that the
   * owner is correctly re-firing the events.
   */
  public void testKeyEvents() {
    // Create a keyboard event listener with a DeletagingWidgetknown owner
    this.delegatingWidget = new DelegatingWidget();
    this.delegatingWidget.addKeyboardListener(this);

    // Fire events from through delegate, which should set the correct owner
    this.fireKeyEvent("onKeyDown");
    this.fireKeyEvent("onKeyUp");
    this.fireKeyEvent("onKeyPress");
  }

  /**
   * This helper method simulates the firing of an event by the delegating
   * widget, with a generic Widget as the source.
   * 
   * @param eventName the name of the event to fire
   */
  private void fireKeyEvent(String eventName) {
    this.lastEventName = eventName; // Set the name of this event
    this.eventHandled = false; // Mark that we haven't handled it yet

    // Fire the actual event through the delegate
    if (eventName.compareTo("onKeyDown") == 0) {
      this.delegatingWidget.getKeyboardListeners().onKeyDown(new Widget(), 'a',
          0);
    } else if (eventName.compareTo("onKeyUp") == 0) {
      this.delegatingWidget.getKeyboardListeners().onKeyUp(new Widget(), 'a', 0);
    } else if (eventName.compareTo("onKeyPress") == 0) {
      this.delegatingWidget.getKeyboardListeners().onKeyPress(new Widget(),
          'a', 0);
    } else {
      fail("The event " + eventName + " is not supported");
    }

    // Verify that the event was handled
    assertTrue(this.eventHandled);
  }

  /**
   * Handle an event from the ownerOfEvents by verifying the the event the the
   * source are correct.
   * 
   * @param sender the Widget that fired the event
   * @param eventName the name of the event
   */
  private void handleKeyEvent(Widget sender, String eventName) {
    assertEquals(this.delegatingWidget.getEventOwner(), sender);
    assertEquals(this.lastEventName, eventName);
    this.eventHandled = true;
  }
}
