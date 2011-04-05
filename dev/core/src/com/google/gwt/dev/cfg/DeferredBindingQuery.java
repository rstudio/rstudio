/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;

import java.util.Set;

/**
 * A query into the deferred binding system. A {@link Condition} can be tested
 * against such a query.
 */
public class DeferredBindingQuery {
  private int fallbackEvalCost = Integer.MAX_VALUE;
  private final Set<String> linkerNames;
  private final PropertyOracle propertyOracle;
  private final String testType;
  private final CompilationState compilationState;
  /**
   * Construct a query for contexts where a type is not available. Such a query
   * also does not need a type oracle.
   */
  public DeferredBindingQuery(PropertyOracle propertyOracle,
      Set<String> linkerNames) {
    this(propertyOracle, linkerNames, null, null);
  }

  /**
   * Construct a fully general query, including a query type and type oracle.
   */
  public DeferredBindingQuery(PropertyOracle propertyOracle, Set<String> linkerNames,
      CompilationState compilationState, String testType) {
    assert propertyOracle != null;
    assert linkerNames != null;

    this.propertyOracle = propertyOracle;
    this.linkerNames = linkerNames;
    this.testType = testType;
    this.compilationState = compilationState;
  }

  public CompilationState getCompilationState() {
    return compilationState;
  }
  
  public int getFallbackEvaluationCost() {
    return fallbackEvalCost;
  }
  
  public Set<String> getLinkerNames() {
    return linkerNames;
  }

  public PropertyOracle getPropertyOracle() {
    return propertyOracle;
  }

  public String getTestType() {
    return testType;
  }

  public TypeOracle getTypeOracle() {
    return compilationState.getTypeOracle();
  }

  public void setFallbackEvaluationCost(int cost) {
    fallbackEvalCost = cost;
  }
}
