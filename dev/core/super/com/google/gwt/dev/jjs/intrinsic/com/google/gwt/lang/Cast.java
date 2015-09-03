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

import static javaemul.internal.InternalPreconditions.checkType;

import com.google.gwt.core.client.JavaScriptObject;

import javaemul.internal.annotations.HasNoSideEffects;

// CHECKSTYLE_NAMING_OFF: Uses legacy conventions of underscore prefixes.

/**
 * This is a magic class the compiler uses to perform any cast operations that require code.<br />
 *
 * The cast operations are only as accurate as the contents of the castableTypeMaps and should not
 * be used directly by user code. The compiler takes care to record most cast operations in user
 * code so that it can build limited but accurate castableTypeMaps.
 */
final class Cast {

  /**
   * As plain JavaScript Strings (not monkey patched) are used to model Java Strings,
   * {@code  stringCastMap} stores runtime type info for cast purposes for string objects.
   *
   * NOTE: it is important that the field is left uninitialized so that Cast does not
   * require a clinit.
   */
  // NOTE: if any of these three are edited, update JProgram.DispatchType's constructor
  private static JavaScriptObject stringCastMap;

  // the next two are implemented exactly as the former
  private static JavaScriptObject doubleCastMap;
  private static JavaScriptObject booleanCastMap;

  @HasNoSideEffects
  static native boolean canCast(Object src, JavaScriptObject dstId) /*-{
    return @com.google.gwt.lang.Cast::isJavaString(*)(src) &&
        !!@com.google.gwt.lang.Cast::stringCastMap[dstId] ||
        src.@java.lang.Object::castableTypeMap && !!src.@java.lang.Object::castableTypeMap[dstId] ||
        @com.google.gwt.lang.Cast::isJavaDouble(*)(src) &&
        !!@com.google.gwt.lang.Cast::doubleCastMap[dstId] ||
        // this occurs last because it is much rarer and less likely to be in hot code
        @com.google.gwt.lang.Cast::isJavaBoolean(*)(src) &&
        !!@com.google.gwt.lang.Cast::booleanCastMap[dstId];
  }-*/;

  @HasNoSideEffects
  static native boolean canCastClass(Class<?> srcClazz, Class<?> dstClass) /*-{
    var srcTypeId = srcClazz.@java.lang.Class::typeId;
    var dstTypeId = dstClass.@java.lang.Class::typeId;
    var prototype = @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId[srcTypeId];
    return @com.google.gwt.lang.Cast::canCast(*)(prototype, dstTypeId);
  }-*/;

  static native String charToString(char x) /*-{
    return String.fromCharCode(x);
  }-*/;

  static Object dynamicCast(Object src, JavaScriptObject dstId) {
    checkType(src == null || canCast(src, dstId));
    return src;
  }

  // NOTE: if any of these three are edited, update JProgram.DispatchType's constructor
  static Object dynamicCastToString(Object src) {
    checkType(src == null || isJavaString(src));
    return src;
  }

  static Object dynamicCastToDouble(Object src) {
    checkType(src == null || isJavaDouble(src));
    return src;
  }

  static Object dynamicCastToBoolean(Object src) {
    checkType(src == null || isJavaBoolean(src));
    return src;
  }

  /**
   * Allow a dynamic cast to an object, always succeeding if it's a JSO.
   */
  static Object dynamicCastAllowJso(Object src, JavaScriptObject dstId) {
    checkType(src == null || isJavaScriptObject(src) || canCast(src, dstId));
    return src;
  }

  /**
   * Allow a cast to JSO only if there's no type ID.
   */
  static Object dynamicCastJso(Object src) {
    checkType(src == null || isJavaScriptObject(src));
    return src;
  }

  /**
   * Allow a dynamic cast to a JsFunction interface only if it is a function.
   */
  static Object dynamicCastToJsFunction(Object src) {
    checkType(src == null || isFunction(src));
    return src;
  }

  /**
   * A dynamic cast that optionally checks for JsType prototypes.
   */
  static Object dynamicCastWithPrototype(Object src, JavaScriptObject dstId, String jsType) {
    checkType(src == null || canCast(src, dstId) || jsInstanceOf(src, jsType));
    return src;
  }

  static boolean instanceOf(Object src, JavaScriptObject dstId) {
    return (src != null) && canCast(src, dstId);
  }

