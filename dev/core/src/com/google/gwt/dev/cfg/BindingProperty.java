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
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
  private final SortedSet<String> definedValues = new TreeSet<String>();
  private String fallback;
  private HashMap<String,LinkedList<LinkedHashSet<String>>> fallbackValueMap;
  private HashMap<String,LinkedList<String>> fallbackValues = new HashMap<String,LinkedList<String>>();
  private PropertyProvider provider;
  private Class<? extends PropertyProviderGenerator> providerGenerator;
  private final ConditionAll rootCondition = new ConditionAll();

  /**
   * The binding values that are allowed for each condition.
   * (Used to determine what the properties were set to in the module file.)
   */
  private final ConditionalValues allowedValues = new ConditionalValues(rootCondition);

  /**
   * The binding values that we need to generate code for. (Affects the number of permutations.)
   * In a normal compile, this is the same as allowedValues, but in some compilation modes,
   * it's changed to restrict permutations. (For example, in Super Dev Mode.)
   */
  private final ConditionalValues generatedValues = new ConditionalValues(rootCondition);

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

  public void addDefinedValue(Condition condition, String definedValue) {
    definedValues.add(definedValue);
    allowedValues.addValue(condition, definedValue);
    generatedValues.addValue(condition, definedValue);
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

  @Override
  public boolean equals(Object object) {
    if (object instanceof BindingProperty) {
      BindingProperty that = (BindingProperty) object;
      return Objects.equal(this.name, that.name)
          && Objects.equal(this.collapsedValues, that.collapsedValues)
          && Objects.equal(this.allowedValues, that.allowedValues)
          && Objects.equal(this.generatedValues, that.generatedValues)
          && Objects.equal(this.definedValues, that.definedValues)
          && Objects.equal(this.fallback, that.fallback)
          && Objects.equal(this.getFallbackValuesMap(), that.getFallbackValuesMap())
          && Objects.equal(this.fallbackValues, that.fallbackValues)
          && Objects.equal(this.provider, that.provider)
          && Objects.equal(this.providerGenerator, that.providerGenerator)
          && Objects.equal(this.rootCondition, that.rootCondition);
    }
    return false;
  }

  /**
   * Returns the set of values defined in the module file.
   * (For code generation, use {@link #getGeneratedValues} because this might be
   * overridden.)
   */
  public String[] getAllowedValues(Condition condition) {
    return allowedValues.getValuesAsArray(condition);
  }

  /**
   * Returns the set of values for which the GWT compiler must generate permutations.
   */
  public String[] getGeneratedValues(Condition condition) {
    return generatedValues.getValuesAsArray(condition);
  }

  public List<SortedSet<String>> getCollapsedValuesSets() {
    return collapsedValues;
  }

  /**
   * Returns a map containing the generated values for each condition, in the order
   * they were added to the module files.
   */
  public ImmutableMap<Condition, SortedSet<String>> getConditionalValues() {
    return generatedValues.toMap();
  }

  /**
   * If the BindingProperty has exactly one generated value across all conditions and
   * permutations, return that value otherwise return <code>null</code>.
   */
  public String getConstrainedValue() {
    if (!generatedValues.allConditionsHaveOneValue()) {
      return null;
    }
    Set<String> values = generatedValues.getAllValues();
    if (values.size() != 1) {
      return null; // For example, two conditions could each have a different value.
    }
    return values.iterator().next();
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

  /**
   * Returns the first value from the list of defined values that is that is also allowed.
   * @throws IllegalStateException if there is not at least one value that's both defined
   * and allowed.
   */
  public String getFirstAllowedValue() {
    String value = allowedValues.getFirstMember(definedValues);
    if (value == null) {
      throw new IllegalStateException("binding property has no allowed values: " + name);
    }
    return value;
  }

  /**
   * Returns the first value from the list of defined values that is actually generated.
   * @throws IllegalStateException if there is not at least one value that's both defined
   * and generated.
   */
  public String getFirstGeneratedValue() {
    if (definedValues.isEmpty()) {
      // This shouldn't happen but a DynamicPropertyOracleTest currently requires it.
      // TODO(skybrian) we should probably require fallback values to be defined.
      // (It's checked when parsing the XML.)
      return fallback;
    }
    String value = generatedValues.getFirstMember(definedValues);
    if (value == null) {
      throw new IllegalStateException("binding property has no generated values: " + name);
    }
    return value;
  }

  public PropertyProvider getProvider() {
    return provider;
  }

  /**
   * @return the provider generator class, or null if none.
   */
  public Class<? extends PropertyProviderGenerator> getProviderGenerator() {
    return providerGenerator;
  }

  public Set<String> getRequiredProperties() {
    Set<String> toReturn = Sets.create();
    for (Condition cond : generatedValues.eachCondition()) {
      toReturn = Sets.addAll(toReturn, cond.getRequiredProperties());
    }
    return toReturn;
  }

  public ConditionAll getRootCondition() {
    return rootCondition;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, collapsedValues, allowedValues, definedValues, fallback,
        getFallbackValuesMap(), fallbackValues, provider, providerGenerator, rootCondition);
  }

  /**
   * Returns <code>true</code> if the value was previously provided to
   * {@link #addDefinedValue(Condition,String)}.
   */
  public boolean isDefinedValue(String value) {
    return definedValues.contains(value);
  }

  /**
   * Returns true if the supplied value is used based on the module file.
   */
  public boolean isAllowedValue(String value) {
    return allowedValues.containsValue(value);
  }

  /**
   * Returns true if the supplied value will be used during code generation.
   */
  public boolean isGeneratedValue(String value) {
    return generatedValues.containsValue(value);
  }

  /**
   * Returns <code>true</code> if the value of this BindingProperty is always
   * derived from other BindingProperties. That is, for each Condition in the
   * BindingProperty, there is exactly one generated value.
   */
  public boolean isDerived() {
    return generatedValues.allConditionsHaveOneValue();
  }

  /**
   * Undo any value restrictions that have been put in place specifically on the set of values used
   * for code generation as opposed to being present in the actual module definition.
   */
  public void resetGeneratedValues() {
    generatedValues.valueMap.clear();
    generatedValues.valueMap.putAll(allowedValues.valueMap);
  }

  public void setFallback(String token) {
    fallback = token;
  }

  public void setProvider(PropertyProvider provider) {
    this.provider = provider;
  }

  /**
   * Set a provider generator for this property.
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
      @Override
      public int compare(SortedSet<String> o1, SortedSet<String> o2) {
        String s1 = o1.toString();
        String s2 = o2.toString();
        assert !s1.equals(s2) : "Should not have seen equal sets";
        return s1.compareTo(s2);
      }
    });
  }

  /**
   * Replaces the allowed and generated values for a condition.
   * If it is the root condition, clears all other conditions.
   *
   * @throws IllegalArgumentException if any value isn't currently defined.
   */
  public void setValues(Condition bindingPropertyCondition, String... values) {
    List<String> valueList = Arrays.asList(values);
    checkAllDefined(valueList);
    allowedValues.putValues(bindingPropertyCondition, valueList);
    generatedValues.putValues(bindingPropertyCondition, valueList);
  }

  /**
   * Overrides the generated values for the root condition and clears the
   * generated values for other conditions.
   *
   * This has no effect on the allowed values and ignores them.
   * It's intended for artificially restricting permutations
   * in special modes like Super Dev Mode and the GWTTestCase runner.
   *
   * @throws IllegalArgumentException if any value isn't currently defined.
   */
  public void setRootGeneratedValues(String... values) {
    List<String> valueList = Arrays.asList(values);
    checkAllDefined(valueList);
    generatedValues.replaceAllValues(valueList);
  }

  private void checkAllDefined(Collection<String> valueList) {
    for (String value : valueList) {
      if (!definedValues.contains(value)) {
        throw new IllegalArgumentException(
            "Attempted to set a binding property to a value that was not previously defined: " +
                name + " = '" + value + "'");
      }
    }
  }

  /**
   * Contains a set of binding values for each condition.
   *
   * <p>Remembers the order in which they were added. This is needed because
   * the order in which properties were set in GWT module files is significant.
   * (The last one wins.)
   */
  private static class ConditionalValues implements Serializable {
    private final Condition root;
    private final Map<Condition, SortedSet<String>> valueMap =
        new LinkedHashMap<Condition, SortedSet<String>>();

    private ConditionalValues(Condition root) {
      // The root condition always has a set of values. By default this is empty.
      this.root = root;
      valueMap.put(root, new TreeSet<String>());
    }

    /**
     * Adds one more value under a condition. If there isn't any set of values for
     * the given condition, creates it by copying the root condition.
     */
    private void addValue(Condition condition, String value) {
      SortedSet<String> set = valueMap.get(condition);
      if (set == null) {
        set = new TreeSet<String>(valueMap.get(root));
        valueMap.put(condition, set);
      }
      set.add(value);
    }

    /**
     * Replaces all the values for a condition and moves it to the end of the map.
     * If it is the root condition, also clears the other conditions.
     */
    private void putValues(Condition condition, Collection<String> values) {

      // XML has a last-one-wins semantic which we reflect in our evaluation order
      if (condition == root) {
        // An unconditional set-property would undo any previous conditional
        // setters, so we can just clear out this map.
        replaceAllValues(values);
      } else {
        // Otherwise, we'll just ensure that this condition is moved to the end.
        valueMap.remove(condition);
        valueMap.put(condition, new TreeSet<String>(values));
      }
    }

    private void replaceAllValues(Collection<String> rootValues) {
      valueMap.clear();
      valueMap.put(root, new TreeSet<String>(rootValues));
    }

    // === queries ===

    private ImmutableMap<Condition, SortedSet<String>> toMap() {
      return ImmutableMap.copyOf(valueMap);
    }

    private String[] getValuesAsArray(Condition condition) {
      Set<String> values = valueMap.get(condition);
      return values.toArray(new String[values.size()]);
    }

    private Iterable<Condition> eachCondition() {
      return valueMap.keySet();
    }

    /**
     * Returns true if the value appears for any condition.
     */
    private boolean containsValue(String value) {
      for (Set<String> values : valueMap.values()) {
        if (values.contains(value)) {
          return true;
        }
      }
      return false;
    }

    /**
     * Returns all values that appear under at least one condition.
     */
    private Set<String> getAllValues() {
      SortedSet<String> result = new TreeSet<String>();
      for (SortedSet<String> valueSet : valueMap.values()) {
        result.addAll(valueSet);
      }
      return result;
    }

    /**
     * Returns the first value from the given list that's used in at least one condition,
     * or null if none are.
     */
    private String getFirstMember(Iterable<String> candidates) {
      Set<String> members = getAllValues();
      for (String candidate : candidates) {
        if (members.contains(candidate)) {
          return candidate;
        }
      }
      return null;
    }

    private boolean allConditionsHaveOneValue() {
      for (Set<String> values : valueMap.values()) {
        if (values.size() != 1) {
          return false;
        }
      }
      return true;
    }
  }
}
