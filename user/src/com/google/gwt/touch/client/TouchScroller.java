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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.PartialSupport;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.TouchCancelEvent;
import com.google.gwt.event.dom.client.TouchCancelHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchEvent;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.dom.client.TouchStartEvent;
import com.google.gwt.event.dom.client.TouchStartHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.touch.client.Momentum.State;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasScrolling;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Adds touch based scrolling to a scroll panel.
 * 
 * <p>
 * Touch based scrolling is only supported on devices that support touch events
 * and do not implement native touch based scrolling.
 * </p>
 */
@PartialSupport
public class TouchScroller {

  /**
   * A point associated with a time.
   * 
   * Visible for testing.
   */
  static class TemporalPoint {
    private Point point;
    private double time;

    public TemporalPoint() {
    }

    /**
     * Construct a new {@link TemporalPoint} for the specified point and time.
     */
    public TemporalPoint(Point point, double time) {
      setTemporalPoint(point, time);
    }

    public Point getPoint() {
      return point;
    }

    public double getTime() {
      return time;
    }

    /**
     * Update the point and time.
     * 
     * @param point the new point
     * @param time the new time
     */
    public void setTemporalPoint(Point point, double time) {
      this.point = point;
      this.time = time;
    }
  }

  /**
   * The command used to apply momentum.
   */
  private class MomentumCommand implements RepeatingCommand {

    private final Duration duration = new Duration();
    private final Point initialPosition = getWidgetScrollPosition();
    private int lastElapsedMillis = 0;
    private State state;
    private HandlerRegistration windowResizeHandler;

    /**
     * Construct a {@link MomentumCommand}.
     * 
     * @param endVelocity the final velocity of the user drag
     */
    public MomentumCommand(Point endVelocity) {
      state = momentum.createState(initialPosition, endVelocity);

      /**
       * If the user resizes the window (which happens on orientation change of
       * a mobile device), cancel the momentum. The scrollable widget may be
       * resized, which will cause its content to reflow and invalidates the
       * current scrolling position.
       */
      windowResizeHandler = Window.addResizeHandler(new ResizeHandler() {
        public void onResize(ResizeEvent event) {
          finish();
        }
      });
    }

    @Override
    public boolean execute() {
      /*
       * Stop the command if another touch event starts or if momentum is
       * disabled.
       */
      if (this != momentumCommand) {
        finish();
        return false;
      }

      // Get the current position from the momentum.
      int cumulativeElapsedMillis = duration.elapsedMillis();
      state.setElapsedMillis(cumulativeElapsedMillis - lastElapsedMillis);
      lastElapsedMillis = cumulativeElapsedMillis;
      state.setCumulativeElapsedMillis(cumulativeElapsedMillis);

      // Calculate the new state.
      boolean notDone = momentum.updateState(state);

      // Momentum is finished, so the user is free to click.
      if (!notDone) {
        finish();
      }

      /*
       * Apply the new position. Even if there is no additional momentum, we
       * want to respect the end position that the momentum returns.
       */
      setWidgetScrollPosition(state.getPosition());

      /*
       * If the scroll position reached the min/max of the scrollable area,
       * finish the momentum command.
       */
      int hPos = (int) state.getPosition().getX();
      int hMin = widget.getMinimumHorizontalScrollPosition();
      int hMax = widget.getMaximumHorizontalScrollPosition();
      int vMin = widget.getMinimumVerticalScrollPosition();
      int vMax = widget.getMaximumVerticalScrollPosition();
      int vPos = (int) state.getPosition().getY();
      if ((vMax <= vPos || vMin >= vPos) && (hMax <= hPos || hMin >= hPos)) {
        finish();
        return false;
      }

      return notDone;
    }

    /**
     * Finish and cleanup this momentum command.
     */
    private void finish() {
      if (windowResizeHandler != null) {
        windowResizeHandler.removeHandler();
        windowResizeHandler = null;
      }
      if (this == momentumCommand) {
        momentumCommand = null;
      }
    }
  }

  /**
   * The command used to remove touch points that occurred while the scrolling
   * momentum is active. Only the touch points that occurred before
   * TIME_THRESHOLD get to be removed.
   */
  private class MomentumTouchRemovalCommand implements RepeatingCommand {
    @Override
    public boolean execute() {
      double currentTime = Duration.currentTimeMillis();
      Iterator<TemporalPoint> iter = touchPositionsDuringMomentum.iterator();
      while (iter.hasNext()) {
        TemporalPoint point = iter.next();
        if (currentTime - point.getTime() >= TIME_THRESHOLD) {
          iter.remove();
        }
      }
      return !touchPositionsDuringMomentum.isEmpty();
    }
  }

