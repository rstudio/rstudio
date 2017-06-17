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

import javaemul.internal.annotations.DoNotAutobox;
import javaemul.internal.annotations.UncheckedCast;
import jsinterop.annotations.JsMethod;

/**
 * Provides an interface for simple JavaScript idioms that can not be expressed in Java.
 */
@SuppressWarnings("unusable-by-js")
public class JsUtils {

  @JsMethod(namespace = "<window>", name = "Date.now")
  public static native double getTime();

  @JsMethod(namespace = "<window>")
  public static native int parseInt(String s, int radix);

  public static native boolean isUndefined(Object value) /*-{
    return value === undefined;
  }-*/;

  public static native double unsafeCastToDouble(Object number) /*-{
   return number;
  }-*/;

  public static native boolean unsafeCastToBoolean(Object bool) /*-{
   return bool;
  }-*/;

  @UncheckedCast
  public static native <T> T uncheckedCast(@DoNotAutobox Object o) /*-{
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

