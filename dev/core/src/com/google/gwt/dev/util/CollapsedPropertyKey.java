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
package com.google.gwt.dev.util;

import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.BindingProperties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.SortedSetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.TreeMultimap;

import java.util.List;
import java.util.Set;

/**
 * Creates a string representation of the binding property key/value pairs used
 * in a Permutation. The value of a collapsed property will be represented by
 * the set of equivalent values.
 * <p>
 * Assume that the <code>safari</code> and <code>ie8</code>
 * <code>user.agent</code> values have been collapsed. Instead of printing
 * <code>user.agent=safari</code>, this class will use
 * <code>user.agent = { ie8, safari }</code>.
 */
public class CollapsedPropertyKey extends StringKey {
  /**
   * Create the string key for a collection of property oracles.
   */
  private static String collapse(List<BindingProperties> allPropertiesSets) {
    // The map used to create the string key
    SortedSetMultimap<String, String> propertyValuesByPropertyName = TreeMultimap.create();
    for (BindingProperties bindingProperties : allPropertiesSets) {
      BindingProperty[] properties = bindingProperties.getOrderedProps();
      String[] values = bindingProperties.getOrderedPropValues();
      for (int i = 0; i < properties.length; i++) {
        BindingProperty property = properties[i];
        String value = values[i];

        // For "hard" properties, add the singleton value.
        propertyValuesByPropertyName.put(property.getName(), value);

        // Iterate over the equivalence sets defined in the property
        for (Set<String> collapsedValues : property.getCollapsedValuesSets()) {
          if (collapsedValues.contains(value)) {
          /*
           * If we find a set that contains the current value, add all the
           * values in the set. This accounts for the transitive nature of
           * equality.
           */
            propertyValuesByPropertyName.putAll(property.getName(), collapsedValues);
          }
        }
      }
    }
    return propertyValuesByPropertyName.toString();
  }

  private final Permutation permutation;

  /**
   * Constructor that constructs a key containing all collapsed property/value
   * pairs used by a Permutation. The given Permutation can be retrieved later
   * through {@link #getPermutation()}.
   */
  public CollapsedPropertyKey(Permutation permutation) {
    super(collapse(permutation.getProperties().getSoftProperties()));
    this.permutation = permutation;
  }

  /**
   * Creates a key based on all collapsed property/value pairs for a single permutation.
   */
  public CollapsedPropertyKey(BindingProperties props) {
    super(collapse(ImmutableList.of(props)));
    this.permutation = null;
  }

  public Permutation getPermutation() {
    return permutation;
  }
}
