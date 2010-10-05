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

/**
 * A description of how rows are to be styled in a {@link CellTable}.
 *
 * @param <T> the data type of each row
 */
public interface RowStyles<T> {

  /**
   * Get extra style names that should be applied to a row.
   *
   * @param row the data stored in the row.
   * @param rowIndex the zero-based index of the row.
   *
   * @return the extra styles of the given row in a space-separated list, or
   * {@code null} if there are no extra styles for this row.
   */
  String getStyleNames(T row, int rowIndex);
}
