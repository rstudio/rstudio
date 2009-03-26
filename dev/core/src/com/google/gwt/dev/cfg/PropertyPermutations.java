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
package com.google.gwt.dev.cfg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Generates all possible permutations of properties in a module. Each
 * permutation consists of the list of active property values associated with
 * that permutation. That list of property values is represented as an array of
 * Strings corresponding to the list of properties returned by
 * {@link Properties#getBindingProperties()}.
 */
public class PropertyPermutations implements Iterable<String[]> {

  /**
   * Returns the list of all permutations. This method must return results in a
   * consistently sorted order over multiple invocations.
   */
  private static List<String[]> allPermutationsOf(Properties properties) {
    BindingProperty[] bindingProperties = getOrderedPropertiesOf(properties);

    int permCount = properties.numPermutations();

    List<String[]> permutations = new ArrayList<String[]>(permCount);
    if (bindingProperties.length > 0) {
      permute(bindingProperties, null, 0, permutations);
      assert (permCount == permutations.size());
    } else {
      permutations.add(new String[0]);
    }
    return permutations;
  }

  private static BindingProperty[] getOrderedPropertiesOf(Properties properties) {
    SortedSet<BindingProperty> bindingProps = properties.getBindingProperties();
    return bindingProps.toArray(new BindingProperty[bindingProps.size()]);
  }

  private static void permute(BindingProperty[] properties, String[] soFar,
      int whichProp, List<String[]> permutations) {
    int lastProp = properties.length - 1;

    BindingProperty prop = properties[whichProp];
    String[] options = prop.getAllowedValues();

    for (int i = 0; i < options.length; i++) {
      String knownValue = options[i];

      String[] nextStep = new String[whichProp + 1];
      if (whichProp > 0) {
        System.arraycopy(soFar, 0, nextStep, 0, soFar.length);
      }
      nextStep[whichProp] = knownValue;

      if (whichProp < lastProp) {
        permute(properties, nextStep, whichProp + 1, permutations);
      } else {
        // Finished this permutation.
        permutations.add(nextStep);
      }
    }
  }

  private final Properties properties;
  private final List<String[]> values;

  public PropertyPermutations(Properties properties) {
    this.properties = properties;
    this.values = allPermutationsOf(properties);
  }

  public PropertyPermutations(Properties properties, int firstPerm, int numPerms) {
    this.properties = properties;
    values = allPermutationsOf(properties).subList(firstPerm,
        firstPerm + numPerms);
  }

  public BindingProperty[] getOrderedProperties() {
    return getOrderedPropertiesOf(properties);
  }

  public String[] getOrderedPropertyValues(int permutation) {
    return values.get(permutation);
  }

  /**
   * Enumerates each permutation as an array of strings such that the index of
   * each string in the array corresponds to the property at the same index in
   * the array returned from {@link #getOrderedProperties()}.
   */
  public Iterator<String[]> iterator() {
    return values.iterator();
  }

  public int size() {
    return values.size();
  }
}
