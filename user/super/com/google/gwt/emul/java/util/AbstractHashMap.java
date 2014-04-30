/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.core.client.impl.SpecializeMethod;

/**
 * Implementation of Map interface based on a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type
 * @param <V> value type
 */
abstract class AbstractHashMap<K, V> extends AbstractMap<K, V> {
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
  private final class EntrySet extends AbstractSet<Entry<K, V>> {

    @Override
    public void clear() {
      AbstractHashMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (o instanceof Map.Entry) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        Object key = entry.getKey();
        if (AbstractHashMap.this.containsKey(key)) {
          Object value = AbstractHashMap.this.get(key);
          return AbstractHashMap.this.equals(entry.getValue(), value);
        }
      }
      return false;
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntrySetIterator();
    }

    @Override
    public boolean remove(Object entry) {
      if (contains(entry)) {
        Object key = ((Map.Entry<?, ?>) entry).getKey();
        AbstractHashMap.this.remove(key);
        return true;
      }
      return false;
    }

    @Override
    public int size() {
      return AbstractHashMap.this.size();
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
      if (nullSlotLive) {
        list.add(new MapEntryNull());
      }
      addAllStringEntries(list);
      addAllHashEntries(list);
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
        AbstractHashMap.this.remove(last.getKey());
        last = null;
      }
    }
  }

  private final class MapEntryNull extends AbstractMapEntry<K, V> {

    public K getKey() {
      return null;
    }

    public V getValue() {
      return nullSlot;
    }

    public V setValue(V object) {
      return putNullSlot(object);
    }
  }

  // Instantiated from JSNI
  @SuppressWarnings("unused")
  private final class MapEntryString extends AbstractMapEntry<K, V> {

    private final String key;

    public MapEntryString(String key) {
      this.key = key;
    }

    @SuppressWarnings("unchecked")
    public K getKey() {
      return (K) key;
    }

    public V getValue() {
      return getStringValue(key);
    }

    public V setValue(V object) {
      return putStringValue(key, object);
    }
  }

  /**
   * A map of integral hashCodes onto entries.
   */
  // Used from JSNI.
  @SuppressWarnings("unused")
  private transient JavaScriptObject hashCodeMap;

  /**
   * This is the slot that holds the value associated with the "null" key.
   */
  private transient V nullSlot;

  private transient boolean nullSlotLive;

  private int size;

  /**
   * A map of Strings onto values.
   */
  // Used from JSNI.
  @SuppressWarnings("unused")
  private transient JavaScriptObject stringMap;

  {
    clearImpl();
  }

  public AbstractHashMap() {
  }

  public AbstractHashMap(int ignored) {
    // This implementation of HashMap has no need of initial capacities.
    this(ignored, 0);
  }

  public AbstractHashMap(int ignored, float alsoIgnored) {
    // This implementation of HashMap has no need of load factors or capacities.
    if (ignored < 0 || alsoIgnored < 0) {
      throw new IllegalArgumentException(
          "initial capacity was negative or load factor was non-positive");
    }
  }

  public AbstractHashMap(Map<? extends K, ? extends V> toBeCopied) {
    this.putAll(toBeCopied);
  }

  @Override
  public void clear() {
    clearImpl();
  }

  @Override
  public boolean containsKey(Object key) {
    return (key == null) ? nullSlotLive : (!(key instanceof String)
        ? hasHashValue(key, getHashCode(key)) : hasStringValue((String) key));
  }

  @Override
  public boolean containsValue(Object value) {
    if (nullSlotLive && equals(nullSlot, value)) {
      return true;
    } else if (containsStringValue(value)) {
      return true;
    } else if (containsHashValue(value)) {
      return true;
    }
    return false;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @SpecializeMethod(params = {String.class},
    target = "getStringValue")
  @Override
  public V get(Object key) {
    return (key == null) ? nullSlot : (!(key instanceof String) ? getHashValue(
        key, getHashCode(key)) : getStringValue((String) key));
  }

  @SpecializeMethod(params = {String.class, Object.class},
                    target = "putStringValue")
  @Override
  public V put(K key, V value) {
    return (key == null) ? putNullSlot(value) : (!(key instanceof String)
        ? putHashValue(key, value, getHashCode(key)) : putStringValue(
            (String) key, value));
  }

  @SpecializeMethod(params = {String.class}, target = "removeStringValue")
  @Override
  public V remove(Object key) {
    return (key == null) ? removeNullSlot() : (!(key instanceof String)
        ? removeHashValue(key, getHashCode(key))
        : removeStringValue((String) key));
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * Subclasses must override to return a whether or not two keys or values are
   * equal.
   */
  abstract boolean equals(Object value1, Object value2);

  /**
   * Subclasses must override to return a hash code for a given key. The key is
   * guaranteed to be non-null and not a String.
   */
  abstract int getHashCode(Object key);

  private native void addAllHashEntries(Collection<?> dest) /*-{
    var hashCodeMap = this.@java.util.AbstractHashMap::hashCodeMap;
    for ( var hashCode in hashCodeMap) {
      // sanity check that it's really an integer
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = hashCodeMap[hashCodeInt];
        for ( var i = 0, c = array.length; i < c; ++i) {
          dest.@java.util.Collection::add(Ljava/lang/Object;)(array[i]);
        }
      }
    }
  }-*/;

  private native void addAllStringEntries(Collection<?> dest) /*-{
    var stringMap = this.@java.util.AbstractHashMap::stringMap;
    for (var key in stringMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entry = @java.util.AbstractHashMap$MapEntryString::new(Ljava/util/AbstractHashMap;Ljava/lang/String;)(this, key.substring(1));
        dest.@java.util.Collection::add(Ljava/lang/Object;)(entry);
      }
    }
  }-*/;

  private void clearImpl() {
    hashCodeMap = JavaScriptObject.createArray();
    stringMap = JavaScriptObject.createObject();
    nullSlotLive = false;
    nullSlot = null;
    size = 0;
  }

  /**
   * Returns true if hashCodeMap contains any Map.Entry whose value is Object
   * equal to <code>value</code>.
   */
  private native boolean containsHashValue(Object value) /*-{
    var hashCodeMap = this.@java.util.AbstractHashMap::hashCodeMap;
    for ( var hashCode in hashCodeMap) {
      // sanity check that it's really one of ours
      var hashCodeInt = parseInt(hashCode, 10);
      if (hashCode == hashCodeInt) {
        var array = hashCodeMap[hashCodeInt];
        for ( var i = 0, c = array.length; i < c; ++i) {
          var entry = array[i];
          var entryValue = entry.@java.util.Map$Entry::getValue()();
          if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
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
  private native boolean containsStringValue(Object value) /*-{
    var stringMap = this.@java.util.AbstractHashMap::stringMap;
    for ( var key in stringMap) {
      // only keys that start with a colon ':' count
      if (key.charCodeAt(0) == 58) {
        var entryValue = stringMap[key];
        if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(value, entryValue)) {
          return true;
        }
      }
    }
    return false;
  }-*/;

  /**
   * Bridge method from JSNI that keeps us from having to make polymorphic calls
   * in JSNI. By putting the polymorphism in Java code, the compiler can do a
   * better job of optimizing in most cases.
   */
  @SuppressWarnings("unused")
  private boolean equalsBridge(Object value1, Object value2) {
    return equals(value1, value2);
  }

  /**
   * Returns the Map.Entry whose key is Object equal to <code>key</code>,
   * provided that <code>key</code>'s hash code is <code>hashCode</code>;
   * or <code>null</code> if no such Map.Entry exists at the specified
   * hashCode.
   */
  private native V getHashValue(Object key, int hashCode) /*-{
    var array = this.@java.util.AbstractHashMap::hashCodeMap[hashCode];
    if (array) {
      for ( var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          return entry.@java.util.Map$Entry::getValue()();
        }
      }
    }
    return null;
  }-*/;

  /**
   * Returns the value for the given key in the stringMap. Returns
   * <code>null</code> if the specified key does not exist.
   */
  protected native V getStringValue(String key) /*-{
    if (key == null) {
      return this.@java.util.AbstractHashMap::nullSlot;
    }
    return this.@java.util.AbstractHashMap::stringMap[':' + key];
  }-*/;

  /**
   * Returns true if the a key exists in the hashCodeMap that is Object equal to
   * <code>key</code>, provided that <code>key</code>'s hash code is
   * <code>hashCode</code>.
   */
  private native boolean hasHashValue(Object key, int hashCode) /*-{
    var array = this.@java.util.AbstractHashMap::hashCodeMap[hashCode];
    if (array) {
      for ( var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          return true;
        }
      }
    }
    return false;
  }-*/;

  /**
   * Returns true if the given key exists in the stringMap.
   */
  private native boolean hasStringValue(String key) /*-{
    return (':' + key) in this.@java.util.AbstractHashMap::stringMap;
  }-*/;

  /**
   * Sets the specified key to the specified value in the hashCodeMap. Returns
   * the value previously at that key. Returns <code>null</code> if the
   * specified key did not exist.
   */
  private native V putHashValue(K key, V value, int hashCode) /*-{
    var array = this.@java.util.AbstractHashMap::hashCodeMap[hashCode];
    if (array) {
      for ( var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map.Entry::getKey()();
        if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          // Found an exact match, just update the existing entry
          return entry.@java.util.Map.Entry::setValue(Ljava/lang/Object;)(value);
        }
      }
    } else {
      array = this.@java.util.AbstractHashMap::hashCodeMap[hashCode] = [];
    }
    var entry = @java.util.AbstractMap.SimpleEntry::new(Ljava/lang/Object;Ljava/lang/Object;)(key, value);
    array.push(entry);
    ++this.@java.util.AbstractHashMap::size;
    return null;
  }-*/;

  private V putNullSlot(V value) {
    V result = nullSlot;
    nullSlot = value;
    if (!nullSlotLive) {
      nullSlotLive = true;
      ++size;
    }
    return result;
  }

  /**
   * Sets the specified key to the specified value in the stringMap. Returns the
   * value previously at that key. Returns <code>null</code> if the specified
   * key did not exist.
   */
  protected native V putStringValue(String key, V value) /*-{
    if (key == null) {
      return this.@java.util.AbstractHashMap::putNullSlot(Ljava/lang/Object;)(value);
    }
    var result, stringMap = this.@java.util.AbstractHashMap::stringMap;
    key = ':' + key;
    if (key in stringMap) {
      result = stringMap[key];
    } else {
      ++this.@java.util.AbstractHashMap::size;
    }
    stringMap[key] = value;
    return result;
  }-*/;

  /**
   * Removes the pair whose key is Object equal to <code>key</code> from
   * <code>hashCodeMap</code>, provided that <code>key</code>'s hash code
   * is <code>hashCode</code>. Returns the value that was associated with the
   * removed key, or null if no such key existed.
   */
  private native V removeHashValue(Object key, int hashCode) /*-{
    var array = this.@java.util.AbstractHashMap::hashCodeMap[hashCode];
    if (array) {
      for ( var i = 0, c = array.length; i < c; ++i) {
        var entry = array[i];
        var entryKey = entry.@java.util.Map$Entry::getKey()();
        if (this.@java.util.AbstractHashMap::equalsBridge(Ljava/lang/Object;Ljava/lang/Object;)(key, entryKey)) {
          if (array.length == 1) {
            // remove the whole array
            delete this.@java.util.AbstractHashMap::hashCodeMap[hashCode];
          } else {
            // splice out the entry we're removing
            array.splice(i, 1);
          }
          --this.@java.util.AbstractHashMap::size;
          return entry.@java.util.Map$Entry::getValue()();
        }
      }
    }
    return null;
  }-*/;

  private V removeNullSlot() {
    V result = nullSlot;
    nullSlot = null;
    if (nullSlotLive) {
      nullSlotLive = false;
      --size;
    }
    return result;
  }

  /**
   * Removes the specified key from the stringMap and returns the value that was
   * previously there. Returns <code>null</code> if the specified key does not
   * exist.
   */
  protected native V removeStringValue(String key) /*-{
    if (key == null) {
      return this.@java.util.AbstractHashMap::removeNullSlot()();
    }

    var result, stringMap = this.@java.util.AbstractHashMap::stringMap;
    key = ':' + key;
    if (key in stringMap) {
      result = stringMap[key];
      --this.@java.util.AbstractHashMap::size;
      delete stringMap[key];
    }
    return result;
  }-*/;
}
