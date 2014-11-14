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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Call graph, which records {callee->callers} and {caller->callees} pairs.
 */
public class CallGraph {

  /**
   * Visitor used to build call graph.
   */
  private class BuildCallGraphVisitor extends JVisitor {

    // TODO(leafwang): This call graph does not take into account overloads nor calls that happen
    // in JSNI methods.

    private JMethod currentMethod;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      assert (currentMethod == x);
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod calleeMethod = x.getTarget();
      assert (currentMethod != null);
      calleeCallersPairs.put(calleeMethod, currentMethod);
      callerCalleesPairs.put(currentMethod, calleeMethod);
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      assert (currentMethod == null);
      currentMethod = x;
      return true;
    }
  }

  private Multimap<JMethod, JMethod> calleeCallersPairs = HashMultimap.create();
  private Multimap<JMethod, JMethod> callerCalleesPairs = HashMultimap.create();

  /**
   * Build the call graph of a JProgram.
   */
  public void buildCallGraph(JProgram program) {
    resetCallGraph();
    BuildCallGraphVisitor buildCallGraphVisitor = new BuildCallGraphVisitor();
    buildCallGraphVisitor.accept(program);
  }

  public void resetCallGraph() {
    calleeCallersPairs.clear();
    callerCalleesPairs.clear();
  }

  /**
   * Update call graph of a JMethod.
   */
  public void updateCallGraphOfMethod(JMethod method) {
    removeMethod(method);
    BuildCallGraphVisitor callSiteVisitor = new BuildCallGraphVisitor();
    callSiteVisitor.currentMethod = method;
    callSiteVisitor.accept(method.getBody());
  }

  /**
   * For removing a method, remove the {caller->callees} and {callee->callers} pairs that are
   * related to the method.
   */
  public void removeMethod(JMethod method) {
    for (JMethod calleeMethod : callerCalleesPairs.get(method)) {
      calleeCallersPairs.remove(calleeMethod, method);
    }
    callerCalleesPairs.removeAll(method);
  }

  /**
   * Return all the callers of a set of callee methods.
   */
  public Set<JMethod> getCallers(Set<JMethod> calleeMethods) {
    assert (calleeMethods != null);
    Set<JMethod> callerMethods = Sets.newLinkedHashSet();
    for (JMethod calleeMethod : calleeMethods) {
      callerMethods.addAll(calleeCallersPairs.get(calleeMethod));
    }
    return callerMethods;
  }
}
