/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.core.client.GwtScriptOnly;

/**
 * A class associating a (String, Object) map with arbitrary source objects
 * (except for Strings). This implementation is used in web mode.
 */
@GwtScriptOnly
public class WeakMapping {

  /*
   * This implementation is used in web mode only. It stores the (key, value)
   * maps in an expando field on their source objects.
   */

  /**
   * Returns the Object associated with the given key in the (key, value)
   * mapping associated with the given Object instance.
   * 
   * @param instance the source Object.
   * @param key a String key.
   * @return an Object associated with that key on the given instance, or null.
   */
  public static native Object get(Object instance, String key) /*-{
    if (instance.@java.lang.Object::expando) {
      return instance.@java.lang.Object::expando[':' + key];
    }
    return null;
  }-*/;

  /**
   * Associates a value with a given key in the (key, value) mapping associated
   * with the given Object instance. Note that the key space is module-wide, so
   * some care should be taken to choose sufficiently unique identifiers.
   * 
   * @param instance the source Object.
   * @param key a String key.
   * @param value the Object to associate with the key on the given source
   *          Object.
   */
  public static void set(Object instance, String key, Object value) {
    assert !(instance instanceof String) : "Cannot use Strings with WeakMapping";
    setNative(instance, key, value);
  }

  public static void setWeak(Object instance, String key, Object value) {
    set(instance, key, value);
  }

  private static native void setNative(Object instance, String key, Object value) /*-{
    if (!instance.@java.lang.Object::expando) {
      instance.@java.lang.Object::expando = {};
    }
    instance.@java.lang.Object::expando[':' + key] = value;
  }-*/;
}