  static boolean instanceOfJsPrototype(Object src, JavaScriptObject dstId, String jsType) {
    return instanceOf(src, dstId) || jsInstanceOf(src, jsType);
  }

  static boolean instanceOfJso(Object src) {
    return (src != null) && isJavaScriptObject(src);
  }

  /**
   * Returns true if the object is a Java object and can be cast, or if it's a
   * non-null JSO.
   */
  static boolean instanceOfOrJso(Object src, JavaScriptObject dstId) {
    return (src != null) &&
        (isJavaScriptObject(src) || canCast(src, dstId));
  }

  /**
   * Returns true if the object is a function.
   */
  static boolean instanceOfJsFunction(Object src) {
    return (src != null) && isFunction(src);
  }

  /**
   * Returns whether the Object is a function.
   */
  @HasNoSideEffects
  static native boolean isFunction(Object src) /*-{
    return typeof(src) === "function";
  }-*/;

  @HasNoSideEffects
  static boolean isJavaScriptObject(Object src) {
    return isJsObjectOrFunction(src) && !Util.hasTypeMarker(src);
  }

  /**
   * Uses the not operator to perform a null-check; do NOT use on anything that
   * could be a String, 'unboxed' Double, or 'unboxed' Boolean.
   */
  static native boolean isNotNull(Object src) /*-{
    // Coerce to boolean.
    return !!src;
  }-*/;

  /**
   * Uses the not operator to perform a null-check; do NOT use on anything that
   * could be a String, 'unboxed' Double, or 'unboxed' Boolean.
   */
  static native boolean isNull(Object src) /*-{
    return !src;
  }-*/;

  static native boolean jsEquals(Object a, Object b) /*-{
    return a == b;
  }-*/;

  /**
   * Determine if object is an instanceof jsType regardless of window or frame.
   */
  @HasNoSideEffects
  static native boolean jsInstanceOf(Object obj, String jsTypeStr) /*-{
    if (!obj) {
        return false;
    }

    var jsType = $wnd;
    for (var i = 0, parts = jsTypeStr.split("."), l = parts.length; i < l ; i++) {
      jsType = jsType && jsType[parts[i]];
    }
    return jsType && obj instanceof jsType;
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
    return x | 0;
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
    return Math.max(Math.min(x, 2147483647), -2147483648) | 0;
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
  static Object throwClassCastExceptionUnlessNull(Object o) throws ClassCastException {
    checkType(o == null);
    return o;
  }

  @HasNoSideEffects
  static native boolean isJsObjectOrFunction(Object src) /*-{
    return typeof(src) === "object" || typeof(src) === "function";
  }-*/;

  // NOTE: if any of these three are edited, update JProgram.DispatchType's constructor
  /**
   * Returns whether the Object is a Java String.
   *
   * Java strings are translated to JavaScript strings.
   */
  @HasNoSideEffects
  static native boolean isJavaString(Object src) /*-{
    return typeof(src) === "string";
  }-*/;

  /**
   * Returns whether the Object is a Java Double.
   *
   * Java Numbers are translated to JavaScript numbers.
   */
  @HasNoSideEffects
  static native boolean isJavaDouble(Object src) /*-{
    return typeof(src) === "number";
  }-*/;

  /**
   * Returns whether the Object is a Java Boolean. (*)
   *
   * Java Booleans are translated to JavaScript booleans.
   */
  @HasNoSideEffects
  static native boolean isJavaBoolean(Object src) /*-{
    return typeof(src) === "boolean";
  }-*/;

  /**
   * Returns true if Object can dispatch instance methods and does not need a compiler
   * provided trampoline.
   *
   * Java non primitive objects fall into 3 classes: Strings, arrays (of primitive or non primitive
   * types) and regular Java Objects (all others).
   *
   * Only regular Java object have dynamic instance dispatch, strings and arrays need compiler
   * generated trampolines to implement instance dispatch.
   */
  static boolean hasJavaObjectVirtualDispatch(Object src) {
    return !instanceofArray(src) && Util.hasTypeMarker(src);
  }

  /**
   * Returns true if {@code src} is a Java array.
   */
  static boolean isJavaArray(Object src) {
    return instanceofArray(src) && Util.hasTypeMarker(src);
  }

  /**
   * Returns true if {@code src} is an array (native or not).
   */
  static native boolean instanceofArray(Object src) /*-{
    return Array.isArray(src);
  }-*/;

}

// CHECKSTYLE_NAMING_ON
