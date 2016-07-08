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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * Abstract interface for maps.
 *
 * @param <K> key type.
 * @param <V> value type.
 */
@JsType
public interface Map<K, V> {

  /**
   * Represents an individual map entry.
   */
  interface Entry<K, V> {
    @Override
    boolean equals(Object o);

    K getKey();

    V getValue();

    @Override
    int hashCode();

    V setValue(V value);

    static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K,V>> comparingByKey() {
      return comparingByKey(Comparator.naturalOrder());
    }

    static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
      checkNotNull(cmp);
      return (Comparator<Map.Entry<K, V>> & Serializable)
          (a, b) -> cmp.compare(a.getKey(), b.getKey());
    }

    static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K,V>> comparingByValue() {
      return comparingByValue(Comparator.naturalOrder());
    }

    static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
      checkNotNull(cmp);
      return (Comparator<Map.Entry<K, V>> & Serializable)
          (a, b) -> cmp.compare(a.getValue(), b.getValue());
    }
  }

  void clear();

  @JsIgnore
  default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);

    V value = remappingFunction.apply(key, get(key));
    if (value != null) {
      put(key, value);
    } else {
      remove(key);
    }
    return value;
  }

  @JsIgnore
  default V computeIfAbsent(K key, Function<? super K, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);

    V value = get(key);
    if (value == null) {
      value = remappingFunction.apply(key);
      if (value != null) {
        put(key, value);
      }
    }
    return value;
  }

  @JsIgnore
  default V computeIfPresent(
      K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);

    V value = get(key);
    if (value != null) {
      value = remappingFunction.apply(key, value);
      if (value != null) {
        put(key, value);
      } else {
        remove(key);
      }
    }
    return value;
  }

  boolean containsKey(Object key);

  boolean containsValue(Object value);

  @JsIgnore
  Set<Entry<K, V>> entrySet();

  @JsIgnore
  default void forEach(BiConsumer<? super K, ? super V> consumer) {
    checkNotNull(consumer);
    for (Entry<K, V> entry : entrySet()) {
      consumer.accept(entry.getKey(), entry.getValue());
    }
  }

  V get(Object key);

  default V getOrDefault(Object key, V defaultValue) {
    V currentValue = get(key);
    return (currentValue == null && !containsKey(key)) ? defaultValue : currentValue;
  }

  boolean isEmpty();

  Set<K> keySet();

  @JsIgnore
  default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    checkNotNull(remappingFunction);
    checkNotNull(value);

    V currentValue = get(key);
    V newValue = currentValue == null ? value : remappingFunction.apply(currentValue, value);
    if (newValue == null) {
      remove(key);
    } else {
      put(key, newValue);
    }
    return newValue;
  }

  V put(K key, V value);

  default V putIfAbsent(K key, V value) {
    V currentValue = get(key);
    return currentValue != null ? currentValue : put(key, value);
  }

  void putAll(Map<? extends K, ? extends V> t);

  V remove(Object key);

  @JsIgnore
  default boolean remove(Object key, Object value) {
    Object currentValue = get(key);
    if (!Objects.equals(currentValue, value) || (currentValue == null && !containsKey(key))) {
      return false;
    }
    remove(key);
    return true;
  }

  default V replace(K key, V value) {
    return containsKey(key) ? put(key, value) : null;
  }

  @JsIgnore
  default boolean replace(K key, V oldValue, V newValue) {
    Object currentValue = get(key);
    if (!Objects.equals(currentValue, oldValue) || (currentValue == null && !containsKey(key))) {
      return false;
    }
    put(key, newValue);
    return true;
  }

  @JsIgnore
  default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    checkNotNull(function);
    for (Entry<K, V> entry : entrySet()) {
      entry.setValue(function.apply(entry.getKey(), entry.getValue()));
    }
  }

  int size();

  Collection<V> values();
}
