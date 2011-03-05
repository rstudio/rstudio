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
 * A simple point class.
 */
public class Point {

  private final double x;
  private final double y;

  public Point() {
    this(0.0, 0.0);
  }

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public Point(Point c) {
    this(c.x, c.y);
  }

  /**
   * Divide this point {@link Point} by specified point and return the result.
   * Does not modified this {@link Point}.
   * 
   * @param c the value by which to divide
   * @return the resulting point
   */
  public Point div(Point c) {
    return new Point(x / c.x, y / c.y);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Point)) {
      return false;
    }
    Point c = (Point) obj;
    return (x == c.x) && (y == c.y);
  }

  /**
   * Get the x value of the point.
   * 
   * @return the x value
   */
  public double getX() {
    return x;
  }

  /**
   * Get the y value of the point.
   * 
   * @return the y value
   */
  public double getY() {
    return y;
  }

  @Override
  public int hashCode() {
    return (int) x ^ (int) y;
  }

  /**
   * Subtract the specified {@link Point} from this point and return the result.
   * Does not modified this {@link Point}.
   * 
   * @param c the value to subtract
   * @return the resulting point
   */
  public Point minus(Point c) {
    return new Point(x - c.x, y - c.y);
  }

  /**
   * Multiple this point {@link Point} by specified point and return the result.
   * Does not modified this {@link Point}.
   * 
   * @param c the value by which to multiply
   * @return the resulting point
   */
  public Point mult(Point c) {
    return new Point(x * c.x, y * c.y);
  }

  /**
   * Add the specified {@link Point} to this point and return the result. Does
   * not modified this {@link Point}.
   * 
   * @param c the value to add
   * @return the resulting point
   */
  public Point plus(Point c) {
    return new Point(x + c.x, y + c.y);
  }

  @Override
  public String toString() {
    return "Point(" + x + "," + y + ")";
  }
}
