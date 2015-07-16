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

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkState;

import javaemul.internal.ArrayHelper;

/**
 * A {@link java.util.Map} of {@link Enum}s. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/EnumMap.html">[Sun
 * docs]</a>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> {

  private final class EntrySet extends AbstractSet<Entry<K, V>> {

    @Override
    public void clear() {
      EnumMap.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (o instanceof Map.Entry) {
        return containsEntry((Map.Entry<?, ?>) o);
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
        EnumMap.this.remove(key);
        return true;
      }
      return false;
    }

    @Override
    public int size() {
      return EnumMap.this.size();
    }
  }

  private final class EntrySetIterator implements Iterator<Entry<K, V>> {
    private Iterator<K> it = keySet.iterator();
    private K key;

    @Override
    public boolean hasNext() {
      return it.hasNext();
    }

    @Override
    public Entry<K, V> next() {
      key = it.next();
      return new MapEntry(key);
    }

    @Override
    public void remove() {
      checkState(key != null);

      EnumMap.this.remove(key);
      key = null;
    }
  }

  private class MapEntry extends AbstractMapEntry<K, V> {

    private final K key;

    public MapEntry(K key) {
      this.key = key;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return values[key.ordinal()];
    }

    @Override
    public V setValue(V value) {
      return set(key.ordinal(), value);
    }
  }

  private EnumSet<K> keySet;

  private V[] values;

  public EnumMap(Class<K> type) {
    init(type);
  }

  public EnumMap(EnumMap<K, ? extends V> m) {
    init(m);
  }

  public EnumMap(Map<K, ? extends V> m) {
    if (m instanceof EnumMap) {
      init((EnumMap<K, ? extends V>) m);
    } else {
      checkArgument(!m.isEmpty(), "Specified map is empty");
      init(m.keySet().iterator().next().getDeclaringClass());
      putAll(m);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    keySet.clear();
    values = (V[]) new Object[values.length];
  }

  public EnumMap<K, V> clone() {
    return new EnumMap<K, V>(this);
  }

  @Override
  public boolean containsKey(Object key) {
    return keySet.contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    for (K key : keySet) {
      if (Objects.equals(value, values[key.ordinal()])) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new EntrySet();
  }

  @Override
  public V get(Object k) {
    return keySet.contains(k) ? values[asOrdinal(k)] : null;
  }

  @Override
  public V put(K key, V value) {
    keySet.add(key);
    return set(key.ordinal(), value);
  }

  @Override
  public V remove(Object key) {
    return keySet.remove(key) ? set(asOrdinal(key), null) : null;
  }

  @Override
  public int size() {
    return keySet.size();
  }

  /**
   * Returns <code>key</code> as <code>K</code>. Only runtime checks that
   * key is an Enum, not that it's the particular Enum K. Should only be called
   * when you are sure <code>key</code> is of type <code>K</code>.
   */
  @SuppressWarnings("unchecked")
  private K asKey(Object key) {
    return (K) key;
  }

  private int asOrdinal(Object key) {
    return asKey(key).ordinal();
  }

  @SuppressWarnings("unchecked")
  private void init(Class<K> type) {
    keySet = EnumSet.noneOf(type);
    values = (V[]) new Object[keySet.capacity()];
  }

  private void init(EnumMap<K, ? extends V> m) {
    keySet = m.keySet.clone();
    values = ArrayHelper.clone(m.values, 0, m.values.length);
  }

  private V set(int ordinal, V value) {
    V was = values[ordinal];
    values[ordinal] = value;
    return was;
  }
}
