/*
 * Copyright 2009 Google Inc.
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

package com.google.gwt.core.ext;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * A named deferred binding (property, value) pair for use in generators.
 * 
 * @see com.google.gwt.core.ext.linker.SelectionProperty A similarly-named
 * analog for linkers.
 */
public interface SelectionProperty {
  
  /**
   * The name of the property.
   * 
   * @return the property name as a String.
   * */
  String getName();

  /**
   * The value for the permutation currently being considered.
   * 
   * @return the property value as a String.
   */
  String getCurrentValue();

  /**
   * Gets the fallback value for the property.
   * @return the fallback, or ""
   */
  String getFallbackValue(); 

  /**
   * Returns the list of fall back values for a given value. 
   * @param value the property value
   * @return the fall back list of values by increasing order
   *         of preference.
   */
  List<? extends Set<String>> getFallbackValues(String value);

  /**
   * Returns the possible values for the property in sorted order.
   * 
   * @return a SortedSet of Strings containing the possible property values.
   */
  SortedSet<String> getPossibleValues();
}
