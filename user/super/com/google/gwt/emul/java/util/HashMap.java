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

package java.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * See Sun's JDK 1.4 documentation for documentation on the <code>HashMap</code>
 * API.
 * 
 * This implementation of <code>HashMap</code> uses JavaScript native data
 * structures to provide high performance in web mode, using string keys to
 * index strings, and sparse int keys (via hashCode()) to index everything else.
 */
public class HashMap extends AbstractMap {
  /*
   * Implementation notes:  
   *   String keys must be handled specially because we need only store one value 
   *     for a given string key. 
   *   String keys can collide with integer ones, as JavaScript treats '0' and 0 
   *     as the same key.  We resolve this by appending an 'S' on each string key.
   *   Integer keys are used for everything but Strings, as we get an integer by 
   *     taking the hashcode, and storing the value there.  Since several keys
   *     may have the same hashcode, we store a list of key/value pairs at the  
   *     index of the hashcode.  Javascript arrays are sparse, so this usage costs 
   *     nothing in performance.  Also, String and integer keys can coexist in the
   *     same data structure, which reduces storage overhead for small HashMaps.
   *   Things to pursue:
   *     Benchmarking shared data store (with the 'S' append) vs one structure for 
   *       Strings and another for everything else).
   *     Benchmarking the effect of gathering the common parts of get/put/remove 
   *       into a shared method.  These methods would have several parameters, and 
   *       some extra control flow.
   */

  /**
   * Implementation of <code>HashMap</code> entry.
   */
  private static class EntryImpl implements Map.Entry {

    private Object key;

    private Object value;

