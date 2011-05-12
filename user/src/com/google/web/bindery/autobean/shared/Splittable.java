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
package com.google.web.bindery.autobean.shared;

import com.google.web.bindery.autobean.shared.impl.StringQuoter;

import java.util.List;

/**
 * This interface provides an abstraction around the underlying data model
 * (JavaScriptObject, {@code org.json}, or XML) used to encode an AutoBeanCodex
 * payload.
 */
public interface Splittable {
  /**
   * A value that represents {@code null}.
   */
  Splittable NULL = StringQuoter.nullValue();

  /**
   * Returns a boolean representation of the data.
   */
  boolean asBoolean();

  /**
   * Returns a numeric representation of the data.
   */
  double asNumber();

  /**
   * Assign the splittable to the specified index of the {@code parent} object.
   */
  void assign(Splittable parent, int index);

  /**
   * Assign the splittable to the named property of the {@code parent} object.
   */
  void assign(Splittable parent, String propertyName);

  /**
   * Returns a string representation of the data.
   */
  String asString();

  /**
   * Clones the Splittable, ignoring cycles and tags.
   */
  Splittable deepCopy();

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
   * Returns a value previously set with {@link #setReified(String, Object)}.
   */
  Object getReified(String key);

  /**
   * Returns {@code true} if the value of the Splittable is a boolean.
   */
  boolean isBoolean();

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
   * Indicates if the nth element of a list is null or undefined.
   */
  boolean isNull(int index);

  /**
   * Indicates if the named property is null or undefined.
   */
  boolean isNull(String key);

  /**
   * Returns {@code true} if the value of the Splittable is numeric.
   */
  boolean isNumber();

  /**
   * Returns {@code true} if {@link #setReified(String, Object)} has been called
   * with the given key.
   */
  boolean isReified(String key);

  /**
   * Returns {@code} true if {@link #asString()} can be expected to return a
   * meaningful value.
   */
  boolean isString();

  /**
   * Returns {@code true} if the value of the key is undefined.
   */
  boolean isUndefined(String key);

  /**
   * Associates a tag value with the Splittable.
   */
  void setReified(String key, Object object);

  /**
   * Resets the length of an indexed Splittable.
   */
  void setSize(int i);

  /**
   * Returns the size of an indexed Splittable.
   */
  int size();
}
