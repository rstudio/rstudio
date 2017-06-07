/*
 * Copyright 2015 Google Inc.
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
package javaemul.internal;

import javaemul.internal.annotations.UncheckedCast;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

/**
 * Provides an interface for simple JavaScript idioms that can not be expressed in Java.
 */
public class JsUtils {

  @JsMethod(namespace = "<window>", name = "Date.now")
  public static native double getTime();

  @JsMethod(namespace = "<window>")
  public static native boolean isFinite(double d);

  @JsMethod(namespace = "<window>")
  public static native boolean isNaN(double d);

  @JsMethod(namespace = "<window>")
  public static native int parseInt(String s, int radix);

  @JsProperty(namespace = "<window>")
  public static native Object getUndefined();

  public static boolean isUndefined(Object value) {
    return isSame(value, getUndefined());
  }

  public static native boolean isSame(Object x, Object y) /*-{
    return x === y;
   }-*/;

  public static native double unsafeCastToDouble(Object number) /*-{
   return number;
  }-*/;

  public static native boolean unsafeCastToBoolean(Object bool) /*-{
   return bool;
  }-*/;

  @UncheckedCast
  public static native <T> T uncheckedCast(Object o) /*-{
    return o;
  }-*/;

  @UncheckedCast
  public static native <T> T getProperty(Object map, String key) /*-{
    return map[key];
  }-*/;

  public static native void setProperty(Object map, String key, Object value) /*-{
    map[key] = value;
  }-*/;

  public static native void setPropertySafe(Object map, String key, Object value) /*-{
    try {
      // This may throw exception in strict mode.
      map[key] = value;
    } catch(ignored) { }
  }-*/;

  public static native String typeOf(Object o) /*-{
    return typeof o;
  }-*/;
}