    /**
     * Constructor for <code>EntryImpl</code>.
     */
    public EntryImpl(Object key, Object value) {
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

    /**
     * Calculate the hash code using Sun's specified algorithm.
     */
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

    public String toString() {
      return getKey() + "=" + getValue();
    }

    /**
     * Checks to see of a == b correctly handling the case where a is null.
     */
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
  
  /**
   * Iterator for <code>EntrySetImpl</code>.
   */
  private final class EntrySetImplIterator implements Iterator {
    Object last = null;

    private final Iterator iter;

    /**
     * Constructor for <code>EntrySetIterator</code>.
     */
    public EntrySetImplIterator() {
      final List l = new ArrayList();
      addAllFromJavascriptObject(l, map, BOTH_POS);
      final Iterator lstIter = l.iterator();
      this.iter = lstIter;
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public Object next() {
      last = iter.next();
      return last;
    }

    public void remove() {
      if (last == null) {
        throw new IllegalStateException("Must call next() before remove().");
      } else {
        iter.remove();
        HashMap.this.remove(((Entry) last).getKey());
      }
    }
  }

  /**
   * Keys are stored in the first position of each pair within the JavaScript
   * dictionary. This constant encodes that fact.
   */
  private static final int KEYS_POS = 0;

  /**
   * Values are stored in the second position of each pair within the JavaScript
   * dictionary. This constant encodes that fact.
   */
  private static final int VALUES_POS = 1;

  /**
   * This constant is used when new mapping entries should be generated from the
   * JavaScript item.
   */
  private static final int BOTH_POS = 2;

  protected static Map.Entry createEntry(Object key, Object value) {
    return new EntryImpl(key, value);
  }

  // Used by JSNI for key processing, not private to avoid eclipse "unused
  // method" warning.
  static String asString(Object o) {
    // All keys must either be entirely numeric, or end with 'S' or be
    // the sentinel value "null".

    if (o instanceof String) {
      // Mark all string keys so they do not conflict with numeric ones.
      return ((String) o) + "S";
    } else if (o == null) {
      return "null";
    } else {
      return null;
    }
  }

  /**
   * Finds the <code>JavaScriptObject</code> associated with the given
   * non-string key.  Used in JSNI.
   */
  private static native JavaScriptObject findNode(HashMap me, Object key) /*-{
    var KEYS_POS = 0;
    var map = me.@java.util.HashMap::map;
    var k = key.@java.lang.Object::hashCode()();
    var candidates = map[k];
    if (candidates != null) {
      for (var index in candidates) {
        var candidate = candidates[index];
        if (candidate[KEYS_POS].@java.lang.Object::equals(Ljava/lang/Object;)(key)) {
          return [k, index];
        }
      }
    }
    return null;
  }-*/;

  /**
   * Underlying JavaScript map.
   */
  private transient JavaScriptObject map;

  private int currentSize = 0;

  public HashMap() {
    init();
  }

  public HashMap(HashMap toBeCopied) {
    this();
    this.putAll(toBeCopied);
  }

  public HashMap(int ignored) {
    // This implementation of HashMap has no need of initial capacities.
    this(ignored, 0);
  }

  public HashMap(int ignored, float alsoIgnored) {
    this();
    // This implementation of HashMap has no need of load factors or capacities.
    if (ignored < 0 || alsoIgnored < 0) {
      throw new IllegalArgumentException(
          "initial capacity was negative or load factor was non-positive");
    }
  }

  public void clear() {
    init();
    currentSize = 0;
  }

  public Object clone() {
    return new HashMap(this);
  }

  public native boolean containsKey(Object key) /*-{
    var k = @java.util.HashMap::asString(Ljava/lang/Object;)(key);
    if (k == null) {
      var location = @java.util.HashMap::findNode(Ljava/util/HashMap;Ljava/lang/Object;)(this, key);
      return (location != null);
    } else {
      return this.@java.util.HashMap::map[k] !== undefined;
    }
  }-*/;

  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  public Set entrySet() {
    return new AbstractSet() {
      public boolean contains(Object entryObj) {
        Entry entry = (Entry) entryObj;
        if (entry != null) {
          Object key = entry.getKey();
          Object value = entry.getValue();
          // If the value is null, we only want to return true if the found
          // value equals null AND the HashMap
          // contains the given key.
          if (value != null || HashMap.this.containsKey(key)) {
            Object foundValue = HashMap.this.get(key);
            if (value == null) {
              return foundValue == null;
            } else {
              return value.equals(foundValue);
            }
          }
        }
        return false;
      }

      public Iterator iterator() {
        return new EntrySetImplIterator();
      }

      public boolean remove(Object entry) {
        if (contains(entry)) {
          Object key = ((Entry) entry).getKey();
          HashMap.this.remove(key);
          return true;
        } else {
          return false;
        }
      }

      public int size() {
        return HashMap.this.size();
      }
    };
  }

  public native Object get(Object key) /*-{
    var KEYS_POS = 0;
    var VALUES_POS = 1;
    var k = @java.util.HashMap::asString(Ljava/lang/Object;)(key);
    if (k != null) {
      var current = this.@java.util.HashMap::map[k];
      if (current === undefined) {
        return null;
      } else { 
        return current;
      }
    } else {    
      k = key.@java.lang.Object::hashCode()();
    }
    var candidates = this.@java.util.HashMap::map[k];
    if (candidates == null) { 
      return null;
    }  
  
    // Used because candidates may develop holes as deletions are performed.
    for (var i in candidates) {
      if (candidates[i][KEYS_POS].@java.lang.Object::equals(Ljava/lang/Object;)(key)) {
        return candidates[i][VALUES_POS];
      }
    }
    return null;    
  }-*/;

  public boolean isEmpty() {
    return size() == 0;
  }

  public native Object put(Object key, Object value) /*-{
    var KEYS_POS = 0;
    var VALUES_POS = 1;
    var previous = null;
    var k = @java.util.HashMap::asString(Ljava/lang/Object;)(key);
    if (k != null) {
      previous = this.@java.util.HashMap::map[k];
      this.@java.util.HashMap::map[k] = value;
      if (previous === undefined) {
        this.@java.util.HashMap::currentSize++;
        return null;
      } else { 
        return previous;
      }
    } else {
      k = key.@java.lang.Object::hashCode()();
    }
    var candidates = this.@java.util.HashMap::map[k];
    if (candidates == null) { 
      candidates = [];
      this.@java.util.HashMap::map[k] = candidates;
    }  
  
    // Used because candidates may develop holes as deletions are performed.
    for (var i in candidates) {
      if (candidates[i][KEYS_POS].@java.lang.Object::equals(Ljava/lang/Object;)(key)) {
        previous = candidates[i][VALUES_POS];
        candidates[i] = [key, value]; 
        return previous;
      }
    }
    this.@java.util.HashMap::currentSize++;
    candidates[candidates.length] = [key,value];
    return null;    
  }-*/;

  public void putAll(Map otherMap) {
    Iterator iter = otherMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry entry = (Entry) iter.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  public native Object remove(Object key) /*-{
    var VALUES_POS = 1;
    var map = this.@java.util.HashMap::map;
    var k = @java.util.HashMap::asString(Ljava/lang/Object;)(key);
    var previous = null;
    if (k != null) {
      previous = map[k];
      delete map[k];
      if (previous !== undefined) {
        this.@java.util.HashMap::currentSize--;
        return previous;
      } else {
        return null;
      }
    }
    var location = @java.util.HashMap::findNode(Ljava/util/HashMap;Ljava/lang/Object;)(this, key);
    if (location == null) {
      return null;
    }
    this.@java.util.HashMap::currentSize--;
    var hashCode = location[0];
    var index = location[1];
    var previous = map[hashCode][index][VALUES_POS];
    map[hashCode].splice(index,1);
    if (map[hashCode].length > 0) {
      // Not the only cell for this hashCode, so no deletion.
      return previous;
    }
    delete map[hashCode];
    return previous;
  }-*/;

  public int size() {
    return currentSize;
  }

  public Collection values() {
    List values = new Vector();
    addAllValuesFromJavascriptObject(values, map);
    return values;
  }

  // Package protected to remove eclipse warning marker.
  void addAllKeysFromJavascriptObject(Collection source,
      JavaScriptObject javaScriptObject) {
    addAllFromJavascriptObject(source, javaScriptObject, KEYS_POS);
  }

  /**
   * Adds all keys, values, or entries to the collection depending upon
   * typeToAdd.
   */
  private native void addAllFromJavascriptObject(Collection source,
      JavaScriptObject javaScriptObject, int typeToAdd)
  /*-{
    var KEYS_POS = 0;
    var VALUES_POS = 1;
    var BOTH_POS = 2;
    var map = this.@java.util.HashMap::map;
    for (var hashCode in javaScriptObject) {
      var entry = null;
      if (hashCode == "null" || hashCode.charAt(hashCode.length - 1) == 'S') {
        var key = null;
        if (typeToAdd != VALUES_POS && hashCode != "null") {
          key = hashCode.substring(0, hashCode.length - 1);
        }
         if (typeToAdd == KEYS_POS) {
          entry = key
        } else if (typeToAdd == VALUES_POS) {
          entry = map[hashCode];
        } else if (typeToAdd == BOTH_POS) {
          entry = 
            @java.util.HashMap::createEntry(Ljava/lang/Object;Ljava/lang/Object;)(
            key, map[hashCode]);
        } 
        source.@java.util.Collection::add(Ljava/lang/Object;)(entry);
      } else {
        var candidates = map[hashCode];
        for (var index in candidates) { 
          if (typeToAdd != BOTH_POS) {
            entry = candidates[index][typeToAdd];
          } else {
            entry = 
              @java.util.HashMap::createEntry(Ljava/lang/Object;Ljava/lang/Object;)(
              candidates[index][0], candidates[index][1]);
          }
          source.@java.util.Collection::add(Ljava/lang/Object;)(entry);
        }
      }
    }
  }-*/;

  private void addAllValuesFromJavascriptObject(Collection source,
      JavaScriptObject javaScriptObject) {
    addAllFromJavascriptObject(source, javaScriptObject, VALUES_POS);
  }

  private native void init() /*-{
    this.@java.util.HashMap::map = [];
  }-*/;

}
