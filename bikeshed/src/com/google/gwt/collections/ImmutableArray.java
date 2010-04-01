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
package com.google.gwt.collections;

/**
 * An array that is guaranteed not to change, thus making it safe for disparate
 * portions of code to maintain references to a shared instance, rather than
 * feeling the need to make defensive copies.
 * 
 * @param <E> The type stored in the array elements
 */
public abstract class ImmutableArray<E> extends Array<E> {

  @SuppressWarnings("unchecked")
  private static final ImmutableArray EMPTY = new ImmutableArrayEmptyImpl();

  @SuppressWarnings("unchecked")
  static <E> ImmutableArray<E> getEmptyInstance() {
    return EMPTY;
  }

  ImmutableArray() {
  }

}
