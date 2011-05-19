/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.touch.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.TouchEvent;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.touch.client.TouchScroller.TemporalPoint;
import com.google.gwt.user.client.ui.HasScrolling;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * Tests for {@link TouchScroller}.
 * 
 * <p>
 * Many of the tests in this class can run even in HtmlUnit and browsers that do
 * not support touch events because we create mock touch events.
 * </p>
 */
public class TouchScrollTest extends GWTTestCase {

  /**
   * A custom {@link ScrollPanel} that doesn't rely on the DOM to calculate its
   * vertical and horizontal position. Allows testing in HtmlUnit.
   */
  private static class CustomScrollPanel extends ScrollPanel {
    private final int maxHorizontalScrollPosition;
    private final int maxVerticalScrollPosition;
    private final int minHorizontalScrollPosition;
    private final int minVerticalScrollPosition;
    private int horizontalScrollPosition;
    private int verticalScrollPosition;

    /**
     * Construct a new {@link CustomScrollPanel} using 0 as the minimum vertical
     * and horizontal scroll position and INTEGER.MAX_VALUE as the maximum
     * positions.
     */
    public CustomScrollPanel() {
      this.minVerticalScrollPosition = 0;
      this.maxVerticalScrollPosition = 5000;
      this.minHorizontalScrollPosition = 0;
      this.maxHorizontalScrollPosition = 5000;
    }

    @Override
    public int getHorizontalScrollPosition() {
      return horizontalScrollPosition;
    }

    @Override
    public int getMaximumHorizontalScrollPosition() {
      return maxHorizontalScrollPosition;
    }

    @Override
    public int getMaximumVerticalScrollPosition() {
      return maxVerticalScrollPosition;
    }

    @Override
    public int getMinimumHorizontalScrollPosition() {
      return minHorizontalScrollPosition;
    }

    @Override
    public int getMinimumVerticalScrollPosition() {
      return minVerticalScrollPosition;
    }

    @Override
    public int getVerticalScrollPosition() {
      return verticalScrollPosition;
    }

    @Override
    public void setHorizontalScrollPosition(int position) {
      this.horizontalScrollPosition = position;
      super.setHorizontalScrollPosition(position);
    }

    @Override
    public void setVerticalScrollPosition(int position) {
      this.verticalScrollPosition = position;
      super.setVerticalScrollPosition(position);
    }
  }

  /**
   * A custom touch event.
   */
  private static class CustomTouchEvent extends TouchStartEvent {
  }

  /**
   * A {@link TouchScroller} that overrides drag events.
   */
  private static class CustomTouchScroller extends TouchScroller {

    private boolean setupBustClickHandlerCalled;
    private boolean removeBustClickHandlerCalled;
    private boolean removeAttachHandlerCalled;
    private boolean onDragEndCalled;
    private boolean onDragMoveCalled;
    private boolean onDragStartCalled;

    public CustomTouchScroller(HasScrolling widget) {
      super();
      setTargetWidget(widget);
    }

    public void assertOnDragEndCalled(boolean expected) {
      assertEquals(expected, onDragEndCalled);
      onDragEndCalled = false;
    }

    public void assertOnDragMoveCalled(boolean expected) {
      assertEquals(expected, onDragMoveCalled);
      onDragMoveCalled = false;
    }

    public void assertOnDragStartCalled(boolean expected) {
      assertEquals(expected, onDragStartCalled);
      onDragStartCalled = false;
    }

    public void assertSetupBustClickHandlerCalled(boolean expected) {
      assertEquals(expected, setupBustClickHandlerCalled);
      setupBustClickHandlerCalled = false;
    }

    public void assertRemoveBustClickHandlerCalled(boolean expected) {
      assertEquals(expected, removeBustClickHandlerCalled);
      removeBustClickHandlerCalled = false;
    }

    public void assertRemoveAttachHandlerCalled(boolean expected) {
      assertEquals(expected, removeAttachHandlerCalled);
      removeAttachHandlerCalled = false;
    }

