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
 * A utility class for accessing global functions and values in an ECMAScript
 * context.
 */
public class JsGlobals {
  /**
   * Decodes an encoded URI to a URI string.
   */
  public native static String decodeURI(String encodedURI) /*-{
    return decodeURI(encodedURI);
  }-*/;

  /**
   * Decodes an encoded URI component to a URI component string.
   */
  public native static String decodeURIComponent(String encodedURIComponent) /*-{
    return decodeURIComponent(encodedURIComponent);
  }-*/;

  /**
   * Encodes a URI string by escaping all characters not allowed in URIs.
   */
  public native static String encodeURI(String uri) /*-{
    return encodeURI(uri);
  }-*/;

  /**
   * Encodes a URI component string by escaping all characters not allowed in
   * URIs.
   */
  public static native String encodeURIComponent(String uriComponent) /*-{
    return encodeURIComponent(uriComponent);
  }-*/;

  /**
   * Indicates if <code>value</code> is a finite number.
   */
  public native static boolean isFinite(double value) /*-{
    return isFinite(value);
  }-*/;

  /**
   * Indicates if <code>value</code> is <code>NaN</code>.
   */
  public native static boolean isNaN(double value) /*-{
    return isNaN(value);
  }-*/;

  /**
   * Produces a <code>double</code> value by interpreting the contents of
   * <code>value</code> as a decimal literal.
   */
  public native static double parseFloat(String value) /*-{
    return parseFloat(value);
  }-*/;

  /**
   * Produces a <code>double</code> value by interpreting the contents of
   * <code>value</code> as a decimal literal.
   */
  public native static double parseFloat(String value, int radix) /*-{
    return parseFloat(value, radix);
  }-*/;

  /**
   * Produces a integral value by interpreting the contents of
   * <code>value</code> as a integer literal. The value returned by this method
   * will be an integral value or <code>NaN</code>, so the return value is a
   * <code>double</code>.
   * 
   * <pre>
   * Example use:
   * // Will yield zero if s is not a valid integer.
   * int value = (int)parseInt(s);
   * 
   * // To check if the value is valid.
   * double value = parseInt(s);
   * if (isNaN(value))
   *   //Invalid value.
   * </pre>
   */
  public native static double parseInt(String value) /*-{
    return parseInt(value);
  }-*/;

  /**
   * Produces a integral value by interpreting the contents of
   * <code>value</code> as a integer literal. The value returned by this method
   * will be an integral value or <code>NaN</code>, so the return value is a
   * <code>double</code>.
   * 
   * <pre>
   * Example use:
   * // Will yield zero if s is not a valid integer.
   * int value = (int)parseInt(s);
   * 
   * // To check if the value is valid.
   * double value = parseInt(s);
   * if (isNaN(value))
   *   //Invalid value.
   * </pre>
   */
  public native static double parseInt(String value, int radix) /*-{
    return parseInt(value, radix);
  }-*/;

  private JsGlobals() {
  }
}
