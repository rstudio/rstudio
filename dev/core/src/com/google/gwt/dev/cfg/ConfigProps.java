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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap.Builder;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The compiler's representation of a set of configuration properties.
 * These are properties that don't vary by permutation.
 * They may be single-valued or multi-valued.
 */
public class ConfigProps implements Serializable {

  public static final ConfigProps EMPTY =
      new ConfigProps(Collections.<ConfigurationProperty>emptyList());

  private static final Splitter SPLIT_ON_COMMAS =
      Splitter.on(',').omitEmptyStrings().trimResults();

  /**
   * Maps a configuration property's name to its values.
   *
   * <p>The value list may be empty, to represent a multi-valued property that has no values.
   * (This is distinct from a property that doesn't exist at all.)
   *
   * <p>The value list may contain nulls. By default, an undefined, single-valued
   * configuration property is represented as a list containing one null.
   *
   * <p>We don't have a way to distinguish a single-valued property from
   * a multi-valued property that happens to contain one entry. (It's available in
   * {@link ConfigurationProperty} but not preserved.)
   *
   * <p>(We can't use a Multimap due to the above requirements.)
   */
  private final ImmutableMap<String, List<String>> props;

  /**
   * Takes a snapshot of some ConfigurationProperty instances.
   */
  public ConfigProps(Iterable<ConfigurationProperty> props) {
    Builder<String, List<String>> builder = ImmutableMap.builder();
    for (ConfigurationProperty prop : props) {
      builder.put(prop.getName(), copyOf(prop.getValues()));
    }
    this.props = builder.build();
  }

  /**
   * Takes a snapshot of a module's configuration properties.
   */
  public ConfigProps(ModuleDef def) {
    this(def.getProperties().getConfigurationProperties());
  }

  /**
   * Construct from a map (for testing).
   */
  public ConfigProps(Map<String, List<String>> map) {
    Builder<String, List<String>> builder = ImmutableMap.builder();
    for (Entry<String, List<String>> entry : map.entrySet()) {
      builder.put(entry.getKey(), copyOf(entry.getValue()));
    }
    this.props = builder.build();
  }

  /**
   * Returns a single-valued property as a boolean if possible.
   * If not set or not single-valued, returns the default value.
   */
  public boolean getBoolean(String key, boolean defaultValue) {
    List<String> values = getStrings(key);
    if (values.size() != 1 || values.get(0) == null) {
      return defaultValue;
    }
    return Boolean.parseBoolean(values.get(0));
  }

  /**
   * Returns a single-valued configuration property as an integer if possible.
   * If not set, not single-valued, or not an integer, returns the default value.
   */
  public int getInteger(String key, int defaultValue) {
    List<String> values = getStrings(key);
    if (values.size() != 1 || values.get(0) == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(values.get(0));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /**
   * Returns all the values of a multi-valued configuration property, or an empty list
   * if not found.
   *
   * <p>A single-valued and unset configuration property will be returned as a list
   * containing one null.
   */
  public List<String> getStrings(String key) {
    if (!props.containsKey(key)) {
      return Collections.emptyList();
    }
    return props.get(key);
  }

  /**
   * Reads a configuration property as a comma-separated list of strings.
   * It may be a single-valued or multi-valued property. If multi-valued,
   * each value will be split on commas and all items concatenated.
   *
   * <p>Leading and trailing space is automatically trimmed, and nulls and
   * empty strings are automatically ignored.
   *
   * <p>Returns an empty list if the property doesn't exist.
   */
  public List<String> getCommaSeparatedStrings(String key) {
    List<String> result = Lists.newArrayList();
    for (String value : getStrings(key)) {
      if (value != null) {
        result.addAll(SPLIT_ON_COMMAS.splitToList(value));
      }
    }
    return result;
  }

  /**
   * Returns the ConfigurationProperty with the given key.
   * (API-compatible with {@link com.google.gwt.core.ext.PropertyOracle}.)
   */
  com.google.gwt.core.ext.ConfigurationProperty getConfigurationProperty(String key)
      throws BadPropertyValueException {
    if (!props.containsKey(key)) {
      throw new BadPropertyValueException(key);
    }
    return new DefaultConfigurationProperty(key, props.get(key));
  }

  /**
   * Returns an unmodifiable copy of a list that may contain nulls.
   */
  private static List<String> copyOf(List<String> values) {
    return Collections.unmodifiableList(Lists.newArrayList(values));
  }
}
