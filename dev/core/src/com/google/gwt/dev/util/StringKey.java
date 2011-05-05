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
package com.google.gwt.dev.util;

import java.io.Serializable;

/**
 * A "typed string" utility class that improves the readability of generic code
 * by using proper types. Equality and comparison are implemented by using both
 * the concrete type and string value.
 */
public abstract class StringKey implements Comparable<StringKey>, Serializable {

  /**
   * The hashcode is computed and stored since this type is expected to be used
   * as a map key.
   */
  private final int hashCode;
  private final String value;

  /**
   * Constructor. A <code>null</code> value is legal.
   * 
   * @param value the value this key object represents
   */
  protected StringKey(String value) {
    this.value = value;
    this.hashCode = getClass().getName().hashCode() * 13
        + (value == null ? 0 : value.hashCode());
  }

  /**
   * Compares based on concrete type name and then value.
   */
  public final int compareTo(StringKey o) {
    if (getClass() == o.getClass()) {
      if (value == null) {
        return o.value == null ? 0 : -1;
      } else if (o.value == null) {
        return value == null ? 0 : 1;
      } else {
        return value.compareTo(o.value);
      }
    }
    return getClass().getName().compareTo(o.getClass().getName());
  }

  /**
   * Delegates to {@link #compareTo}.
   */
  @Override
  public final boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    return compareTo((StringKey) o) == 0 ? true : false;
  }

  /**
   * Returns the value of the StringKey.
   */
  public final String get() {
    return value;
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  /**
   * Delegates to {@link #get()}.
   */
  @Override
  public String toString() {
    return value;
  }
}
