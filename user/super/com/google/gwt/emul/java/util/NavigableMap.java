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

/**
 * Sorted map providing additional query operations and views.
 *
 * @param <K> key type.
 * @param <V> value type.
 */
public interface NavigableMap<K, V> extends SortedMap<K, V> {
  Map.Entry<K, V> ceilingEntry(K key);

  K ceilingKey(K key);

  NavigableSet<K> descendingKeySet();

  NavigableMap<K, V> descendingMap();

  Map.Entry<K, V> firstEntry();

  Map.Entry<K, V> floorEntry(K key);

  K floorKey(K key);

  NavigableMap<K, V> headMap(K toKey, boolean inclusive);

  Map.Entry<K, V> higherEntry(K key);

  K higherKey(K key);

  Map.Entry<K, V> lastEntry();

  Map.Entry<K, V> lowerEntry(K key);

  K lowerKey(K key);

  NavigableSet<K> navigableKeySet();

  Map.Entry<K, V> pollFirstEntry();

  Map.Entry<K, V> pollLastEntry();

  NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

  NavigableMap<K, V> tailMap(K fromKey, boolean inclusive);
}
