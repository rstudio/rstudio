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

/**
 * A {@link java.util.Map} of {@link Enum}s. <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/EnumMap.html">[Sun
 * docs]</a>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> { 

  K[] allEnums;

  EnumSet<K> keySet;

  ArrayList<V> values;

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
      if (m.isEmpty()) {
        throw new IllegalArgumentException("The map must not be empty.");
      }
      init(m.keySet().iterator().next().getDeclaringClass());
      putAll(m);
    }
  }

  @Override
  public void clear() {
    keySet.clear();
    Collections.fill(values, null);
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
    if (value != null) {
      return values.contains(value);
    }

    for (int i = 0, n = values.size(); i < n; ++i) {
      V v = values.get(i);
      if (v == null && keySet.contains(allEnums[i])) {
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
  public boolean equals(Object o) {
    if (!(o instanceof EnumMap)) {
      return super.equals(o);
    }

    EnumMap enumMap = (EnumMap) o;
    return keySet.equals(enumMap.keySet()) && values.equals(enumMap.values());
  }

  @Override
  public V get(Object k) {
    return keySet.contains(k) ? values.get(asKey(k).ordinal()) : null;
  }

  @Override
  public V put(K key, V value) {
    keySet.add(key);
    return values.set(key.ordinal(), value);
  }

  @Override
  public V remove(Object key) {
    return keySet.remove(key) ? values.set(asKey(key).ordinal(), null) : null;
  }

  @Override
  public int size() {
    return keySet.size();
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {

    public Iterator<Map.Entry<K, V>> iterator() {
      return new Iterator<Entry<K, V>>() {
        Iterator<K> it = keySet.iterator();

        K key;

        public boolean hasNext() {
          return it.hasNext();
        }

        public Entry<K, V> next() {
          key = it.next();
          return new MapEntryImpl<K, V>(key, values.get(key.ordinal()));
        }

        public void remove() {
          if (key == null) {
            throw new IllegalStateException("No current value");
          }
          EnumMap.this.remove(key);
          key = null;
        }
      };
    }

    public int size() {
      return keySet.size();
    }
  }

  /**
   * Returns <code>key</code> as <code>K</code>. Doesn't actually perform any
   * runtime checks. Should only be called when you are sure <code>key</code> is
   * of type <code>K</code>.
   */
  @SuppressWarnings("unchecked")
  private K asKey(Object key) {
    return (K) key;
  }

  private void init(Class<K> type) {
    allEnums = type.getEnumConstants();
    keySet = EnumSet.noneOf(type);
    int length = allEnums.length;
    values = new ArrayList<V>(length);
    for (int i = 0; i < length; ++i) {
      values.add(null);
    }
  }

  private void init(EnumMap<K, ? extends V> m) {
    allEnums = m.allEnums;
    keySet = m.keySet.clone();
    values = new ArrayList<V>(m.values);
  }
}