  /**
   * The number of frames per second the animation should run at.
   */
  private static final double FRAMES_PER_SECOND = 60;

  /**
   * The number of ms to wait during a drag before updating the reported start
   * position of the drag.
   */
  private static final double MAX_TRACKING_TIME = 200;

  /**
   * The number of ms to wait before putting a position on deck.
   */
  private static final double MAX_TRACKING_TIME_ON_DECK = MAX_TRACKING_TIME / 2;

  /**
   * Minimum movement of touch required to be considered a drag.
   */
  private static final double MIN_TRACKING_FOR_DRAG = 5;

  /**
   * The threshold for how close the time of a click event has to be to that of
   * a touch event for us to consider those events to be related; that is,
   * caused by the same user action (tap).
   */
  private static final int TIME_THRESHOLD = 2500;

  /**
   * The threshold for how close the coordinates of a click event has to be to
   * those of a touch event for us to consider those events to be related;
   * that is, caused by the same user action (tap).
   */
  private static final int DISTANCE_THRESHOLD = 25;

  /**
   * The number of milliseconds per animation frame.
   */
  private static final int MS_PER_FRAME = (int) (1000 / FRAMES_PER_SECOND);

  /**
   * A cached boolean indicating whether or not touch scrolling is supported.
   * Set to a non-null value the first time {@link #isSupported()} is called.
   */
  private static Boolean isSupported;

  /**
   * Return a new {@link TouchScroller}.
   * 
   * @return a new {@link TouchScroller} if supported, and null otherwise
   */
  public static TouchScroller createIfSupported() {
    return isSupported() ? new TouchScroller() : null;
  }

  /**
   * Return a new {@link TouchScroller} that augments the specified scrollable
   * widget if supported, and null otherwise.
   * 
   * @param widget the scrollable widget
   * @return a new {@link TouchScroller} if supported, and null otherwise
   */
  public static TouchScroller createIfSupported(HasScrolling widget) {
    TouchScroller scroller = createIfSupported();
    if (scroller != null) {
      scroller.setTargetWidget(widget);
    }
    return scroller;
  }

  /**
   * Runtime check for whether touch scrolling is supported in this browser.
   * Returns true if touch events are supported but touch based scrolling is not
   * natively supported.
   * 
   * @return true if touch scrolling is supported, false if not
   */
  public static boolean isSupported() {
    if (isSupported == null) {
      /*
       * Android 3.0 devices support touch scrolling natively.
       * 
       * TODO(jlabanca): Find a more reliable way to detect if native touch
       * scrolling is supported.
       */
      isSupported = TouchEvent.isSupported() && !isAndroid3();
    }
    return isSupported;
  }

  /**
   * Check if the user agent is android 3.0 or greater.
   * 
   * @return true if android 3.0+
   * 
   */
  private static native boolean isAndroid3() /*-{
    var ua = navigator.userAgent.toLowerCase();
    return /android ([3-9]+)\.([0-9]+)/.exec(ua) != null;
  }-*/;

  /**
   * The registration for the preview handler used to bust click events.
   */
  private HandlerRegistration bustClickHandlerReg;

  /**
   * The registration for the attach event handler.
   */
  private HandlerRegistration attachHandlerReg;

  /**
   * A boolean indicating that we are in a drag sequence. Dragging occurs after
   * the user moves beyond a threshold distance.
   */
  private boolean dragging;

  /**
   * Registrations for the handlers added to the widget.
   */
  private final List<HandlerRegistration> handlerRegs =
      new ArrayList<HandlerRegistration>();

  /**
   * The last (most recent) touch position. We need to keep track of this when
   * we handle touch move events because the Touch is already destroyed before
   * the touch end event fires.
   */
  private final TemporalPoint lastTouchPosition = new TemporalPoint();

  /**
   * The momentum that determines how the widget scrolls after the user
   * completes a gesture. Can be null if momentum is not supported.
   */
  private Momentum momentum;

  /**
   * The repeating command used to continue momentum after the gesture ends. The
   * command is instantiated after the user finishes a drag sequence. A non null
   * value indicates that momentum is occurring.
   */
  private RepeatingCommand momentumCommand;

  /**
   * The coordinate of the most recent relevant touch event. For most drag
   * sequences this will be the same as the startCoordinate. If the touch
   * gesture changes direction significantly or pauses for a while this
   * coordinate will be updated to the coordinate of the on deck touchmove
   * event.
   */
  private final TemporalPoint recentTouchPosition = new TemporalPoint();

