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
import java.util.Map;

/**
 * A convenience {@link SelectionModel} that allows records to be selected
 * according to a subclass-defined rule, plus a list of positive or negative
 * exceptions.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <T> the data type of records in the list
 */
public abstract class DefaultSelectionModel<T> extends AbstractSelectionModel<
    T> {

  private final Map<Object, Boolean> exceptions = new HashMap<Object, Boolean>();

  // Changes to be propagated into exceptions map
  private final HashMap<T, Boolean> selectionChanges = new HashMap<T, Boolean>();

  /**
   * Removes all exceptions.
   */
  public void clearExceptions() {
    exceptions.clear();
    selectionChanges.clear();
    scheduleSelectionChangeEvent();
  }

  /**
   * Returns true if the given object should be selected by default. Subclasses
   * implement this method in order to define the default selection behavior.
   */
  public abstract boolean isDefaultSelected(T object);

  /**
   * If the given object is marked as an exception, return the exception value.
   * Otherwise, return the value of isDefaultSelected for the given object.
   */
  public boolean isSelected(T object) {
    resolveChanges();

    // Check exceptions first
    Object key = getKey(object);
    Boolean exception = exceptions.get(key);
    if (exception != null) {
      return exception.booleanValue();
    }
    // If not in exceptions, return the default
    return isDefaultSelected(object);
  }

  /**
   * Sets an object's selection state. If the object is currently marked as an
   * exception, and the new selected state differs from the previous selected
   * state, the object is removed from the list of exceptions. Otherwise, the
   * object is added to the list of exceptions with the given selected state.
   */
  public void setSelected(T object, boolean selected) {
    selectionChanges.put(object, selected);
    scheduleSelectionChangeEvent();
  }

  // Coalesce selection changes since the last event firing
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
    for (Map.Entry<T, Boolean> entry : selectionChanges.entrySet()) {
      T object = entry.getKey();
      boolean selected = entry.getValue();
      boolean defaultSelected = isDefaultSelected(object);
      Object key = getKey(object);
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
