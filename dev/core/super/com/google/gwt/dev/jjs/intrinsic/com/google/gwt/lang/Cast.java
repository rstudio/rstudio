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

  protected static native boolean canCast(int srcId, int dstId) /*-{
    // either a null or 0 both will be false, short circuit
    if (!srcId)
      return false;
    
    // force to boolean
    return !!@com.google.gwt.lang.Cast::typeIdArray[srcId][dstId];
  }-*/;

  static native String charToString(char x) /*-{
    return String.fromCharCode(x);
  }-*/;

  static native Object dynamicCast(Object src, int dstId) /*-{
    if (src != null)
      @com.google.gwt.lang.Cast::canCast(II)(src.@java.lang.Object::typeId,dstId)
      || @com.google.gwt.lang.Exceptions::throwClassCastException()();

    return src;
  }-*/;

  static native boolean instanceOf(Object src, int dstId) /*-{
    if (src==null)
      return false;

    return @com.google.gwt.lang.Cast::canCast(II)(src.@java.lang.Object::typeId,dstId);
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native byte narrow_byte(Object x) /*-{
    return x << 24 >> 24;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native char narrow_char(Object x) /*-{
    return x & 0xFFFF;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native int narrow_int(Object x) /*-{
    return ~~x;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native short narrow_short(Object x) /*-{
    return x << 16 >> 16;
  }-*/;

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we rount to int, then
   * narrow to byte.
   */
  static byte round_byte(Object x) {
    return narrow_byte(floatToInt(x));
  }

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we rount to int, then
   * narrow to char.
   */
  static char round_char(Object x) {
    return narrow_char(floatToInt(x));
  }

  /**
   * See JLS 5.1.3.
   */
  static native int round_int(Object x) /*-{
    if (x > @java.lang.Integer::MAX_VALUE) return @java.lang.Integer::MAX_VALUE;
    if (x < @java.lang.Integer::MIN_VALUE) return @java.lang.Integer::MIN_VALUE;
    return x >= 0 ? Math.floor(x) : Math.ceil(x);
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  static native long round_long(Object x) /*-{
    if (x > @java.lang.Long::MAX_VALUE) return @java.lang.Long::MAX_VALUE;
    if (x < @java.lang.Long::MIN_VALUE) return @java.lang.Long::MIN_VALUE;
    return x >= 0 ? Math.floor(x) : Math.ceil(x);
  }-*/;

  /**
   * See JLS 5.1.3 for why we do a two-step cast. First we rount to int, then
   * narrow to short.
   */
  static short round_short(Object x) {
    return narrow_short(floatToInt(x));
  }

  /**
   * Check a statically false cast, which can succeed if the argument is null.
   */
  static Object throwClassCastExceptionUnlessNull(Object o)
      throws ClassCastException {
    if (o != null) {
      throw new ClassCastException();
    }
    return null;
  }

  static native JavaScriptObject wrapJSO(JavaScriptObject jso, Object seed) /*-{
    _ = seed.prototype;

    // WEIRD: The inequality below represents the fact that superclasses always
    // have typeId < any subclass typeId.  This code lets us wrap the same JSO 
    // "tighter" but never "looser". This would break if the compiler did not
    // ensure that superclass ids are less than subclass ids.
    //
    // Note also that the inequality is false (and thus allows wrapping) if
    // jso's typeId is undefined, because (undefined < positive int).

    if (jso && !(jso.@java.lang.Object::typeId >= _.@java.lang.Object::typeId)) {
      for (var i in _) {
        jso[i] = _[i];
      }
    }
    return jso;
  }-*/;

  /**
   * See JLS 5.1.3.
   */
  private static native Object floatToInt(Object x) /*-{
    if (x > @java.lang.Integer::MAX_VALUE) return @java.lang.Integer::MAX_VALUE;
    if (x < @java.lang.Integer::MIN_VALUE) return @java.lang.Integer::MIN_VALUE;
    return x >= 0 ? Math.floor(x) : Math.ceil(x);
  }-*/;

}

// CHECKSTYLE_NAMING_ON
