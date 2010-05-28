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

import java.util.Set;
import java.util.TreeSet;

/**
 * A simple selection model that allows multiple objects to be selected.
 * 
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 * 
 * @param <T> the record data type
 */
public class MultiSelectionModel<T> extends AbstractSelectionModel<T> {

  private Set<T> selectedSet = new TreeSet<T>();
  private Set<Object> selectedKeys = new TreeSet<Object>();

  /**
   * Get the set of selected items.
   * 
   * @return the set of selected items
   */
  public Set<T> getSelectedSet() {
    return selectedSet;
  }

  public boolean isSelected(T object) {
    return selectedKeys.contains(getKey(object));
  }

  public void setSelected(T object, boolean selected) {
    if (selected) {
      selectedSet.add(object);
      selectedKeys.add(getKey(object));
    } else {
      selectedSet.remove(object);
      selectedKeys.remove(getKey(object));
    }
    scheduleSelectionChangeEvent();
  }
}
