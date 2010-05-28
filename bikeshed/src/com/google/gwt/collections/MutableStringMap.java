/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.collections;

import java.util.HashMap;

/**
 * A {@link MutableMap} whose keys are supported by Strings. Standard byte code
 * implementation backed by {@code HashMap}.
 * 
 * This class may contain mappings from {@code null} keys. More specifically a
 * key is equal to a stored key k if {@code key == null? k == null :
 * key.equals(k)}. This class may also store {@code null} values.
 * 
 * @param <V> the type of value stored in the {@link MutableMap}
 */
public class MutableStringMap<V> extends MutableMap<String, V> {

  java.util.Map<String, V> entries;

  @Override
  @ConstantTime
  public void clear() {
    entries = null;
  }

  @Override
  @ConstantTime
  public boolean containsKey(String key) {
    return entries != null && entries.containsKey(key);
  }

  @Override
  @ConstantTime
  public V get(String key) {
    return isEmpty() ? null : entries.get(key);
  }

  @Override
  @ConstantTime
  public boolean isEmpty() {
    return entries == null;
  }

  @Override
  @ConstantTime
  public void put(String key, V value) {
    if (entries == null) {
      entries = new HashMap<String, V>();
    }
    entries.put(key, value);
  }

  @Override
  @ConstantTime
  public void remove(String key) {
    entries.remove(key);
    if (entries.isEmpty()) {
      entries = null;
    }
  }

}
