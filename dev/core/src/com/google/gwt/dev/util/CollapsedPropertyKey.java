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

import com.google.common.collect.ImmutableList;
import com.google.gwt.dev.Permutation;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.BindingProps;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
  private static String collapse(List<BindingProps> allProps) {
    // The map used to create the string key
    SortedMap<String, SortedSet<String>> collapsedPropertyMap = new TreeMap<String, SortedSet<String>>();
    for (BindingProps bindingProps : allProps) {
      BindingProperty[] props = bindingProps.getOrderedProps();
      String[] values = bindingProps.getOrderedPropValues();
      for (int i = 0; i < props.length; i++) {
        BindingProperty prop = props[i];
        String value = values[i];
        boolean isCollapsed = false;

        // Iterate over the equivalence sets defined in the property
        for (Set<String> equivalenceSet : prop.getCollapsedValues()) {
          if (equivalenceSet.contains(value)) {
            /*
             * If we find a set that contains the current value, add all the
             * values in the set. This accounts for the transitive nature of
             * equality.
             */
            SortedSet<String> toAdd = collapsedPropertyMap.get(prop.getName());
            if (toAdd == null) {
              toAdd = new TreeSet<String>();
              collapsedPropertyMap.put(prop.getName(), toAdd);
              isCollapsed = true;
            }
            toAdd.addAll(equivalenceSet);
          }
        }
        if (!isCollapsed) {
          // For "hard" properties, add the singleton value
          collapsedPropertyMap.put(prop.getName(), new TreeSet<String>(
              Arrays.asList(value)));
        }
      }
    }
    return collapsedPropertyMap.toString();
  }

  private final Permutation permutation;

  /**
   * Constructor that constructs a key containing all collapsed property/value
   * pairs used by a Permutation. The given Permutation can be retrieved later
   * through {@link #getPermutation()}.
   */
  public CollapsedPropertyKey(Permutation permutation) {
    super(collapse(permutation.getProps().getSoftProps()));
    this.permutation = permutation;
  }

  /**
   * Creates a key based on all collapsed property/value pairs for a single permutation.
   */
  public CollapsedPropertyKey(BindingProps props) {
    super(collapse(ImmutableList.of(props)));
    this.permutation = null;
  }

  public Permutation getPermutation() {
    return permutation;
  }
}
