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

import java.util.List;

/**
 * A view that can display a range of data.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <T> the data type of each row
 */
public interface HasData<T> extends HasRows {

  /**
   * Get the {@link SelectionModel} used by this {@link HasData}.
   *
   * @return the {@link SelectionModel}
   */
  SelectionModel<? super T> getSelectionModel();

  /**
   * Set a values associated with the rows in the visible range.
   *
   * @param start the start index of the data
   * @param values the values within the range
   */
  void setRowValues(int start, List<T> values);

  /**
   * Set the {@link SelectionModel} used by this {@link HasData}.
   *
   * @param selectionModel the {@link SelectionModel}
   */
  void setSelectionModel(SelectionModel<? super T> selectionModel);
}
