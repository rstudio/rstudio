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
import com.google.gwt.user.client.Timer;

/**
 * This class can be used to simulate the deceleration of an element within a
 * certain region. To use this behavior you need to provide a distance and time
 * that is meant to represent a gesture that is initiating this deceleration.
 * You also provide the bounds of the region that the element exists in, and the
 * current offset of the element within that region. This behavior will step
 * through all of the intermediate points necessary to decelerate the element
 * back to a velocity of zero. In doing so, the element may 'bounce' in and out
 * of the boundaries of the region, but will always come to rest within the
 * region.
 * 
 * This is primarily designed to solve the problem of slow scrolling in mobile
 * safari. You can use this along with the Scroller behavior
 * (wireless.fx.Scroller) to make a scrollable area scroll as well as it would
 * in a native application.
 * 
 * This class does not maintain any references to HTML elements, and therefore
 * cannot do any redrawing of elements. It only calculates where the element
 * should be on an interval. It is the delegate's responsibility to redraw the
 * element when the onDecelerate callback is invoked. It is recommended that you
 * move the element with a hardware accelerated method such as using
 * 'translate3d' on the element's -webkit-transform style property.
 */
class Momentum {

  /**
   * You are required to implement this interface in order to use the
   * {@link Momentum} behavior.
   */
  public interface Delegate {

    /**
     * Callback for a deceleration step. The delegate is responsible for redrawing
     * the element in its new position.
     *
     * @param floorX The new x offset
     * @param floorY The new y offset
     * @param velocity The current velocitiy
     */
    void onDecelerate(double floorX, double floorY, Point velocity);

    /**
     * Callback for end of deceleration.
     */
    void onDecelerationEnd();
  }

  /**
   * The constant factor applied to velocity at each frame to simulate
   * deceleration.
   */
  private static final double DECELERATION_FACTOR = 0.98;

  /**
   * The velocity threshold at which declereration will end.
   */
  private static final double DECELERATION_STOP_VELOCITY = 0.01;

  /**
   * The number of frames per second the animation should run at.
   */
  private static final double FRAMES_PER_SECOND = 60;

  /**
   * Boost the initial velocity by a certain factor before applying momentum.
   * This just gives the momentum a better feel.
   */
  private static final double INITIAL_VELOCITY_BOOST_FACTOR = 1.5;

  /**
   * Minimum velocity required to start deceleration.
   */
  private static final double MIN_START_VELOCITY = 0.3;

  /**
   * The number of milliseconds per animation frame.
   */
  private static final double MS_PER_FRAME = 1000 / FRAMES_PER_SECOND;

  /**
   * The spring coefficient for when the element is bouncing back from a
   * stretched offset to a min or max position. Each frame, the velocity will be
   * changed to x times this coefficient, where x is the current stretch value
   * of the element from its boundary. This will end when the stretch value
   * reaches 0.
   */
  private static final double POST_BOUNCE_COEFFICIENT = 9 / FRAMES_PER_SECOND;

  /**
   * The spring coefficient for when the element has passed a boundary and is
   * decelerating to change direction and bounce back. Each frame, the velocity
   * will be changed by x times this coefficient, where x is the current stretch
   * value of the element from its boundary. This will end when velocity reaches
   * zero.
   */
  private static final double PRE_BOUNCE_COEFFICIENT = 1.8 / FRAMES_PER_SECOND;

  /**
   * True when the momentum of has carried the position outside the allowable
   * range but before the velocity has changed directions.
   */
  private boolean bouncingX;

  /**
   * True when the momentum of has carried the position outside the allowable
   * range but before the velocity has changed directions.
   */
  private boolean bouncingY;

  /**
   * The current offset of the element. These x, y values can be decimal values.
   * It is necessary to store these values for the sake of precision.
   */
  private Point currentOffset;

  /**
   * Whether or not deceleration is currently in progress.
   */
  private boolean decelerating;

  private Delegate delegate;

  /**
   * The maximum boundary for the element.
   */
  private Point maxCoord;

  /**
   * The minimum boundary for the element.
   */
  private Point minCoord;

  /**
   * The previous offset of the element. These x, y values are whole numbers.
   * Their values are derived from rounding of the currentOffset_ member.
   */
  private Point previousOffset;

  /**
   * The start time of the deceleration.
   */
  private double startTime;

