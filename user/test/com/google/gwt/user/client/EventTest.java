/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

/**
 * Test Case for {@link Event}.
 */
public class EventTest extends GWTTestCase {
  /**
   * An EventPreview used for testing.
   */
  @SuppressWarnings("deprecation")
  private static class TestEventPreview implements EventPreview {
    private boolean doCancel;
    private boolean isFired = false;

    /**
     * Construct a new {@link TestEventPreview}.
     * 
     * @param doCancel if true, cancel the event
     */
    public TestEventPreview(boolean doCancel) {
      this.doCancel = doCancel;
    }

    @Deprecated
    public boolean onEventPreview(Event event) {
      assertFalse(isFired);
      isFired = true;
      return !doCancel;
    }

    public void assertIsFired(boolean expected) {
      assertEquals(expected, isFired);
    }
  }

  /**
   * A NativePreviewHandler used for testing.
   */
  private static class TestNativePreviewHandler implements NativePreviewHandler {
    private boolean doCancel;
    private boolean doPreventCancel;
    private boolean isFired = false;

    /**
     * Construct a new {@link TestNativePreviewHandler}.
     * 
     * @param doCancel if true, cancel the event
     * @param doPreventCancel if true, prevent the event from being canceled
     */
    public TestNativePreviewHandler(boolean doCancel, boolean doPreventCancel) {
      this.doCancel = doCancel;
      this.doPreventCancel = doPreventCancel;
    }

    public void onPreviewNativeEvent(NativePreviewEvent event) {
      assertFalse(isFired);
      isFired = true;
      if (doCancel) {
        event.cancel();
      }
      if (doPreventCancel) {
        event.consume();
      }
    }

