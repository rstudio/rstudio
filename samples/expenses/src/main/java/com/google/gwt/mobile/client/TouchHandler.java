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
package com.google.gwt.mobile.client;

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

/**
 * Class that handles all touch events and uses them to interpret higher level
 * gestures and behaviors.
 * 
 * Examples of higher level gestures this class is intended to support - click,
 * double click, long click - dragging, swiping, zooming
 * 
 * Touch Behavior: Use this class to make your elements 'touchable' (see
 * touchable.js). Intended to work with all webkit browsers, tested only on
 * iPhone 3.x so far.
 * 
 * Drag Behavior: Use this class to make your elements 'draggable' (see
 * draggable.js). This behavior will handle all of the required events and
 * report the properties of the drag to you while the touch is happening and at
 * the end of the drag sequence. This behavior will NOT perform the actual
 * dragging (redrawing the element) for you, this responsibility is left to the
 * client code. This behavior contains a work around for a mobile safari but
 * where the 'touchend' event is not dispatched when the touch goes past the
 * bottom of the browser window. This is intended to work well in iframes.
 * Intended to work with all webkit browsers, tested only on iPhone 3.x so far.
 */
public class TouchHandler implements EventListener {

  /**
   * Delegate to receive drag events.
   */
  public interface DragDelegate {

    /**
     * The object's drag sequence is now complete.
     *
     * @param e The touchend event.
     */
    void onDragEnd(TouchEvent e);

    /**
     * The object has been dragged to a new position.
     *
     * @param e The touchmove event.
     */
    void onDragMove(TouchEvent e);

    /**
     * The object has started dragging.
     *
     * @param e The touchmove event.
     */
    void onDragStart(TouchEvent e);
  }

  /**
   * Delegate to receive touch events.
   */
  public interface TouchDelegate {

    /**
     * The object has received a touchend event.
     *
     * @param e The touchend event.
     */
    void onTouchEnd(TouchEvent e);

    /**
     * The object has received a touchstart event.
     *
     * @param e The touchstart event.
     * @return true if you want to allow a drag sequence to begin,
     *      false you want to disable dragging for the duration of this touch.
     */
    boolean onTouchStart(TouchEvent e);
  }

  /**
   * Whether or not the browser supports touches.
   */
  private static final boolean SUPPORTS_TOUCHES = supportsTouch();

  /**
   * Cancel event name.
   */
  private static final String CANCEL_EVENT = "touchcancel";

  /**
   * Threshold in pixels within which to bust clicks.
   */
  private static final int CLICK_BUST_THRESHOLD = 25;

  /**
   * End event name.
   */
  private static final String END_EVENT = SUPPORTS_TOUCHES ? "touchend"
      : "mouseup";

  /**
   * The number of ms to wait during a drag before updating the reported start
   * position of the drag.
   */
  private static final double MAX_TRACKING_TIME = 200;

  /**
   * Minimum movement of touch required to be considered a drag.
   */
  private static final double MIN_TRACKING_FOR_DRAG = 5;

  /**
   * Move event name.
   */
  private static final String MOVE_EVENT = SUPPORTS_TOUCHES ? "touchmove"
      : "mousemove";

  /**
   * Start event name.
   */
  private static final String START_EVENT = SUPPORTS_TOUCHES ? "touchstart"
      : "mousedown";

  /**
   * The threshold for when to start tracking whether a touch has left the
   * bottom of the browser window. Used to implement a workaround for a mobile
   * safari bug on the iPhone where a touchend event will never be fired if the
   * touch goes past the bottom of the window.
   */
  private static final double TOUCH_END_WORKAROUND_THRESHOLD = 20;

  /**
   * Get touch from event. Supports desktop events by returning the event that
   * is passed in as a parameter.
   * 
   * @param e the event
   * @return the touch object
   */
  public static Touch getTouchFromEvent(TouchEvent e) {
    if (SUPPORTS_TOUCHES) {
      return e.getOldTouchesUntilMyFriendFredSauerCleansUpTheSample().get(0);
    }

    // This is cheating a little bit, but it turns out that the Touch interface
    // overlays nicely on the regular NativeEvent interface.
    return e.cast();
  }

  /**
   * Determines whether the current platform supports touch events.
   * 
   * TODO(jgw): This should probably be implemented using deferred binding.
   */
  public static native boolean supportsTouch() /*-{
    // document.createTouch doesn't exist on Android, even though touch works.
    var android = navigator.userAgent.indexOf('Android') != -1;
    return android || !!('createTouch' in document);
  }-*/;

