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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A simple selection model that allows multiple items to be selected and
 * retains order of selection. Selecting the same item multiple times does not
 * change the order (item won't be moved to the end of selection).
 * 
 *  OrderedMultiSelectionModel uses LinkedHashMaps, which may increase the size
 * of your compiled output if you do not use LinkedHashMaps elsewhere in your
 * application.
 * 
 * @param <T> the record data type
 */
public class OrderedMultiSelectionModel<T> extends MultiSelectionModel<T> {
  /**
   * Constructs a OrderedMultiSelectionModel without a key provider.
   */
  public OrderedMultiSelectionModel() {
    this(null);
  }

  /**
   * Constructs a OrderedMultiSelectionModel with the given key provider.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   */
  public OrderedMultiSelectionModel(ProvidesKey<T> keyProvider) {
    super(keyProvider, new LinkedHashMap<Object, T>(),
        new LinkedHashMap<Object, SelectionChange<T>>());
  }

  /**
   * Get the List of selected items as a copy.
   * 
   * @return the list of selected items in the order of additions. Selecting an
   *         element already in the selection does not move it to the end of the
   *         list.
   */
  public List<T> getSelectedList() {
    resolveChanges();
    return new ArrayList<T>(selectedSet.values());
  }
}
