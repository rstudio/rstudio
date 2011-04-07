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
package com.google.gwt.autobean.shared;

import java.util.List;

/**
 * This interface provides an abstraction around the underlying data model
 * (JavaScriptObject, {@code org.json}, or XML) used to encode an AutoBeanCodex
 * payload.
 */
public interface Splittable {
  /**
   * Returns a string representation of the data.
   */
  String asString();

  /**
   * Returns the nth element of a list.
   */
  Splittable get(int index);

  /**
   * Returns the named property.
   */
  Splittable get(String key);

  /**
   * Returns a wire-format representation of the data.
   */
  String getPayload();

  /**
   * Returns all keys available in the Splittable. This method may be expensive
   * to compute.
   */
  List<String> getPropertyKeys();

  /**
   * Returns {@code} true if {@link #size()} and {@link #get(int)} can be
   * expected to return meaningful values.
   */
  boolean isIndexed();

  /**
   * Returns {@code} true if {@link #getPropertyKeys()} and {@link #get(String)}
   * can be expected to return meaningful values.
   */
  boolean isKeyed();

  /**
   * Indicates if the nth element of a list is null.
   */
  boolean isNull(int index);

  /**
   * Indicates if the named property is null.
   */
  boolean isNull(String key);

  /**
   * Returns {@code} true if {@link #asString()} can be expected to return a
   * meaningful value.
   */
  boolean isString();

  /**
   * Returns the size of the list.
   */
  int size();
}