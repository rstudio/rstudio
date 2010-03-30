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
package com.google.gwt.bikeshed.list.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * A model for selection within a list.
 * 
 * @param <T> the data type of records in the list
 */
public interface SelectionModel<T> {
  
  /**
   * A listener who will be updated when the selection changes.
   */
  interface SelectionListener {
    void selectionChanged();
  }
  
  /**
   * A default implementation of SelectionModel that provides listener
   * addition and removal.
   *
   * @param <T> the data type of records in the list
   */
  abstract class DefaultSelectionModel<T> implements SelectionModel<T> {

    protected List<SelectionListener> listeners = new ArrayList<SelectionListener>();

    public void addListener(SelectionListener listener) {
      if (!listeners.contains(listener)) {
        listeners.add(listener);
      }
    }
    
    public void removeListener(SelectionListener listener) {
      if (listeners.contains(listener)) {
        listeners.remove(listener);
      }
    }
    
    public void updateListeners() {
      // Inform the listeners
      for (SelectionListener listener : listeners) {
        listener.selectionChanged();
      }
    }
  }
  
  void addListener(SelectionListener listener);

  boolean isSelected(T object, int index);
  
  void removeListener(SelectionListener listener);
  
  void setSelected(int index, boolean selected);

  void setSelected(T object, boolean selected);
}
