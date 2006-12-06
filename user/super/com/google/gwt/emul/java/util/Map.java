/*
 * Copyright 2006 Google Inc.
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
 * Abstract interface for maps.
 */
public interface Map {

  /**
   * Represents an individual map entry.
   */
  public static interface Entry {
    boolean equals(Object o);

    Object getKey();

    Object getValue();

    int hashCode();

    Object setValue(Object value);
  }

  void clear();

  boolean containsKey(Object key);

  boolean containsValue(Object value);

  Set entrySet();

  boolean equals(Object o);

  Object get(Object key);

  int hashCode();

  boolean isEmpty();

  Set keySet();

  Object put(Object key, Object value);

  void putAll(Map t);

  Object remove(Object key);

  int size();

  Collection values();
}
