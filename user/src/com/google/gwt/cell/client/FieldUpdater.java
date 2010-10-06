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
package com.google.gwt.cell.client;

/**
 * A {@link FieldUpdater} may be added to a Column to update a particular field
 * of a data item.
 * 
 * @param <T> the data type that will be modified
 * @param <C> the data type of the modified field
 */
public interface FieldUpdater<T, C> {

  /**
   * Announces a new value for a field within a base object.
   * 
   * @param index the current row index of the object
   * @param object the base object to be updated
   * @param value the new value of the field being updated
   */
  void update(int index, T object, C value);
}