  /**
   * This would not be safe on all browsers, but none of the WebKit browsers
   * that actually support touch events have the kinds of memory leak problems
   * that this would trigger. And they all support event capture.
   */
  private static native void addEventListener(Element elem, String name,
      EventListener listener, boolean capture) /*-{
    elem.addEventListener(name, function(e) {
      listener.@com.google.gwt.user.client.EventListener::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(e);
    }, capture);
  }-*/;

  private boolean bustNextClick;
  private DragDelegate dragDelegate;

  private Element element;

  /**
   * Start/end time of the touchstart event.
   */
  private double endTime;
  /**
   * The touch position of the last event before the touchend event.
   */
  private Point endTouchPosition;
  private TouchEvent lastEvent;

  private Point lastTouchPosition;

  /**
   * The time of the most recent relevant occurence. For most drag sequences
   * this will be the same as the startTime. If the touch gesture changes
   * direction significantly or pauses for a while this time will be updated to
   * the time of the last touchmove event.
   */
  private double recentTime;

  /**
   * The coordinate of the most recent relevant touch event. For most drag
   * sequences this will be the same as the startCoordinate. If the touch
   * gesture changes direction significantly or pauses for a while this
   * coordinate will be updated to the coordinate of the last touchmove event.
   */
  private Point recentTouchPosition;

  private Timer scrollOffTimer;

  private Point startTouchPosition;
  /**
   * The absolute sum of all touch x/y deltas.
   */
  private double totalMoveX, totalMoveY = 0;
  private TouchDelegate touchDelegate;
  private boolean touching, tracking, dragging;

  public TouchHandler(Element elem) {
    this.element = elem;
    this.totalMoveY = 0;
    this.totalMoveX = 0;
  }

  /**
   * Start listenting for events.
   */
  public void enable() {
    addEventListener(element, START_EVENT, this, false);
    addEventListener(element, MOVE_EVENT, this, false);
    addEventListener(element, CANCEL_EVENT, this, false);
    addEventListener(element, END_EVENT, this, false);

    // Capture click so we can properly bust it, no matter what order handlers
    // get fired in.
    addEventListener(element, "click", this, true);
  }

  /**
   * Get end velocity of the drag. This method is specific to drag behavior, so
   * if touch behavior and drag behavior is split then this should go with drag
   * behavior. End velocity is defined as deltaXY / deltaTime where deletaXY is
   * the difference between endPosition and recentPosition, and deltaTime is the
   * difference between endTime and recentTime.
   * 
   * @return The x and y velocity.
   */
  public Point getEndVelocity() {
    assert recentTouchPosition != null : "Recent position not set";
    assert endTouchPosition != null : "End position not set";

    double time = endTime - recentTime;
    return new Point(
        (endTouchPosition.x - recentTouchPosition.x) / time,
        (endTouchPosition.y - recentTouchPosition.y) / time);
  }

  /**
   * Is the touch manager currently tracking touch moves to detect a drag?
   * 
   * @return True if currently tracking.
   */
  public boolean isTracking() {
    return tracking;
  }

  public void onBrowserEvent(Event event) {
    TouchEvent e = event.cast();
    String type = e.getType();
    if (START_EVENT.equals(type)) {
      onStart(e);
    } else if (MOVE_EVENT.equals(type)) {
      onMove(e);
    } else if (END_EVENT.equals(type) || CANCEL_EVENT.equals(type)) {
      onEnd(e);
    } else if ("click".equals(type)) {
      if (bustNextClick) {
        event.stopPropagation();
        event.preventDefault();
        bustNextClick = false;
      }
    }
  }

  /**
   * Sets the delegate to receive drag events.
   */
  public void setDragDelegate(DragDelegate dragDelegate) {
    this.dragDelegate = dragDelegate;
  }

  /**
   * Sets the delegate to receive touch events.
   */
  public void setTouchDelegate(TouchDelegate touchDelegate) {
    this.touchDelegate = touchDelegate;
  }

  /**
   * Begin tracking the touchable element, it is eligible for dragging.
   */
  private void beginTracking() {
    tracking = true;
  }

  /**
   * Stop tracking the touchable element, it is no longer dragging.
   */
  private void endTracking() {
    tracking = false;
    dragging = false;
    totalMoveY = 0;
    totalMoveX = 0;
  }

