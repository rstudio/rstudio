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
  private static JavaScriptObject stringCastMap;

  static native boolean canCast(Object src, JavaScriptObject dstId) /*-{
    return @com.google.gwt.lang.Cast::isJavaString(*)(src) &&
        !!@com.google.gwt.lang.Cast::stringCastMap[dstId] ||
        src.@java.lang.Object::castableTypeMap && !!src.@java.lang.Object::castableTypeMap[dstId];
  }-*/;

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
    if (src != null && !canCast(src, dstId)) {
      throw new ClassCastException();
    }
    return src;
  }

  static Object dynamicCastToString(Object src) {
    if (src != null && !isJavaString(src)) {
      throw new ClassCastException();
    }
    return src;
  }

  /**
   * Allow a dynamic cast to an object, always succeeding if it's a JSO.
   */
  static Object dynamicCastAllowJso(Object src, JavaScriptObject dstId) {
    if (src != null && !isJavaScriptObject(src) && !canCast(src, dstId)) {
      throw new ClassCastException();
    }
    return src;
  }

  /**
   * Allow a cast to JSO only if there's no type ID.
   */
  static Object dynamicCastJso(Object src) {
    if (src != null && !isJavaScriptObject(src)) {
      throw new ClassCastException();
    }
    return src;
  }

  /**
   * A dynamic cast that optionally checks for JsInterface prototypes.
   */
  static Object dynamicCastWithPrototype(Object src, JavaScriptObject dstId, String jsType) {
    if (src != null && !canCast(src, dstId) && !jsInstanceOf(src, jsType)) {
      throw new ClassCastException();
    }
    return src;
  }

  static boolean instanceOf(Object src, JavaScriptObject dstId) {
    return (src != null) && canCast(src, dstId);
  }

  static boolean instanceOfJsInterface(Object src, JavaScriptObject dstId, String jsType) {
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

  static boolean isJavaScriptObject(Object src) {
    return !isJavaString(src) && !Util.hasTypeMarker(src);
  }

  static boolean isJavaScriptObjectOrString(Object src) {
    return !Util.hasTypeMarker(src);
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

  /**
   * Determine if object is an instanceof jsType regardless of window or frame.
   */
  static native boolean jsInstanceOf(Object obj, String jsType) /*-{
    if (!obj) {
        return false;
    }

    // fast check for $wnd versions when CastNormalizer can pass function refs directly
    // TODO(cromwellian) restore JSymbolLiteral to allow JS reference literals to be passed through
    if (typeof(jsType) === 'function'  && obj instanceof jsType) {
        return true;
    }

    // early out for non-Java types because this check is expensive
    if (typeof(obj) === 'string' || @Util::hasTypeMarker(Ljava/lang/Object;)(obj)) {
        return false;
    }

    // hack workaround for HtmlUnit and fast early exit
    // This *ONLY* works for functions in JS that are non-anonymous and doesn't obey hierarchy
    // TODO(cromwellian) TEMPORARY: fix HtmlUnit patch upstream and remove this
    if (obj.constructor && obj.constructor.name == jsType) {
        return true;
    }

    // More general check, works on all browsers
    if (obj.constructor) {
      // TODO: remove support for $wnd
      var isMainWindow = jsType.substring(0, 5) == '$wnd.';
      var jsFunction = obj.constructor;
      var jsTypeInContext = $wnd;
      if (!isMainWindow) {
        // Find native 'Function' function
        while (jsFunction != jsFunction.constructor) {
          jsFunction = jsFunction.constructor;
        }
        // eval 'return this' in context to get global scope which defines obj's constructor
        var jsTypeInContext = jsFunction("return window || self;")();
      } else {
          // strip of $wnd.
          jsType = jsType.substring(5);
      }
      // build up contextWindow.some.type.Path
      for (var parts = jsType.split("."), i = 0, l = parts.length; i < l && jsTypeInContext; i++) {
          jsTypeInContext = jsTypeInContext[parts[i]];
      }
      return obj instanceof jsTypeInContext;
    }
    return false;
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

  /**
   * Returns whether the Object is a Java String.
   *
   * Java strings are translated to JavaScript strings.
   */
  // Visible for getIndexedMethod()
  static native boolean isJavaString(Object src) /*-{
    // TODO(rluble): This might need to be specialized by browser.
    return typeof(src) === "string" || src instanceof String;
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
  // Visible for getIndexedMethod()
  static boolean hasJavaObjectVirtualDispatch(Object src) {
    return !instanceofArray(src) && Util.hasTypeMarker(src);
  }

  /**
   * Returns true if the object is tagged and can respond to cast queries.
   *
   * All regular Java objects and arrays are tagged.
   */


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
