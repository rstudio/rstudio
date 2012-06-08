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

import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.dev.util.collect.IdentityHashSet;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Sets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Represents a single named deferred binding or configuration property that can
 * answer with its value. The BindingProperty maintains two sets of values, the
 * "defined" set and the "allowed" set. The allowed set must always be a subset
 * of the defined set.
 */
public class BindingProperty extends Property {
  public static final String GLOB_STAR = "*";
  private static final String EMPTY = "";

  private List<SortedSet<String>> collapsedValues = Lists.create();
  private final Map<Condition, SortedSet<String>> conditionalValues = new LinkedHashMap<Condition, SortedSet<String>>();
  private final SortedSet<String> definedValues = new TreeSet<String>();
  private PropertyProvider provider;
  private Class<? extends PropertyProviderGenerator> providerGenerator;
  private String fallback;
  private HashMap<String,LinkedList<LinkedHashSet<String>>> fallbackValueMap;
  private HashMap<String,LinkedList<String>> fallbackValues = new HashMap<String,LinkedList<String>>();
  private final ConditionAll rootCondition = new ConditionAll();

  {
    conditionalValues.put(rootCondition, new TreeSet<String>());
  }

  public BindingProperty(String name) {
    super(name);
    fallback = EMPTY;
  }

  /**
   * Add an equivalence set of property values.
   */
  public void addCollapsedValues(String... values) {

    // Sanity check caller
    for (String value : values) {
      if (value.contains(GLOB_STAR)) {
        // Expanded in normalizeCollapsedValues()
        continue;
      } else if (!definedValues.contains(value)) {
        throw new IllegalArgumentException(
            "Attempting to collapse unknown value " + value);
      }
    }

    // We want a mutable set, because it simplifies normalizeCollapsedValues
    SortedSet<String> temp = new TreeSet<String>(Arrays.asList(values));
    collapsedValues = Lists.add(collapsedValues, temp);
  }

  public void addDefinedValue(Condition condition, String newValue) {
    definedValues.add(newValue);
    SortedSet<String> set = conditionalValues.get(condition);
    if (set == null) {
      set = new TreeSet<String>();
      set.addAll(conditionalValues.get(rootCondition));
      conditionalValues.put(condition, set);
    }
    set.add(newValue);
  }

  /**
   * Adds fall back value to given property name. 
   * @param value the property value.
   * @param fallbackValue the fall back value for given property value.
   */
  public void addFallbackValue(String value, String fallbackValue) {
    LinkedList<String> values = fallbackValues.get(fallbackValue);
    if (values == null) {
      values = new LinkedList<String>();
      fallbackValues.put(fallbackValue, values);
    }
    values.addFirst(value);
  }

  /**
   * Returns the set of allowed values in sorted order when a certain condition
   * is satisfied.
   */
  public String[] getAllowedValues(Condition condition) {
    Set<String> allowedValues = conditionalValues.get(condition);
    return allowedValues.toArray(new String[allowedValues.size()]);
  }

  public List<SortedSet<String>> getCollapsedValues() {
    return collapsedValues;
  }

  public Map<Condition, SortedSet<String>> getConditionalValues() {
    return Collections.unmodifiableMap(conditionalValues);
  }

  /**
   * If the BindingProperty has exactly one value across all conditions and
   * permutations, return that value otherwise return <code>null</code>.
   */
  public String getConstrainedValue() {
    String constrainedValue = null;
    for (SortedSet<String> allowedValues : conditionalValues.values()) {
      if (allowedValues.size() != 1) {
        return null;
      } else if (constrainedValue == null) {
        constrainedValue = allowedValues.iterator().next();
      } else if (!constrainedValue.equals(allowedValues.iterator().next())) {
        return null;
      }
    }
    return constrainedValue;
  }

  /**
   * Returns the set of defined values in sorted order.
   */
  public String[] getDefinedValues() {
    return definedValues.toArray(new String[definedValues.size()]);
  }

  /**
   * Returns the fallback value for this property, or the empty string if none.
   * 
   * @return the fallback value
   */
  public String getFallback() {
    return fallback;
  }

  /**
   * Returns the map of values to fall back values. the list of fall
   * back values is in decreasing order of preference.
   * @return map of property value to fall back values.
   */
  public Map<String,? extends List<? extends Set<String>>> getFallbackValuesMap() {
    if (fallbackValueMap == null) {
      HashMap<String,LinkedList<LinkedHashSet<String>>> valuesMap = new HashMap<String,LinkedList<LinkedHashSet<String>>>();
      // compute closure of fall back values preserving order
      for (Entry<String, LinkedList<String>> e : fallbackValues.entrySet()) {
        String from = e.getKey();
        LinkedList<LinkedHashSet<String>> alternates = new LinkedList<LinkedHashSet<String>>();
        valuesMap.put(from, alternates);
        LinkedList<String> childList = fallbackValues.get(from);
        LinkedHashSet<String> children = new LinkedHashSet<String>();
        children.addAll(childList);
        while (children != null && children.size() > 0) {
          alternates.add(children);
          LinkedHashSet<String> newChildren = new LinkedHashSet<String>();
          for (String child : children) {
            childList = fallbackValues.get(child);
            if (null == childList) {
              continue;
            }
            for (String val : childList) {
              newChildren.add(val);
            }
          }
          children = newChildren;
        }
      }
      fallbackValueMap = valuesMap;
    }
    return fallbackValueMap;
  }
  
