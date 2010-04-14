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

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.FieldUpdater;

/**
 * An interface for extracting a value from an underlying data type, provide a
 * cell to render that value, and provide a FieldUpdater to perform notification
 * of updates to the cell.
 *
 * @param <T> the underlying data type
 * @param <C> the cell data type
 * @param <V> the view data type
 */
public interface HasCell<T, C, V> {

  Cell<C, V> getCell();

  FieldUpdater<T, C, V> getFieldUpdater();

  C getValue(T object);
}
