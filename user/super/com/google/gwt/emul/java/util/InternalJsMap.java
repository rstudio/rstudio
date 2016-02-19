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
package java.util;

import javaemul.internal.JsUtils;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

// TODO(goktug): These classes should be interfaces with defender methods instead.
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
class InternalJsMap<V> {

  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  static class Iterator<V> {
    public native IteratorEntry<V> next();
  }

  @JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
  static class IteratorEntry<V> {
    private Object[] value;
    public boolean done;
    @JsOverlay
    public final String getKey() { return JsUtils.unsafeCastToString(value[0]); }
    @JsOverlay
    public final V getValue() { return (V) value[1]; }
  }

  public native V get(int key);
  public native V get(String key);
  public native void set(int key, V value);
  public native void set(String key, V value);
  @JsOverlay
  public final void delete(int key) { JsHelper.delete(this, key); }
  @JsOverlay
  public final void delete(String key) { JsHelper.delete(this, key); }
  public native Iterator<V> entries();

  // Calls to delete are via brackets to be compatible with old browsers where delete is keyword.
  private static class JsHelper {
    static native void delete(InternalJsMap obj, int key) /*-{ obj["delete"](key); }-*/;
    static native void delete(InternalJsMap obj, String key) /*-{ obj["delete"](key); }-*/;
  }
}
