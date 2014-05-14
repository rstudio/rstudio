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
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.SortedSet;

/**
 * Represents the state of a single permutation for compile.
 */
public final class Permutation implements Serializable {

  private final int id;

  private List<StaticPropertyOracle> orderedPropertyOracles = Lists.newArrayList();
  private List<GwtCreateMap> gwtCreateAnswers = Lists.newArrayList();

  /**
   * Clones an existing permutation, but with a new id.
   *
   * @param id new permutation id
   * @param other Permutation to copy
   */
  public Permutation(int id, Permutation other) {
    this.id = id;
    orderedPropertyOracles = Lists.newArrayList(other.orderedPropertyOracles);
    gwtCreateAnswers = Lists.newArrayList(other.gwtCreateAnswers);
  }

  public Permutation(int id, StaticPropertyOracle propertyOracle) {
    this.id = id;
    orderedPropertyOracles.add(propertyOracle);
    gwtCreateAnswers.add(new GwtCreateMap());
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the GWT.create() answers for each soft permutation,
   * ordered by soft permutation id.
   */
  public List<GwtCreateMap> getGwtCreateAnswers() {
    return Lists.newArrayList(gwtCreateAnswers);
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
      // Traverse and compare in unison.
      assertSameAnswers(liveRebindRequests, gwtCreateAnswers, other.gwtCreateAnswers);
    }
    mergeRebindsFromCollapsed(other);
  }

  /**
   * This is called to collapse one permutation into another where the rebinds
   * vary between the two permutations.
   */
  public void mergeRebindsFromCollapsed(Permutation other) {
    assert other.orderedPropertyOracles.size() == other.gwtCreateAnswers.size();
    orderedPropertyOracles.addAll(other.orderedPropertyOracles);
    gwtCreateAnswers.addAll(other.gwtCreateAnswers);
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
    assert gwtCreateAnswers.size() == 1 : "Cannot add rebind to merged Permutation";
    GwtCreateMap answerMap = gwtCreateAnswers.get(0);
    assert answerMap != null;
    answerMap.put(requestType, resultType);
  }

  private static void assertSameAnswers(SortedSet<String> liveRebindRequests,
      List<GwtCreateMap> theseAnswers, List<GwtCreateMap> thoseAnswers) {
    assert theseAnswers.size() == thoseAnswers.size();
    for (int i = 0; i < theseAnswers.size(); i++) {
      theseAnswers.get(i).assertSameAnswers(thoseAnswers.get(i), liveRebindRequests);
    }
  }

  /**
   * Clear the state of the Permutation. This aids the correctness-checking code
   * in {@link #mergeFrom}.
   */
  private void destroy() {
    orderedPropertyOracles = Lists.newArrayList();
    gwtCreateAnswers = Lists.newArrayList();
  }
}