  /**
   * If the gesture takes too long, we update the recentTouchPosition to the
   * position on deck, which occurred halfway through the max tracking time. We
   * do this so that we don't base the velocity on two touch events that
   * occurred very close to each other at the end of a long gesture.
   */
  private TemporalPoint recentTouchPositionOnDeck;

  /**
   * The coordinate of the most recent touch event that triggered scrolling.
   */
  private TemporalPoint recentScrollTriggeringTouchPosition =
      new TemporalPoint();

  /**
   * The coordinates of the touch positions while the momentum is active. The
   * points stored in this array will be removed after TIME_THRESHOLD.
   */
  private List<TemporalPoint> touchPositionsDuringMomentum =
      new ArrayList<TemporalPoint>();

  /**
   * The repeating command used to remove the touch points that were added to
   * touchPositionsDuringMomentum after TIME_THRESHOLD.
   */
  private RepeatingCommand momentumTouchRemovalCommand =
      new MomentumTouchRemovalCommand();

  /**
   * The position of the scrollable when the first touch occured.
   */
  private Point startScrollPosition;

  /**
   * The position of the first touch.
   */
  private Point startTouchPosition;

  /**
   * A boolean indicating that we are in a touch sequence.
   */
  private boolean touching;

  /**
   * The widget being augmented.
   */
  private HasScrolling widget;

  /**
   * Construct a new {@link TouchScroller}. This constructor should be called
   * using the static method {@link #createIfSupported()}.
   * 
   * @see #createIfSupported()
   */
  protected TouchScroller() {
    setMomentum(new DefaultMomentum());
  }

  /**
   * Get the {@link Momentum} that controls scrolling after the user completes a
   * gesture.
   * 
   * @return the scrolling {@link Momentum}, or null if disabled
   */
  public Momentum getMomentum() {
    return momentum;
  }

  /**
   * Get the target {@link HasScrolling} widget that this scroller affects.
   * 
   * @return the target widget
   */
  public HasScrolling getTargetWidget() {
    return widget;
  }

  /**
   * Set the {@link Momentum} that controls scrolling after the user completes a
   * gesture.
   * 
   * @param momentum the scrolling {@link Momentum}, or null to disable
   */
  public void setMomentum(Momentum momentum) {
    this.momentum = momentum;
    if (momentum == null) {
      // Cancel the current momentum.
      momentumCommand = null;
    }
  }

  /**
   * Set the target {@link HasScrolling} widget that this scroller affects.
   * 
   * @param widget the target widget, or null to disbale
   */
  public void setTargetWidget(HasScrolling widget) {
    if (this.widget == widget) {
      return;
    }

    // Cancel drag and momentum.
    cancelAll();

    // Release the old widget.
    for (HandlerRegistration reg : handlerRegs) {
      reg.removeHandler();
    }
    handlerRegs.clear();
    removeAttachHandler();
    removeBustClickHandler();

    // Attach to the new widget.
    this.widget = widget;
    if (widget != null) {
      if (widget.asWidget().isAttached()) {
        setupBustClickHandler();
      }

      // Make sure to add/remove the bust click handler if the widget is attached/detached.
      attachHandlerReg = widget.asWidget().addAttachHandler(new AttachEvent.Handler() {
        @Override
        public void onAttachOrDetach(AttachEvent event) {
          if (event.isAttached()) {
            setupBustClickHandler();
          } else {
            removeBustClickHandler();
          }
        }
      });

      // Add touch start handler.
      handlerRegs.add(widget.asWidget().addDomHandler(new TouchStartHandler() {
        @Override
        public void onTouchStart(TouchStartEvent event) {
          TouchScroller.this.onTouchStart(event);
        }
      }, TouchStartEvent.getType()));

      // Add touch move handler.
      handlerRegs.add(widget.asWidget().addDomHandler(new TouchMoveHandler() {
        @Override
        public void onTouchMove(TouchMoveEvent event) {
          TouchScroller.this.onTouchMove(event);
        }
      }, TouchMoveEvent.getType()));

      // Add touch end handler.
      handlerRegs.add(widget.asWidget().addDomHandler(new TouchEndHandler() {
        @Override
        public void onTouchEnd(TouchEndEvent event) {
          TouchScroller.this.onTouchEnd(event);
        }
      }, TouchEndEvent.getType()));

      // Add touch cancel handler.
      handlerRegs.add(widget.asWidget().addDomHandler(new TouchCancelHandler() {
        @Override
        public void onTouchCancel(TouchCancelEvent event) {
          TouchScroller.this.onTouchCancel(event);
        }
      }, TouchCancelEvent.getType()));
    }
  }

