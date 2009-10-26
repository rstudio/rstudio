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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * Tests for the native event triggering methods in {@link Event}.
 */
public class CreateEventTest extends GWTTestCase {

  /**
   * Listener for use with key[down up press].
   */
  private class KeyEventListener extends BubbleAssertingEventListener {
    public KeyEventListener(String eventType) {
      super(eventType);
    }

    public void onBrowserEvent(Event event) {
      super.onBrowserEvent(event);
      assertAllShiftKeysOn(event);
      assertEquals(KEY_CODE, event.getKeyCode());
    }
  }

  /**
   * Listener for use with mouse[down up move over out].
   */
  private class MouseEventListener extends BubbleAssertingEventListener {
    public MouseEventListener(String eventType) {
      super(eventType);
    }

    public void onBrowserEvent(Event event) {
      super.onBrowserEvent(event);
      assertMouseCoordinates(event);
      assertAllShiftKeysOn(event);
      assertEquals(Event.BUTTON_LEFT, event.getButton());
    }
  }

  /**
   * An event listener that asserts that the event is passed to child, then
   * parent.
   */
  private class BubbleAssertingEventListener implements EventListener {
    public boolean parentReceived, childReceived;
    private final String eventType;

    public BubbleAssertingEventListener(String eventType) {
      this.eventType = eventType;
    }

    public void onBrowserEvent(Event event) {
      assertEquals(eventType, event.getType());

      EventTarget target = event.getCurrentEventTarget();
      if (Element.is(target)) {
        if (Element.as(target) == child) {
          assertFalse("Expecting child to receive the event only once",
              childReceived);
          assertFalse("Expecting child to receive the event before parent",
              parentReceived);

          childReceived = true;
        } else if (Element.as(target) == parent) {
          assertFalse("Expecting parent to receive the event only once",
              parentReceived);
          assertTrue("Expecting parent to receive the event after the child",
              childReceived);

          parentReceived = true;
        }
      }
    }
  }

  /**
   * Used with {@link CreateEventTest#testGetCurrentEvent()}.
   */
  private static class CurrentEventListener implements EventListener {
    public boolean gotClick, gotKeyPress, gotFocus;

    public void onBrowserEvent(Event event) {
      switch (Event.getCurrentEvent().getTypeInt()) {
        case Event.ONCLICK:
          gotClick = true;
          break;
        case Event.ONKEYPRESS:
          gotKeyPress = true;
          break;
        case Event.ONFOCUS:
          gotFocus = true;
          break;
      }
    }
  }

  /**
   * An event listener that asserts that the event is passed only to child.
   */
  private class NonBubbleAssertingEventListener implements EventListener {
    private boolean childReceived;
    private String eventType;

    public NonBubbleAssertingEventListener(String eventType) {
      this.eventType = eventType;
    }

    public void onBrowserEvent(Event event) {
      assertEquals(eventType, event.getType());

      if (event.getTarget() == child) {
        assertFalse("Expecting child to receive the event only once",
            childReceived);
        childReceived = true;
      } else if (event.getTarget() == parent) {
        fail("Not expecting parent to receive the event");
      }
    }

    protected String getEventType() {
      return eventType;
    }
  }

  /**
   * An event listener that asserts that events are received properly for the
   * img element.
   */
  private static class ImgEventListener implements EventListener {
    private boolean imgReceived;
    private final String eventType;

    public ImgEventListener(String eventType) {
      this.eventType = eventType;
    }

    public void onBrowserEvent(Event event) {
      if (event.getType().equals(eventType)) {
        if (event.getTarget() == img) {
          assertFalse("Expecting img to receive the event only once",
              imgReceived);

          imgReceived = true;
        } else if (event.getTarget() == parent) {
          fail("Not expecting parent to receive the event");
        }
      }
    }
  }

  private static final int MOUSE_DETAIL = 1;
  private static final int CLIENT_X = 2;
  private static final int CLIENT_Y = 3;
  private static final int SCREEN_X = 4;
  private static final int SCREEN_Y = 5;
  private static final int KEY_CODE = 'A';

