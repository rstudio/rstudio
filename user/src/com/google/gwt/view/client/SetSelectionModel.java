/*
 * Copyright 2012 Google Inc.
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

import java.util.Set;

/**
 * A model that allows getting all elements and clearing the selection.
 *
 * @param <T> the record data type
 */
public interface SetSelectionModel<T> extends SelectionModel<T> {
  /**
   * Clears the current selection.
   */
  void clear();

  /**
   * Get the set of selected items.
   *
   * @return the set of selected items
   */
  Set<T> getSelectedSet();
}
