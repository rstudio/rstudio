/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.client;

/**
 * A utility class for measuring elapsed time.
 */
public class Duration {

  /**
   * Returns the same result as {@link System#currentTimeMillis()}, but as a
   * double. Because emulated long math is significantly slower than doubles in
   * Production Mode, this method is to be preferred.
   */
  public static native double currentTimeMillis() /*-{
    return (new Date()).getTime();
  }-*/;

  private static native int uncheckedConversion(double elapsed) /*-{
    return elapsed;
  }-*/;

  private double start = currentTimeMillis();

  /**
   * Creates a new Duration whose start time is now.
   */
  public Duration() {
  }

  /**
   * Returns the number of milliseconds that have elapsed since this object was
   * created.
   */
  public int elapsedMillis() {
    return uncheckedConversion(currentTimeMillis() - start);
  }

  /**
   * Returns the time when the object was created.
   */
  public double getStartMillis() {
    return start;
  }
}