  /**
   * Get touch from event.
   * 
   * @param event the event
   * @return the touch object
   */
  protected Touch getTouchFromEvent(TouchEvent<?> event) {
    JsArray<Touch> touches = event.getTouches();
    return (touches.length() > 0) ? touches.get(0) : null;
  }

  /**
   * Called when the object's drag sequence is complete.
   * 
   * @param event the touch event
   */
  protected void onDragEnd(TouchEvent<?> event) {
    // There is no momentum or it isn't supported.
    if (momentum == null) {
      return;
    }

    // Schedule the momentum.
    Point endVelocity = calculateEndVelocity(recentTouchPosition, lastTouchPosition);
    if (endVelocity != null) {
      momentumCommand = new MomentumCommand(endVelocity);
      Scheduler.get().scheduleFixedDelay(momentumCommand, MS_PER_FRAME);
    }
  }

  /**
   * Called when the object has been dragged to a new position.
   * 
   * @param event the touch event
   */
  protected void onDragMove(TouchEvent<?> event) {
    /*
     * Scroll to the new position. Touch scrolling moves in the same direction
     * as the finger dragging, whereas scrolling is inverted with traditional
     * scrollbars.
     */
    Point diff = startTouchPosition.minus(lastTouchPosition.getPoint());
    Point curScrollPosition = startScrollPosition.plus(diff);
    setWidgetScrollPosition(curScrollPosition);
  }

  /**
   * Called when the object has started dragging.
   * 
   * @param event the touch event
   */
  protected void onDragStart(TouchEvent<?> event) {
  }

  /**
   * Called when the user cancels a touch. This can happen if the user touches
   * the screen with too many fingers.
   * 
   * @param event the touch event
   */
  protected void onTouchCancel(TouchEvent<?> event) {
    onTouchEnd(event);
  }

  /**
   * Called when the user releases a touch.
   * 
   * @param event the touch event
   */
  protected void onTouchEnd(TouchEvent<?> event) {
    // Ignore the touch if we didn't catch a touch start event.
    if (!touching) {
      return;
    }
    touching = false;

    // Stop dragging.
    if (dragging) {
      dragging = false;
      onDragEnd(event);
    }
  }

  /**
   * Called when the user moves a touch.
   *
   * @param event the touch event
   */
  protected void onTouchMove(TouchEvent<?> event) {
    // Ignore the touch if we never caught a touch start event.
    if (!touching) {
      return;
    }

    // Check if we should start dragging.
    Touch touch = getTouchFromEvent(event);
    Point touchPoint = new Point(touch.getPageX(), touch.getPageY());
    double touchTime = Duration.currentTimeMillis();
    lastTouchPosition.setTemporalPoint(touchPoint, touchTime);
    if (!dragging) {
      Point diff = touchPoint.minus(startTouchPosition);
      double absDiffX = Math.abs(diff.getX());
      double absDiffY = Math.abs(diff.getY());
      if (absDiffX > MIN_TRACKING_FOR_DRAG || absDiffY > MIN_TRACKING_FOR_DRAG) {
        recentScrollTriggeringTouchPosition.setTemporalPoint(
            recentTouchPosition.getPoint(), recentTouchPosition.getTime());

        /*
         * Check if we should defer to native scrolling. If the scrollable
         * widget is already scrolled as far as it will go, then we don't want
         * to prevent scrolling of the document.
         * 
         * We cannot prevent native scrolling in only one direction (ie. we
         * cannot allow native horizontal scrolling but prevent native vertical
         * scrolling), so we make a best guess based on the direction of the
         * drag.
         */
        if (absDiffX > absDiffY) {
          /*
           * The user scrolled primarily in the horizontal direction, so check
           * if we should defer left/right scrolling to the document.
           */
          int hPosition = widget.getHorizontalScrollPosition();
          int hMin = widget.getMinimumHorizontalScrollPosition();
          int hMax = widget.getMaximumHorizontalScrollPosition();
          if (diff.getX() < 0 && hMax <= hPosition) {
            // Already scrolled to the right.
            cancelAll();
            return;
          } else if (diff.getX() > 0 && hMin >= hPosition) {
            // Already scrolled to the left.
            cancelAll();
            return;
          }
        } else {
          /*
           * The user scrolled primarily in the vertical direction, so check if
           * we should defer up/down scrolling to the document.
           */
          int vPosition = widget.getVerticalScrollPosition();
          int vMin = widget.getMinimumVerticalScrollPosition();
          int vMax = widget.getMaximumVerticalScrollPosition();
          if (diff.getY() < 0 && vMax <= vPosition) {
            // Already scrolled to the bottom.
            cancelAll();
            return;
          } else if (diff.getY() > 0 && vMin >= vPosition) {
            // Already scrolled to the top.
            cancelAll();
            return;
          }
        }

        // Start dragging.
        dragging = true;
        onDragStart(event);
      }
    }

    // Prevent native document level scrolling.
    event.preventDefault();

    if (dragging) {
      // Continue dragging.
      onDragMove(event);

      /*
       * Update the recent position. This happens when they are dragging slowly.
       * If they are dragging slowly then we should reset the start time and
       * position to where they are now. This will be important during the drag
       * end when we report to the draggable delegate what kind of drag just
       * happened.
       */
      double trackingTime = touchTime - recentTouchPosition.getTime();
      if (trackingTime > MAX_TRACKING_TIME && recentTouchPositionOnDeck != null) {
        // See comment below.
        recentTouchPosition.setTemporalPoint(recentTouchPositionOnDeck.getPoint(),
            recentTouchPositionOnDeck.getTime());
        recentTouchPositionOnDeck = null;
      } else if (trackingTime > MAX_TRACKING_TIME_ON_DECK && recentTouchPositionOnDeck == null) {
        /*
         * When we are halfway to the max tracking time, put the current touch
         * on deck. When we switch the recent touch position, we use the on deck
         * position. That prevents us from calculating the velocity from two
         * points that are too close in time (or the same time).
         */
        recentTouchPositionOnDeck = new TemporalPoint(touchPoint, touchTime);
      }
    }
  }