    public void assertIsFired(boolean expected) {
      assertEquals(expected, isFired);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that {@link Event#fireNativePreviewEvent(Event)} returns the correct
   * value if the native event is canceled.
   */
  public void testFireNativePreviewEventCancel() {
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(true,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(true,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    assertFalse(Event.fireNativePreviewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
  }

  /**
   * Test that {@link Event#fireNativePreviewEvent(Event)} returns the correct
   * value if the native event is prevented from being canceled, even if another
   * handler cancels the event.
   */
  public void testFireNativePreviewEventPreventCancel() {
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        true);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(true,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    assertTrue(Event.fireNativePreviewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
  }

  /**
   * Test that {@link Event#fireNativePreviewEvent(Event)} fires handlers in
   * reverse order. Also verify that the legacy EventPreview fires last.
   */
  @SuppressWarnings("deprecation")
  public void testFireNativePreviewEventReverseOrder() {
    final TestEventPreview preview = new TestEventPreview(false);
    final TestNativePreviewHandler handler0 = new TestNativePreviewHandler(
        false, false) {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        super.onPreviewNativeEvent(event);
        preview.assertIsFired(false);
      }
    };
    final TestNativePreviewHandler handler1 = new TestNativePreviewHandler(
        false, false) {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        super.onPreviewNativeEvent(event);
        handler0.assertIsFired(false);
        preview.assertIsFired(false);
      }
    };
    final TestNativePreviewHandler handler2 = new TestNativePreviewHandler(
        false, false) {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        super.onPreviewNativeEvent(event);
        handler0.assertIsFired(false);
        handler1.assertIsFired(false);
        preview.assertIsFired(false);
      }
    };
    final TestNativePreviewHandler handler3 = new TestNativePreviewHandler(
        false, false) {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        super.onPreviewNativeEvent(event);
        handler0.assertIsFired(false);
        handler1.assertIsFired(false);
        handler2.assertIsFired(false);
        preview.assertIsFired(false);
      }
    };
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    HandlerRegistration reg2 = Event.addNativePreviewHandler(handler2);
    HandlerRegistration reg3 = Event.addNativePreviewHandler(handler3);
    DOM.addEventPreview(preview);
    assertTrue(DOM.previewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    handler2.assertIsFired(true);
    handler3.assertIsFired(true);
    preview.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
    reg2.removeHandler();
    reg3.removeHandler();
    DOM.removeEventPreview(preview);
  }

  /**
   * Test removal of a {@link NativePreviewHandler} works.
   */
  public void testFireNativePreviewEventRemoval() {
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(false,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    reg1.removeHandler();
    assertTrue(Event.fireNativePreviewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(false);
    reg0.removeHandler();
  }

  /**
   * Test that {@link Event#fireNativePreviewEvent(Event)} returns the correct
   * value if the native event is not canceled.
   */
  public void testFireNativePreviewEventWithoutCancel() {
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(false,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    assertTrue(Event.fireNativePreviewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
  }

  /**
   * Test that {@link Event#fireNativePreviewEvent(Event)} returns the correct
   * value if no handlers are present.
   */
  public void testFireNativePreviewEventWithoutHandlers() {
    assertTrue(Event.fireNativePreviewEvent(null));
  }

  /**
   * Test that legacy EventPreview and NativePreviewHandlers can both cancel the
   * event.
   */
  @Deprecated
  public void testLegacyEventPreviewCancelByBoth() {
    // Add handlers
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(true,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);

    // Add legacy EventPreview
    TestEventPreview preview = new TestEventPreview(true);
    DOM.addEventPreview(preview);

    // Fire the event
    assertFalse(DOM.previewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    preview.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
    DOM.removeEventPreview(preview);
  }

  /**
   * Test that legacy EventPreview can cancel the event.
   */
  @Deprecated
  public void testLegacyEventPreviewCancelByEventPreview() {
    // Add handlers
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(false,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);

    // Add legacy EventPreview
    TestEventPreview preview = new TestEventPreview(true);
    DOM.addEventPreview(preview);

    // Fire the event
    assertFalse(DOM.previewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    preview.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
    DOM.removeEventPreview(preview);
  }

  /**
   * Test that legacy EventPreview still fires after the NativeHandler cancels
   * the event.
   */
  @Deprecated
  public void testLegacyEventPreviewCancelByHandler() {
    // Add handlers
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(true,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);

    // Add legacy EventPreview
    TestEventPreview preview = new TestEventPreview(false);
    DOM.addEventPreview(preview);

    // Fire the event
    assertFalse(DOM.previewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    preview.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
    DOM.removeEventPreview(preview);
  }

  /**
   * Test that legacy EventPreview still fires after the NativeHandlers without
   * canceling the event.
   */
  @Deprecated
  public void testLegacyEventPreviewWithoutCancel() {
    // Add handlers
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(false,
        false);
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(false,
        false);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);

    // Add legacy EventPreview
    TestEventPreview preview = new TestEventPreview(false);
    DOM.addEventPreview(preview);

    // Fire the event
    assertTrue(DOM.previewEvent(null));
    handler0.assertIsFired(true);
    handler1.assertIsFired(true);
    preview.assertIsFired(true);
    reg0.removeHandler();
    reg1.removeHandler();
    DOM.removeEventPreview(preview);
  }

  /**
   * Test the accessors in {@link NativePreviewEvent}.
   */
  public void testNativePreviewEventAccessors() {
    // cancelNativeEvent
    {
      NativePreviewEvent event = new NativePreviewEvent();
      assertFalse(event.isCanceled());
      event.cancel();
      assertTrue(event.isCanceled());
    }

    // preventCancelNativeEvent
    {
      NativePreviewEvent event = new NativePreviewEvent();
      assertFalse(event.isConsumed());
      event.consume();
      assertTrue(event.isConsumed());
    }

    // revive
    {
      NativePreviewEvent event = new NativePreviewEvent();
      event.cancel();
      event.consume();
      assertTrue(event.isCanceled());
      assertTrue(event.isConsumed());
      event.revive();
      assertFalse(event.isCanceled());
      assertFalse(event.isConsumed());
    }
  }

  /**
   * Test that the singleton instance of {@link NativePreviewEvent} is revived
   * correctly.
   */
  public void testReviveNativePreviewEvent() {
    // Fire the event and cancel it
    TestNativePreviewHandler handler0 = new TestNativePreviewHandler(true, true);
    HandlerRegistration reg0 = Event.addNativePreviewHandler(handler0);
    Event.fireNativePreviewEvent(null);
    handler0.assertIsFired(true);
    reg0.removeHandler();

    // Fire the event again, but don't cancel it
    TestNativePreviewHandler handler1 = new TestNativePreviewHandler(false,
        false) {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        assertFalse(event.isCanceled());
        assertFalse(event.isConsumed());
        super.onPreviewNativeEvent(event);
      }
    };
    HandlerRegistration reg1 = Event.addNativePreviewHandler(handler1);
    assertTrue(Event.fireNativePreviewEvent(null));
    handler1.assertIsFired(true);
    reg1.removeHandler();
  }
}
