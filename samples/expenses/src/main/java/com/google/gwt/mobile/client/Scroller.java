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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;

/**
 * This behavior overrides native scrolling for an area. This area can be a
 * single defined part of a page, the entire page, or several different parts of
 * a page.
 * 
 * To use this scrolling behavior you need to define a frame and the content.
 * The frame defines the area that the content will scroll within. The frame and
 * content must both be HTML Elements, with the content being a direct child of
 * the frame. Usually the frame is smaller in size than the content. This is not
 * necessary though, if the content is smaller then bouncing will occur to
 * provide feedback that you are past the scrollable area.
 * 
 * <?>
 * The scrolling behavior works using the webkit translate3d transformation,
 * which means browsers that do not have hardware accelerated transformations
 * will not perform as well using this. Simple scrolling should be fine even
 * without hardware acceleration, but animating momentum and deceleration is
 * unacceptably slow without it.
 * 
 * For this to work properly you need to set -webkit-text-size-adjust to 'none'
 * on an ancestor element of the frame, or on the frame itself. If you forget
 * this you may see the text content of the scrollable area changing size as it
 * moves.
 * 
 * Browsers that support hardware accelerated transformations:
 * - Mobile Safari 3.x
 * </?>
 * 
 * The behavior is intended to support vertical and horizontal scrolling, and
 * scrolling with momentum when a touch gesture flicks with enough velocity.
 */