  private Timer stepFunction = new Timer() {
    @Override
    public void run() {
      step();
    }
  };

  /**
   * The step number of the deceleration.
   */
  private double stepNumber;

  /**
   * Current velocity of the element. In this class velocity is measured as
   * pixels per frame. That is, the number of pixels to move the element in the
   * next frame.
   */
  private Point velocity;

  /**
   * Creates a new Momentum object.
   * 
   * @param delegate The momentum delegate.
   */
  public Momentum(Delegate delegate) {
    this.delegate = delegate;
  }

  /**
   * Whether or not the element is currently bouncing. Bouncing is the behavior
   * of an element moving past an allowable boundary and decelerating to change
   * direction and snap back into place. Not to be confused with bouncing back.
   * 
   * @return True if the element is currently bouncing in either the x or y
   *         direction.
   */
  public boolean isBouncing() {
    return bouncingY || bouncingX;
  }

  /**
   * Start decelerating. Checks if the current velocity is above the minumum
   * threshold to start decelerating. If so then deceleration will begin, if not
   * then nothing happens.
   * 
   * @param velocity The initial velocity. The velocity passed here should be in
   *          terms of number of pixels / millisecond. initiating deceleration.
   * @param minCoord The content's scrollable boundary.
   * @param maxCoord The content's scrollable boundary.
   * @param initialOffset The current offset of the element within its
   *          boundaries.
   * @return True if deceleration has been initiated.
   */
  public boolean start(Point velocity, Point minCoord,
      Point maxCoord, Point initialOffset) {
    this.minCoord = minCoord;
    this.maxCoord = maxCoord;

    currentOffset = new Point(initialOffset);
    previousOffset = new Point(initialOffset);
    this.velocity = adjustInitialVelocity(velocity);

    if (isVelocityAboveThreshold(MIN_START_VELOCITY)) {
      decelerating = true;
      startTime = Duration.currentTimeMillis();
      stepNumber = 0;
      stepFunction.schedule((int) MS_PER_FRAME);
      return true;
    }

    return false;
  }

  /**
   * Stop decelerating.
   */
  public void stop() {
    decelerating = false;
    bouncingX = false;
    bouncingY = false;

    delegate.onDecelerationEnd();
  }

  /**
   * Helper method to calculate initial velocity.
   * 
   * @param velocity The initial velocity. The velocity passed here should be in
   *          terms of number of pixels / millisecond.
   * @return The adjusted x and y velocities.
   */
  private Point adjustInitialVelocity(Point velocity) {
    return new Point(adjustInitialVelocityForDirection(velocity.x,
        currentOffset.x, minCoord.x, maxCoord.x),
        adjustInitialVelocityForDirection(velocity.y, currentOffset.y,
            minCoord.y, maxCoord.y));
  }

  /**
   * Helper method to calculate the initial velocity for a specific direction.
   * 
   * @param originalVelocity The velocity we are adjusting.
   * @param offset The offset for this direction.
   * @param min The min coordinate for this direction.
   * @param max The max coordinate for this direction.
   * @return The calculated velocity.
   */
  private double adjustInitialVelocityForDirection(double originalVelocity,
      double offset, double min, double max) {
    // Convert from pixels/ms to pixels/frame
    double vel = originalVelocity * MS_PER_FRAME
        * INITIAL_VELOCITY_BOOST_FACTOR;

    // If the initial velocity is below the minimum threshold, it is possible
    // that we need to bounce back depending on where the element is.
    if (Math.abs(vel) < MIN_START_VELOCITY) {
      // If either of these cases are true, then the element is outside of its
      // allowable region and we need to apply a bounce back acceleration to
      // bring it back to rest in its defined area.
      if (offset < min) {
        vel = (min - offset) * POST_BOUNCE_COEFFICIENT;
        vel = Math.max(vel, MIN_START_VELOCITY);
      } else if (offset > max) {
        vel = (offset - max) * POST_BOUNCE_COEFFICIENT;
        vel = -Math.max(vel, MIN_START_VELOCITY);
      }
    }

    return vel;
  }

  /**
   * Decelerate the current velocity.
   */
  private void adjustVelocity() {
    adjustVelocityComponent(currentOffset.x, minCoord.x, maxCoord.x,
        velocity.x, bouncingX, false /* horizontal */
    );
    adjustVelocityComponent(currentOffset.y, minCoord.y, maxCoord.y,
        velocity.y, bouncingY, true /* vertical */
    );
  }

