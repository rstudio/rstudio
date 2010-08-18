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

import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.util.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Represents the state of a single permutation for compile.
 */
public final class Permutation implements Serializable {

  private final int id;

  private List<StaticPropertyOracle> orderedPropertyOracles = Lists.create();
  private List<SortedMap<String, String>> orderedRebindAnswers = Lists.create();

  /**
   * Clones an existing permutation, but with a new id.
   *
   * @param id new permutation id
   * @param other Permutation to copy
   */
  public Permutation(int id, Permutation other) {
    this.id = id;
    orderedPropertyOracles = Lists.create(other.orderedPropertyOracles);
    orderedRebindAnswers = Lists.create(other.orderedRebindAnswers);
  }

  public Permutation(int id, StaticPropertyOracle propertyOracle) {
    this.id = id;
    orderedPropertyOracles = Lists.add(orderedPropertyOracles, propertyOracle);
    orderedRebindAnswers = Lists.add(orderedRebindAnswers,
        new TreeMap<String, String>());
  }

  public int getId() {
    return id;
  }

  public SortedMap<String, String>[] getOrderedRebindAnswers() {
    @SuppressWarnings("unchecked")
    SortedMap<String, String>[] arr = new SortedMap[orderedRebindAnswers.size()];
    return orderedRebindAnswers.toArray(arr);
  }

  /**
   * Returns the property oracles, sorted by property values.
   */
  public StaticPropertyOracle[] getPropertyOracles() {
    return orderedPropertyOracles.toArray(new StaticPropertyOracle[orderedPropertyOracles.size()]);
  }

  /**
   * This is called to merge two permutations that either have identical rebind
   * answers or were explicitly collapsed using <collapse-property>.
   */
  public void mergeFrom(Permutation other, SortedSet<String> liveRebindRequests) {
    if (getClass().desiredAssertionStatus()) {
      for (SortedMap<String, String> myRebind : orderedRebindAnswers) {
        for (SortedMap<String, String> otherRebind : other.orderedRebindAnswers) {
          for (String rebindRequest : liveRebindRequests) {
            String myAnswer = myRebind.get(rebindRequest);
            String otherAnswer = otherRebind.get(rebindRequest);
            assert myAnswer.equals(otherAnswer);
          }
        }
      }
    }
    mergeRebindsFromCollapsed(other);
  }

  /**
   * This is called to collapse one permutation into another where the rebinds
   * vary between the two permutations.
   */
  public void mergeRebindsFromCollapsed(Permutation other) {
    assert other.orderedPropertyOracles.size() == other.orderedRebindAnswers.size();
    orderedPropertyOracles = Lists.addAll(orderedPropertyOracles,
        other.orderedPropertyOracles);
    orderedRebindAnswers = Lists.addAll(orderedRebindAnswers,
        other.orderedRebindAnswers);
    other.destroy();
  }

  /**
   * Shows a list of the property settings that were set to make up this
   * permutation in human readable form.
   */
  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    for (StaticPropertyOracle oracle : orderedPropertyOracles) {
      String[] values = oracle.getOrderedPropValues();
      BindingProperty[] props = oracle.getOrderedProps();
      assert values.length == props.length;
      for (int i = 0; i < props.length; i++) {
        builder.append(props[i].getName() + "=" + values[i]);
        if (i < props.length - 1) {
          builder.append(",");
        }
      }
    }
    return builder.toString();
  }

  public void putRebindAnswer(String requestType, String resultType) {
    assert orderedRebindAnswers.size() == 1 : "Cannot add rebind to merged Permutation";
    SortedMap<String, String> answerMap = orderedRebindAnswers.get(0);
    assert answerMap != null;
    answerMap.put(requestType, resultType);
  }

  /**
   * Clear the state of the Permutation. This aids the correctness-checking code
   * in {@link #mergeFrom}.
   */
  private void destroy() {
    orderedPropertyOracles = Lists.create();
    orderedRebindAnswers = Lists.create();
  }
}