    @Override
    protected void onDragEnd(TouchEvent<?> event) {
      assertFalse("onDragEnd called twice", onDragEndCalled);
      super.onDragEnd(event);
      onDragEndCalled = true;
    }

    @Override
    protected void onDragMove(TouchEvent<?> event) {
      assertFalse("onDragMove called twice", onDragMoveCalled);
      super.onDragMove(event);
      onDragMoveCalled = true;
    }

    @Override
    protected void onDragStart(TouchEvent<?> event) {
      assertFalse("onDragStart called twice", onDragStartCalled);
      super.onDragStart(event);
      onDragStartCalled = true;
    }

    @Override
    protected void setupBustClickHandler() {
      super.setupBustClickHandler();
      setupBustClickHandlerCalled = true;
    }

    @Override
    protected void removeBustClickHandler() {
      super.removeBustClickHandler();
      removeBustClickHandlerCalled = true;
    }

    @Override
    protected void removeAttachHandler() {
      super.removeAttachHandler();
      removeAttachHandlerCalled = true;
    }
  }

  /**
   * Create a mock native touch event that contains no touches.
   * 
   * @return an empty mock touch event
   */
  private static native NativeEvent createNativeTouchEvent() /*-{
    // Create a real event so standard event methods are available.
    var touches = [];
    return {
      "changedTouches" : touches,
      "targetTouches" : touches,
      "touches" : touches,
      "preventDefault" : function() {} // Called by TouchScroller.
    };
  }-*/;

  /**
   * Create a mock {@link Touch} for the specified x and y coordinate.
   * 
   * @param x the x coordinate
   * @param y the y coordinate
   * @return a mock touch
   */
  private static native Touch createTouch(int x, int y) /*-{
    return {
      "clientX" : x,
      "clientY" : y,
      "identifier" : 0,
      "pageX" : x,
      "pageY" : y,
      "screenX" : x,
      "screenY" : y,
      "target" : null
    };
  }-*/;

  /**
   * Create a mock TouchEndEvent. Touch end events do not have any touches.
   * 
   * @return a mock TouchEndEvent
   */
  private static TouchEvent<?> createTouchEndEvent() {
    CustomTouchEvent event = new CustomTouchEvent();
    event.setNativeEvent(createNativeTouchEvent());
    return event;
  }

  /**
   * Create a mock TouchMoveEvent for the specified x and y coordinate.
   * 
   * @param x the x coordinate
   * @param y the y coordinate
   * @return a mock TouchMoveEvent
   */
  private static TouchEvent<?> createTouchMoveEvent(int x, int y) {
    // TouchScroller doesn't care about the actual event subclass.
    return createTouchStartEvent(x, y);
  }

  /**
   * Create a mock {@link TouchStartEvent} for the specified x and y coordinate.
   * 
   * @param x the x coordinate
   * @param y the y coordinate
   * @return a mock {@link TouchStartEvent}
   */
  private static TouchEvent<?> createTouchStartEvent(int x, int y) {
    CustomTouchEvent event = new CustomTouchEvent();
    NativeEvent nativeEvent = createNativeTouchEvent();
    nativeEvent.getTouches().push(createTouch(x, y));
    event.setNativeEvent(nativeEvent);
    return event;
  }

  private CustomTouchScroller scroller;
  private CustomScrollPanel scrollPanel;

  @Override
  public String getModuleName() {
    return "com.google.gwt.touch.Touch";
  }

  public void testCalculateEndVlocity() {
    // Two points at the same time should return null.
    TemporalPoint from = new TemporalPoint(new Point(100.0, 200.0), 0);
    TemporalPoint sameTime = new TemporalPoint(new Point(100.0, 100.0), 0);
    assertNull(scroller.calculateEndVelocity(from, sameTime));

    // Two different points should return a velocity.
    TemporalPoint to = new TemporalPoint(new Point(250.0, 150.0), 25);
    assertEquals(new Point(-6.0, 2.0), scroller.calculateEndVelocity(from, to));
  }

