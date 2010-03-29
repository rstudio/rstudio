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
package com.google.gwt.bikeshed.list.client;

/**
 * An interface for extracting a key from a value.  The extracted key
 * must contain suitable implementations of hashCode() and equals().
 *
 * @param <C> the value type for which keys are to be returned
 */
public interface HasKey<C> {

  /**
   * Return a key that may be used to identify values that should
   * be treated as the same in UI views.
   *
   * @param value a value of type C.
   * @return an Object that implements appropriate hashCode() and equals()
   * methods.
   */
  Object getKey(C value);
}