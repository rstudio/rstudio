/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.requestfactory.shared;

import com.google.web.bindery.autobean.shared.Splittable;

/**
 * A ProxyStore provides a {@link ProxySerializer} with access to a low-level
 * persistence mechanism. The ProxyStore does not need to be able to interpret
 * the data sent to it by the ProxySerializer; it is merely a wrapper around a
 * persistence mechanism.
 * 
 * @see DefaultProxyStore
 */
public interface ProxyStore {
  /**
   * Called by {@link ProxySerializer} to retrieve a value previously provided
   * to {@link #put(String, Splittable)}.
   * 
   * @param key the key
   * @return the associated value or {@code null} if {@code key} is unknown
   */
  Splittable get(String key);

  /**
   * Returns a non-negative sequence number. The actual sequence of values
   * returned by this method is unimportant, as long as the numbers in the
   * sequence are unique.
   */
  int nextId();

  /**
   * Called by {@link ProxySerializer} to store a value.
   * 
   * @param key a key value that will be passed to {@link #get(String)}
   * @param value the data to store
   * @see Splittable#getPayload()
   */
  void put(String key, Splittable value);
}