  public PropertyProvider getProvider() {
    return provider;
  }

  /**
   * @return the the provider generator class, or null if none.
   */
  public Class<? extends PropertyProviderGenerator> getProviderGenerator() {
    return providerGenerator;
  }

  public Set<String> getRequiredProperties() {
    Set<String> toReturn = Sets.create();
    for (Condition cond : conditionalValues.keySet()) {
      toReturn = Sets.addAll(toReturn, cond.getRequiredProperties());
    }
    return toReturn;
  }

  public ConditionAll getRootCondition() {
    return rootCondition;
  }

  /**
   * Returns true if the supplied value is legal under some condition.
   */
  public boolean isAllowedValue(String value) {
    for (Set<String> values : conditionalValues.values()) {
      if (values.contains(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns <code>true</code> if the value was previously provided to
   * {@link #addDefinedValue(Condition,String)}.
   */
  public boolean isDefinedValue(String value) {
    return definedValues.contains(value);
  }

  /**
   * Returns <code>true</code> if the value of this BindingProperty is always
   * derived from other BindingProperties. That is, for each Condition in the
   * BindingProperty, there is exactly one allowed value.
   */
  public boolean isDerived() {
    for (Set<String> allowedValues : conditionalValues.values()) {
      if (allowedValues.size() != 1) {
        return false;
      }
    }
    return true;
  }

  /**
   * Set the currently allowed values. The values provided must be a subset of
   * the currently-defined values.
   * 
   * @throws IllegalArgumentException if any of the provided values were not
   *     provided to {@link #addDefinedValue(Condition,String)}.
   */
  public void setAllowedValues(Condition condition, String... values) {
    SortedSet<String> temp = new TreeSet<String>(Arrays.asList(values));
    if (!definedValues.containsAll(temp)) {
      throw new IllegalArgumentException(
          "Attempted to set an allowed value that was not previously defined");
    }

    // XML has a last-one-wins semantic which we reflect in our evaluation order
    if (condition == rootCondition) {
      /*
       * An unconditional set-property would undo any previous conditional
       * setters, so we can just clear out this map.
       */
      conditionalValues.clear();
    } else {
      /*
       * Otherwise, we'll just ensure that this condition is moved to the end.
       */
      conditionalValues.remove(condition);
    }
    conditionalValues.put(condition, temp);
  }

  public void setFallback(String token) {
    fallback = token;
  }

  public void setProvider(PropertyProvider provider) {
    this.provider = provider;
  }

  /**
   * Set a provider generator for this property.
   * 
   * @param generator
   */
  public void setProviderGenerator(Class<? extends PropertyProviderGenerator> generator) {
    providerGenerator = generator;
  }

  /**
   * Create a minimal number of equivalence sets, expanding any glob patterns.
   */
  void normalizeCollapsedValues() {
    if (collapsedValues.isEmpty()) {
      return;
    }

    // Expand globs
    for (Set<String> set : collapsedValues) {
      // Compile a regex that matches all glob expressions that we see
      StringBuilder pattern = new StringBuilder();
      for (Iterator<String> it = set.iterator(); it.hasNext();) {
        String value = it.next();
        if (value.contains(GLOB_STAR)) {
          it.remove();
          if (pattern.length() > 0) {
            pattern.append("|");
          }

          // a*b ==> (a.*b)
          pattern.append("(");
          // We know value is a Java ident, so no special escaping is needed
          pattern.append(value.replace(GLOB_STAR, ".*"));
          pattern.append(")");
        }
      }

      if (pattern.length() == 0) {
        continue;
      }

      Pattern p = Pattern.compile(pattern.toString());
      for (String definedValue : definedValues) {
        if (p.matcher(definedValue).matches()) {
          set.add(definedValue);
        }
      }
    }

    // Minimize number of sets

    // Maps a value to the set that contains that value
    Map<String, SortedSet<String>> map = new HashMap<String, SortedSet<String>>();

    // For each equivalence set we have
    for (SortedSet<String> set : collapsedValues) {
      // Examine each original value in the set
      for (String value : new LinkedHashSet<String>(set)) {
        // See if the value was previously assigned to another set
        SortedSet<String> existing = map.get(value);
        if (existing == null) {
          map.put(value, set);
        } else {
          // If so, merge the existing set into this one and update pointers
          set.addAll(existing);
          for (String mergedValue : existing) {
            map.put(mergedValue, set);
          }
        }
      }
    }

    // The values of the maps will now contain the minimal number of sets
    collapsedValues = new ArrayList<SortedSet<String>>(
        new IdentityHashSet<SortedSet<String>>(map.values()));

    // Sort the list
    Lists.sort(collapsedValues, new Comparator<SortedSet<String>>() {
      public int compare(SortedSet<String> o1, SortedSet<String> o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        assert !s1.equals(s2) : "Should not have seen equal sets";
        return s1.compareTo(s2);
      }
    });
  }
}
