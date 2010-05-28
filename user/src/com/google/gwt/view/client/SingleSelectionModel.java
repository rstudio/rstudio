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
 * A simple selection model that allows only one object to be selected a a time.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * @param <T> the record data type
 */
public class SingleSelectionModel<T> extends AbstractSelectionModel<T> {

  private T curSelection;
  private Object curKey;

  /**
   * Gets the currently-selected object.
   */
  public T getSelectedObject() {
    return curSelection;
  }

  public boolean isSelected(T object) {
    if (curSelection == null || curKey == null || object == null) {
      return false;
    }
    return curKey.equals(getKey(object));
  }

  public void setSelected(T object, boolean selected) {
    Object key = getKey(object);
    if (selected) {
      curSelection = object;
      curKey = key;
    } else if (curKey != null && curKey.equals(key)) {
      curSelection = null;
      curKey = null;
    }
    scheduleSelectionChangeEvent();
  }
}
