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

import junit.framework.TestCase;

/**
 * Tests for {@link Point}.
 */
public class PointTest extends TestCase {

  public void testDiv() {
    Point p0 = new Point(6.0, 10.0);
    Point p1 = new Point(2.0, 5.0);
    Point result = p0.div(p1);
    assertEquals(6.0, p0.getX());
    assertEquals(10.0, p0.getY());
    assertEquals(2.0, p1.getX());
    assertEquals(5.0, p1.getY());
    assertEquals(3.0, result.getX());
    assertEquals(2.0, result.getY());
  }

  public void testEquals() {
    Point base = new Point(1.0, 2.0);

    // Equals.
    assertTrue(base.equals(new Point(1.0, 2.0)));

    // Different x.
    assertFalse(base.equals(new Point(3.0, 2.0)));

    // Different y.
    assertFalse(base.equals(new Point(1.0, 3.0)));

    // Different x and y.
    assertFalse(base.equals(new Point(3.0, 4.0)));

    // Null.
    assertFalse(base.equals(null));
  }

  public void testMinus() {
    Point p0 = new Point(5.0, 7.0);
    Point p1 = new Point(1.0, 2.0);
    Point result = p0.minus(p1);
    assertEquals(5.0, p0.getX());
    assertEquals(7.0, p0.getY());
    assertEquals(1.0, p1.getX());
    assertEquals(2.0, p1.getY());
    assertEquals(4.0, result.getX());
    assertEquals(5.0, result.getY());
  }

  public void testMult() {
    Point p0 = new Point(5.0, 7.0);
    Point p1 = new Point(2.0, 3.0);
    Point result = p0.mult(p1);
    assertEquals(5.0, p0.getX());
    assertEquals(7.0, p0.getY());
    assertEquals(2.0, p1.getX());
    assertEquals(3.0, p1.getY());
    assertEquals(10.0, result.getX());
    assertEquals(21.0, result.getY());
  }

  public void testPlus() {
    Point p0 = new Point(1.0, 2.0);
    Point p1 = new Point(4.0, 5.0);
    Point result = p0.plus(p1);
    assertEquals(1.0, p0.getX());
    assertEquals(2.0, p0.getY());
    assertEquals(4.0, p1.getX());
    assertEquals(5.0, p1.getY());
    assertEquals(5.0, result.getX());
    assertEquals(7.0, result.getY());
  }
}
