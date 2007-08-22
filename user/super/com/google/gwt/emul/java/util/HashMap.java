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
package java.util;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Implementation of Map interface based on a hash table.
 * 
 * @link http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class HashMap<K, V> extends AbstractMap<K, V> {
  /*
   * Implementation notes:
   * 
   * String keys are stored in a separate map from non-String keys. String keys
   * are mapped to their values via a JS associative map, stringMap. String keys
   * could collide with intrinsic properties (like watch, constructor) so we
   * prepend each key with a ':' inside of stringMap.
   * 
   * Integer keys are used to index all non-string keys. A key's hashCode is the
   * index in hashCodeMap which should contain that key. Since several keys may
   * have the same hash, each value in hashCodeMap is actually an array
   * containing all entries whose keys share the same hash.
   */

  /**
   * Implementation of <code>HashMap</code> entry.
   */
  private static class EntryImpl<K, V> implements Map.Entry<K, V> {

    /**
     * Helper method for constructing Map.Entry objects from JSNI code.
     */
    static <K, V> Map.Entry<K, V> create(K key, V value) {
      return new EntryImpl<K, V>(key, value);
    }

    private K key;

    private V value;

    /**
     * Constructor for <code>EntryImpl</code>.
     */
    public EntryImpl(K key, V value) {
      this.key = key;
      this.value = value;
    }

    public boolean equals(Object other) {
      if (other instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
        if (equalsWithNullCheck(key, entry.getKey())
            && equalsWithNullCheck(value, entry.getValue())) {
          return true;
        }
      }
      return false;
    }

    public K getKey() {
      return key;
    }

    public V getValue() {
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

    public V setValue(V object) {
      V old = value;
      value = object;
      return old;
    }

    public String toString() {
      return getKey() + "=" + getValue();
    }
  }

  private final class EntrySet extends AbstractSet<Entry<K, V>> {

    public void clear() {
      HashMap.this.clear();
    }

    public boolean contains(Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        Object key = entry.getKey();
        if (HashMap.this.containsKey(key)) {
          Object value = HashMap.this.get(key);
          return equalsWithNullCheck(entry.getValue(), value);
        }
      }
      return false;
    }

    public Iterator<Entry<K, V>> iterator() {
      return new EntrySetIterator();
    }

    public boolean remove(Object entry) {
      if (contains(entry)) {
        Object key = ((Map.Entry<?, ?>) entry).getKey();
        HashMap.this.remove(key);
        return true;
      }
      return false;
    }

    public int size() {
      return HashMap.this.size();
    }
  }

  /**
   * Iterator for <code>EntrySetImpl</code>.
   */
  private final class EntrySetIterator implements Iterator<Entry<K, V>> {
    private final Iterator<Map.Entry<K, V>> iter;
    private Map.Entry<K, V> last = null;

    /**
     * Constructor for <code>EntrySetIterator</code>.
     */
    public EntrySetIterator() {
      List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>();
      if (nullSlot != UNDEFINED) {
        EntryImpl<K, V> entryImpl = new EntryImpl<K, V>(null, nullSlot);
        list.add(entryImpl);
      }
      addAllStringEntries(stringMap, list);
      addAllHashEntries(hashCodeMap, list);
      this.iter = list.iterator();
    }

    public boolean hasNext() {
      return iter.hasNext();
    }

    public Map.Entry<K, V> next() {
      return last = iter.next();
    }

    public void remove() {
      if (last == null) {
        throw new IllegalStateException("Must call next() before remove().");
      } else {
        iter.remove();
        HashMap.this.remove(last.getKey());
        last = null;
      }
    }
  }

  /**
   * Contains JS <code>undefined</code>. This is technically a violation of
   * the JSNI contract, so we have to be very careful with this.
   */
  private static final Object UNDEFINED = createUndefinedValue();

  private static native void addAllHashEntries(JavaScriptObject hashCodeMap,
      Collection<?> dest) /*-{
    for (var hashCode in hashCodeMap) {
      // sanity check that it's really an integer
      if (hashCode == parseInt(hashCode)) {
        var array = hashCodeMap[hashCode];
        for (var i = 0, c = array.length; i < c; ++i) {
          dest.@java.util.Collection::add(Ljava/lang/Object;)(array[i]);
        }
      }
    }
  }-*/;
  
  private static native void addAllStringEntries(JavaScriptObject stringMap,
      Collection<?> dest) /*-{
    for (var key in stringMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var value = stringMap[key];
        var entry = @java.util.HashMap$EntryImpl::create(Ljava/lang/Object;Ljava/lang/Object;)(key.substring(1), value);
        dest.@java.util.Collection::add(Ljava/lang/Object;)(entry);
      }
    }
  }-*/;

  /**
   * Returns true if hashCodeMap contains any Map.Entry whose value is Object
   * equal to <code>value</code>.
   */
  private static native boolean containsHashValue(JavaScriptObject hashCodeMap, Object value) /*-{
    for (var hashCode in hashCodeMap) {
      // sanity check that it's really one of ours
      if (hashCode == parseInt(hashCode)) {
        var array = hashCodeMap[hashCode];
        for (var i = 0, c = array.length; i < c; ++i) {
          var entry = array[i];
          var entryValue = entry.@java.util.Map$Entry::getValue()();
          if (@java.util.HashMap::equalsWithNullCheck(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
            return true;
          }
        }
      }
    }
    return false;
  }-*/;
  
  /**
   * Returns true if stringMap contains any key whose value is Object equal to
   * <code>value</code>.
   */
  private static native boolean containsStringValue(JavaScriptObject stringMap, Object value) /*-{
    for (var key in stringMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entryValue = stringMap[key];
        if (@java.util.HashMap::equalsWithNullCheck(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
          return true;
        }
      }
    }
    return false;
  }-*/;

  /**
   * Returns <code>undefined</code>. This is technically a violation of the
   * JSNI contract, so we have to be very careful how we use the result.
   */
  private static native JavaScriptObject createUndefinedValue() /*-{
    // intentionally return undefined
  }-*/;

  /**
   * Checks for equality of Objects, null-sensitive.
   */
  private static boolean equalsWithNullCheck(Object a, Object b) {
    if (a == b) {
      return true;
    } else if (a == null) {
      return false;
    } else {
      return a.equals(b);
    }
  }

  /**
   * Returns the Map.Entry whose key is equal to <code>key</code>, provided
   * that <code>key</code>'s hash code is Object equal to
   * <code>hashCode</code>; or <code>null</code> if no such Map.Entry
   * exists at the specified hashCode.
   */
  private static native <K,V> V getHashValue(JavaScriptObject hashCodeMap,
      K key, int hashCode) /*-{
    var array = hashCodeMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (@java.util.HashMap::equalsWithNullCheck(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          return entry.@java.util.Map$Entry::getValue()();
        }
      }
    }
    // intentionally return undefined
  }-*/;

  /**
   * Returns the value for the given key in the stringMap. Returns
   * <code>undefined</code> if the specified key does not exist.
   */
  private static native <V> V getStringValue(JavaScriptObject stringMap,
      String key) /*-{
    return stringMap[':' + key];
  }-*/;

  /**
   * Sets the specified key to the specified value in the hashCodeMap. Returns
   * the value previously at that key. Returns <code>undefined</code> if the
   * specified key did not exist.
   */
  private static native <K,V> V putHashValue(JavaScriptObject hashCodeMap,
      K key, V value, int hashCode) /*-{
    var array = hashCodeMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (@java.util.HashMap::equalsWithNullCheck(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          // Found an exact match, just update the existing entry
          var previous = entry.@java.util.Map$Entry::getValue()();
          entry.@java.util.Map$Entry::setValue(Ljava/lang/Object;)(value);
          return previous;
        }
      }
    } else {
      array = hashCodeMap[hashCode] = [];
    }
    var entry = @java.util.HashMap$EntryImpl::create(Ljava/lang/Object;Ljava/lang/Object;)(key, value);
    array.push(entry);
    // intentionally return undefined
  }-*/;

  /**
   * Sets the specified key to the specified value in the stringMap. Returns the
   * value previously at that key. Returns <code>undefined</code> if the
   * specified key did not exist.
   */
  private static native <V> V putStringValue(JavaScriptObject stringMap,
      String key, V value) /*-{
    key = ':' + key;
    var result = stringMap[key] 
    stringMap[key] = value;
    return result;
  }-*/;

  /**
   * Removes the pair whose key is equal to <code>key</code> from
   * <code>hashCodeMap</code>, provided that <code>key</code>'s hash code
   * is <code>hashCode</code>. Returns the value that was associated with the
   * removed key, or undefined if no such key existed.
   * 
   * @param <K> key type
   * @param <V> value type
   */
  private static native <K,V> V removeHashValue(JavaScriptObject hashCodeMap,
      K key, int hashCode) /*-{
    var array = hashCodeMap[hashCode];
    if (array) {
      for (var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (@java.util.HashMap::equalsWithNullCheck(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          if (array.length == 1) {
            // remove the whole array
            delete hashCodeMap[hashCode];
          } else {
            // splice out the entry we're removing
            array.splice(i, 1);
          }
          return entry.@java.util.Map$Entry::getValue()();
        }
      }
    }
    // intentionally return undefined
  }-*/;
  
  /**
   * Removes the specified key from the stringMap and returns the value that was
   * previously there. Returns <code>undefined</code> if the specified key
   * does not exist.
   */
  private static native <V> V removeStringValue(JavaScriptObject stringMap,
      String key) /*-{
    key = ':' + key;
    var result = stringMap[key];
    delete stringMap[key];
    return result;
  }-*/;

  /**
   * A map of integral hashCodes onto entries.
   */
  private transient JavaScriptObject hashCodeMap;

  /**
   * This is the slot that holds the value associated with the "null" key.
   * 
   * TODO(jat): reconsider this implementation -- this can hold values that
   * aren't of type V, such as UNDEFINED (which is an Object). We should
   * reimplement this in a more typesafe manner.
   */
  private transient V nullSlot;

  private int size;

  /**
   * A map of Strings onto values.
   */
  private transient JavaScriptObject stringMap;

  {
    clearImpl();
  }

  public HashMap() {
  }

  public HashMap(int ignored) {
    // This implementation of HashMap has no need of initial capacities.
    this(ignored, 0);
  }

  public HashMap(int ignored, float alsoIgnored) {
    // This implementation of HashMap has no need of load factors or capacities.
    if (ignored < 0 || alsoIgnored < 0) {
      throw new IllegalArgumentException(
          "initial capacity was negative or load factor was non-positive");
    }
  }

  public HashMap(Map<? extends K, ? extends V> toBeCopied) {
    this.putAll(toBeCopied);
  }

  public void clear() {
    clearImpl();
  }

  public Object clone() {
    return new HashMap<K, V>(this);
  }

  public boolean containsKey(Object key) {
    if (key instanceof String) {
      return getStringValue(stringMap, (String) key) != UNDEFINED;
    } else if (key == null) {
      return nullSlot != UNDEFINED;
    } else {
      return getHashValue(hashCodeMap, key, key.hashCode()) != UNDEFINED;
    }
  }

  public boolean containsValue(Object value) {
    if (nullSlot != UNDEFINED && equalsWithNullCheck(nullSlot, value)) {
      return true;
    } else if (containsStringValue(stringMap, value)) {
      return true;
    } else if (containsHashValue(hashCodeMap, value)) {
      return true;
    }
    return false;
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  public V get(Object key) {
    V result;
    if (key instanceof String) {
      result = getStringValue(stringMap, (String) key);
    } else if (key == null) {
      result = nullSlot;
    } else {
      result = getHashValue(hashCodeMap, key, key.hashCode());
    }
    return (result == UNDEFINED) ? null : result;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public V put(K key, V value) {
    V previous;
    if (key instanceof String) {
      previous = putStringValue(stringMap, (String) key, value);
    } else if (key == null) {
      previous = nullSlot;
      nullSlot = value;
    } else {
      previous = putHashValue(hashCodeMap, key, value, key.hashCode());
    }
    if (previous == UNDEFINED) {
      ++size;
      return null;
    } else {
      return previous;
    }
  }

  public void putAll(Map<? extends K, ? extends V> otherMap) {
    Iterator<? extends Map.Entry<? extends K, ? extends V>> iter = otherMap.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<? extends K, ? extends V> entry = iter.next();
      put(entry.getKey(), entry.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    V previous;
    if (key instanceof String) {
      previous = removeStringValue(stringMap, (String) key);
    } else if (key == null) {
      previous = nullSlot;
      nullSlot = (V) UNDEFINED; // unchecked warning suppressed
    } else {
      previous = removeHashValue(hashCodeMap, key, key.hashCode());
    }
    if (previous == UNDEFINED) {
      return null;
    } else {
      --size;
      return previous;
    }
  }

  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  private void clearImpl() {
    hashCodeMap = JavaScriptObject.createArray();
    stringMap = JavaScriptObject.createObject();
    nullSlot = (V) UNDEFINED; // unchecked warning suppressed
    size = 0;
  }

}