public class Scroller implements Momentum.Delegate, TouchHandler.DragDelegate,
    TouchHandler.TouchDelegate {

  /**
   * The muted label metadata constant.
   */
  private static final Point ORIGIN = new Point(0, 0);

  /**
   * Initialize the current content offset.
   */
  private Point contentOffset;

  /**
   * The size of the content that is scrollable.
   */
  private Point contentSize;

  /**
   * The offset of the scrollable content when a touch begins. Used to track
   * delta x and y's of the scrolling content.
   */
  private Point contentStartOffset;

  /**
   * Frame is the node that will serve as the container for the scrolling
   * content.
   */
  private Element frame;

  /**
   * Is horizontal scrolling enabled.
   */
  private boolean horizontalEnabled;

  /**
   * Layer is the node that will actually scroll.
   */
  private Element layer;

  /**
   * The minimum coordinate that the left upper corner of the content can scroll
   * to.
   */
  private Point minPoint;

  /**
   * The momentum behavior.
   */
  private Momentum momentum;

  /**
   * Is momentum enabled.
   */
  private boolean momentumEnabled;

  /**
   * The size of the frame.
   */
  private Point scrollSize;

  /**
   * Create a touch manager to track the events on the scrollable area.
   */
  private TouchHandler touchHandler;

  /**
   * The start position of a touch. Used to track delta x and y's of the
   * scrollable content.
   */
  private Point touchStartPosition;

  /**
   * Creates a new scroller
   * 
   * The frame needs to have its dimensions set, and the scrollable content will
   * be allowed to move within those dimensions. It is required that the layer
   * element be a direct child node of the frame.
   * 
   * @param frame the element that is the frame
   * @param layer the element that is the scrolling content
   */
  public Scroller(Element frame, Element layer) {
    this.frame = frame;
    this.layer = layer;

    touchHandler = new TouchHandler(frame);
    touchHandler.setTouchDelegate(this);
    touchHandler.setDragDelegate(this);
    touchHandler.enable();

    momentum = new Momentum(this);
    contentOffset = new Point();

    initLayer();
  }

  /**
   * Gets the current x offset of the content.
   */
  public double getContentOffsetX() {
    return contentOffset.x;
  }

  /**
   * Gets the current y offset of the content.
   */
  public double getContentOffsetY() {
    return contentOffset.y;
  }

  /**
   * Provide access to the touch handler that the scroller created to manage
   * touch events.
   * 
   * @return {!wireless.events.TouchHandler} the touch handler.
   */
  public TouchHandler getTouchHandler() {
    return touchHandler;
  }

  /**
   * Callback for a deceleration step.
   * 
   * @param offsetX The new x offset.
   * @param offsetY The new y offset.
   * @param velocity The current velocitiy.
   */
  public void onDecelerate(double offsetX, double offsetY, Point velocity) {
    setContentOffset(offsetX, offsetY);
  }

  /**
   * Callback for end of deceleration.
   */
  public void onDecelerationEnd() {
  }

  /**
   * The object's drag sequence is now complete.
   * 
   * @param e The touchmove event.
   */
  public void onDragEnd(TouchEvent e) {
    boolean decelerating = false;

    if (momentumEnabled) {
      decelerating = startDeceleration(touchHandler.getEndVelocity());
    }

    if (!decelerating) {
      snapContentOffsetToBounds();
    }
  }

  /**
   * The object has been dragged to a new position.
   * 
   * @param e The touchmove event.
   */
  public void onDragMove(TouchEvent e) {
    Touch touch = TouchHandler.getTouchFromEvent(e);
    Point touchCoord = new Point(touch.getPageX(), touch.getPageY());

    assert touchStartPosition != null : "Touch start not set";
    assert contentStartOffset != null : "Content start not set";

    Point touchStart = touchStartPosition;
    Point contentStart = contentStartOffset;

    Point diffXY = touchCoord.minus(touchStart);
    Point newXY = contentStart.plus(diffXY);

    // If they are dragging beyond bounds of frame then we will start
    // backing off on the effect of their drag.
    newXY.y = adjustValue(newXY.y, minPoint.y);

    // If horizontal scrolling is enabled and the content is wider than
    // the frame, then we should calculate a new X position.
    if (shouldScrollHorizontally()) {
      newXY.x = adjustValue(newXY.x, minPoint.x);
    } else {
      newXY.x = 0;
    }

    setContentOffset(newXY.x, newXY.y);
  }

  /**
   * Dragging has begun.
   * 
   * @param e The touchmove event
   */
  public void onDragStart(TouchEvent e) {
  }

  /**
   * Touch has ended.
   * 
   * @param e The touchend event
   */
  public void onTouchEnd(TouchEvent e) {
  }

  /**
   * Touch has begun on the scrollable area. Prepare the scrollable area for
   * possible movement.
   * 
   * @param e The touchstart event.
   * @return True if the object is eligible for dragging.
   */
  public boolean onTouchStart(TouchEvent e) {
    reconfigure();
    Touch touch = TouchHandler.getTouchFromEvent(e);

    // Save the initial position of touch and content.
    touchStartPosition = new Point(touch.getPageX(), touch.getPageY());
    contentStartOffset = new Point(contentOffset);

    // If the content is currently decelerating then we should stop it
    // immediately.
    momentum.stop();

    // Content should be snapped back in to place at this point if it is
    // currently
    // offset.
    snapContentOffsetToBounds();

    // Returning true here indicates that we are accepting a drag sequence.
    return true;
  }

  /**
   * Recalculate dimensions of the frame and the content. Adjust the minPoint
   * allowed for scrolling. Call this method if you know the frame or content
   * has been updated. Called internally on every touchstart event the frame
   * receives.
   */
  public void reconfigure() {
    scrollSize = new Point(frame.getOffsetWidth(),
        frame.getOffsetHeight());
    contentSize = new Point(layer.getScrollWidth(),
        layer.getScrollHeight());

    Point adjusted = getAdjustedContentSize();
    minPoint = new Point(scrollSize.x - adjusted.x, scrollSize.y
        - adjusted.y);
  }

  /**
   * Reset the scroll offset and any transformations previously applied.
   */
  public void reset() {
    setContentOffset(0, 0);
    reconfigure();
  }

  /**
   * Translate the content to a new position.
   * 
   * @param x The new x position.
   * @param y The new y position.
   */
  public void setContentOffset(double x, double y) {
    contentOffset.x = x;
    contentOffset.y = y;

    // TODO(jgw): decide whether we can just use scroll-offset. It may be faster
    // to use -webkit-transform:translate3d(Xpx, Ypx, 0).
    frame.setScrollLeft((-(int) x));
    frame.setScrollTop((-(int) y));
  }

  /**
   * Enable or disable horizontal scrolling.
   * 
   * @param enable True if it should be enabled.
   */
  public void setHorizontalScrolling(boolean enable) {
    horizontalEnabled = enable;
  }

  /**
   * Enable or disable momentum.
   */
  public void setMomentum(boolean enable) {
    momentumEnabled = enable;
  }

  /**
   * Adjust the new calculated scroll position based on the minimum allowed
   * position.
   * 
   * @param y The new position before adjusting.
   * @param y2 The minimum allowed position.
   * @return the adjusted scroll value.
   */
  private double adjustValue(double y, double y2) {
    if (y < y2) {
      y -= (y - y2) / 2;
    } else if (y > 0) {
      y /= 2;
    }
    return y;
  }

  private double clamp(double value, double min, double max) {
    return Math.min(Math.max(value, min), max);
  }

  /**
   * Adjusted content size is a size with the combined largest height and width
   * of both the content and the frame.
   * 
   * @return the adjusted size.
   */
  private Point getAdjustedContentSize() {
    return new Point(Math.max(scrollSize.x, contentSize.x), Math.max(
        scrollSize.y, contentSize.y));
  }

  /**
   * Initialize the dom elements necessary for the scrolling to work. - Sets the
   * overflow of the frame to hidden.
   * 
   * - Asserts that the content is a direct child of the frame.
   */
  private void initLayer() {
    assert layer.getParentNode() == frame :
      "The scrollable node provided to Scroller must be "
        + "a direct child of the scrollable frame.";

    frame.getStyle().setOverflow(Overflow.HIDDEN);

    // Applying this tranform on initialization avoids flickering issues the
    // first time elements are moved.
    setContentOffset(0, 0);
  }

  /**
   * Whether or not the scrollable area should scroll horizontally or not. Only
   * returns true if the client has enabled horizontal scrolling, and the
   * content is wider than the frame.
   * 
   * @return True if should scroll horizontally.
   */
  private boolean shouldScrollHorizontally() {
    return horizontalEnabled && scrollSize.x < contentSize.y;
  }

  /**
   * In the event that the content is currently beyond the bounds of the frame,
   * snap it back in to place.
   */
  private void snapContentOffsetToBounds() {
    Point point = new Point(clamp(minPoint.x, contentOffset.x, 0),
        clamp(minPoint.y, contentOffset.y, 0));

    // If move is required
    if (!point.equals(contentOffset)) {
      setContentOffset(point.x, point.y);
    }
  }

  /**
   * Initiate the deceleration behavior.
   * 
   * @param velocity The initial velocity.
   * @return True if deceleration has been initiated.
   */
  private boolean startDeceleration(Point velocity) {
    if (!shouldScrollHorizontally()) {
      velocity.x = 0;
    }

    assert minPoint != null : "Min point is not set";
    return momentum.start(velocity, minPoint, ORIGIN,
        contentOffset);
  }
}
