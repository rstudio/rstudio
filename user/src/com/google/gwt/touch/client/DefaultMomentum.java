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

/**
 * Default implementation of momentum.
 */
public class DefaultMomentum implements Momentum {

  /**
   * The constant factor applied to velocity every millisecond to simulate
   * deceleration.
   */
  private static final double DECELERATION_FACTOR = 0.9993;

  /**
   * The velocity threshold at which declereration will end.
   */
  private static final double DECELERATION_STOP_VELOCITY = 0.02;

  /**
   * The minimum deceleration rate in pixels per millisecond^2.
   */
  private static final double MIN_DECELERATION = 0.0005;

  public State createState(Point initialPosition, Point initialVelocity) {
    return new State(initialPosition, initialVelocity);
  }

  public boolean updateState(State state) {
    // Calculate the new velocity.
    int ellapsedMillis = state.getElapsedMillis();
    int totalEllapsedMillis = state.getCumulativeElapsedMillis();
    Point initialVelocity = state.getInitialVelocity();
    Point oldVelocity = state.getVelocity();
    double decelFactor = Math.pow(DECELERATION_FACTOR, totalEllapsedMillis);
    double minDecel = ellapsedMillis * MIN_DECELERATION;
    double newVelocityX =
        calcNewVelocity(initialVelocity.getX(), decelFactor, oldVelocity.getX(), minDecel);
    double newVelocityY =
        calcNewVelocity(initialVelocity.getY(), decelFactor, oldVelocity.getY(), minDecel);

    // Save the new velocity.
    Point newVelocity = new Point(newVelocityX, newVelocityY);
    state.setVelocity(newVelocity);

    // Calculate the distance traveled.
    int elapsedMillis = state.getElapsedMillis();
    Point dist = newVelocity.mult(new Point(elapsedMillis, elapsedMillis));

    // Update the state with the new point.
    Point position = state.getPosition();
    state.setPosition(position.plus(dist));

    // End momentum when we reach the threshold.
    if (Math.abs(newVelocity.getX()) < DECELERATION_STOP_VELOCITY
        && Math.abs(newVelocity.getY()) < DECELERATION_STOP_VELOCITY) {
      return false;
    }
    return true;
  }

  /**
   * Calculate the new velocity.
   * 
   * @param initialVelocity the initial velocity
   * @param decelFactor the deceleration factor based on the cumulative elapsed
   *          time
   * @param oldVelocity the previous velocity
   * @param minDecel the absolute value of the minimum deceleration over the
   *          elapsed time since the last update
   * @return the new velocity
   */
  private double calcNewVelocity(double initialVelocity, double decelFactor, double oldVelocity,
      double minDecel) {
    // Calculate the new velocity based on the deceleration factor.
    double newVelocity = initialVelocity * decelFactor;

    // Ensure that we are decelerating faster than the minimum rate.
    if (oldVelocity >= 0) {
      double maxVelocityX = Math.max(0.0, oldVelocity - minDecel);
      newVelocity = Math.min(newVelocity, maxVelocityX);
    } else {
      double minVelocityX = Math.min(0.0, oldVelocity + minDecel);
      newVelocity = Math.max(newVelocity, minVelocityX);
    }

    return newVelocity;
  }
}
