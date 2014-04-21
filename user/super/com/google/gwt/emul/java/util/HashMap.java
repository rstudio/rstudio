/*
 * Copyright 2008 Google Inc.
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

import java.io.Serializable;

/**
 * Implementation of Map interface based on a hash table. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/HashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class HashMap<K, V> extends AbstractHashMap<K, V> implements Cloneable,
    Serializable {

  /**
   * Ensures that RPC will consider type parameter K to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private K exposeKey;

  /**
   * Ensures that RPC will consider type parameter V to be exposed. It will be
   * pruned by dead code elimination.
   */
  @SuppressWarnings("unused")
  private V exposeValue;

  public HashMap() {
  }

  public HashMap(int ignored) {
    super(ignored);
  }

  public HashMap(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
  }

  public HashMap(Map<? extends K, ? extends V> toBeCopied) {
    super(toBeCopied);
  }

  public Object clone() {
    return new HashMap<K, V>(this);
  }

  @Override
  boolean equals(Object value1, Object value2) {
    return Objects.equals(value1, value2);
  }

  @Override
  int getHashCode(Object key) {
    // Coerce to int -- our classes all do this, but a user-written class might not.
    return ~~key.hashCode();
  }
}
