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

import com.google.gwt.view.client.MultiSelectionModel.SelectionChange;
import com.google.gwt.view.client.SelectionModel.AbstractSelectionModel;

import java.util.HashMap;
import java.util.Map;

/**
 * A convenience {@link SelectionModel} that allows items to be selected
 * according to a subclass-defined rule, plus a list of positive or negative
 * exceptions.
 * 
 * @param <T> the data type of records in the list
 */
public abstract class DefaultSelectionModel<T> extends AbstractSelectionModel<T> {

  private final Map<Object, Boolean> exceptions = new HashMap<Object, Boolean>();

  /**
   * A map of keys to the item and its pending selection state.
   */
  private final Map<Object, SelectionChange<T>> selectionChanges =
      new HashMap<Object, SelectionChange<T>>();

  /**
   * Constructs a DefaultSelectionModel without a key provider.
   */
  public DefaultSelectionModel() {
    super(null);
  }

  /**
   * Constructs a DefaultSelectionModel with the given key provider.
   * 
   * @param keyProvider an instance of ProvidesKey<T>, or null if the item
   *          should act as its own key
   */
  public DefaultSelectionModel(ProvidesKey<T> keyProvider) {
    super(keyProvider);
  }

  /**
   * Removes all exceptions.
   */
  public void clearExceptions() {
    exceptions.clear();
    selectionChanges.clear();
    scheduleSelectionChangeEvent();
  }

  /**
   * Returns true if the given item should be selected by default. Subclasses
   * implement this method in order to define the default selection behavior.
   * 
   * @param item an object of this {@link SelectionModel}'s type
   * @return true if the item should be selected by default
   */
  public abstract boolean isDefaultSelected(T item);

  /**
   * If the given item is marked as an exception, return the exception value.
   * Otherwise, return the value of isDefaultSelected for the given item.
   */
  @Override
  public boolean isSelected(T item) {
    resolveChanges();

    // Check exceptions first
    Object key = getKey(item);
    Boolean exception = exceptions.get(key);
    if (exception != null) {
      return exception.booleanValue();
    }
    // If not in exceptions, return the default
    return isDefaultSelected(item);
  }

  /**
   * Sets an item's selection state. If the item is currently marked as an
   * exception, and the new selected state differs from the previous selected
   * state, the object is removed from the list of exceptions. Otherwise, the
   * object is added to the list of exceptions with the given selected state.
   */
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

  /**
   * Copies the exceptions map into a user-supplied map.
   * 
   * @param output the user supplied map
   * @return the user supplied map
   */
  protected Map<Object, Boolean> getExceptions(Map<Object, Boolean> output) {
    output.clear();
    output.putAll(exceptions);
    return output;
  }

  private void resolveChanges() {
    boolean changed = false;
    for (Map.Entry<Object, SelectionChange<T>> entry : selectionChanges.entrySet()) {
      Object key = entry.getKey();
      SelectionChange<T> value = entry.getValue();
      T item = value.getItem();
      boolean selected = value.isSelected();
      boolean defaultSelected = isDefaultSelected(item);
      Boolean previousException = exceptions.get(key);

      if (defaultSelected == selected) {
        // Not an exception, remove from exceptions table if present
        if (previousException != null) {
          exceptions.remove(key);
          changed = true;
        }
      } else {
        // Add as an exception if not already there
        if (previousException != Boolean.valueOf(selected)) {
          exceptions.put(key, selected);
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
