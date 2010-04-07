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
package com.google.gwt.bikeshed.list.shared;

/**
 * <p>
 * Implementors of {@link ProvidesKey} provide a key for list items.
 * </p>
 * <p>
 * The key must implement a coherent set of {@link #equals(Object)} and
 * {@link #hashCode()} methods. If the item type is a not uniquely identifiable,
 * such as a list of {@link String}, the index can be used as the key.
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
