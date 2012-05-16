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
 * A simple selection model that allows multiple items to be selected.
 * 
 * @param <T> the data type of the items
 */
public class MultiSelectionModel<T> extends AbstractSelectionModel<T> {

  /**
   * Stores an item and its pending selection state.
   * 
   * @param <T> the data type of the item
   */
  static class SelectionChange<T> {
    private final T item;
    private final boolean isSelected;

    SelectionChange(T item, boolean isSelected) {
      this.item = item;
      this.isSelected = isSelected;
    }

    public T getItem() {
      return item;
    }

    public boolean isSelected() {
      return isSelected;
    }
  }

  // Ensure one value per key
  final Map<Object, T> selectedSet;

  /**
   * A map of keys to the item and its pending selection state.
   */
  private final Map<Object, SelectionChange<T>> selectionChanges;

  /**
   * Constructs a MultiSelectionModel without a key provider.
   */
  public MultiSelectionModel() {
    this(null);
  }

  /**
   * Constructs a MultiSelectionModel with the given key provider.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   */
  public MultiSelectionModel(ProvidesKey<T> keyProvider) {
    this(keyProvider, new HashMap<Object, T>(), new HashMap<Object, SelectionChange<T>>());
  }

  /**
   * Construct a MultiSelectionModel with the given key provider and
   * implementations of selectedSet and selectionChanges. Different
   * implementations allow for enforcing order on selection.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   * @param selectedSet an instance of Map
   * @param selectionChanges an instance of Map
   */
  MultiSelectionModel(ProvidesKey<T> keyProvider, Map<Object, T> selectedSet,
      Map<Object, SelectionChange<T>> selectionChanges) {
    super(keyProvider);
    this.selectedSet = selectedSet;
    this.selectionChanges = selectionChanges;
  }

  /**
   * Deselect all selected values.
   */
  public void clear() {
    // Clear the current list of pending changes.
    selectionChanges.clear();

    /*
     * Add a pending change to deselect each key that is currently selected. We
     * cannot just clear the selected set, because then we would not know which
     * keys were selected before we cleared, which we need to know to determine
     * if we should fire an event.
     */
    for (T value : selectedSet.values()) {
      selectionChanges.put(getKey(value), new SelectionChange<T>(value, false));
    }
    scheduleSelectionChangeEvent();
  }

  /**
   * Get the set of selected items as a copy. If multiple selected items share
   * the same key, only the last selected item is included in the set.
   * 
   * @return the set of selected items
   */
  public Set<T> getSelectedSet() {
    resolveChanges();
    return new HashSet<T>(selectedSet.values());
  }

  @Override
  public boolean isSelected(T item) {
    resolveChanges();
    return selectedSet.containsKey(getKey(item));
  }

  @Override
  public void setSelected(T item, boolean selected) {
    selectionChanges.put(getKey(item), new SelectionChange<T>(item, selected));
    scheduleSelectionChangeEvent();
  }

  @Override
  protected void fireSelectionChangeEvent() {
    if (isEventScheduled()) {
      setEventCancelled(true);
    }
    resolveChanges();
  }

  void resolveChanges() {
    if (selectionChanges.isEmpty()) {
      return;
    }

    boolean changed = false;
    for (Map.Entry<Object, SelectionChange<T>> entry : selectionChanges.entrySet()) {
      Object key = entry.getKey();
      SelectionChange<T> value = entry.getValue();
      boolean selected = value.isSelected;

      T oldValue = selectedSet.get(key);
      if (selected) {
        selectedSet.put(key, value.item);
        Object oldKey = getKey(oldValue);
        if (!changed) {
          changed = (oldKey == null) ? (key != null) : !oldKey.equals(key);
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
