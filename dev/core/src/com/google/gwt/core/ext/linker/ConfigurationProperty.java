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

import java.util.List;

/**
 * Represents a configuration property. These properties do not affect
 * deferred-binding decisions, but may affect the behavior of Linkers and
 * Generators.
 */
public interface ConfigurationProperty {
  /*
   * NB: This is not a super-interface of SelectionProperty, since these kinds
   * of properties are always guaranteed to have a defined value. The only
   * commonality is getName() which doesn't seem all that useful to extract.
   */

  /**
   * Returns the name of the configuration property.
   */
  String getName();
  
  /**
   * Returns the defined value for the configuration property.  If the property
   * has multiple values, this returns the first value only.
   */
  @Deprecated
  String getValue();
  
  /**
   * Returns the defined values for the configuration property as a List
   * of Strings.
   */
  List<String> getValues();

  /**
   * Returns true if this property has more than one value.
   */
  boolean hasMultipleValues();
}
