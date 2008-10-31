/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.i18n.client.impl;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map used when creating <code>Constants</code> maps. This class is to be
 * used only by the GWT code. The map is immediately wrapped in
 * Collections.unmodifiableMap(..) preventing any changes after construction.
 */
public class ConstantMap extends AbstractMap<String, String> {

  /**
   * A cache of a synthesized entry set.
   */
  private Set<Map.Entry<String, String>> entries;

  /**
   * The original set of keys.
   */
  private final String[] keys;

  /*
   * Stores a fast lookup in a JSO using ':' to prevent conflict with built-in
   * JavaScript properties.
   */
  @SuppressWarnings("unused")
  private JavaScriptObject map;

  public ConstantMap(String keys[], String values[]) {
    this.keys = keys;

    init();

    for (int i = 0; i < keys.length; ++i) {
      putImpl(keys[i], values[i]);
    }
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public Set<Map.Entry<String, String>> entrySet() {
    if (entries == null) {
      Map<String, String> copy = new HashMap<String, String>();
      for (String key : keys) {
        copy.put(key, get(key));
      }
      entries = Collections.unmodifiableMap(copy).entrySet();
    }
    return entries;
  }

  @Override
  public String get(Object key) {
    return (key instanceof String) ? get((String) key) : null;
  }

  public native String get(String key) /*-{
    // Prepend ':' to avoid conflicts with built-in Object properties.
    return this.@com.google.gwt.i18n.client.impl.ConstantMap::map[':' + key];
  }-*/;

  @Override
  public Set<String> keySet() {
    return new AbstractSet<String>() {
      @Override
      public boolean contains(Object o) {
        return containsKey(o);
      }

      @Override
      public Iterator<String> iterator() {
        return Arrays.asList(keys).iterator();
      }

      @Override
      public int size() {
        return ConstantMap.this.size();
      }
    };
  }

  @Override
  public int size() {
    return keys.length;
  }

  /**
   * Overridable for testing purposes, see ConstantMapTest.
   */
  protected void init() {
    map = JavaScriptObject.createObject();
  }

  protected native void putImpl(String key, String value) /*-{
    // Prepend ':' to avoid conflicts with built-in Object properties.
    this.@com.google.gwt.i18n.client.impl.ConstantMap::map[':' + key] = value;
  }-*/;
}
