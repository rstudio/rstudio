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
package com.google.gwt.user.cellview.client;

import com.google.gwt.cell.client.Cell;

/**
 * A passthrough column, useful for giving cells access to the entire row
 * object.
 *
 * @param <T> the row type
 */
public class IdentityColumn<T> extends Column<T, T> {

  /**
   * Construct a new IdentityColumn with a given {@link Cell}.
   * 
   * @param cell the {@link Cell} responsible for rendering items in the column
   */
  public IdentityColumn(Cell<T> cell) {
    super(cell);
  }

  /**
   * Return the passed-in object.
   *
   * @param object the object to return
   */
  @Override
  public T getValue(T object) {
    return object;
  }
}
