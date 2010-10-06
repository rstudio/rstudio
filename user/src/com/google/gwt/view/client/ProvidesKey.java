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
package com.google.gwt.view.client;

/**
 * <p>
 * Implementors of {@link ProvidesKey} provide a key for list items, such that
 * items that are to be treated as distinct (for example, for editing) have
 * distinct keys.
 * </p>
 * <p>
 * The key must implement a coherent set of {@link Object#equals(Object)} and
 * {@link Object#hashCode()} methods such that if objects {@code A} and {@code
 * B} are to be treated as identical, then {@code A.equals(B)}, {@code
 * B.equals(A)}, and {@code A.hashCode() == B.hashCode()}. If {@code A} and
 * {@code B} are to be treated as unequal, then it must be the case that {@code
 * A.equals(B) == false} and {@code B.equals(A) == false}.
 * </p>
 * 
 * @param <T> the data type of records in the list
 */
public interface ProvidesKey<T> {

  /**
   * Get the key for a list item.
   *
   * @param item the list item
   * @return the key that represents the item
   */
  Object getKey(T item);
}
