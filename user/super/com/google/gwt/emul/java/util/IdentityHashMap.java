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
package java.util;

import com.google.gwt.core.client.impl.Impl;

import java.io.Serializable;

/**
 * Map using reference equality on keys. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/IdentityHashMap.html">[Sun
 * docs]</a>
 * 
 * @param <K> key type
 * @param <V> value type
 */
public class IdentityHashMap<K, V> extends AbstractHashMap<K, V> implements
    Map<K, V>, Cloneable, Serializable {

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

  public IdentityHashMap() {
  }

  public IdentityHashMap(int ignored) {
    super(ignored);
  }

  public IdentityHashMap(Map<? extends K, ? extends V> toBeCopied) {
    super(toBeCopied);
  }

  @Override
  public Object clone() {
    return new IdentityHashMap<K, V>(this);
  }

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
      if (otherValue != get(otherKey)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    for (Entry<K, V> entry : entrySet()) {
      hashCode += System.identityHashCode(entry.getKey());
      hashCode += System.identityHashCode(entry.getValue());
    }
    return hashCode;
  }

  @Override
  protected boolean equals(Object value1, Object value2) {
    return value1 == value2;
  }

  @Override
  protected int getHashCode(Object key) {
    return Impl.getHashCode(key);
  }
}