  /**
   * Called when the user starts a touch.
   * 
   * @param event the touch event
   */
  protected void onTouchStart(TouchEvent<?> event) {
    // Touch start event always precedes the click event. So this
    // recentScrollTriggeringTouchPosition gets reset before a click event
    // occurs. That's why we don't need to compare the time of the ghost click
    // and the recentScrollTriggeringTouchPosition.
    recentScrollTriggeringTouchPosition.setTemporalPoint(null, 0);

    // Ignore the touch if there is already a touch happening.
    if (touching) {
      return;
    }

    // Record the starting touch position.
    Touch touch = getTouchFromEvent(event);
    startTouchPosition = new Point(touch.getPageX(), touch.getPageY());
    double startTouchTime = Duration.currentTimeMillis();
    recentTouchPosition.setTemporalPoint(startTouchPosition, startTouchTime);
    lastTouchPosition.setTemporalPoint(startTouchPosition, startTouchTime);
    recentTouchPositionOnDeck = null;

    /**
     * If the user touches the screen while the scrolling momentum is active,
     * add the touch point to an array so that the later click events can be
     * compared to the points in the array and busted if it shouldn't be
     * considered as click.
     */
    if (isMomentumActive()) {
      touchPositionsDuringMomentum.add(
          new TemporalPoint(startTouchPosition, startTouchTime));
      Scheduler.get().scheduleFixedDelay(
          momentumTouchRemovalCommand, TIME_THRESHOLD);
    }

    // Record the starting scroll position.
    startScrollPosition = getWidgetScrollPosition();

    cancelAll();
    touching = true;
  }

  /**
   * Calculate the end velocity. Visible for testing.
   * 
   * @param from the starting point
   * @param to the ending point
   * @return the end velocity, or null if it cannot be calculated
   */
  Point calculateEndVelocity(TemporalPoint from, TemporalPoint to) {
    /*
     * Calculate the time since the recent touch. The time can be zero if the
     * user pauses for too long, which updates the recentTouchPosition, then
     * lets go without moving again.
     */
    double time = to.getTime() - from.getTime();
    if (time <= 0) {
      return null;
    }

    /*
     * Calculate the end velocities. The velocity is inverted from the direction
     * of the gesture.
     */
    Point dist = from.getPoint().minus(to.getPoint());
    return new Point(dist.getX() / time, dist.getY() / time);
  }

  /**
   * Visible for testing.
   */
  TemporalPoint getLastTouchPosition() {
    return lastTouchPosition;
  }

  /**
   * Visible for testing.
   */
  TemporalPoint getRecentTouchPosition() {
    return recentTouchPosition;
  }