  private static final int ALL_EVENTS = Event.MOUSEEVENTS | Event.KEYEVENTS
      | Event.FOCUSEVENTS | Event.ONCHANGE | Event.ONCLICK | Event.ONDBLCLICK
      | Event.ONCONTEXTMENU | Event.ONLOAD | Event.ONERROR | Event.ONSCROLL;
  private static DivElement parent;
  private static InputElement child;
  private static ImageElement img;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.UserTest";
  }

  /**
   * Tests that {@link Event#getCurrentEvent()} returns the right value for
   * synthesized events.
   */
  public void testGetCurrentEvent() {
    CurrentEventListener listener = new CurrentEventListener();
    Event.setEventListener(child, listener);

    // Test all three major event types.
    child.dispatchEvent(Document.get().createClickEvent(0, 0, 0, 0, 0, false,
        false, false, false));
    child.dispatchEvent(Document.get().createKeyPressEvent(false, false, false,
        false, 65, 65));
    child.dispatchEvent(Document.get().createFocusEvent());

    assertTrue("Expecting click as current event", listener.gotClick);
    assertTrue("Expecting keypress as current event", listener.gotKeyPress);
    assertTrue("Expecting focus as current event", listener.gotFocus);
  }

  /**
   * Tests NativeEvent.stopPropagation().
   */
  public void testStopPropagation() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "click") {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        if (event.getCurrentTarget() == child) {
          event.stopPropagation();
        }
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createClickEvent(MOUSE_DETAIL, SCREEN_X,
        SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createBlurEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerBlurEvent() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "blur") {
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        assertEquals("blur", event.getType());
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createBlurEvent());

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createChangeEvent().
   */
  public void testTriggerChangeEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "change");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createChangeEvent());

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createClickEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerClickEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "click") {
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        assertMouseCoordinates(event);
        assertAllShiftKeysOn(event);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createClickEvent(MOUSE_DETAIL, SCREEN_X,
        SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createContextMenuEvent().
   * TODO: Re-enable this test when we no longer support Firefox2 and earlier
   * (which doesn't appear to dispatch contextmenu events properly).
   */
//  public void testTriggerContextMenuEvent() {
//    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
//        "contextmenu");
//    Event.setEventListener(parent, listener);
//    Event.setEventListener(child, listener);
//
//    child.dispatchEvent(Document.get().createContextMenuEvent());
//
//    assertTrue("Expected child to receive event", listener.childReceived);
//    assertTrue("Expected parent to receive event", listener.parentReceived);
//  }

  /**
   * Tests createDblClickEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerDblClickEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "dblclick") {
      public void onBrowserEvent(Event event) {
        if (event.getTypeInt() == Event.ONCLICK) {
          // Some browsers (IE, I'm looking at you) synthesize an extra click
          // event when a double-click is triggered. This synthesized click
          // will *not* have the same properties as the dblclick, so we will
          // not try to assert them here.
          return;
        }

        super.onBrowserEvent(event);
        assertMouseCoordinates(event);
        assertAllShiftKeysOn(event);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createDblClickEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createErrorEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerErrorEvent() {
    ImgEventListener listener = new ImgEventListener("error");
    Event.setEventListener(parent, listener);
    Event.setEventListener(img, listener);

    img.dispatchEvent(Document.get().createErrorEvent());

    assertTrue("Expected child to receive event", listener.imgReceived);
  }

  /**
   * Tests createFocusEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerFocusEvent() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "focus") {
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        assertEquals("focus", event.getType());
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createFocusEvent());

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createKeyDownEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerKeyDownEvent() {
    KeyEventListener listener = new KeyEventListener("keydown");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createKeyDownEvent(true, true, true,
        true, KEY_CODE, KEY_CODE));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createKeyPressEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerKeyPressEvent() {
    KeyEventListener listener = new KeyEventListener("keypress");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createKeyPressEvent(true, true, true,
        true, KEY_CODE, KEY_CODE));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createKeyUpEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerKeyUpEvent() {
    KeyEventListener listener = new KeyEventListener("keyup");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createKeyUpEvent(true, true, true, true,
        KEY_CODE, KEY_CODE));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createLoadEvent().
   */
  public void testTriggerLoadEvent() {
    ImgEventListener listener = new ImgEventListener("load");
    Event.setEventListener(parent, listener);
    Event.setEventListener(img, listener);

    img.dispatchEvent(Document.get().createLoadEvent());

    assertTrue("Expected img to receive event", listener.imgReceived);
  }

  /**
   * Tests createMouseDownEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerMouseDownEvent() {
    MouseEventListener listener = new MouseEventListener("mousedown");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createMouseDownEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true,
        Event.BUTTON_LEFT));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createMouseMoveEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerMouseMoveEvent() {
    MouseEventListener listener = new MouseEventListener("mousemove");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createMouseMoveEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true,
        Event.BUTTON_LEFT));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createMouseOutEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerMouseOutEvent() {
    MouseEventListener listener = new MouseEventListener("mouseout") {
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

// TODO: Re-enable this assertion when we no longer support Firefox2 and earlier.
// Old Firefoxen throw away the relatedTarget parameter of initMouseEvent().
//        Element relatedTarget = event.getRelatedTarget();
//        assertEquals("Expected relatedElement to be img", img, relatedTarget);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createMouseOutEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true,
        Event.BUTTON_LEFT, img));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createMouseOverEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerMouseOverEvent() {
    MouseEventListener listener = new MouseEventListener("mouseover") {
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);

// TODO: Re-enable this assertion when we no longer support Firefox2 and earlier.
// Old Firefoxen throw away the relatedTarget parameter of initMouseEvent().
//        Element relatedTarget = event.getRelatedTarget();
//        assertEquals("Expected relatedElement to be img", img, relatedTarget);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createMouseOverEvent(MOUSE_DETAIL, SCREEN_X, SCREEN_Y,
        CLIENT_X, CLIENT_Y, true, true, true, true, Event.BUTTON_LEFT, img));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createMouseUpEvent().
   */
  @DoNotRunWith({Platform.Htmlunit})
  public void testTriggerMouseUpEvent() {
    MouseEventListener listener = new MouseEventListener("mouseup");
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createMouseUpEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true,
        Event.BUTTON_LEFT));

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createScrollEvent().
   * TODO: Re-enable this test when we no longer support Firefox2 and earlier
   * (which doesn't appear to dispatch contextmenu events properly).
   */
//  public void testTriggerScrollEvent() {
//    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
//        "scroll") {
//      public void onBrowserEvent(Event event) {
//        super.onBrowserEvent(event);
//        assertEquals("scroll", event.getType());
//      }
//    };
//    Event.setEventListener(parent, listener);
//    Event.setEventListener(child, listener);
//
//    child.dispatchEvent(Document.get().createScrollEvent());
//
//    assertTrue("Expected child to receive event", listener.childReceived);
//  }

  @Override
  protected void gwtSetUp() throws Exception {
    parent = Document.get().createDivElement();
    child = Document.get().createTextInputElement();
    img = Document.get().createImageElement();

    Document.get().getBody().appendChild(parent);
    parent.appendChild(child);
    parent.appendChild(img);

    Event.sinkEvents(parent, ALL_EVENTS);
    Event.sinkEvents(child, ALL_EVENTS);
    Event.sinkEvents(img, ALL_EVENTS);
  }

  private void assertAllShiftKeysOn(Event event) {
    assertEquals("Expecting ctrl on", true, event.getCtrlKey());
    assertEquals("Expecting alt on", true, event.getAltKey());
    assertEquals("Expecting shift on", true, event.getShiftKey());
    assertEquals("Expecting meta on", true, event.getMetaKey());
  }

  private void assertMouseCoordinates(Event event) {
    assertEquals("clientX", CLIENT_X, event.getClientX());
    assertEquals("clientY", CLIENT_Y, event.getClientY());
    assertEquals("screenX", SCREEN_X, event.getScreenX());
    assertEquals("screenY", SCREEN_Y, event.getScreenY());
  }
}
