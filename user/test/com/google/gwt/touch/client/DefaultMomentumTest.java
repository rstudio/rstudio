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

import com.google.gwt.touch.client.Momentum.State;

import junit.framework.TestCase;

/**
 * Tests for {@link DefaultMomentum}.
 */
public class DefaultMomentumTest extends TestCase {

  /**
   * Test updating the state before the acceleration falls below the minimum
   * acceleration.
   */
  public void testUpdateStateFast() {
    DefaultMomentum momentum = new DefaultMomentum();
    State state = momentum.createState(new Point(0.0, 0.0), new Point(1.0, 2.0));
    state.setPosition(new Point(0.1, 0.2));
    state.setCumulativeElapsedMillis(10);
    state.setElapsedMillis(5);

    assertTrue(momentum.updateState(state));

    // Check the new velocity.
    Point velocity = state.getVelocity();
    assertEquals(0.99302, velocity.getX(), 0.000009); // 1.0 * .9993 ^ 10
    assertEquals(1.98604, velocity.getY(), 0.000009); // 2.0 * .9993 ^ 10

    // Check the new position.
    Point position = state.getPosition();
    assertEquals(5.0651, position.getX(), 0.0001); // .1 + v * 5ms
    assertEquals(10.1302, position.getY(), 0.0001); // .2 + v * 5ms5
  }

  /**
   * Test updating the state after the X acceleration falls below the minimum
   * acceleration.
   */
  public void testUpdateStateSlowX() {
    DefaultMomentum momentum = new DefaultMomentum();
    State state = momentum.createState(new Point(0.0, 0.0), new Point(0.005, 1.0));
    state.setPosition(new Point(0.2, 0.1));
    state.setCumulativeElapsedMillis(10);
    state.setElapsedMillis(5);

    assertTrue(momentum.updateState(state));

    // Check the new velocity.
    Point velocity = state.getVelocity();
    assertEquals(0.0025, velocity.getX(), 0.0001); // 0.005 - 0.0005 * 5
    assertEquals(0.99302, velocity.getY(), 0.000009); // 1.0 * .9993 ^ 10

    // Check the new position.
    Point position = state.getPosition();
    assertEquals(0.2125, position.getX(), 0.0001); // .2 + v * 5ms
    assertEquals(5.0651, position.getY(), 0.0001); // .1 + v * 5ms
  }

  /**
   * Test updating the state after the Y acceleration falls below the minimum
   * acceleration.
   */
  public void testUpdateStateSlowY() {
    DefaultMomentum momentum = new DefaultMomentum();
    State state = momentum.createState(new Point(0.0, 0.0), new Point(1.0, 0.005));
    state.setPosition(new Point(0.1, 0.2));
    state.setCumulativeElapsedMillis(10);
    state.setElapsedMillis(5);

    assertTrue(momentum.updateState(state));

    // Check the new velocity.
    Point velocity = state.getVelocity();
    assertEquals(0.99302, velocity.getX(), 0.000009); // 1.0 * .9993 ^ 10
    assertEquals(0.0025, velocity.getY(), 0.0001); // 0.005 - 0.0005 * 5

    // Check the new position.
    Point position = state.getPosition();
    assertEquals(5.0651, position.getX(), 0.0001); // .1 + v * 5ms
    assertEquals(0.2125, position.getY(), 0.0001); // .2 + v * 5ms
  }

  /**
   * Test updating the state returns null when we reach the minimum velocity.
   */
  public void testUpdateStateMinimumVelociy() {
    DefaultMomentum momentum = new DefaultMomentum();
    State state = momentum.createState(new Point(0.0, 0.0), new Point(0.02, 0.02));
    state.setPosition(new Point(0.1, 0.2));
    state.setCumulativeElapsedMillis(10);
    state.setElapsedMillis(5);

    assertFalse(momentum.updateState(state));
  }
}
