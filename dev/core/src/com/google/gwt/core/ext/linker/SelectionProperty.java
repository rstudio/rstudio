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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.SortedSet;

/**
 * Represents a deferred binding property. The deferred binding property may or
 * may not have a single value applied across all permutations.
 *
 * SelectionProperty implementations must support object identity comparisons.
 *
 * @see com.google.gwt.core.ext.SelectionProperty A similarly-named interface
 *      used in generators.
 */
public interface SelectionProperty {

  /**
   * Returns the fallback value or an empty string if not defined.
   */
  String getFallbackValue();

  /**
   * Returns the name of the deferred binding property.
   */
  String getName();

  /**
   * Returns all possible values for this deferred binding property.
   */
  SortedSet<String> getPossibleValues();

  /**
   * Returns a raw function body that provides the runtime value to be used for
   * a deferred binding property.
   *
   * @param logger logger to use for any warnings/errors
   * @param configProperties set of configuration properties
   * @throws UnableToCompleteException if execution cannot continue, after
   *     having logged a message
   */
  String getPropertyProvider(TreeLogger logger,
      SortedSet<ConfigurationProperty> configProperties)
      throws UnableToCompleteException;

  /**
   * Returns <code>true</code> if the value of the SelectionProperty is always
   * derived from other SelectionProperties and, as a consequence, the property
   * provider never needs to be evaluated.
   */
  boolean isDerived();

  /**
   * Returns the defined value for the deferred binding property or
   * <code>null</code> if the value of the property is not constant.
   *
   * @see CompilationResult#getPropertyMap()
   */
  String tryGetValue();
}