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
import java.util.Map;
import java.util.Set;

/**
 * A Map implementation for regular JSON maps with value-type keys.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class SplittableSimpleMap<K, V> implements Map<K, V>, HasSplittable {
  private final Splittable data;
  private final Coder keyCoder;
  private final EncodeState state;
  private final Coder valueCoder;
  /**
   * Don't hang the reified data from {@link #data} since we can't tell the
   * __reified field from the actual data.
   */
  private Splittable reified = StringQuoter.createSplittable();

  public SplittableSimpleMap(Splittable data, Coder keyCoder, Coder valueCoder, EncodeState state) {
    this.data = data;
    this.keyCoder = keyCoder;
    this.state = state;
    this.valueCoder = valueCoder;
  }

  public void clear() {
    for (String key : data.getPropertyKeys()) {
      Splittable.NULL.assign(data, key);
      reified.setReified(key, null);
    }
  }

  public boolean containsKey(Object key) {
    String encodedKey = encodedKey(key);
    return !data.isUndefined(encodedKey) || reified.isReified(encodedKey);
  }

  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new AbstractSet<Map.Entry<K, V>>() {
      final List<String> keys = data.getPropertyKeys();

      @Override
      public Iterator<java.util.Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {
          Iterator<String> keyIterator = keys.iterator();
          String encodedKey;

          public boolean hasNext() {
            return keyIterator.hasNext();
          }

          public java.util.Map.Entry<K, V> next() {
            encodedKey = keyIterator.next();
            return new Map.Entry<K, V>() {
              @SuppressWarnings("unchecked")
              final K key = (K) keyCoder.decode(state, StringQuoter.split(StringQuoter
                  .quote(encodedKey)));
              @SuppressWarnings("unchecked")
              final V value = (V) valueCoder.decode(state, data.get(encodedKey));

              public K getKey() {
                return key;
              }

              public V getValue() {
                return value;
              }

              public V setValue(V newValue) {
                return put(key, newValue);
              }
            };
          }

          public void remove() {
            Splittable.NULL.assign(data, encodedKey);
            reified.setReified(encodedKey, null);
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
    String encodedKey = encodedKey(key);
    return getRaw(encodedKey);
  }

  public Splittable getSplittable() {
    return data;
  }

  public boolean isEmpty() {
    return data.getPropertyKeys().isEmpty();
  }

  public Set<K> keySet() {
    return new AbstractSet<K>() {
      final List<String> keys = data.getPropertyKeys();

      @Override
      public Iterator<K> iterator() {
        return new Iterator<K>() {
          final Iterator<String> it = keys.iterator();
          String lastEncodedKey;

          public boolean hasNext() {
            return it.hasNext();
          }

          public K next() {
            lastEncodedKey = it.next();
            @SuppressWarnings("unchecked")
            K toReturn =
                (K) keyCoder.decode(state, StringQuoter.split(StringQuoter.quote(lastEncodedKey)));
            return toReturn;
          }

          public void remove() {
            Splittable.NULL.assign(data, lastEncodedKey);
            reified.setReified(lastEncodedKey, null);
          }
        };
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }

  public V put(K key, V value) {
    V toReturn = get(key);
    String encodedKey = encodedKey(key);
    reified.setReified(encodedKey, value);
    Splittable encodedValue = valueCoder.extractSplittable(state, value);
    if (encodedValue == null) {
      // External datastructure
      reified.setReified(AbstractAutoBean.UNSPLITTABLE_VALUES_KEY, true);
    } else {
      encodedValue.assign(data, encodedKey);
    }
    return toReturn;
  }

  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  public V remove(Object key) {
    V toReturn = get(key);
    String encodedKey = encodedKey(key);
    reified.setReified(encodedKey, null);
    Splittable.NULL.assign(data, encodedKey);
    return toReturn;
  }

  public int size() {
    return data.getPropertyKeys().size();
  }

  public Collection<V> values() {
    return new AbstractCollection<V>() {
      final List<String> keys = data.getPropertyKeys();

      @Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          final Iterator<String> it = keys.iterator();
          String lastEncodedKey;

          public boolean hasNext() {
            return it.hasNext();
          }

          public V next() {
            lastEncodedKey = it.next();
            return getRaw(lastEncodedKey);
          }

          public void remove() {
            Splittable.NULL.assign(data, lastEncodedKey);
            reified.setReified(lastEncodedKey, null);
          }
        };
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }

  private String encodedKey(Object key) {
    return keyCoder.extractSplittable(state, key).asString();
  }

  private V getRaw(String encodedKey) {
    if (reified.isReified(encodedKey)) {
      @SuppressWarnings("unchecked")
      V toReturn = (V) reified.getReified(encodedKey);
      return toReturn;
    }
    // Both undefined or an explicit null should return null here
    if (data.isNull(encodedKey)) {
      return null;
    }
    Splittable value = data.get(encodedKey);
    @SuppressWarnings("unchecked")
    V toReturn = (V) valueCoder.decode(state, value);
    reified.setReified(encodedKey, toReturn);
    return toReturn;
  }
}
