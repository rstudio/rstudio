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
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "Map", namespace = JsPackage.GLOBAL)
interface InternalJsMap<V> {

  @JsType(isNative = true, name = "IteratorIterable", namespace = JsPackage.GLOBAL)
  interface Iterator<V> {
    IteratorEntry<V> next();
  }

  // IteratorEntry<V> is the modeling for IIterableResult<Array<String|V>> as IteratorEntry<V> but
  // java and jsinterop lack expressibility to represent this abstraction (Java does not have
  // union types and JsInterop does not allow to map type variables). So IteratorEntry<V> ends up
  // mapping to IIterableResult<V> which is not an accurate mapping.
  // It is convenient to model in this way so that users of this internal class don't have to deal
  // with the internal implementation and the mismatch is handled here with overlay methods.
  @JsType(isNative = true, name = "IIterableResult", namespace = JsPackage.GLOBAL)
  interface IteratorEntry<V> {
    @JsProperty
    boolean isDone();
    @JsProperty(name = "value")
    Object[] getValueInternal();
    @JsOverlay
    default String getKey() { return JsUtils.uncheckedCast(getValueInternal()[0]); }
    @JsOverlay
    default V getValue() { return JsUtils.uncheckedCast(getValueInternal()[1]); }
  }

  V get(int key);
  V get(String key);
  void set(int key, V value);
  void set(String key, V value);
  Iterator<V> entries();

  @JsOverlay
  default void delete(int key) {
    // Calls delete without map.delete in order to be compatible with old browsers where delete is a
    // keyword.
    DeleteFunction fn = JsUtils.getProperty(this, "delete");
    fn.call(this, key);
  }

  @JsOverlay
  default void delete(String key) {
    // Calls delete without map.delete in order to be compatible with old browsers where delete is a
    // keyword.
    DeleteFunction fn = JsUtils.getProperty(this, "delete");
    fn.call(this, key);
  }

  @JsType(isNative = true, name = "Function", namespace = JsPackage.GLOBAL)
  interface DeleteFunction {
    void call(InternalJsMap<?> thisArg, String key);
    void call(InternalJsMap<?> thisArg, int key);
  }
}
