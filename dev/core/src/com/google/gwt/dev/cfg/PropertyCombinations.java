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

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.CollapsedPropertyKey;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Generates all possible permutations of properties in a module. Each
 * permutation consists of the list of active property values associated with
 * that permutation. That list of property values is represented as an array of
 * Strings corresponding to the list of properties returned by
 * {@link Properties#getBindingProperties()}.
 */
public class PropertyCombinations implements Iterable<String[]> {

  /**
   * A bundle of the ordered names of binding and configuration properties and a single set value
   * for each that together uniquely identify a particular permutation.
   */
  public static class PermutationDescription {

    private List<String> bindingPropertyNames = Lists.newArrayList();
    private List<String> configurationPropertyNames = Lists.newArrayList();
    private List<String> propertyValues = Lists.newArrayList();

    @Override
    public int hashCode() {
      return Objects.hashCode(bindingPropertyNames, configurationPropertyNames, propertyValues);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof PermutationDescription) {
        PermutationDescription that = (PermutationDescription) obj;
        return Objects.equal(this.bindingPropertyNames, that.bindingPropertyNames)
            && Objects.equal(this.configurationPropertyNames, that.configurationPropertyNames)
            && Objects.equal(this.propertyValues, that.propertyValues);
      }
      return false;
    }

    @Override
    public String toString() {
      return String.format(
          "PermutationDescription [bindingPropertyNames=%s, configurationPropertyNames=%s, "
          + "propertyValues=%s]", bindingPropertyNames, configurationPropertyNames, propertyValues);
    }
  }

  /**
   * Returns the list of all permutations. This method must return results in a
   * consistently sorted order over multiple invocations.
   */
  private static List<String[]> allPermutationsOf(Properties properties,
      Set<String> activeLinkerNames) {
    BindingProperty[] bindingProperties = getOrderedPropertiesOf(properties);

    List<String[]> permutations = new ArrayList<String[]>();
    if (bindingProperties.length > 0) {
      permute(bindingProperties, activeLinkerNames, null, 0, permutations);
    } else {
      permutations.add(new String[0]);
    }
    return permutations;
  }

  private static BindingProperty[] getOrderedPropertiesOf(Properties properties) {
    /*
     * We delete items from this set, but want to retain the original order as
     * much as possible.
     */
    Set<BindingProperty> bindingProperties = Sets.newLinkedHashSet(
        properties.getBindingProperties());

    // Accumulates the order in which the properties should be evaluated
    Map<String, BindingProperty> evaluationOrder = Maps.newLinkedHashMap();

    /*
     * Insert a property after all of the properties that it depends upon have
     * been inserted.
     */
    while (!bindingProperties.isEmpty()) {
      boolean changed = false;

      for (Iterator<BindingProperty> it = bindingProperties.iterator(); it.hasNext();) {
        BindingProperty prop = it.next();

        Set<String> deps = prop.getRequiredProperties();
        if (evaluationOrder.keySet().containsAll(deps)) {
          it.remove();
          evaluationOrder.put(prop.getName(), prop);
          changed = true;
        }
      }

      if (!changed) {
        throw new IllegalStateException(
            "Cycle detected within remaining property dependencies "
                + bindingProperties.toString());
      }
    }

    return evaluationOrder.values().toArray(
        new BindingProperty[evaluationOrder.size()]);
  }

  private static void permute(BindingProperty[] properties,
      Set<String> activeLinkerNames, String[] soFar, int whichProp,
      List<String[]> permutations) {
    int lastProp = properties.length - 1;

    BindingProperty prop = properties[whichProp];

    // Find the last-one-wins Condition
    Condition winner = null;
    if (prop.getConditionalValues().size() == 1) {
      winner = prop.getRootCondition();
    } else {
      BindingProperty[] answerable = new BindingProperty[soFar.length];
      System.arraycopy(properties, 0, answerable, 0, soFar.length);
      PropertyOracle propertyOracle = new BindingProperties(answerable, soFar,
              ConfigurationProperties.EMPTY).toPropertyOracle();

      for (Condition cond : prop.getConditionalValues().keySet()) {
        try {
          if (cond.isTrue(TreeLogger.NULL, new DeferredBindingQuery(
              propertyOracle, activeLinkerNames))) {
            winner = cond;
          }
        } catch (UnableToCompleteException e) {
          throw new IllegalStateException("Should never get here for simple properties", e);
        }
      }
    }

    assert winner != null;

    String[] options = prop.getGeneratedValues(winner);
    for (int i = 0; i < options.length; i++) {
      String knownValue = options[i];

      String[] nextStep = new String[whichProp + 1];
      if (whichProp > 0) {
        System.arraycopy(soFar, 0, nextStep, 0, soFar.length);
      }
      nextStep[whichProp] = knownValue;

      if (whichProp < lastProp) {
        permute(properties, activeLinkerNames, nextStep, whichProp + 1,
            permutations);
      } else {
        // Finished this permutation.
        permutations.add(nextStep);
      }
    }
  }

  private final Properties properties;
  private final List<String[]> values;

  public PropertyCombinations(Properties properties,
      Set<String> activeLinkerNames) {
    this.properties = properties;
    this.values = allPermutationsOf(properties, activeLinkerNames);
  }

  /**
   * Copy constructor that allows the list of property values to be reset.
   */
  public PropertyCombinations(PropertyCombinations allPermutations,
      List<String[]> values) {
    this.properties = allPermutations.properties;
    this.values = values;
  }

  /**
   * Return a list of PropertyPermutations that represent the hard permutations
   * that result from collapsing the soft properties in the
   * PropertyPermutation's Properties object.
   */
  public List<PropertyCombinations> collapseProperties() {
    // Collate property values in this map
    SortedMap<CollapsedPropertyKey, List<String[]>> map = Maps.newTreeMap();

    BindingProperty[] propertyKeys = getOrderedProperties();
    // Loop over all possible property value permutations
    for (Iterator<String[]> it = iterator(); it.hasNext();) {
      String[] propertyValues = it.next();
      assert propertyValues.length == propertyKeys.length;

      BindingProperties bindingProperties =
          new BindingProperties(propertyKeys, propertyValues, ConfigurationProperties.EMPTY);
      CollapsedPropertyKey key = new CollapsedPropertyKey(bindingProperties);

      List<String[]> list = map.get(key);
      if (list == null) {
        list = Lists.newArrayList();
        map.put(key, list);
      }
      list.add(propertyValues);
    }

    // Return the collated values
    List<PropertyCombinations> toReturn = new ArrayList<PropertyCombinations>(
        map.size());
    for (List<String[]> list : map.values()) {
      toReturn.add(new PropertyCombinations(this, list));
    }

    return toReturn;
  }

  /**
   * Returns the properties used to generate permutations.
   * (Parallel to {@link #getOrderedPropertyValues}.)
   */
  public BindingProperty[] getOrderedProperties() {
    return getOrderedPropertiesOf(properties);
  }

  /**
   * Returns the value of each property used to generate the given permutation.
   * (Parallel to {@link #getOrderedProperties()}.)
   */
  public String[] getOrderedPropertyValues(int permutation) {
    return values.get(permutation);
  }

  /**
   * Returns a unique description for the given permutation.
   */
  public PermutationDescription getPermutationDescription(int permutationId) {
    PermutationDescription permutationDescription = new PermutationDescription();
    for (BindingProperty bindingProperty : properties.getBindingProperties()) {
      permutationDescription.bindingPropertyNames.add(bindingProperty.getName());
    }
    for (ConfigurationProperty configurationProperty : properties.getConfigurationProperties()) {
      permutationDescription.configurationPropertyNames.add(configurationProperty.getName());
    }
    permutationDescription.propertyValues.addAll(Arrays.asList(this.values.get(permutationId)));
    return permutationDescription;
  }

  /**
   * Enumerates each permutation as an array of strings such that the index of
   * each string in the array corresponds to the property at the same index in
   * the array returned from {@link #getOrderedProperties()}.
   */
  @Override
  public Iterator<String[]> iterator() {
    return values.iterator();
  }

  public int size() {
    return values.size();
  }
}