  public void testCreateIfSupported() {
    // createIfSupported()
    TouchScroller scroller = TouchScroller.createIfSupported();
    if (TouchScroller.isSupported()) {
      assertNotNull("TouchScroll not created, but touch is supported", scroller);
      assertNull(scroller.getTargetWidget());

    } else {
      assertNull("TouchScroll created, but touch is not supported", scroller);
    }

    // createIfSupported(HasScrolling)
    HasScrolling target = new ScrollPanel();
    scroller = TouchScroller.createIfSupported(target);
    if (TouchScroller.isSupported()) {
      assertNotNull("TouchScroll not created, but touch is supported", scroller);
      assertEquals(target, scroller.getTargetWidget());

    } else {
      assertNull("TouchScroll created, but touch is not supported", scroller);
    }
  }

  public void testDeferToNativeScrollingBottom() {
    testDeferToNativeScrolling(0, scrollPanel.getMaximumVerticalScrollPosition(), 0, -100);
  }

  public void testDeferToNativeScrollingLeft() {
    testDeferToNativeScrolling(0, 0, 100, 0);
  }

  public void testDeferToNativeScrollingRight() {
    testDeferToNativeScrolling(scrollPanel.getMaximumHorizontalScrollPosition(), 0, -100, 0);
  }

  public void testDeferToNativeScrollingTop() {
    testDeferToNativeScrolling(0, 0, 0, 100);
  }

  /**
   * Test that touch events correctly initiate drag events.
   */
  public void testDragSequence() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Initial state.
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Start touching.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    scroller.assertOnDragStartCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Move, but not enough to drag.
    scroller.onTouchMove(createTouchMoveEvent(-1, 0));
    scroller.assertOnDragStartCalled(false);
    scroller.assertOnDragMoveCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Move.
    scroller.onTouchMove(createTouchMoveEvent(-100, 0));
    scroller.assertOnDragStartCalled(true);
    scroller.assertOnDragMoveCalled(true);
    assertTrue(scroller.isTouching());
    assertTrue(scroller.isDragging());

    // Move again.
    scroller.onTouchMove(createTouchMoveEvent(-200, 0));
    scroller.assertOnDragStartCalled(false); // drag already started.
    scroller.assertOnDragMoveCalled(true);
    assertTrue(scroller.isTouching());
    assertTrue(scroller.isDragging());

