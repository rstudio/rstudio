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
package elemental.js.util;

/**
 * Utility class for miscellaneous numeric stuff.
 */
public class Numbers {
  /**
   * Converts a <code>double</code> to a {@link String} with a specified number
   * of decimal points.
   * 
   * @param number the number to be convert to a string
   * @param n number of decimal points
   * @return string representation of the number
   */
  public static native String toFixed(double number, int n) /*-{
    return number.toFixed(n);
  }-*/;

  /**
   * Converts a <code>double</code> to a {@link String} representing the integer
   * floor value.
   * 
   * @param number the number to convert to a string
   * @return string representation of the number
   */
  public static String toInt(double number) {
    return toFixed(number, 0);
  }

  private Numbers() {
    // You cannot have one of these, they're like Swedish Fish in the mini kitchens.
  }
}
