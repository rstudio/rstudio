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

package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
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

class FastStringMap extends AbstractMap {
  private static class ImplMapEntry implements Map.Entry {

    private Object key;

    private Object value;

    ImplMapEntry(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    public boolean equals(Object a) {
      if (a instanceof Map.Entry) {
        Map.Entry s = (Map.Entry) a;
        if (equalsWithNullCheck(key, s.getKey())
            && equalsWithNullCheck(value, s.getValue())) {
          return true;
        }
      }
      return false;
    }

    public Object getKey() {
      return key;
    }

    public Object getValue() {
      return value;
    }

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

    public Object setValue(Object object) {
      Object old = value;
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

  JavaScriptObject map;

  public FastStringMap() {
    init();
  }

  public void clear() {
    init();
  }

  public boolean containsKey(Object key) {
    return containsKey(keyMustBeString(key), map);
  }

  public boolean containsValue(Object arg0) {
    return values().contains(arg0);
  }

  public Set entrySet() {
    return new AbstractSet() {

      public boolean contains(Object key) {
        Map.Entry s = (Map.Entry) key;
        Object value = get(s.getKey());
        if (value == null) {
          return value == s.getValue();
        } else {
          return value.equals(s.getValue());
        }
      }

      public Iterator iterator() {

        Iterator custom = new Iterator() {
          Iterator keys = keySet().iterator();

          public boolean hasNext() {
            return keys.hasNext();
          }

          public Object next() {
            String key = (String) keys.next();
            return new ImplMapEntry(key, get(key));
          }

          public void remove() {
            keys.remove();
          }
        };
        return custom;
      }

      public int size() {
        return FastStringMap.this.size();
      }

    };
  }

  public Object get(Object key) {
    return get(keyMustBeString(key));
  }

  public native Object get(String key) /*-{
   var value = this.@com.google.gwt.user.client.ui.FastStringMap::map[key];
   if(value == null){
     return null;
   } else{
     return value;
   }
   }-*/;

  public boolean isEmpty() {
    return size() == 0;
  }

  public Set keySet() {
    return new AbstractSet() {
      public boolean contains(Object key) {
        return containsKey(key);
      }

      public Iterator iterator() {
        List l = new ArrayList();
        addAllKeysFromJavascriptObject(l, map);
        return l.iterator();
      }

      public int size() {
        return FastStringMap.this.size();
      }
    };
  }

  public Object put(Object key, Object widget) {
    return put(keyMustBeString(key), widget);
  }

  public native Object put(String key, Object widget) /*-{
   var previous =  this.@com.google.gwt.user.client.ui.FastStringMap::map[key];
   this.@com.google.gwt.user.client.ui.FastStringMap::map[key] = widget; 
   if(previous == null){
     return null;
   } else{
     return previous;
   }
   }-*/;

  public void putAll(Map arg0) {
    Iterator iter = arg0.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Entry) iter.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public Object remove(Object key) {
    return remove(keyMustBeString(key));
  }

  public native int size() /*-{
   var value = this.@com.google.gwt.user.client.ui.FastStringMap::map;
   var count = 0;
   for(var key in value){
     ++count;
   }
     return count;
   }-*/;

  public Collection values() {
    List values = new ArrayList();
    addAllValuesFromJavascriptObject(values, map);
    return values;
  }

  private native void addAllKeysFromJavascriptObject(Collection s,
      JavaScriptObject javaScriptObject) /*-{
   for(var key in javaScriptObject) {
     s.@java.util.Collection::add(Ljava/lang/Object;)(key);
   }
   }-*/;

  private native void addAllValuesFromJavascriptObject(Collection s,
      JavaScriptObject javaScriptObject) /*-{
   for(var key in javaScriptObject) {
     var value = javaScriptObject[key];
     s.@java.util.Collection::add(Ljava/lang/Object;)(value);
   }
   }-*/;

  private native boolean containsKey(String key, JavaScriptObject obj)/*-{
   return obj[key] !== undefined;
   }-*/;

  private native void init() /*-{
   this.@com.google.gwt.user.client.ui.FastStringMap::map = [];
   }-*/;

  private String keyMustBeString(Object key) {
    if (key instanceof String) {
      return (String) key;
    } else {
      throw new IllegalArgumentException(GWT.getTypeName(this)
          + " can only have Strings as keys, not" + key);
    }
  }

  private native Object remove(String key) /*-{
   var previous =  this.@com.google.gwt.user.client.ui.FastStringMap::map[key];
   delete this.@com.google.gwt.user.client.ui.FastStringMap::map[key];
   if(previous == null){
     return null;
   } else{
     return previous;
   }
   }-*/;

}
