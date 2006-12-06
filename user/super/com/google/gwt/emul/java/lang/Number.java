/*
 * Copyright 2006 Google Inc.
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
package java.lang;

/**
 * Abstract base class for numberic wrapper classes.
 */
public abstract class Number {

  // CHECKSTYLE_OFF: A special need to use unusual identifiers to avoid
  // introducing name collisions.

  /**
   * @skip
   */
  protected static String[] __hexDigits = new String[] {
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
      "e", "f"};

  /**
   * @skip
   */
  protected static native boolean __isLongNaN(long x) /*-{
    return isNaN(x);
  }-*/;

  /**
   * @skip
   */
  protected static native long __parseLongRadix(String s, int radix) /*-{
    return parseInt(s, radix);
  }-*/;

  /**
   * @skip
   */
  protected static native long __parseLongInfer(String s) /*-{
    return parseInt(s);
  }-*/;

  /**
   * @skip
   */
  protected static native double __parseDouble(String str) /*-{
    return parseFloat(str);
  }-*/;

  /**
   * @skip
   */
  static native float __parseFloat(String str) /*-{
    return parseFloat(str);
  }-*/;

  // CHECKSTYLE_ON

  public abstract byte byteValue();

  public abstract double doubleValue();

  public abstract float floatValue();

  public abstract int intValue();

  public abstract long longValue();

  public abstract short shortValue();
}
