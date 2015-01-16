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
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.HashMultiset;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multiset;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains dependence and modification information for AST optimizers.
 * <p>
 * Is updated incrementally.
 */
public class FullOptimizerContext implements OptimizerContext {
  private int optimizationStep = -1;

  private CallGraph callGraph = new CallGraph();
  private FieldReferencesGraph fieldReferencesGraph = new FieldReferencesGraph();

  /**
   * The deleted sub call graph and added sub call graph at each step.
   */
  private List<CallGraph> deletedSubCallGraphs = Lists.newArrayList();
  private List<CallGraph> addedSubCallGraphs = Lists.newArrayList();

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

  public FullOptimizerContext(JProgram program) {
    incOptimizationStep();
    initializeModifications(program);
    buildCallGraph(program);
    buildFieldReferencesGraph(program);
    incOptimizationStep();
  }

  @Override
  public Set<JMethod> getCallees(Collection<JMethod> callerMethods) {
    return callGraph.getCallees(callerMethods);
  }

  @Override
  public Set<JMethod> getCallers(Collection<JMethod> calleeMethods) {
    return callGraph.getCallers(calleeMethods);
  }

  @Override
  public int getLastStepFor(String optimizerName) {
    return lastStepForOptimizer.count(optimizerName);
  }

  @Override
  public Set<JMethod> getMethodsByReferencedFields(Collection<JField> fields) {
    return fieldReferencesGraph.getReferencingMethodsForFields(fields);
  }

  @Override
  public Set<JField> getModifiedFieldsSince(int stepSince) {
    Set<JField> result = Sets.newLinkedHashSet();
    for (int i = stepSince; i < optimizationStep; i++) {
      result.addAll(fieldsByModificationStep.get(i));
    }
    return result;
  }

  @Override
  public Set<JMethod> getModifiedMethodsSince(int stepSince) {
    Set<JMethod> result = Sets.newLinkedHashSet();
    for (int i = stepSince; i < optimizationStep; i++) {
      result.addAll(methodsByModificationStep.get(i));
    }
    return result;
  }

  @Override
  public int getOptimizationStep() {
    return optimizationStep;
  }

  @Override
  public Set<JField> getReferencedFieldsByMethods(Collection<JMethod> methods) {
    return fieldReferencesGraph.getReferencedFieldsByMethods(methods);
  }

  @Override
  public Set<JMethod> getRemovedCalleeMethodsSince(int stepSince) {
    Set<JMethod> removedCalleeMethods = Sets.newLinkedHashSet();
    for (int i = stepSince; i < optimizationStep; i++) {
      removedCalleeMethods.addAll(deletedSubCallGraphs.get(i).getAllCallees());
    }
    return removedCalleeMethods;
  }

  @Override
  public void incOptimizationStep() {
    methodsByModificationStep.add(new LinkedHashSet<JMethod>());
    fieldsByModificationStep.add(new LinkedHashSet<JField>());
    deletedSubCallGraphs.add(new CallGraph());
    addedSubCallGraphs.add(new CallGraph());
    optimizationStep++;
  }

  @Override
  public void markModified(JField modifiedField) {
    fieldsByModificationStep.get(modificationStepByField.count(modifiedField)).remove(
        modifiedField);
    fieldsByModificationStep.get(optimizationStep).add(modifiedField);
    modificationStepByField.setCount(modifiedField, optimizationStep);
    // TODO(leafwang): update related dependence information here.
  }

  @Override
  public void markModified(JMethod modifiedMethod) {
    methodsByModificationStep.get(modificationStepByMethod.count(modifiedMethod)).remove(
        modifiedMethod);
    methodsByModificationStep.get(optimizationStep).add(modifiedMethod);
    modificationStepByMethod.setCount(modifiedMethod, optimizationStep);

    callGraph.updateCallGraphOfMethod(modifiedMethod, deletedSubCallGraphs.get(optimizationStep),
        addedSubCallGraphs.get(optimizationStep));
    fieldReferencesGraph.updateFieldReferencesOfMethod(modifiedMethod);
  }

  @Override
  public void remove(JField field) {
    fieldsByModificationStep.get(modificationStepByField.count(field)).remove(field);
    modificationStepByField.remove(field);
    fieldReferencesGraph.removeField(field);
  }

  @Override
  public void remove(JMethod method) {
    methodsByModificationStep.get(modificationStepByMethod.count(method)).remove(method);
    modificationStepByMethod.remove(method);
    Set<JMethod> calleeMethods = callGraph.removeCallerMethod(method);
    deletedSubCallGraphs.get(optimizationStep).addCallerMethod(method,
        Sets.difference(calleeMethods, callGraph.getCallees(Collections.singleton(method))));
    addedSubCallGraphs.get(optimizationStep).addCallerMethod(method,
        Sets.difference(callGraph.getCallees(Collections.singleton(method)), calleeMethods));
    fieldReferencesGraph.removeMethod(method);
  }

  @Override
  public void removeFields(Collection<JField> fields) {
    for (JField field : fields) {
      remove(field);
    }
  }

  @Override
  public void removeMethods(Collection<JMethod> methods) {
    for (JMethod method : methods) {
      remove(method);
    }
  }

  @Override
  public void setLastStepFor(String optimizerName, int step) {
    lastStepForOptimizer.setCount(optimizerName, step);
  }

  @Override
  public void syncDeletedSubCallGraphsSince(int step, Collection<JMethod> prunedMethods) {
    for (int i = step; i < optimizationStep; i++) {
      for (JMethod prunedMethod : prunedMethods) {
        deletedSubCallGraphs.get(i).removeCallerMethod(prunedMethod);
        deletedSubCallGraphs.get(i).removeCalleeMethod(prunedMethod);
      }
    }
  }

  @Override
  public void traverse(JVisitor visitor, Set<? extends JNode> nodes) {
    assert (nodes != null);
    for (JNode node : nodes) {
      visitor.accept(node);
    }
  }

  private void buildCallGraph(JProgram program) {
    callGraph.buildCallGraph(program);
  }

  private void buildFieldReferencesGraph(JProgram program) {
    fieldReferencesGraph.buildFieldReferencesGraph(program);
  }

  private void initializeModifications(JProgram program) {
    assert optimizationStep == 0;
    for (JDeclaredType type : program.getModuleDeclaredTypes()) {
      fieldsByModificationStep.get(0).addAll(type.getFields());
      methodsByModificationStep.get(0).addAll(type.getMethods());
    }
  }
}