  /**
   * Return the touch of the last event.
   * 
   * @return the touch.
   */
  private Touch getLastTouch() {
    assert lastEvent != null : "Last event not set";
    return getTouchFromEvent(lastEvent);
  }

  /**
   * Touch end handler.
   * 
   * @param e The touchend event.
   */
  private void onEnd(TouchEvent e) {
    touching = false;
    if (touchDelegate != null) {
      touchDelegate.onTouchEnd(e);
    }

    if (!tracking || dragDelegate == null) {
      return;
    }

    Touch touch = getLastTouch();
    Point touchCoordinate = new Point(touch.getPageX(),
        touch.getPageY());

    if (dragging) {
      endTime = e.getTimeStamp();
      endTouchPosition = touchCoordinate;
      dragDelegate.onDragEnd(e);

      if ((Math.abs(endTouchPosition.x - startTouchPosition.x) > CLICK_BUST_THRESHOLD)
          || (Math.abs(endTouchPosition.y - startTouchPosition.y) > CLICK_BUST_THRESHOLD)) {
        bustNextClick = true;
      }
    }

    endTracking();
  }

  /**
   * Touch move handler.
   * 
   * @param e The touchmove event.
   */
  private void onMove(final TouchEvent e) {
    if (!tracking || dragDelegate == null) {
      return;
    }

    // Prevent native scrolling.
    e.preventDefault();

    Touch touch = getTouchFromEvent(e);
    Point touchCoordinate = new Point(touch.getPageX(),
        touch.getPageY());

    double moveX = lastTouchPosition.x - touchCoordinate.x;
    double moveY = lastTouchPosition.y - touchCoordinate.y;
    totalMoveX += Math.abs(moveX);
    totalMoveY += Math.abs(moveY);
    lastTouchPosition.x = touchCoordinate.x;
    lastTouchPosition.y = touchCoordinate.y;

    // Handle case where they are getting close to leaving the window.
    // End events are unreliable when the touch is leaving the viewport area.
    // If they are close to the bottom or the right, and we don't get any other
    // touch events for another 100ms, assume they have left the screen. This
    // does not seem to be a problem for scrolling off the top or left of the
    // viewport area.
    if (scrollOffTimer != null) {
      scrollOffTimer.cancel();
    }
    if ((Window.getClientHeight() - touchCoordinate.y) < TOUCH_END_WORKAROUND_THRESHOLD
        || (Window.getClientWidth() - touchCoordinate.x) < TOUCH_END_WORKAROUND_THRESHOLD) {

      scrollOffTimer = new Timer() {
        @Override
        public void run() {
          e.setTimeStamp(Duration.currentTimeMillis());
          onEnd(e);
        }
      };
      scrollOffTimer.schedule(100);
    }

    if (!dragging) {
      if (totalMoveY > MIN_TRACKING_FOR_DRAG
          || totalMoveX > MIN_TRACKING_FOR_DRAG) {
        dragging = true;
        dragDelegate.onDragStart(e);
      }
    }

    if (dragging) {
      dragDelegate.onDragMove(e);

      lastEvent = e;

      // This happens when they are dragging slowly. If they are dragging slowly
      // then we should reset the start time and position to where they are now.
      // This will be important during the drag end when we report to the
      // draggable delegate what kind of drag just happened.
      if (e.getTimeStamp() - recentTime > MAX_TRACKING_TIME) {
        recentTime = e.getTimeStamp();
        recentTouchPosition = touchCoordinate;
      }
    }
  }

  /**
   * Touch start handler.
   * 
   * @param e The touchstart event.
   * @private
   */
  private void onStart(TouchEvent e) {
    // Ignore the touch if it is manufactured or if there is already a
    // touch happening.
    if (touching) {
      return;
    }

    touching = true;

    Touch touch = getTouchFromEvent(e);
    Point touchCoordinate = new Point(touch.getPageX(),
        touch.getPageY());

    // Do not start tracking if...
    // - we already are tracking
    // - the touchable delegate refuses to accept the start event at this time
    // - there is no draggable delegate
    if ((dragDelegate == null) ||
        ((touchDelegate != null) && !touchDelegate.onTouchStart(e))) {
      return;
    }

    startTouchPosition = touchCoordinate;
    recentTouchPosition = touchCoordinate;
    recentTime = e.getTimeStamp();
    lastEvent = e;
    lastTouchPosition = new Point(touchCoordinate);

    beginTracking();
  }
}
