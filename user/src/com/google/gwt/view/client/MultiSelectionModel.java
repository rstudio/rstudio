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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple selection model that allows multiple objects to be selected.
 *
 * @param <T> the record data type
 */
public class MultiSelectionModel<T> extends AbstractSelectionModel<T> {

  // Ensure one value per key
  private final HashMap<Object, T> selectedSet = new HashMap<Object, T>();

  private final HashMap<T, Boolean> selectionChanges = new HashMap<T, Boolean>();

  /**
   * Constructs a MultiSelectionModel without a key provider.
   */
  public MultiSelectionModel() {
    super(null);
  }
  
  /**
   * Constructs a MultiSelectionModel with the given key provider.
   *
   * @param keyProvider an instance of ProvidesKey<T>, or null if the record
   *        object should act as its own key
   */
  public MultiSelectionModel(ProvidesKey<T> keyProvider) {
    super(keyProvider);
  }

  /**
   * Deselect all selected values.
   */
  public void clear() {
    // Clear the current list of pending changes. 
    selectionChanges.clear();

    /*
     * Add a pending change to deselect each value that is currently selected.
     * We cannot just clear the selected set, because then we would not know
     * which values were selected before we cleared, which we need to know to
     * determine if we should fire an event.
     */
    for (T value : selectedSet.values()) {
      selectionChanges.put(value, false);
    }
    scheduleSelectionChangeEvent();
  }

  /**
   * Get the set of selected items as a copy.
   *
   * @return the set of selected items
   */
  public Set<T> getSelectedSet() {
    resolveChanges();
    return new HashSet<T>(selectedSet.values());
  }

  public boolean isSelected(T object) {
    resolveChanges();
    return selectedSet.containsKey(getKey(object));
  }

  public void setSelected(T object, boolean selected) {
    selectionChanges.put(object, selected);
    scheduleSelectionChangeEvent();
  }

  @Override
  protected void fireSelectionChangeEvent() {
    if (isEventScheduled()) {
      setEventCancelled(true);
    }
    resolveChanges();
  }

  private void resolveChanges() {
    if (selectionChanges.isEmpty()) {
      return;
    }

    boolean changed = false;
    for (Map.Entry<T, Boolean> entry : selectionChanges.entrySet()) {
      T object = entry.getKey();
      boolean selected = entry.getValue();

      Object key = getKey(object);
      T oldValue = selectedSet.get(key);
      if (selected) {
        if (oldValue == null || !oldValue.equals(object)) {
          selectedSet.put(getKey(object), object);
          changed = true;
        }
      } else {
        if (oldValue != null) {
          selectedSet.remove(key);
          changed = true;
        }
      }
    }
    selectionChanges.clear();

    // Fire a selection change event.
    if (changed) {
      SelectionChangeEvent.fire(this);
    }
  }
}
