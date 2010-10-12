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
 * An interface for extracting a value of type C from an underlying data value
 * of type T, provide a {@link Cell} to render that value, and provide a
 * {@link FieldUpdater} to perform notification of updates to the cell.
 * 
 * @param <T> the underlying data type
 * @param <C> the cell data type
 */
public interface HasCell<T, C> {

  /**
   * Returns the {@link Cell} of type C.
   * 
   * @return a Cell
   */
  Cell<C> getCell();

  /**
   * Returns the {@link FieldUpdater} instance.
   * 
   * @return an instance of FieldUpdater<T, C>
   */
  FieldUpdater<T, C> getFieldUpdater();

  /**
   * Returns the value of type C extracted from the record of type T.
   * 
   * @param object a record of type T
   * @return a value of type C suitable for passing to the cell
   */
  C getValue(T object);
}
