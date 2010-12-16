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
import com.google.gwt.dom.client.NativeEvent;
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
  private static class KeyCodeEventListener extends BubbleAssertingEventListener {

    public KeyCodeEventListener(String eventType) {
      super(eventType, true);
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (cancelled) {
        return;
      }
      super.onBrowserEvent(event);
      assertEquals(KEY_CODE, event.getKeyCode());
      // shouldn't throw:
      event.getCharCode();
    }
  }

  /**
   * Listener for use with key[down up press].
   */
  private static class KeyPressEventListener extends BubbleAssertingEventListener {
    public KeyPressEventListener() {
      super("keypress", true);
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (cancelled) {
        return;
      }
      super.onBrowserEvent(event);

      assertEquals(KEY_CODE, event.getCharCode());
      // shouldn't throw:
      event.getKeyCode();
    }
  }

  /**
   * Listener for use with mouse[down up move over out].
   */
  private static class MouseEventListener extends BubbleAssertingEventListener {
    public MouseEventListener(String eventType) {
      super(eventType, true);
    }

    @Override
    public void onBrowserEvent(Event event) {
      if (cancelled) {
        return;
      }
      super.onBrowserEvent(event);
      assertMouseCoordinates(event);
      assertEquals(Event.BUTTON_LEFT, event.getButton());
    }
  }

  /**
   * An event listener that asserts that the event is passed to child, then
   * parent.
   */
  private static class BubbleAssertingEventListener implements EventListener {
    protected boolean cancelled = false;
    private final String eventType;
    private boolean expectedAlt = true;
    private boolean expectedCtrl = true;
    private boolean expectedMeta = true;
    private boolean expectedShift = true;
    private boolean parentReceived, childReceived;
    private boolean supportsShiftKeys;

    public BubbleAssertingEventListener(String eventType,
        boolean supportsShiftKeys) {
      this.eventType = eventType;
      this.supportsShiftKeys = supportsShiftKeys;
    }

    public void assertReceived() {
      if (cancelled) {
        return;
      }

      assertTrue("Expected child to receive event", childReceived);
      assertTrue("Expected parent to receive event", parentReceived);
      childReceived = false;
      parentReceived = false;
    }

    public void cancel() {
      cancelled = true;
    }

    public void onBrowserEvent(Event event) {
      if (cancelled) {
        return;
      }

      assertEquals(eventType, event.getType());
      if (supportsShiftKeys) {
        assertAllShiftKeys(event, expectedCtrl, expectedAlt, expectedShift,
            expectedMeta);
      }

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

    /**
     * Set the expected shift keys that should be on during the next event.
     */
    public void setExpectedShiftKeys(boolean expectedCtrl, boolean expectedAlt,
        boolean expectedShift, boolean expectedMeta) {
      this.expectedCtrl = expectedCtrl;
      this.expectedAlt = expectedAlt;
      this.expectedShift = expectedShift;
      this.expectedMeta = expectedMeta;
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
  private static class NonBubbleAssertingEventListener implements EventListener {
    protected boolean cancelled = false;
    private boolean childReceived;
    private String eventType;

    public NonBubbleAssertingEventListener(String eventType) {
      this.eventType = eventType;
    }

    public void cancel() {
      cancelled = true;
    }

    public void onBrowserEvent(Event event) {
      if (cancelled) {
        return;
      }

      assertEquals(eventType, event.getType());

      if (event.getEventTarget().equals(child)) {
        assertFalse("Expecting child to receive the event only once",
            childReceived);
        childReceived = true;
      } else if (event.getEventTarget().equals(parent)) {
        fail("Not expecting parent to receive the event");
      }
    }
  }

  /**
   * Interface to create a new event for testing.
   */
  private static interface EventCreator {
    NativeEvent createEvent(boolean ctrlKey, boolean altKey, boolean shiftKey,
        boolean metaKey);
  }

  /**
   * An event listener that asserts that events are received properly for the
   * img element.
   */
  private static class ImgEventListener implements EventListener {
    private final String eventType;
    private boolean imgReceived;

    public ImgEventListener(String eventType) {
      this.eventType = eventType;
    }

    public void onBrowserEvent(Event event) {
      if (event.getType().equals(eventType)) {
        if (event.getEventTarget().equals(img)) {
          assertFalse("Expecting img to receive the event only once",
              imgReceived);

          imgReceived = true;
        } else if (event.getEventTarget().equals(parent)) {
          fail("Not expecting parent to receive the event");
        }
      }
    }
  }

  private static final int ALL_EVENTS = Event.MOUSEEVENTS | Event.KEYEVENTS
      | Event.FOCUSEVENTS | Event.ONCHANGE | Event.ONCLICK | Event.ONDBLCLICK
      | Event.ONCONTEXTMENU | Event.ONLOAD | Event.ONERROR | Event.ONSCROLL;

  private static InputElement child;

  private static final int CLIENT_X = 2;
  private static final int CLIENT_Y = 3;
  private static ImageElement img;
  private static final int KEY_CODE = 'A';
  private static final int MOUSE_DETAIL = 1;
  private static DivElement parent;

  private static final int SCREEN_X = 4;
  private static final int SCREEN_Y = 5;

  /**
   * Assert that all shift keys are in the expected state.
   *
   * @param event the event that was triggered
   */
  private static void assertAllShiftKeys(Event event, boolean expectedCtrl,
      boolean expectedAlt, boolean expectedShift, boolean expectedMeta) {
    assertEquals("Expecting ctrl = " + expectedCtrl, expectedCtrl,
        event.getCtrlKey());
    assertEquals("Expecting alt = " + expectedAlt, expectedAlt,
        event.getAltKey());
    assertEquals("Expecting shift = " + expectedShift, expectedShift,
        event.getShiftKey());
    assertEquals("Expecting meta = " + expectedMeta, expectedMeta,
        event.getMetaKey());
  }
  private static void assertMouseCoordinates(Event event) {
    assertEquals("clientX", CLIENT_X, event.getClientX());
    assertEquals("clientY", CLIENT_Y, event.getClientY());
    assertEquals("screenX", SCREEN_X, event.getScreenX());
    assertEquals("screenY", SCREEN_Y, event.getScreenY());
  }

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
        false, 65));
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
        if (event.getCurrentEventTarget().equals(child)) {
          event.stopPropagation();
        }
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createClickEvent(MOUSE_DETAIL, SCREEN_X,
        SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createBlurEvent().
   */
  public void testTriggerBlurEvent() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "blur") {
      @Override
      public void onBrowserEvent(Event event) {
        if (cancelled) {
          return;
        }
        super.onBrowserEvent(event);
        assertEquals("blur", event.getType());
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createBlurEvent());
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createChangeEvent().
   */
  public void testTriggerChangeEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "change", false);
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createChangeEvent());
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createClickEvent().
   */
  public void testTriggerClickEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "click", true) {
      @Override
      public void onBrowserEvent(Event event) {
        if (cancelled) {
          return;
        }
        super.onBrowserEvent(event);
        assertMouseCoordinates(event);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createClickEvent(MOUSE_DETAIL, SCREEN_X,
        SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createContextMenuEvent().
   */
  public void testTriggerContextMenuEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "contextmenu", false);
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createContextMenuEvent());
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createDblClickEvent().
   */
  public void testTriggerDblClickEvent() {
    BubbleAssertingEventListener listener = new BubbleAssertingEventListener(
        "dblclick", true) {
      @Override
      public void onBrowserEvent(Event event) {
        if (cancelled) {
          return;
        }
        if (event.getTypeInt() == Event.ONCLICK) {
          // Some browsers (IE, I'm looking at you) synthesize an extra click
          // event when a double-click is triggered. This synthesized click
          // will *not* have the same properties as the dblclick, so we will
          // not try to assert them here.
          return;
        }

        super.onBrowserEvent(event);
        assertMouseCoordinates(event);
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createDblClickEvent(MOUSE_DETAIL,
        SCREEN_X, SCREEN_Y, CLIENT_X, CLIENT_Y, true, true, true, true));
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
    assertTrue("Expected parent to receive event", listener.parentReceived);
  }

  /**
   * Tests createErrorEvent().
   *
   * Failed in all modes due to HtmlUnit bug:
   * https://sourceforge.net/tracker/?func
   * =detail&aid=2888342&group_id=47038&atid=448266
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
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
  public void testTriggerFocusEvent() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "focus") {
      @Override
      public void onBrowserEvent(Event event) {
        if (cancelled) {
          return;
        }
        super.onBrowserEvent(event);
        assertEquals("focus", event.getType());
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createFocusEvent());
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
  }

  /**
   * Tests createKeyDownEvent().
   */
  public void testTriggerKeyDownEvent() {
    KeyCodeEventListener listener = new KeyCodeEventListener("keydown");
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createKeyDownEvent(ctrlKey, altKey, shiftKey,
            metaKey, KEY_CODE);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createKeyPressEvent().
   *
   * Failed in all modes due to HtmlUnit bug:
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testTriggerKeyPressEvent() {
    KeyPressEventListener listener = new KeyPressEventListener();
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createKeyPressEvent(ctrlKey, altKey, shiftKey,
            metaKey, KEY_CODE);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createKeyUpEvent().
   */
  public void testTriggerKeyUpEvent() {
    KeyCodeEventListener listener = new KeyCodeEventListener("keyup");
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createKeyUpEvent(ctrlKey, altKey, shiftKey,
            metaKey, KEY_CODE);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
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
  public void testTriggerMouseDownEvent() {
    MouseEventListener listener = new MouseEventListener("mousedown");
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createMouseDownEvent(MOUSE_DETAIL, SCREEN_X,
            SCREEN_Y, CLIENT_X, CLIENT_Y, ctrlKey, altKey, shiftKey, metaKey,
            Event.BUTTON_LEFT);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createMouseMoveEvent().
   */
  public void testTriggerMouseMoveEvent() {
    MouseEventListener listener = new MouseEventListener("mousemove");
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createMouseMoveEvent(MOUSE_DETAIL, SCREEN_X,
            SCREEN_Y, CLIENT_X, CLIENT_Y, ctrlKey, altKey, shiftKey, metaKey,
            Event.BUTTON_LEFT);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createMouseOutEvent().
   *
   * Failed in all modes due to HtmlUnit bug:
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testTriggerMouseOutEvent() {
    MouseEventListener listener = new MouseEventListener("mouseout") {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        Element relatedTarget = event.getRelatedEventTarget().cast();
        assertEquals("Expected relatedElement to be img", img, relatedTarget);
      }
    };
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createMouseOutEvent(MOUSE_DETAIL, SCREEN_X,
            SCREEN_Y, CLIENT_X, CLIENT_Y, ctrlKey, altKey, shiftKey, metaKey,
            Event.BUTTON_LEFT, img);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createMouseOverEvent().
   *
   * Failed in all modes due to HtmlUnit bug:
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testTriggerMouseOverEvent() {
    MouseEventListener listener = new MouseEventListener("mouseover") {
      @Override
      public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        Element relatedTarget = event.getRelatedEventTarget().cast();
        assertEquals("Expected relatedElement to be img", img, relatedTarget);
      }
    };
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createMouseOverEvent(MOUSE_DETAIL, SCREEN_X,
            SCREEN_Y, CLIENT_X, CLIENT_Y, ctrlKey, altKey, shiftKey, metaKey,
            Event.BUTTON_LEFT, img);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createMouseUpEvent().
   */
  public void testTriggerMouseUpEvent() {
    MouseEventListener listener = new MouseEventListener("mouseup");
    EventCreator creator = new EventCreator() {
      public NativeEvent createEvent(boolean ctrlKey, boolean altKey,
          boolean shiftKey, boolean metaKey) {
        return Document.get().createMouseUpEvent(MOUSE_DETAIL, SCREEN_X,
            SCREEN_Y, CLIENT_X, CLIENT_Y, ctrlKey, altKey, shiftKey, metaKey,
            Event.BUTTON_LEFT);
      }
    };
    testTriggerEventWithShiftKeys(listener, creator);
    listener.cancel();
  }

  /**
   * Tests createKeyPressEvent().
   *
   * Failed in all modes due to HtmlUnit bug:
   */
  @DoNotRunWith({Platform.HtmlUnitBug})
  public void testTriggerScrollEvent() {
    NonBubbleAssertingEventListener listener = new NonBubbleAssertingEventListener(
        "scroll") {
      @Override
      public void onBrowserEvent(Event event) {
        if (cancelled) {
          return;
        }
        super.onBrowserEvent(event);
        assertEquals("scroll", event.getType());
      }
    };
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    child.dispatchEvent(Document.get().createScrollEvent());
    listener.cancel();

    assertTrue("Expected child to receive event", listener.childReceived);
  }

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

  /**
   * Test an event that supports shift keys by testing each shift key
   * individually.
   */
  private void testTriggerEventWithShiftKeys(
      BubbleAssertingEventListener listener, EventCreator creator) {
    Event.setEventListener(parent, listener);
    Event.setEventListener(child, listener);

    listener.setExpectedShiftKeys(true, true, true, true);
    child.dispatchEvent(creator.createEvent(true, true, true, true));
    listener.assertReceived();

    listener.setExpectedShiftKeys(true, false, false, false);
    child.dispatchEvent(creator.createEvent(true, false, false, false));
    listener.assertReceived();

    listener.setExpectedShiftKeys(false, true, false, false);
    child.dispatchEvent(creator.createEvent(false, true, false, false));
    listener.assertReceived();

    listener.setExpectedShiftKeys(false, false, true, false);
    child.dispatchEvent(creator.createEvent(false, false, true, false));
    listener.assertReceived();

    listener.setExpectedShiftKeys(false, false, false, true);
    child.dispatchEvent(creator.createEvent(false, false, false, true));
    listener.assertReceived();
  }
}