  /**
   * Apply deceleration to a specifc direction.
   * 
   * @param offset The offset for this direction.
   * @param min The min coordinate for this direction.
   * @param max The max coordinate for this direction.
   * @param velocity The velocity for this direction.
   * @param bouncing Whether this direction is bouncing.
   * @param vertical Whether or not the direction is vertical.
   */
  private void adjustVelocityComponent(double offset, double min, double max,
      double velocity, boolean bouncing, boolean vertical) {
    double speed = Math.abs(velocity);

    // Apply the deceleration factor several times as we get closer to stopping.
    int numTimes = speed < 15 ? (speed < 3 ? 3 : 2) : 1;
    velocity *= Math.pow(DECELERATION_FACTOR, numTimes);

    double stretchDistance = 0;

    // We make special adjustments to the velocity if the element is outside of
    // its region.
    if (offset < min) {
      stretchDistance = min - offset;
    } else if (offset > max) {
      stretchDistance = max - offset;
    }

    // If stretchDistance has a value then we are either bouncing or bouncing
    // back.
    if (stretchDistance != 0) {
      // If our adjustment is in the opposite direction of our velocity then we
      // are still trying to turn around. Else we are bouncing back.
      if (stretchDistance * velocity < 0) {
        bouncing = true;
        velocity += stretchDistance * PRE_BOUNCE_COEFFICIENT;
      } else {
        bouncing = false;
        velocity = stretchDistance * POST_BOUNCE_COEFFICIENT;
      }
    }

    if (vertical) {
      this.velocity.y = velocity;
      bouncingY = bouncing;
    } else {
      this.velocity.x = velocity;
      bouncingX = bouncing;
    }
  }

  /**
   * Checks whether or not an animation step is necessary or not. Animations
   * steps are not necessary when the velocity gets so low that in several
   * frames the offset is the same.
   * 
   * @return True if there is movement to be done in the next frame.
   */
  private boolean isStepNecessary() {
    return Math.abs(currentOffset.y + velocity.y - previousOffset.y) > 1
        || Math.abs(currentOffset.x + velocity.x - previousOffset.x) > 1;
  }

  /**
   * Whether or not the current velocity is above the threshold required to
   * continue decelerating. Once both the x and y velocity fall below the
   * threshold, the element should stop moving entirely.
   * 
   * @param threshold The threshold to measure against.
   * @return True if the x or y velocity is still above the threshold.
   */
  private boolean isVelocityAboveThreshold(double threshold) {
    return Math.abs(velocity.x) >= threshold
        || Math.abs(velocity.y) >= threshold;
  }

  /**
   * Calculate the next offset of the element and animate it to that position.
   */
  private void step() {
    // If deceleration is stopped between frames this is possible. Need to abort
    // the step if this happens.
    if (!decelerating) {
      return;
    }

    double now = Duration.currentTimeMillis();
    double framesExpected = Math.floor((now - startTime) / MS_PER_FRAME);

    // Do at least one step, and more if subsequent steps are not necessary or
    // if we are falling behind.
    do {
      stepWithoutAnimation();
    } while (isVelocityAboveThreshold(DECELERATION_STOP_VELOCITY)
        && (!isStepNecessary() || framesExpected > stepNumber));

    double floorY = currentOffset.y;
    double floorX = currentOffset.x;

    // If we have moved a whole integer then notify the delegate and update the
    // previous position.
    if (decelerating) {
      delegate.onDecelerate(floorX, floorY, velocity);
      previousOffset.y = floorY;
      previousOffset.x = floorX;
    }

    // This condition checks of deceleration is over.
    if (!isBouncing()
        && !isVelocityAboveThreshold(DECELERATION_STOP_VELOCITY)) {
      stop();
      return;
    }

    stepFunction.schedule((int) (MS_PER_FRAME * (1 + stepNumber - framesExpected)));
  }

  /**
   * Update the x, y values of the element offset without actually moving the
   * element. This is done because we store decimal values for x, y for
   * precision, but moving is only required when the offset is changed by at
   * least a whole integer.
   */
  private void stepWithoutAnimation() {
    stepNumber++;
    currentOffset.y += velocity.y;
    currentOffset.x += velocity.x;

    adjustVelocity();
  }
}
