/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.thirdparty.guava.common.base.Objects;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * The properties for one hard permutation.
 *
 * <p>This is essentially a table where columns are soft permutations
 * and rows are property names. Each values for each column are stored
 * in an instance of {@link BindingProps}).
 */
public class PermProps {
  private final ImmutableList<BindingProps> props;

  public PermProps(Iterable<BindingProps> softProps) {
    this.props = ImmutableList.copyOf(softProps);
    assert props.size() >= 1;
    assert sameBindingProperties(props) :
        "The binding properties should be the same for each soft permutation.";
  }

  /**
   * Returns the permutation-independent properties.
   */
  public ConfigProps getConfigProps() {
    // They are all the same, so just take the first one.
    return props.get(0).getConfigProps();
  }

  /**
   * Returns the binding properties in dependency order (permutation-independent).
   */
  public ImmutableList<BindingProperty> getBindingProperties() {
    // Just take the first one.
    return ImmutableList.copyOf(props.get(0).getOrderedProps());
  }

  /**
   * Returns the properties for each soft permutation, ordered by soft permutation id.
   *
   * <p>If soft permutations aren't turned on, the list will contain one item.
   */
  public ImmutableList<BindingProps> getSoftProps() {
    return props;
  }

  /**
   * Returns the value of a binding property as a string.
   *
   * @throws IllegalStateException if it doesn't exist or if the soft permutations
   * don't all have the same value.
   */
  public String mustGetString(String key) {
    if (!isEqualInEachPermutation(key)) {
      throw new IllegalStateException("The '" + key +
          "' binding property must be the same in each soft permutation");
    }
    String value = props.get(0).getString(key, null);
    if (value == null) {
      throw new IllegalStateException("The '" + key + "' binding property is not defined");
    }
    return value;
  }

  /**
   * Returns true if a binding property has the same value in every soft permutation.
   */
  public boolean isEqualInEachPermutation(String key) {
    String expected = props.get(0).getString(key, null);
    for (BindingProps prop : props.subList(1, props.size())) {
      String actual = prop.getString(key, null);
      if (!Objects.equal(expected, actual)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if a boolean binding property is set to true in any soft permutation.
   */
  public boolean isTrueInAnyPermutation(String name) {
    for (BindingProps bindingProps : props) {
      if (bindingProps.getBoolean(name, false)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the binding property values to be embedded into the initial JavaScript fragment
   * for this permutation. (There will be one map for each soft permutation.)
   */
  public ImmutableList<ImmutableMap<String, String>> findEmbeddedProperties(TreeLogger logger) {

    Set<String> propsWanted = Sets.newTreeSet(getConfigProps().getStrings(
        "js.embedded.properties"));

    // Filter out any binding properties that don't exist.
    SortedSet<String> propsToSave = Sets.newTreeSet();
    for (BindingProperty prop : getBindingProperties()) {
      String name = prop.getName();
      if (propsWanted.remove(name)) {
        propsToSave.add(name);
      }
    }

    // Warn about binding properties that don't exist.
    if (!propsWanted.isEmpty()) {
      TreeLogger branch = logger.branch(Type.WARN,
          propsWanted.size() + "properties listed in js.embedded.properties are undefined");
      for (String prop : propsWanted) {
        branch.log(Type.WARN, "undefined property: '" + prop + "'");
      }
    }

    // Find the values.
    List<ImmutableMap<String, String>> result = Lists.newArrayList();
    for (BindingProps softProps : getSoftProps()) {
      ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
      for (String key : propsToSave) {
        values.put(key, softProps.getString(key, null));
      }
      result.add(values.build());
    }

    return ImmutableList.copyOf(result);
  }

  /**
   * Dumps the properties for this hard permuation, for logging and soyc.
   */
  public String prettyPrint() {
    StringBuilder out = new StringBuilder();
    for (BindingProps bindingProps : getSoftProps()) {
      if (out.length() > 0) {
        out.append("; ");
      }
      out.append(bindingProps.prettyPrint());
    }
    return out.toString();
  }

  private boolean sameBindingProperties(ImmutableList<BindingProps> props) {
    BindingProps expected = props.get(0);
    for (BindingProps actual : props.subList(1, props.size())) {
      if (!expected.hasSameBindingProperties(actual)) {
        return false;
      }
    }
    return true;
  }
}
