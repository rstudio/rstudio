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

import com.google.gwt.bikeshed.list.shared.SelectionModel.AbstractSelectionModel;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SelectionModel} that allows records to be selected according to
 * a subclass-defined rule, plus a list of positive or negative exceptions.
 *
 * @param <T> the data type of records in the list
 */
public abstract class DefaultSelectionModel<T> extends
    AbstractSelectionModel<T> {

  private final Map<T, Boolean> exceptions = new HashMap<T, Boolean>();

  /**
   * Removes all exceptions.
   */
  public void clearExceptions() {
    exceptions.clear();
    scheduleSelectionChangeEvent();
  }

  /**
   * Returns true if the given object should be selected by default.
   * Subclasses implement this method in order to define the default
   * selection behavior.
   */
  public abstract boolean isDefaultSelected(T object);

  /**
   * If the given object is marked as an exception, return the exception
   * value.  Otherwise, return the value of isDefaultSelected for the given
   * object.
   */
  public boolean isSelected(T object) {
    // Check exceptions first
    Boolean exception = exceptions.get(object);
    if (exception != null) {
      return exception.booleanValue();
    }
    // If not in exceptions, return the default
    return isDefaultSelected(object);
  }

  /**
   * Sets an object's selection state.  If the object is currently marked
   * as an exception, and the new selected state differs from the previous
   * selected state, the object is removed from the list of exceptions.
   * Otherwise, the object is added to the list of exceptions with the given
   * selected state.
   */
  public void setSelected(T object, boolean selected) {
    Boolean currentlySelected = exceptions.get(object);
    if (currentlySelected != null
        && currentlySelected.booleanValue() != selected) {
      exceptions.remove(object);
    } else {
      exceptions.put(object, selected);
    }

    scheduleSelectionChangeEvent();
  }

  /**
   * Copies the exceptions map into a user-supplied map.
   */
  protected void getExceptions(Map<T, Boolean> output) {
    output.clear();
    output.putAll(exceptions);
  }
}
