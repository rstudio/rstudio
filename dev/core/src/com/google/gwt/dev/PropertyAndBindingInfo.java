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
package com.google.gwt.dev;

import com.google.gwt.thirdparty.guava.common.collect.ArrayListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.SortedSetMultimap;
import com.google.gwt.thirdparty.guava.common.collect.TreeMultimap;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

/**
 * Contains mappings for property names to property values and from request types (GWT.create) to
 * rebound types.
 * <p>
 * Each info object typically contains the information for one (soft) permutation.
 */
public class PropertyAndBindingInfo implements Serializable {
  private final SortedMap<String, String> reboundTypeByGwtCreateType;
  private final SortedMap<String, String> propertyValueByPropertyName;

  public PropertyAndBindingInfo() {
    reboundTypeByGwtCreateType = Maps.newTreeMap();
    propertyValueByPropertyName = Maps.newTreeMap();
  }

  private PropertyAndBindingInfo(
      SortedMap<String, String> reboundTypeByGwtCreateType,
      SortedMap<String, String> propertyValueByPropertyName) {
    this.reboundTypeByGwtCreateType = reboundTypeByGwtCreateType;
    this.propertyValueByPropertyName = propertyValueByPropertyName;
  }

  /**
   * Returns the property value for a propertyname.
   */
  public String getPropertyValue(String key) {
    return propertyValueByPropertyName.get(key);
  }

  /**
   * Returns the Java class that a GWT.create() call will construct.
   *
   * @param key the GWT.create() argument (source type name)
   * @return a source type name
   */
  public String getReboundType(String key) {
    return reboundTypeByGwtCreateType.get(key);
  }

  /**
   * Adds a property value.
   */
  public void putPropertyValue(String propertyName, String propertyValue) {
    propertyValueByPropertyName.put(propertyName, propertyValue);
  }

  /**
   * Sets the Java class that a GWT.create() call will construct.
   *
   * @param key a source type name
   * @param value a source type name
   */
  void putReboundType(String key, String value) {
    reboundTypeByGwtCreateType.put(key, value);
  }

  /**
   * Returns true if the map contains a answer for the given GWT.create() argument.
   */
  public boolean containsProperty(String key) {
    return propertyValueByPropertyName.containsKey(key);
  }

  /**
   * Returns true if the map contains a answer for the given GWT.create() argument.
   */
  public boolean containsType(String key) {
    return reboundTypeByGwtCreateType.containsKey(key);
  }

  /**
   * Asserts that two maps contain the same answers for the given GWT.create() arguments.
   */
  void assertRebindsEqual(PropertyAndBindingInfo other, Iterable<String> keysToCheck) {
    for (String key : keysToCheck) {
      assert reboundTypeByGwtCreateType.get(key).equals(other.reboundTypeByGwtCreateType.get(key));
    }
  }

  /**
   * Returns the answers that are the same in every permutation.
   */
  public static PropertyAndBindingInfo getCommonAnswers(Iterable<PropertyAndBindingInfo> maps) {
    Iterator<PropertyAndBindingInfo> it = maps.iterator();

    // Start with an arbitrary copy
    PropertyAndBindingInfo first = it.next();
    SortedMap<String, String> commonTypesByGwtCreateType =
        Maps.newTreeMap(first.reboundTypeByGwtCreateType);
    SortedMap<String, String> commonPropertiesValuesByPropertyNames =
        Maps.newTreeMap(first.propertyValueByPropertyName);

    while (it.hasNext()) {
      PropertyAndBindingInfo next = it.next();
      // Only keep key/value pairs present in both maps.
      commonTypesByGwtCreateType.entrySet().retainAll(next.reboundTypeByGwtCreateType.entrySet());
      commonPropertiesValuesByPropertyNames.entrySet()
          .retainAll(next.propertyValueByPropertyName.entrySet());
    }

    return new PropertyAndBindingInfo(commonTypesByGwtCreateType,
        commonPropertiesValuesByPropertyNames);
  }

  /**
   * Returns the Java classes that GWT.create() might return in at least one permutation.
   *
   * @param requestTypes the GWT.create() arguments to include (source type names).
   */
  public static SortedSetMultimap<String, String> getPossibleReboundTypesByRequestType(
      Iterable<PropertyAndBindingInfo> permutationPropertyAndBindingInfoList,
      Set<String> requestTypes) {

    SortedSetMultimap<String, String> result = TreeMultimap.create();

    for (PropertyAndBindingInfo map : permutationPropertyAndBindingInfoList) {
      for (Map.Entry<String, String> entry : map.reboundTypeByGwtCreateType.entrySet()) {
        if (!requestTypes.contains(entry.getKey())) {
          continue;
        }
        result.put(entry.getKey(), entry.getValue());
      }
    }

    return result;
  }

  /**
   * Given an argument to GWT.create(), returns a map containing each possible return type and the
   * permutations that would create it.
   *
   * @return a map from a Java class (source type name) to a list of indexes into the maps list.
   */
  public static Multimap<String, Integer> getPermutationIdsByRequestTypes(
      List<PropertyAndBindingInfo> maps, String key) {

    Multimap<String, Integer> result = ArrayListMultimap.create();

    int permutationCount = maps.size();
    for (int i = 0; i < permutationCount; i++) {
      String answerType = maps.get(i).getReboundType(key);
      result.put(answerType, i);
    }
    return result;
  }

  /**
   * Given a property name, returns a map containing each possible return value and the
   * permutations that would create it.
   *
   * @return a map from a Java class (source type name) to a list of indexes into the maps list.
   */
  public static Multimap<String, Integer> getPermutationIdsByPropertyName(
      List<PropertyAndBindingInfo> maps, String key) {

    Multimap<String, Integer> result = ArrayListMultimap.create();

    int permutationCount = maps.size();
    for (int i = 0; i < permutationCount; i++) {
      String answerType = maps.get(i).getPropertyValue(key);
      result.put(answerType, i);
    }
    return result;
  }
}
