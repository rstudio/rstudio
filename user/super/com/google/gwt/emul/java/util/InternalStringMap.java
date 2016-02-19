/*
 * Copyright 2014 Google Inc.
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

import static java.util.ConcurrentModificationDetector.structureChanged;

import java.util.Map.Entry;

import javaemul.internal.JsUtils;

/**
 * A simple wrapper around JavaScript Map for key type is string.
 */
class InternalStringMap<K, V> implements Iterable<Entry<K, V>> {

  private final InternalJsMap<V> backingMap = InternalJsMapFactory.newJsMap();
  private AbstractHashMap<K, V> host;
  private int size;

  /**
   * A mod count to track 'value' replacements in map to ensure that the 'value' that we have in the
   * iterator entry is guaranteed to be still correct.
   * This is to optimize for the common scenario where the values are not modified during
   * iterations where the entries are never stale.
   */
  private int valueMod;

  public InternalStringMap(AbstractHashMap<K, V> host) {
    this.host = host;
  }

  public boolean contains(String key) {
    return !JsUtils.isUndefined(backingMap.get(key));
  }

  public V get(String key) {
    return backingMap.get(key);
  }

  public V put(String key, V value) {
    V oldValue = backingMap.get(key);
    backingMap.set(key, toNullIfUndefined(value));

    if (JsUtils.isUndefined(oldValue)) {
      size++;
      structureChanged(host);
    } else {
      valueMod++;
    }
    return oldValue;
  }

  public V remove(String key) {
    V value = backingMap.get(key);
    if (!JsUtils.isUndefined(value)) {
      backingMap.delete(key);
      size--;
      structureChanged(host);
    } else {
      valueMod++;
    }

    return value;
  }

  public int size() {
    return size;
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return new Iterator<Map.Entry<K,V>>() {
      InternalJsMap.Iterator<V> entries = backingMap.entries();
      InternalJsMap.IteratorEntry<V> current = entries.next();
      InternalJsMap.IteratorEntry<V> last;

      @Override
      public boolean hasNext() {
        return !current.done;
      }
      @Override
      public Entry<K, V> next() {
        last = current;
        current = entries.next();
        return newMapEntry(last, valueMod);
      }
      @Override
      public void remove() {
        InternalStringMap.this.remove(last.getKey());
      }
    };
  }

  private Entry<K, V> newMapEntry(final InternalJsMap.IteratorEntry<V> entry,
      final int lastValueMod) {
    return new AbstractMapEntry<K, V>() {
      @SuppressWarnings("unchecked")
      @Override
      public K getKey() {
        return (K) entry.getKey();
      }
      @Override
      public V getValue() {
        if (valueMod != lastValueMod) {
          // Let's get a fresh copy as the value may have changed.
          return get(entry.getKey());
        }
        return entry.getValue();
      }
      @Override
      public V setValue(V object) {
        return put(entry.getKey(), object);
      }
    };
  }

  private static <T> T toNullIfUndefined(T value) {
    return JsUtils.isUndefined(value) ? null : value;
  }
}