  /**
   * Visible for testing.
   */
  boolean isDragging() {
    return dragging;
  }

  /**
   * Check if momentum is currently active. Visible for testing.
   * 
   * @return true if active, false if not
   */
  boolean isMomentumActive() {
    return (momentumCommand != null);
  }

  /**
   * Visible for testing.
   */
  boolean isTouching() {
    return touching;
  }

  /**
   * Removes the attach handler. Visible for testing.
   */
  void removeAttachHandler() {
    if (attachHandlerReg != null) {
      attachHandlerReg.removeHandler();
      attachHandlerReg = null;
    }
  }

  /**
   * Removes the bust click handler. Visible for testing.
   */
  void removeBustClickHandler() {
    if (bustClickHandlerReg != null) {
      bustClickHandlerReg.removeHandler();
      bustClickHandlerReg = null;
    }
  }

  /**
   * Sets up the bust click handler. Visible for testing.
   */
  void setupBustClickHandler() {
    removeBustClickHandler();
    bustClickHandlerReg = Event.addNativePreviewHandler(new NativePreviewHandler() {
      @Override
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        if (Event.ONCLICK == event.getTypeInt()) {
          Point clickPoint = new Point(
              event.getNativeEvent().getClientX(),
              event.getNativeEvent().getClientY());

          /*
           * In mobile browsers, when the user touches on the screen, drag
           * his finger across and take his finger off, the browser fires a
           * click event at the position where the touch start happen. This
           * click event is called "ghost click". This click is not intended by
           * the user and thus needs to be busted.
           * Also, while the panel is scrolling, if the user taps on the screen
           * we want to stop the scrolling and bust the click event that's fired
           * because the user probably didn't mean to click on something.
           */
          if (isClickScrollTriggeringTouch(clickPoint) ||
              isClickTouchPositionDuringMomentum(clickPoint)) {
            event.cancel();
            event.getNativeEvent().stopPropagation();
            event.getNativeEvent().preventDefault();
          }
        }
      }
    });
  }

  /**
   * Cancel all existing touch, drag, and momentum.
   */
  private void cancelAll() {
    touching = false;
    dragging = false;
    momentumCommand = null;
  }

  /**
   * Get the scroll position of the widget.
   */
  private Point getWidgetScrollPosition() {
    return new Point(widget.getHorizontalScrollPosition(), widget.getVerticalScrollPosition());
  }

  /**
   * Tests if the given points fall within the threshold from each other.
   *
   * @param point1 first point
   * @param point2 second point
   * @return true if the points fall within the threshold from each other
   */
  private boolean hitTest(Point point1, Point point2) {
    Point diff = point1.minus(point2);
    double absDiffX = Math.abs(diff.getX());
    double absDiffY = Math.abs(diff.getY());
    return (absDiffX <= DISTANCE_THRESHOLD) && (absDiffY <= DISTANCE_THRESHOLD);
  }

  /**
   * Checks if the click event is related to the touch event that has triggered
   * scrolling. Such click events occur as a result of ghost click (i.e. click
   * event that is fired at the touch start position after the touch end
   * happens) and should be busted.
   *
   * @param clickPoint point of the click event
   * @return true if clickPoint is the same as the recent scroll triggering
   *     touch
   */
  private boolean isClickScrollTriggeringTouch(Point clickPoint) {
    if (recentScrollTriggeringTouchPosition.getPoint() != null) {
      return hitTest(
          clickPoint, recentScrollTriggeringTouchPosition.getPoint());
    }
    return false;
  }

  /**
   * Checks if the click event point is related to the touch positions
   * during the recent momentum scrolling.
   *
   * @param clickPoint point of the click event
   * @return true if clickPoint is the same as one of the touch positions during
   *     the recent momentum scrolling
   */
  private boolean isClickTouchPositionDuringMomentum(Point clickPoint) {
    double currentTime = Duration.currentTimeMillis();
    boolean same = false;
    for (TemporalPoint point : touchPositionsDuringMomentum) {
      if (currentTime - point.getTime() <= TIME_THRESHOLD &&
          hitTest(clickPoint, point.getPoint())) {
        same = true;
        break;
      }
    }
    return same;
  }

  /**
   * Sets the scroll position of the widget.
   * 
   * @param position the new position
   */
  private void setWidgetScrollPosition(Point position) {
    widget.setHorizontalScrollPosition((int) position.getX());
    widget.setVerticalScrollPosition((int) position.getY());
  }
}
