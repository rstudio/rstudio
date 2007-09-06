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
package com.google.gwt.user.rebind;

import java.util.Map;

/**
 * Common base class for Enum-like 1.4 classes, often needed for generator-like
 * functionality.
 */
class Enum {
  /**
   * Requires the specified object from the pool.
   * 
   * @param key the key associated with the <code>Enum</code>
   * @param pool pool to draw key from
   * @return associated <code>Enum</code>
   */
  public static Enum require(String key, Map<String, Enum> pool) {
    Enum t = pool.get(key);
    if (t == null) {
      throw new IllegalArgumentException(key
          + " is not a valid Enum type. Current options are " + pool.keySet());
    }
    return t;
  }
  
  /**
   * Associated key.
   */
  final String key;

  /**
   * Creates a new <code>Enum</code> in a given pool.
   * 
   * @param key associated key
   * @param pool associated pool
   */
  protected Enum(String key, Map<String, Enum> pool) {
    this.key = key;
    pool.put(key, this);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return key;
  }
}