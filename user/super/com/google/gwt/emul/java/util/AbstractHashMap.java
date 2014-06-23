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

import com.google.gwt.core.client.GWT;
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

  /**
   * A map of integral hashCodes onto entries.
   */
  private transient InternalJsHashcodeMap<K, V> hashCodeMap;

  /**
   * A map of Strings onto values.
   */
  private transient InternalJsStringMap<V> stringMap;

  private int size;

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

  @SpecializeMethod(params = {String.class}, target = "hasStringValue")
  @Override
  public boolean containsKey(Object key) {
    return !(key instanceof String) ? hasHashValue(key) : hasStringValue((String) key);
  }

  @Override
  public boolean containsValue(Object value) {
    return containsStringValue(value) || containsHashValue(value);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @SpecializeMethod(params = {String.class}, target = "getStringValue")
  @Override
  public V get(Object key) {
    return !(key instanceof String) ? getHashValue(key) : getStringValue((String) key);
  }

  @SpecializeMethod(params = {String.class, Object.class}, target = "putStringValue")
  @Override
  public V put(K key, V value) {
    return !(key instanceof String)
        ? putHashValue(key, value)
        : putStringValue((String) key, value);
  }

  @SpecializeMethod(params = {String.class}, target = "removeStringValue")
  @Override
  public V remove(Object key) {
    return !(key instanceof String) ? removeHashValue(key) : removeStringValue((String) key);
  }

  @Override
  public int size() {
    return size + stringMap.size();
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

  /**
   * Returns hash code of the key as calculated by {@link #getHashCode(Object)} but also handles
   * null keys as well.
   */
  private int hash(Object key) {
    return key == null ? 0 : getHashCode(key);
  }

  private void addAllHashEntries(Collection<?> dest) {
    hashCodeMap.addAllEntries(dest);
  }

  private void addAllStringEntries(Collection<?> dest) {
    stringMap.addAllEntries(dest);
  }

  private void clearImpl() {
    hashCodeMap = GWT.create(InternalJsHashcodeMap.class);
    stringMap = GWT.create(InternalJsStringMap.class);
    size = 0;
  }

  /**
   * Returns true if hashCodeMap contains any Map.Entry whose value is Object
   * equal to <code>value</code>.
   */
  private boolean containsHashValue(Object value) {
    return hashCodeMap.containsValue(value, this);
  }

  /**
   * Returns true if stringMap contains any key whose value is Object equal to
   * <code>value</code>.
   */
  private boolean containsStringValue(Object value) {
    return stringMap.containsValue(value, this);
  }

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
  private V getHashValue(Object key) {
    Entry<K, V> entry = hashCodeMap.getEntry(key, hash(key), this);
    return entry == null ? null : entry.getValue();
  }

  /**
   * Returns the value for the given key in the stringMap. Returns
   * <code>null</code> if the specified key does not exist.
   */
  protected V getStringValue(String key) {
    return key == null ? getHashValue(null) : stringMap.get(key);
  }

  /**
   * Returns true if the a key exists in the hashCodeMap that is Object equal to
   * <code>key</code>, provided that <code>key</code>'s hash code is
   * <code>hashCode</code>.
   */
  private boolean hasHashValue(Object key) {
    return hashCodeMap.getEntry(key, hash(key), this) != null;
  }

  /**
   * Returns true if the given key exists in the stringMap.
   */
  protected boolean hasStringValue(String key) {
    return key == null ? hasHashValue(null) : stringMap.contains(key);
  }

  /**
   * Sets the specified key to the specified value in the hashCodeMap. Returns
   * the value previously at that key. Returns <code>null</code> if the
   * specified key did not exist.
   */
  private V putHashValue(K key, V value) {
    return hashCodeMap.put(key, value, hash(key), this);
  }

  /**
   * Sets the specified key to the specified value in the stringMap. Returns the
   * value previously at that key. Returns <code>null</code> if the specified
   * key did not exist.
   */
  protected V putStringValue(String key, V value) {
    return key == null ? putHashValue(null, value) : stringMap.put(key, value);
  }

  /**
   * Removes the pair whose key is Object equal to <code>key</code> from
   * <code>hashCodeMap</code>, provided that <code>key</code>'s hash code
   * is <code>hashCode</code>. Returns the value that was associated with the
   * removed key, or null if no such key existed.
   */
  private V removeHashValue(Object key) {
    return hashCodeMap.remove(key, hash(key), this);
  }

  /**
   * Removes the specified key from the stringMap and returns the value that was
   * previously there. Returns <code>null</code> if the specified key does not
   * exist.
   */
  protected V removeStringValue(String key) {
    return key == null ? removeHashValue(null) : stringMap.remove(key);
  }
}
