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

import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

/**
 * A selection model that does not allow selection, but fires selection change
 * events. Use this model if you want to know when a user selects an item, but
 * do not want the view to update based on the selection.
 * 
 * @param <T> the record data type
 */
public class NoSelectionModel<T> extends AbstractSelectionModel<T> {

  private Object lastKey;
  private T lastSelection;

  /**
   * Constructs a NoSelectionModel without a key provider.
   */
  public NoSelectionModel() {
    super(null);
  }

  /**
   * Constructs a NoSelectionModel with the given key provider.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   */
  public NoSelectionModel(ProvidesKey<T> keyProvider) {
    super(keyProvider);
  }

  /**
   * Gets the item that was last selected.
   * 
   * @return the last selected item
   */
  public T getLastSelectedObject() {
    return lastSelection;
  }

  @Override
  public boolean isSelected(T item) {
    return false;
  }

  @Override
  public void setSelected(T item, boolean selected) {
    Object key = getKey(item);
    if (selected) {
      lastSelection = item;
      lastKey = key;
    } else if (lastKey != null && lastKey.equals(key)) {
      lastSelection = null;
      lastKey = null;
    }
    scheduleSelectionChangeEvent();
  }
}
