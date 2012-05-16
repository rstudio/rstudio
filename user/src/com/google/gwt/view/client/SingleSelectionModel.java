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
 * A simple selection model that allows only one item to be selected a a time.
 * 
 * @param <T> the record data type
 */
public class SingleSelectionModel<T> extends AbstractSelectionModel<T> {

  private Object curKey;
  private T curSelection;

  // Pending selection change
  private boolean newSelected;
  private T newSelectedItem = null;
  private boolean newSelectedPending;

  /**
   * Constructs a SingleSelectionModel without a key provider.
   */
  public SingleSelectionModel() {
    super(null);
  }

  /**
   * Constructs a SingleSelectionModel with the given key provider.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   */
  public SingleSelectionModel(ProvidesKey<T> keyProvider) {
    super(keyProvider);
  }

  /**
   * Gets the currently-selected item.
   * 
   * @return the selected item
   */
  public T getSelectedObject() {
    resolveChanges();
    return curSelection;
  }

  @Override
  public boolean isSelected(T item) {
    resolveChanges();
    if (curSelection == null || curKey == null || item == null) {
      return false;
    }
    return curKey.equals(getKey(item));
  }

  @Override
  public void setSelected(T item, boolean selected) {
    // If we are deselecting an item that isn't actually selected, ignore it.
    if (!selected) {
      Object oldKey = newSelectedPending ? getKey(newSelectedItem) : curKey;
      Object newKey = getKey(item);
      if (!equalsOrBothNull(oldKey, newKey)) {
        return;
      }
    }
    newSelectedItem = item;
    newSelected = selected;
    newSelectedPending = true;
    scheduleSelectionChangeEvent();
  }

  @Override
  protected void fireSelectionChangeEvent() {
    if (isEventScheduled()) {
      setEventCancelled(true);
    }
    resolveChanges();
  }

  private boolean equalsOrBothNull(Object a, Object b) {
    return (a == null) ? (b == null) : a.equals(b);
  }

  private void resolveChanges() {
    if (!newSelectedPending) {
      return;
    }

    Object key = getKey(newSelectedItem);
    boolean sameKey = equalsOrBothNull(curKey, key);
    boolean changed = false;
    if (newSelected) {
      changed = !sameKey;
      curSelection = newSelectedItem;
      curKey = key;
    } else if (sameKey) {
      changed = true;
      curSelection = null;
      curKey = null;
    }

    newSelectedItem = null;
    newSelectedPending = false;

    // Fire a selection change event.
    if (changed) {
      SelectionChangeEvent.fire(this);
    }
  }
}
