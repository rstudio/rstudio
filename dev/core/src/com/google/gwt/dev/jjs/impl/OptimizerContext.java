/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains dependence and modification information for AST optimizers.
 * <p>
 * Is updated incrementally.
 */
public class OptimizerContext {
  private int optimizationStep = -1;

  private CallGraph callGraph = new CallGraph();

  // TODO(leafwang): add other dependencies here

  /**
   * A mapping from methods to the numbers of the most recent step in which they were modified.
   */
  private Multiset<JMethod> modificationStepByMethod = HashMultiset.create();

  /**
   * A list of modified methods in each step.
   */
  private List<Set<JMethod>> methodsByModificationStep = Lists.newArrayList();

  /**
   * A mapping from methods to the numbers of the most recent step in which they were modified.
   */
  private Multiset<JField> modificationStepByField = HashMultiset.create();

  /**
   * A list of modified fields in each step.
   */
  private List<Set<JField>> fieldsByModificationStep = Lists.newArrayList();

  /**
   * A mapping from optimizers to their last modification step.
   */
  private Multiset<String> lastStepForOptimizer = HashMultiset.create();

  public OptimizerContext(JProgram program) {
    incOptimizationStep();
    initializeModifications(program);
    buildCallGraph(program);
    incOptimizationStep();
  }

  /**
   * Add modified field to the modification information.
   */
  public void markModifiedField(JField modifiedField) {
    fieldsByModificationStep.get(modificationStepByField.count(modifiedField)).remove(
        modifiedField);
    fieldsByModificationStep.get(optimizationStep).add(modifiedField);
    modificationStepByField.setCount(modifiedField, optimizationStep);

    // TODO(leafwang): update related dependence information here.
  }

  /**
   * Add modified method to both the modification and dependence information.
   */
  public void markModifiedMethod(JMethod modifiedMethod) {
    methodsByModificationStep.get(modificationStepByMethod.count(modifiedMethod)).remove(
        modifiedMethod);
    methodsByModificationStep.get(optimizationStep).add(modifiedMethod);
    modificationStepByMethod.setCount(modifiedMethod, optimizationStep);
    callGraph.updateCallGraphOfMethod(modifiedMethod);
  }

  public Set<JMethod> getCallers(Set<JMethod> calleeMethods) {
    return callGraph.getCallers(calleeMethods);
  }

  /**
   * Return the last modification step for a given optimizer.
   */
  public int getLastStepFor(String optimizerName) {
    return lastStepForOptimizer.count(optimizerName);
  }

  /**
   * Return all the effective modified fields since a given step.
   */
  public Set<JField> getModifiedFieldsSince(int stepSince) {
    Set<JField> result = Sets.newLinkedHashSet();
    for (int i = stepSince; i <= optimizationStep; i++) {
      result.addAll(fieldsByModificationStep.get(i));
    }
    return result;
  }

  /**
   * Return all the modified methods at a given step.
   */
  public Set<JMethod> getModifiedMethodsAt(int step) {
    assert (step >= 0 && step <= optimizationStep);
    return new LinkedHashSet<JMethod>(methodsByModificationStep.get(step));
  }

  /**
   * Return all the effective modified methods since a given step.
   */
  public Set<JMethod> getModifiedMethodsSince(int stepSince) {
    Set<JMethod> result = Sets.newLinkedHashSet();
    for (int i = stepSince; i <= optimizationStep; i++) {
      result.addAll(methodsByModificationStep.get(i));
    }
    return result;
  }

  /**
   * Return the current optimization step number.
   */
  public int getOptimizationStep() {
    return optimizationStep;
  }

  /**
   * Increase the optimization step by 1, create a new set to record modifications in this step.
   */
  public void incOptimizationStep() {
    methodsByModificationStep.add(new LinkedHashSet<JMethod>());
    fieldsByModificationStep.add(new LinkedHashSet<JField>());
    optimizationStep++;
  }

  /**
   * Remove field from the modification information.
   */
  public void removeField(JField field) {
    fieldsByModificationStep.get(modificationStepByField.count(field)).remove(field);
    modificationStepByField.remove(field);
  }

  /**
   * Remove method from both the dependence and modification information.
   */
  public void removeMethod(JMethod method) {
    methodsByModificationStep.get(modificationStepByMethod.count(method)).remove(method);
    modificationStepByMethod.remove(method);
    callGraph.removeMethod(method);
  }

  /**
   * Set the last modification step of a given optimizer.
   */
  public void setLastStepFor(String optimizerName, int step) {
    lastStepForOptimizer.setCount(optimizerName, step);
  }

  private void buildCallGraph(JProgram program) {
    callGraph.buildCallGraph(program);
  }

  private void initializeModifications(JProgram program) {
    for (JDeclaredType type : program.getModuleDeclaredTypes()) {
      fieldsByModificationStep.get(0).addAll(type.getFields());
      methodsByModificationStep.get(0).addAll(type.getMethods());
    }
  }
}
