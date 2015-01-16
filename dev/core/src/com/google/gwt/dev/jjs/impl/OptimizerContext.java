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

import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JVisitor;

import java.util.Collection;
import java.util.Set;

/**
 * A context used for optimizations that maintains and provides information shared by optimization
 * passes.
 */
public interface OptimizerContext {

  /**
   * An optimization context that does not provide any information.
   */
  OptimizerContext NULL_OPTIMIZATION_CONTEXT = new OptimizerContext() {
    @Override
    public void markModified(JField modifiedField) {
    }

    @Override
    public void markModified(JMethod modifiedMethod) {
    }

    @Override
    public Set<JMethod> getCallers(Collection<JMethod> calleeMethods) {
      return null;
    }

    @Override
    public int getLastStepFor(String optimizerName) {
      return 0;
    }

    @Override
    public Set<JField> getModifiedFieldsSince(int stepSince) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Set<JMethod> getModifiedMethodsSince(int stepSince) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getOptimizationStep() {
      return 0;
    }

    @Override
    public void incOptimizationStep() {
    }

    @Override
    public void remove(JField field) {
    }

    @Override
    public void removeFields(Collection<JField> fields) {
    }

    @Override
    public void remove(JMethod method) {
   }

    @Override
    public void removeMethods(Collection<JMethod> methods) {
    }

    @Override
    public void setLastStepFor(String optimizerName, int step) {
    }

    @Override
    public Set<JMethod> getCallees(Collection<JMethod> callerMethods) {
      return null;
    }

    @Override
    public Set<JMethod> getMethodsByReferencedFields(Collection<JField> fields) {
      return null;
    }

    @Override
    public Set<JField> getReferencedFieldsByMethods(Collection<JMethod> methods) {
      return null;
    }

    @Override
    public void syncDeletedSubCallGraphsSince(int step, Collection<JMethod> prunedMethods) {
    }

    @Override
    public Set<JMethod> getRemovedCalleeMethodsSince(int stepSince) {
      return null;
    }

    @Override
    public void traverse(JVisitor visitor, Set<? extends JNode> nodes) {
      throw new UnsupportedOperationException();
    }
  };

  /**
   * Add modified field to the modification information.
   */
  void markModified(JField modifiedField);

  /**
   * Add modified method to both the modification and dependence information.
   */
  void markModified(JMethod modifiedMethod);

  /**
   * Return caller methods of {@code calleeMethods}.
   */
  Set<JMethod> getCallers(Collection<JMethod> calleeMethods);

  /**
   * Return callee methods of {@code callerMethods}.
   */
  Set<JMethod> getCallees(Collection<JMethod> callerMethods);

  /**
   * Return the last modification step for a given optimizer.
   */
  int getLastStepFor(String optimizerName);

  /**
   * Return all the effective modified fields since a given step.
   */
  Set<JField> getModifiedFieldsSince(int stepSince);

  /**
   * Return all the effective modified methods since a given step.
   */
  Set<JMethod> getModifiedMethodsSince(int stepSince);

  /**
   * Return the current optimization step number.
   */
  int getOptimizationStep();

  /**
   * Increase the optimization step by 1, create a new set to record modifications in this step.
   */
  void incOptimizationStep();

  /**
   * Remove field from the modification information.
   */
  void remove(JField field);

  /**
   * Remove fields from the modification information.
   */
  void removeFields(Collection<JField> fields);

  /**
   * Remove method from both the dependence and modification information.
   */
  void remove(JMethod method);

  /**
   * Remove methods from the modification information.
   */
  void removeMethods(Collection<JMethod> methods);

  /**
   * Set the last modification step of a given optimizer.
   */
  void setLastStepFor(String optimizerName, int step);

  /**
   * Return methods that reference {@code fields}.
   */
  Set<JMethod> getMethodsByReferencedFields(Collection<JField> fields);

  /**
   * Return fields that are referenced by {@code methods}.
   */
  Set<JField> getReferencedFieldsByMethods(Collection<JMethod> methods);

  /**
   * Remove the pruned methods from the deleted sub call graphs since a given step.
   */
  void syncDeletedSubCallGraphsSince(int step, Collection<JMethod> prunedMethods);

  /**
   * Get the removed callee methods since a given step.
   */
  Set<JMethod> getRemovedCalleeMethodsSince(int stepSince);

  /**
   * Traverse the affected methods and fields.
   */
  void traverse(JVisitor visitor, Set<? extends JNode> nodes);
}
