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

/**
 * Basic {@link Map.Entry} implementation that implements hashCode, equals, and
 * toString.
 */
abstract class AbstractMapEntry<K, V> implements Map.Entry<K, V> {

  @Override
  public final boolean equals(Object other) {
    if (!(other instanceof Map.Entry)) {
      return false;
    }
    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
    return Objects.equals(getKey(), entry.getKey())
        && Objects.equals(getValue(), entry.getValue());
  }

  /**
   * Calculate the hash code using Sun's specified algorithm.
   */
  @Override
  public final int hashCode() {
    return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
  }

  @Override
  public final String toString() {
    // for compatibility with the real Jre: issue 3422
    return getKey() + "=" + getValue();
  }
}
