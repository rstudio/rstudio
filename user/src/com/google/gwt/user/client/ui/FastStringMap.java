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

package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Special-case Map implementation which imposes limits on the types of keys
 * that can be used in return for much faster speed. In specific, only strings
 * that could be added to a JavaScript object as keys are valid.
 */

class FastStringMap<T> extends AbstractMap<String, T> {
  private static class ImplMapEntry<T> implements Map.Entry<String, T> {

    private String key;

    private T value;

    ImplMapEntry(String key, T value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object a) {
      if (a instanceof Map.Entry<?, ?>) {
        Map.Entry<?, ?> s = (Map.Entry<?, ?>) a;
        if (equalsWithNullCheck(key, s.getKey())
            && equalsWithNullCheck(value, s.getValue())) {
          return true;
        }
      }
      return false;
    }

    // strip prefix from key
    public String getKey() {
      return key;
    }

    public T getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      int keyHash = 0;
      int valueHash = 0;
      if (key != null) {
        keyHash = key.hashCode();
      }
      if (value != null) {
        valueHash = value.hashCode();
      }
      return keyHash ^ valueHash;
    }

    public T setValue(T object) {
      T old = value;
      value = object;
      return old;
    }

    private boolean equalsWithNullCheck(Object a, Object b) {
      if (a == b) {
        return true;
      } else if (a == null) {
        return false;
      } else {
        return a.equals(b);
      }
    }
  }

  /*
   * Accesses need to be prefixed with ':' to prevent conflict with built-in
   * JavaScript properties.
   */    
  private JavaScriptObject map;

  public FastStringMap() {
    init();
  }

  @Override
  public void clear() {
    init();
  }

  @Override
  public boolean containsKey(Object key) {
    return containsKey(keyMustBeString(key), map);
  }

  @Override
  public boolean containsValue(Object arg0) {
    return values().contains(arg0);
  }

  @Override
  public Set<Map.Entry<String, T>> entrySet() {
    return new AbstractSet<Map.Entry<String, T>>() {

      @Override
      public boolean contains(Object key) {
        Map.Entry<?, ?> s = (Map.Entry<?, ?>) key;
        Object value = get(s.getKey());
        if (value == null) {
          return value == s.getValue();
        } else {
          return value.equals(s.getValue());
        }
      }

      @Override
      public Iterator<Map.Entry<String, T>> iterator() {

        Iterator<Map.Entry<String, T>> custom = new Iterator<Map.Entry<String, T>>() {
          Iterator<String> keys = keySet().iterator();

          public boolean hasNext() {
            return keys.hasNext();
          }

          public Map.Entry<String, T> next() {
            String key = keys.next();
            return new ImplMapEntry<T>(key, get(key));
          }

          public void remove() {
            keys.remove();
          }
        };
        return custom;
      }

      @Override
      public int size() {
        return FastStringMap.this.size();
      }

    };
  }

  @Override
  public T get(Object key) {
    return get(keyMustBeString(key));
  }

  // Prepend ':' to avoid conflicts with built-in Object properties.
  public native T get(String key) /*-{
    return this.@com.google.gwt.user.client.ui.FastStringMap::map[':' + key];
  }-*/;

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<String> keySet() {
    return new AbstractSet<String>() {
      @Override
      public boolean contains(Object key) {
        return containsKey(key);
      }

      @Override
      public Iterator<String> iterator() {
        List<String> l = new ArrayList<String>();
        addAllKeysFromJavascriptObject(l, map);
        return l.iterator();
      }

      @Override
      public int size() {
        return FastStringMap.this.size();
      }
    };
  }

  // Prepend ':' to avoid conflicts with built-in Object properties.
  @Override
  public native T put(String key, T value) /*-{
    key = ':' + key;
    var map = this.@com.google.gwt.user.client.ui.FastStringMap::map;
    var previous = map[key];
    map[key] = value;
    return previous;
  }-*/;

  @Override
  public void putAll(Map<? extends String, ? extends T> arg0) {
    for (Map.Entry<? extends String, ? extends T> entry : arg0.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public T remove(Object key) {
    return remove(keyMustBeString(key));
  }

  // only count keys with ':' prefix
  @Override
  public native int size() /*-{
    var value = this.@com.google.gwt.user.client.ui.FastStringMap::map;
    var count = 0;
    for(var key in value) {
      if (key.charAt(0) == ':') ++count;
    }
    return count;
  }-*/;

  @Override
  public Collection<T> values() {
    List<T> values = new ArrayList<T>();
    addAllValuesFromJavascriptObject(values, map);
    return values;
  }

  // only count keys with ':' prefix
  private native void addAllKeysFromJavascriptObject(Collection<String> s,
      JavaScriptObject javaScriptObject) /*-{
    for(var key in javaScriptObject) {
      if (key.charAt(0) != ':') continue;
      s.@java.util.Collection::add(Ljava/lang/Object;)(key.substring(1));
    }
  }-*/;

  // only count keys with ':' prefix
  private native void addAllValuesFromJavascriptObject(Collection<T> s,
      JavaScriptObject javaScriptObject) /*-{
    for(var key in javaScriptObject) {
      if (key.charAt(0) != ':') continue;
      var value = javaScriptObject[key];
      s.@java.util.Collection::add(Ljava/lang/Object;)(value);
    }
  }-*/;

  // Prepend ':' to avoid conflicts with built-in Object properties.
  private native boolean containsKey(String key, JavaScriptObject obj)/*-{
    return (':' + key) in obj;
  }-*/;

  private native void init() /*-{
    this.@com.google.gwt.user.client.ui.FastStringMap::map = [];
  }-*/;

  private String keyMustBeString(Object key) {
    if (key instanceof String) {
      return (String) key;
    } else {
      throw new IllegalArgumentException(this.getClass().getName()
          + " can only have Strings as keys, not" + key);
    }
  }

  // Prepend ':' to avoid conflicts with built-in Object properties.
  private native T remove(String key) /*-{
    key = ':' + key;
    var map = this.@com.google.gwt.user.client.ui.FastStringMap::map;
    var previous = map[key];
    delete map[key];
    return previous;
  }-*/;
}
