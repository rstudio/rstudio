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
      if (value == null ? v == null : value.equals(v)) {
        return true;
      }
    }
    return false;
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
      Object otherKey = entry.getKey();
      Object otherValue = entry.getValue();
      if (!containsKey(otherKey)) {
        return false;
      }
      if (!Utility.equalsWithNullCheck(otherValue, get(otherKey))) {
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

  public Set<K> keySet() {
    final Set<Entry<K, V>> entrySet = entrySet();
    return new AbstractSet<K>() {
      @Override
      public boolean contains(Object key) {
        return containsKey(key);
      }

      @Override
      public Iterator<K> iterator() {
        final Iterator<Entry<K, V>> outerIter = entrySet.iterator();
        return new Iterator<K>() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public K next() {
            Map.Entry<K, V> entry = outerIter.next();
            return entry.getKey();
          }

          public void remove() {
            outerIter.remove();
          }
        };
      }

      @Override
      public int size() {
        return entrySet.size();
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

  public Collection<V> values() {
    final Set<Entry<K, V>> entrySet = entrySet();
    return new AbstractCollection<V>() {
      @Override
      public boolean contains(Object value) {
        return containsValue(value);
      }

      @Override
      public Iterator<V> iterator() {
        final Iterator<Entry<K, V>> outerIter = entrySet.iterator();
        return new Iterator<V>() {
          public boolean hasNext() {
            return outerIter.hasNext();
          }

          public V next() {
            V value = outerIter.next().getValue();
            return value;
          }

          public void remove() {
            outerIter.remove();
          }
        };
      }

      @Override
      public int size() {
        return entrySet.size();
      }
    };
  }

  private Entry<K, V> implFindEntry(Object key, boolean remove) {
    for (Iterator<Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
      Entry<K, V> entry = iter.next();
      K k = entry.getKey();
      if (key == null ? k == null : key.equals(k)) {
        if (remove) {
          iter.remove();
        }
        return entry;
      }
    }
    return null;
  }
}
