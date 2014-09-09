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
 * Skeletal implementation of the Map interface. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/AbstractMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> the key type.
 * @param <V> the value type.
 */
public abstract class AbstractMap<K, V> implements Map<K, V> {

  /**
   * A mutable {@link Map.Entry} shared by several {@link Map} implementations.
   */
  public static class SimpleEntry<K, V> extends AbstractEntry<K, V> {
    public SimpleEntry(K key, V value) {
      super(key, value);
    }

    public SimpleEntry(Entry<? extends K, ? extends V> entry) {
      super(entry.getKey(), entry.getValue());
    }
  }

  /**
   * An immutable {@link Map.Entry} shared by several {@link Map} implementations.
   */
  public static class SimpleImmutableEntry<K, V> extends AbstractEntry<K, V> {
    public SimpleImmutableEntry(K key, V value) {
      super(key, value);
    }

    public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
      super(entry.getKey(), entry.getValue());
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Basic {@link Map.Entry} implementation used by {@link SimpleEntry}
   * and {@link SimpleImmutableEntry}.
   */
  private abstract static class AbstractEntry<K, V> implements Entry<K, V> {
    private final K key;
    private V value;

    protected AbstractEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Entry)) {
        return false;
      }
      Entry<?, ?> entry = (Entry<?, ?>) other;
      return Objects.equals(key, entry.getKey())
          && Objects.equals(value, entry.getValue());
    }

    /**
     * Calculate the hash code using Sun's specified algorithm.
     */
    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    @Override
    public String toString() {
      // for compatibility with the real Jre: issue 3422
      return key + "=" + value;
    }
  }

  protected AbstractMap() {
  }

  public void clear() {
    entrySet().clear();
  }

  public boolean containsKey(Object key) {
    return implFindEntry(key, false) != null;
  }

  public boolean containsValue(Object value) {
    for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K, V> entry = iter.next();
      V v = entry.getValue();
      if (Objects.equals(value, v)) {
        return true;
      }
    }
    return false;
  }

  boolean containsEntry(Map.Entry<?, ?> entry) {
    Object key = entry.getKey();
    Object value = entry.getValue();
    Object ourValue = get(key);

    if (!Objects.equals(value, ourValue)) {
      return false;
    }

    // Perhaps it was null and we don't contain the key?
    if (ourValue == null && !containsKey(key)) {
      return false;
    }

    return true;
  }

  public abstract Set<Entry<K, V>> entrySet();

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Map)) {
      return false;
    }
    Map<?, ?> otherMap = (Map<?, ?>) obj;
    if (size() != otherMap.size()) {
      return false;
    }

    for (Entry<?, ?> entry : otherMap.entrySet()) {
      if (!containsEntry(entry)) {
        return false;
      }
    }
    return true;
  }

  public V get(Object key) {
    Map.Entry<K, V> entry = implFindEntry(key, false);
    return (entry == null ? null : entry.getValue());
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (Entry<K, V> entry : entrySet()) {
      hashCode += entry.hashCode();
      hashCode = ~~hashCode;
    }
    return hashCode;
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public void clear() {
        AbstractMap.this.clear();
      }

      @Override
      public boolean contains(Object key) {
        return containsKey(key);
      }

      @Override
      public Iterator<K> iterator() {
        final Iterator<Entry<K, V>> outerIter = entrySet().iterator();
        return new Iterator<K>() {
          @Override
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          @Override
          public K next() {
            Entry<K, V> entry = outerIter.next();
            return entry.getKey();
          }

          @Override
          public void remove() {
            outerIter.remove();
          }
        };
      }

      @Override
      public boolean remove(Object key) {
        if (containsKey(key)) {
          AbstractMap.this.remove(key);
          return true;
        }
        return false;
      }

      @Override
      public int size() {
        return AbstractMap.this.size();
      }
    };
  }

  public V put(K key, V value) {
    throw new UnsupportedOperationException("Put not supported on this map");
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    for (Iterator<? extends Entry<? extends K, ? extends V>> iter = t.entrySet().iterator();
        iter.hasNext(); ) {
      Entry<? extends K, ? extends V> e = iter.next();
      put(e.getKey(), e.getValue());
    }
  }

  public V remove(Object key) {
    Map.Entry<K, V> entry = implFindEntry(key, true);
    return (entry == null ? null : entry.getValue());
  }

  public int size() {
    return entrySet().size();
  }

  @Override
  public String toString() {
    String s = "{";
    boolean comma = false;
    for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K, V> entry = iter.next();
      if (comma) {
        s += ", ";
      } else {
        comma = true;
      }
      s += String.valueOf(entry.getKey());
      s += "=";
      s += String.valueOf(entry.getValue());
    }
    return s + "}";
  }

  @Override
  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public void clear() {
        AbstractMap.this.clear();
      }

      @Override
      public boolean contains(Object value) {
        return containsValue(value);
      }

      @Override
      public Iterator<V> iterator() {
        final Iterator<Entry<K, V>> outerIter = entrySet().iterator();
        return new Iterator<V>() {
          @Override
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          @Override
          public V next() {
            Entry<K, V> entry = outerIter.next();
            return entry.getValue();
          }

          @Override
          public void remove() {
            outerIter.remove();
          }
        };
      }

      @Override
      public int size() {
        return AbstractMap.this.size();
      }
    };
  }

  private Entry<K, V> implFindEntry(Object key, boolean remove) {
    for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K, V> entry = iter.next();
      K k = entry.getKey();
      if (Objects.equals(key, k)) {
        if (remove) {
          entry = new SimpleEntry<K, V>(entry.getKey(), entry.getValue());
          iter.remove();
        }
        return entry;
      }
    }
    return null;
  }
}
