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
 * Describes the momentum of a gesture after the gesture has been completed. You
 * can use it to control the speed of scrolling for scrollable widgets affected
 * by {@link TouchScroller}.
 */
public interface Momentum {

  /**
   * A snapshot of the current state.
   */
  public static class State {

    private int cumulativeElapsedMillis = 0;
    private int elapsedMillis = 0;
    private final Point initialPosition;
    private final Point initialVelocity;
    private Point position;
    private Point velocity;

    /**
     * Construct a new {@link State}.
     * 
     * @param initialPosition the initial position, which is also set to the
     *          current position
     * @param initialVelocity the initial velocity in pixels per millisecond,
     *          which is also set to the current velocity
     */
    public State(Point initialPosition, Point initialVelocity) {
      this.initialPosition = initialPosition;
      this.initialVelocity = initialVelocity;
      this.position = new Point(initialPosition);
      this.velocity = new Point(initialVelocity);
    }

    /**
     * Get the cumulative elapsed time in milliseconds since momentum took over.
     * 
     * @return the elapsed time in milliseconds
     */
    public int getCumulativeElapsedMillis() {
      return cumulativeElapsedMillis;
    }

    /**
     * Get the elapsed time in milliseconds since the last time the state was
     * updated.
     * 
     * @return the elapsed time in milliseconds
     */
    public int getElapsedMillis() {
      return elapsedMillis;
    }

    /**
     * Get the initial position when the momentum took over.
     * 
     * @return the initial position
     */
    public Point getInitialPosition() {
      return initialPosition;
    }

    /**
     * Get the initial velocity in pixels per millisecond when the momentum took
     * over.
     * 
     * @return the initial velocity
     */
    public Point getInitialVelocity() {
      return initialVelocity;
    }

    /**
     * Get the current position.
     * 
     * @return the current position
     */
    public Point getPosition() {
      return position;
    }

    /**
     * Get the current velocity in pixels per millisecond.
     * 
     * @return the current velocity
     */
    public Point getVelocity() {
      return velocity;
    }

    /**
     * Set the current position.
     * 
     * @param position set the current position
     */
    public void setPosition(Point position) {
      this.position = position;
    }

    /**
     * Get the current velocity in pixels per millisecond.
     * 
     * @param velocity set the current velocity
     */
    public void setVelocity(Point velocity) {
      this.velocity = velocity;
    }

    /**
     * Set the cumulative elapsed time in milliseconds since momentum took over.
     * 
     * @return the elapsed time in milliseconds
     */
    void setCumulativeElapsedMillis(int cumulativeElapsedMillis) {
      this.cumulativeElapsedMillis = cumulativeElapsedMillis;
    }

    /**
     * Set the elapsed time in milliseconds since the last time the state was
     * updated.
     * 
     * @return the elapsed time
     */
    void setElapsedMillis(int elapsedMillis) {
      this.elapsedMillis = elapsedMillis;
    }
  }

  /**
   * Create a {@link State} instance. The {@link State} instance will be passed
   * to {@link Momentum#updateState(State)} until the momentum is depleted.
   * 
   * @param initialPosition the initial position
   * @param initialVelocity the initial velocity in pixels per millisecond
   */
  State createState(Point initialPosition, Point initialVelocity);

  /**
   * <p>
   * Update the state based on the specified {@link State}. When no more
   * momentum remains, this method should return false to stop future calls.
   * </p>
   * <p>
   * The {@link State} instance is created by a call to
   * {@link #createState(Point, Point)}, and the same instance if used for the
   * duration of the momentum. This method should modify the existing state by
   * calling {@link State#setPosition(Point)} and/or
   * {@link State#setVelocity(Point)}.
   * </p>
   * 
   * @param state the current state
   * @return true to continue momentum, false if no momentum remains
   */
  boolean updateState(State state);
}
