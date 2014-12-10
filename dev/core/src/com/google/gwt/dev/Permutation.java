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
package com.google.gwt.dev;

import com.google.gwt.dev.cfg.BindingProperties;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.PermutationProperties;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.SortedSet;

/**
 * Represents the state of a single permutation for compile.
 */
public final class Permutation implements Serializable {

  private final int id;

  private List<BindingProperties> orderedProps = Lists.newArrayList();
  private List<PropertyAndBindingInfo> propertyAndBindingInfos = Lists.newArrayList();

  /**
   * Clones an existing permutation, but with a new id.
   *
   * @param id new permutation id
   * @param other Permutation to copy
   */
  public Permutation(int id, Permutation other) {
    this.id = id;
    orderedProps = Lists.newArrayList(other.orderedProps);
    propertyAndBindingInfos = Lists.newArrayList(other.propertyAndBindingInfos);
  }

  public Permutation(int id, BindingProperties bindingProperties) {
    this.id = id;
    orderedProps.add(bindingProperties);
    propertyAndBindingInfos.add(new PropertyAndBindingInfo());
    BindingProperty[]  properties = bindingProperties.getOrderedProps();
    String[]  propertyValues = bindingProperties.getOrderedPropValues();
    for (int i = 0; i < properties.length; i++) {
      propertyAndBindingInfos.get(0).putPropertyValue(properties[i].getName(), propertyValues[i]);
    }
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the GWT.create() answers for each soft permutation,
   * ordered by soft permutation id.
   */
  public List<PropertyAndBindingInfo> getPropertyAndBindingInfos() {
    return Lists.newArrayList(propertyAndBindingInfos);
  }

  /**
   * Returns the properties to be used for generating this (hard) permutation.
   */
  public PermutationProperties getProperties() {
    return new PermutationProperties(orderedProps);
  }

  /**
   * This is called to merge two permutations that either have identical rebind
   * answers or were explicitly collapsed using <collapse-property>.
   */
  public void mergeFrom(Permutation other, SortedSet<String> liveRebindRequests) {
    if (getClass().desiredAssertionStatus()) {
      // Traverse and compare in unison.
      assertSameAnswers(liveRebindRequests, propertyAndBindingInfos, other.propertyAndBindingInfos);
    }
    mergeRebindsFromCollapsed(other);
  }

  /**
   * This is called to collapse one permutation into another where the rebinds
   * vary between the two permutations.
   */
  public void mergeRebindsFromCollapsed(Permutation other) {
    assert other.orderedProps.size() == other.propertyAndBindingInfos.size();
    orderedProps.addAll(other.orderedProps);
    propertyAndBindingInfos.addAll(other.propertyAndBindingInfos);
    other.destroy();
  }

  public void putRebindAnswer(String requestType, String resultType) {
    assert propertyAndBindingInfos.size() == 1 : "Cannot add rebind to merged Permutation";
    PropertyAndBindingInfo answerMap = propertyAndBindingInfos.get(0);
    assert answerMap != null;
    answerMap.putReboundType(requestType, resultType);
  }

  private static void assertSameAnswers(SortedSet<String> liveRebindRequests,
      List<PropertyAndBindingInfo> theseAnswers, List<PropertyAndBindingInfo> thoseAnswers) {
    assert theseAnswers.size() == thoseAnswers.size();
    for (int i = 0; i < theseAnswers.size(); i++) {
      theseAnswers.get(i).assertRebindsEqual(thoseAnswers.get(i), liveRebindRequests);
    }
  }

  /**
   * Clear the state of the Permutation. This aids the correctness-checking code
   * in {@link #mergeFrom}.
   */
  private void destroy() {
    orderedProps = Lists.newArrayList();
    propertyAndBindingInfos = Lists.newArrayList();
  }
}
