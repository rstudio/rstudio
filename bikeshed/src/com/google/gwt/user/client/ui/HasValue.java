/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;

/**
 * Extends {@link TakesValue} to allow the value to be pulled back out, and to
 * throw {@ValueChanged} events.
 * <p>
 * An object that implements this interface should be a user input widget, where
 * the user and programmer can both set and get the object's value. It is
 * intended to provide a unified interface to widgets with "atomic" values, like
 * Strings and Dates.
 * 
 * @param <T> the type of value.
 */
public interface HasValue<T> extends TakesValue<T>, HasValueChangeHandlers<T> {

  /**
   * Gets this object's value.
   * 
   * @return the object's value
   */
  T getValue();

  /**
   * Sets this object's value without firing any events. This should be
   * identical to calling setValue(value, false).
   * <p>
   * It is acceptable to fail assertions or throw (documented) unchecked
   * exceptions in response to bad values.
   * <p>
   * By convention, GWT widgets that can be cleared accept null for
   * <code>value</code>, but it is acceptable for widgets that cannot be cleared
   * to throw an exception for null values.
   * 
   * @param value the object's new value
   */
  void setValue(T value);

  /**
   * Sets this object's value. Fires
   * {@link com.google.gwt.event.logical.shared.ValueChangeEvent} when
   * fireEvents is true and the new value does not equal the existing value.
   * <p>
   * It is acceptable to fail assertions or throw (documented) unchecked
   * exceptions in response to bad values.
   * 
   * @param value the object's new value
   * @param fireEvents fire events if true and value is new
   */
  void setValue(T value, boolean fireEvents);
}
