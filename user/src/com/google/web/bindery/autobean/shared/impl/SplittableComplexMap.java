/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.autobean.shared.impl;

import com.google.web.bindery.autobean.shared.Splittable;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.Coder;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.EncodeState;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * A Map implementation for complex keys.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class SplittableComplexMap<K, V> implements Map<K, V>, HasSplittable {
  private final Splittable data;
  private final List<K> keys;
  private final List<V> values;

  public SplittableComplexMap(Splittable data, Coder keyCoder, Coder valueCoder, EncodeState state) {
    this.data = data;
    this.keys = new SplittableList<K>(data.get(0), keyCoder, state);
    this.values = new SplittableList<V>(data.get(1), valueCoder, state);
    assert this.keys.size() == this.values.size();
  }

  public void clear() {
    // Trigger ConcurrentModificationExceptions for any outstanding Iterators
    keys.clear();
    values.clear();
  }

  public boolean containsKey(Object key) {
    return keys.contains(key);
  }

  public boolean containsValue(Object value) {
    return values.contains(value);
  }

  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new AbstractSet<Map.Entry<K, V>>() {

      @Override
      public Iterator<java.util.Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {
          Iterator<K> keyIt = keys.iterator();
          ListIterator<V> valueIt = values.listIterator();

          public boolean hasNext() {
            assert keyIt.hasNext() == valueIt.hasNext();
            return keyIt.hasNext();
          }

          public java.util.Map.Entry<K, V> next() {
            return new Map.Entry<K, V>() {
              final K key = keyIt.next();
              final V value = valueIt.next();

              public K getKey() {
                return key;
              }

              public V getValue() {
                return value;
              }

              public V setValue(V value) {
                valueIt.set(value);
                return value;
              }
            };
          }

          public void remove() {
            keyIt.remove();
            valueIt.remove();
          }
        };
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }

  public V get(Object key) {
    int idx = keys.indexOf(key);
    if (idx == -1) {
      return null;
    }
    return values.get(idx);
  }

  public Splittable getSplittable() {
    return data;
  }

  public boolean isEmpty() {
    return keys.isEmpty();
  }

  public Set<K> keySet() {
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        return keys.iterator();
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }

  public V put(K key, V value) {
    int idx = keys.indexOf(key);
    if (idx == -1) {
      keys.add(key);
      values.add(value);
      return null;
    }
    return values.set(idx, value);
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public V remove(Object key) {
    int idx = keys.indexOf(key);
    if (idx == -1) {
      return null;
    }
    keys.remove(idx);
    return values.remove(idx);
  }

  public int size() {
    return keys.size();
  }

  public Collection<V> values() {
    return new AbstractCollection<V>() {
      @Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          final Iterator<K> keyIt = keys.iterator();
          final Iterator<V> valueIt = values.iterator();

          public boolean hasNext() {
            return keyIt.hasNext();
          }

          public V next() {
            keyIt.next();
            return valueIt.next();
          }

          public void remove() {
            keyIt.remove();
            valueIt.remove();
          }
        };
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }
}