    // End.
    scroller.onTouchEnd(createTouchEndEvent());
    scroller.assertOnDragStartCalled(false);
    scroller.assertOnDragMoveCalled(false);
    scroller.assertOnDragEndCalled(true);
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }

  /**
   * Test that the bust click/attach event handler is removed from the old
   * widget.
   */
  public void testHandlersRemovedFromOldWidget() {
    CustomScrollPanel newScrollPanel = new CustomScrollPanel();

    // Initial state.
    scroller.assertRemoveBustClickHandlerCalled(true);
    scroller.assertRemoveAttachHandlerCalled(true);

    // Replace the old widget (scrollPanel) with the new widget (newScrollPanel)
    scroller.setTargetWidget(newScrollPanel);

    // Verify that the bust click handler and attach event handler are removed.
    scroller.assertRemoveBustClickHandlerCalled(true);
    scroller.assertRemoveAttachHandlerCalled(true);

    // Remove the old widget (scrollPanel) from the root panel.
    RootPanel.get().remove(scrollPanel);

    // Verify that removing the old widget doesn't cause removeBustClickHandler
    // from being called.
    scroller.assertRemoveBustClickHandlerCalled(false);
  }

  /**
   * Test that when momentum ends, the momentum command is set to null (and
   * isMomentumActive() returns false).
   */
  public void testMomentumEnd() {
    // Use a short lived momentum.
    scroller.setMomentum(new DefaultMomentum() {
      @Override
      public boolean updateState(State state) {
        // Immediately end momentum.
        return false;
      }
    });

    // Start a drag sequence.
    double millis = Duration.currentTimeMillis();
    scroller.getRecentTouchPosition().setTemporalPoint(new Point(0, 0), millis);
    scroller.getLastTouchPosition().setTemporalPoint(new Point(100, 100), millis + 100);

    // End the drag sequence.
    scroller.onDragEnd(createTouchEndEvent());
    scroller.assertOnDragEndCalled(true);
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
    assertTrue(scroller.isMomentumActive());

    // Force momentum to run, which causes it to end.
    getMomentumCommand(scroller).execute();
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
    assertFalse(scroller.isMomentumActive());
  }

  public void testOnDragEnd() {
    // Start a drag sequence.
    double millis = Duration.currentTimeMillis();
    scroller.getRecentTouchPosition().setTemporalPoint(new Point(0, 0), millis);
    scroller.getLastTouchPosition().setTemporalPoint(new Point(100, 100), millis + 100);

    // End the drag sequence.
    scroller.onDragEnd(createTouchEndEvent());
    assertTrue(scroller.isMomentumActive());
  }

  public void testOnDragEndNoMomentum() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Start a drag sequence.
    double millis = Duration.currentTimeMillis();
    scroller.getRecentTouchPosition().setTemporalPoint(new Point(0, 0), millis);
    scroller.getLastTouchPosition().setTemporalPoint(new Point(100, 100), millis + 100);

    // End the drag sequence.
    scroller.onDragEnd(createTouchEndEvent());
    assertFalse(scroller.isMomentumActive());
  }

  public void testOnDragMove() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Start at 100x100;
    scrollPanel.setHorizontalScrollPosition(100);
    scrollPanel.setVerticalScrollPosition(150);

    // Start touching.
    scroller.onTouchStart(createTouchStartEvent(0, 0));

    // Drag in a positive direction (negative scroll).
    TouchEvent<?> touchMove = createTouchMoveEvent(40, 50);
    scroller.onTouchMove(touchMove);
    scroller.assertOnDragMoveCalled(true);
    assertEquals(60, scrollPanel.getHorizontalScrollPosition());
    assertEquals(100, scrollPanel.getVerticalScrollPosition());

    // Drag in a negative direction (positive scroll).
    touchMove = createTouchMoveEvent(-20, -30);
    scroller.onTouchMove(touchMove);
    scroller.assertOnDragMoveCalled(true);
    assertEquals(120, scrollPanel.getHorizontalScrollPosition());
    assertEquals(180, scrollPanel.getVerticalScrollPosition());
  }

  /**
   * Test that touch end events are ignored if not touching.
   */
  public void testOnTouchEndIgnored() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Initial state.
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Verify that an extraneous touch end event is ignored.
    scroller.onTouchEnd(createTouchEndEvent());
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }

  /**
   * Test that we handle touch end events that occur without initiating a drag
   * sequence.
   */
  public void testOnTouchEndWithoutDrag() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Initial state.
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Start touching.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    scroller.assertOnDragStartCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Move, but not enough to drag.
    scroller.onTouchMove(createTouchMoveEvent(1, 0));
    scroller.assertOnDragStartCalled(false);
    scroller.assertOnDragMoveCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // End.
    scroller.onTouchEnd(createTouchEndEvent());
    scroller.assertOnDragStartCalled(false);
    scroller.assertOnDragMoveCalled(false);
    scroller.assertOnDragEndCalled(false);
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }

  /**
   * Test that touch move events are ignored if not touching.
   */
  public void testOnTouchMoveIgnored() {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Initial state.
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Verify that an extraneous touchmove event is ignored.
    scroller.onTouchMove(createTouchMoveEvent(0, 0));
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }

  /**
   * Test that touch start events cancel any active momentum.
   */
  public void testOnTouchCancelsMomentum() {
    // Start momentum.
    double millis = Duration.currentTimeMillis();
    scroller.getRecentTouchPosition().setTemporalPoint(new Point(0, 0), millis);
    scroller.getLastTouchPosition().setTemporalPoint(new Point(100, 100), millis + 100);
    scroller.onDragEnd(createTouchEndEvent());
    assertTrue(scroller.isMomentumActive());

    // Touch again.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    assertFalse(scroller.isMomentumActive());
  }

  /**
   * Test that touch start events are ignored if already touching.
   */
  public void testOnTouchStartIgnored() {
    scroller.setMomentum(null); // Disable momentum for this test.

    // Initial state.
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Start touching.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    scroller.assertOnDragStartCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Verify that additional start events do not cause errors.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    scroller.assertOnDragStartCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }

  /**
   * Test that the setupBustClickHandler is called when the widget is detached
   * and re-attached.
   */
  public void testSetupBustClickHandler() {
    // Initial state.
    scroller.assertRemoveBustClickHandlerCalled(true);
    scroller.assertRemoveAttachHandlerCalled(true);
    scroller.assertSetupBustClickHandlerCalled(true);

    RootPanel.get().remove(scrollPanel);

    // Verify that the bust click handler is removed.
    scroller.assertRemoveBustClickHandlerCalled(true);

    RootPanel.get().add(scrollPanel);

    // Verify that the bust click handler is setup.
    scroller.assertSetupBustClickHandlerCalled(true);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    // Create and attach a widget that has scrolling.
    scrollPanel = new CustomScrollPanel();
    scrollPanel.setPixelSize(500, 500);
    Label content = new Label("Content");
    content.setPixelSize(10000, 10000);
    RootPanel.get().add(scrollPanel);

    // Disabled touch scrolling because we will add our own scroller.
    scrollPanel.setTouchScrollingDisabled(true);

    // Add scrolling support.
    scroller = new CustomTouchScroller(scrollPanel);
  }

  /**
   * A replacement for JUnit's {@link #tearDown()} method. This method runs once
   * per test method in your subclass, just after your each test method runs and
   * can be used to perform cleanup. Override this method instead of
   * {@link #tearDown()}. This method is run even in pure Java mode (non-GWT).
   * 
   * @see #setForcePureJava
   */
  @Override
  protected void gwtTearDown() throws Exception {
    // Detach the widget.
    RootPanel.get().remove(scrollPanel.asWidget());
    scrollPanel = null;
    scroller = null;
  }

  /**
   * Get the momentum command from the specified {@link TouchScroller}.
   */
  private native RepeatingCommand getMomentumCommand(TouchScroller scroller) /*-{
    return scroller.@com.google.gwt.touch.client.TouchScroller::momentumCommand;
  }-*/;

  /**
   * Test that {@link TouchScroller} defers to native scrolling if the
   * scrollable widget is already scrolled as far as it can go.
   * 
   * @param hStart the starting horizontal scroll position
   * @param vStart the starting vertical scroll position
   * @param xEnd the ending x touch coordinate
   * @param yEnd the ending y touch coordinate
   */
  private void testDeferToNativeScrolling(int hStart, int vStart, int xEnd, int yEnd) {
    // Disable momentum for this test.
    scroller.setMomentum(null);

    // Scroll to the left.
    scrollPanel.setHorizontalScrollPosition(hStart);
    scrollPanel.setVerticalScrollPosition(vStart);

    // Start touching.
    scroller.onTouchStart(createTouchStartEvent(0, 0));
    scroller.assertOnDragStartCalled(false);
    assertTrue(scroller.isTouching());
    assertFalse(scroller.isDragging());

    // Move to the left.
    scroller.onTouchMove(createTouchMoveEvent(xEnd, yEnd));
    scroller.assertOnDragStartCalled(false);
    scroller.assertOnDragMoveCalled(false);
    assertFalse(scroller.isTouching());
    assertFalse(scroller.isDragging());
  }
}
