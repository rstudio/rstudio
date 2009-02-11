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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptObject;

// CHECKSTYLE_NAMING_OFF: Uses legacy conventions of underscore prefixes.

/**
 * This is a magic class the compiler uses to perform any cast operations that
 * require code.
 */
final class Cast {

  // magic magic magic
  protected static Object typeIdArray;

  static native boolean canCast(int srcId, int dstId) /*-{
    return srcId && !!@com.google.gwt.lang.Cast::typeIdArray[srcId][dstId];
  }-*/;

  /**
   * Danger: value not coerced to boolean; use the result only in a boolean
   * context.
   */
  static native boolean canCastUnsafe(int srcId, int dstId) /*-{
    return srcId && @com.google.gwt.lang.Cast::typeIdArray[srcId][dstId];
  }-*/;

  static native String charToString(char x) /*-{
    return String.fromCharCode(x);
  }-*/;

  static Object dynamicCast(Object src, int dstId) {
    if (src != null && !canCastUnsafe(Util.getTypeId(src), dstId)) {
      throw new ClassCastException();
    }
    return src;
  }

  /**
   * Allow a dynamic cast to an object, always succeeding if it's a JSO.
   */
  static Object dynamicCastAllowJso(Object src, int dstId) {
    if (src != null && !isJavaScriptObject(src) &&
        !canCastUnsafe(Util.getTypeId(src), dstId)) {
      throw new ClassCastException();
    }
    return src;
  }
  
  /**
   * Allow a cast to JSO only if there's no type ID.
   */
  static Object dynamicCastJso(Object src) {
    if (src != null && isJavaObject(src)) {
      throw new ClassCastException();
    }
    return src;
  }

  static boolean instanceOf(Object src, int dstId) {
    return (src != null) && canCast(Util.getTypeId(src), dstId);
  }

  /**
   * Instance of JSO only if there's no type ID.
   */
  static boolean instanceOfJso(Object src) {
    return (src != null) && isJavaScriptObject(src);
  }

  /**
   * Returns true if the object is a Java object and can be cast, or if it's a
   * non-null JSO.
   */
  static boolean instanceOfOrJso(Object src, int dstId) {
    return (src != null) &&
        (isJavaScriptObject(src) || canCast(Util.getTypeId(src), dstId));
  }

  static boolean isJavaObject(Object src) {
    return Util.getTypeMarker(src) == getNullMethod() || Util.getTypeId(src) == 2;
  }

  static boolean isJavaScriptObject(Object src) {
    return Util.getTypeMarker(src) != getNullMethod() && Util.getTypeId(src) != 2;
  }

  static boolean isJavaScriptObjectOrString(Object src) {
    return Util.getTypeMarker(src) != getNullMethod();
  }

  /**
   * Uses the not operator to perform a null-check; do NOT use on anything that
   * could be a String.
   */
  static native boolean isNotNull(Object src) /*-{
    // Coerce to boolean.
    return !!src;
  }-*/;

  /**
   * Uses the not operator to perform a null-check; do NOT use on anything that
   * could be a String.
   */
  static native boolean isNull(Object src) /*-{
    return !src;
  }-*/;

  static native boolean jsEquals(Object a, Object b) /*-{
    return a == b;
  }-*/;

  static native boolean jsNotEquals(Object a, Object b) /*-{
    return a != b;
  }-*/;

  static native Object maskUndefined(Object src) /*-{
    return (src == null) ? null : src;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native byte narrow_byte(double x) /*-{
    return x << 24 >> 24;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native char narrow_char(double x) /*-{
    return x & 0xFFFF;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native int narrow_int(double x) /*-{
    return ~~x;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native short narrow_short(double x) /*-{
    return x << 16 >> 16;
  }-*/;

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we round to int, then
   * narrow to byte.
   */
  static byte round_byte(double x) {
    return narrow_byte(round_int(x));
  }

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we round to int, then
   * narrow to char.
   */
  static char round_char(double x) {
    return narrow_char(round_int(x));
  }

  /**
   * See JLS 5.1.3.
   */
  static native int round_int(double x) /*-{
    // TODO: reference java.lang.Integer::MAX_VALUE when we get clinits fixed
    return ~~Math.max(Math.min(x, 2147483647), -2147483648);
  }-*/;

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we rount to int, then
   * narrow to short.
   */
  static short round_short(double x) {
    return narrow_short(round_int(x));
  }

  /**
   * Check a statically false cast, which can succeed if the argument is null.
   * Called by compiler-generated code based on static type information.
   */
  static Object throwClassCastExceptionUnlessNull(Object o)
      throws ClassCastException {
    if (o != null) {
      throw new ClassCastException();
    }
    return o;
  }

  private static native JavaScriptObject getNullMethod() /*-{
    return @null::nullMethod();
  }-*/;

}

// CHECKSTYLE_NAMING_ON
