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

import com.google.gwt.dev.cfg.StaticPropertyOracle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Represents the state of a single permutation for compile.
 */
public final class Permutation implements Serializable {
  private final int id;
  private final List<StaticPropertyOracle> propertyOracles = new ArrayList<StaticPropertyOracle>();
  private final SortedMap<String, String> rebindAnswers = new TreeMap<String, String>();

  /**
   * Clones an existing permutation, but with a new id.
   *  
   * @param id new permutation id
   * @param other Permutation to copy
   */
  public Permutation(int id, Permutation other) {
    this.id = id;
    this.propertyOracles.addAll(other.propertyOracles);
    this.rebindAnswers.putAll(other.rebindAnswers);
  }

  public Permutation(int id, StaticPropertyOracle propertyOracle) {
    this.id = id;
    this.propertyOracles.add(propertyOracle);
  }

  public int getId() {
    return id;
  }

  public StaticPropertyOracle[] getPropertyOracles() {
    return propertyOracles.toArray(new StaticPropertyOracle[propertyOracles.size()]);
  }

  public SortedMap<String, String> getRebindAnswers() {
    return Collections.unmodifiableSortedMap(rebindAnswers);
  }

  public void mergeFrom(Permutation other, SortedSet<String> liveRebindRequests) {
    if (getClass().desiredAssertionStatus()) {
      for (String rebindRequest : liveRebindRequests) {
        String myAnswer = rebindAnswers.get(rebindRequest);
        String otherAnswer = other.rebindAnswers.get(rebindRequest);
        assert myAnswer.equals(otherAnswer);
      }
    }
    assert !propertyOracles.isEmpty();
    assert !other.propertyOracles.isEmpty();
    propertyOracles.addAll(other.propertyOracles);
    other.propertyOracles.clear();
  }

  public void putRebindAnswer(String requestType, String resultType) {
    rebindAnswers.put(requestType, resultType);
  }
}
