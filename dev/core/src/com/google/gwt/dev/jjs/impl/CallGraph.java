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
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collection;
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

  private LinkedHashMultimap<JMethod, JMethod> calleeCallersPairs = LinkedHashMultimap.create();
  private LinkedHashMultimap<JMethod, JMethod> callerCalleesPairs = LinkedHashMultimap.create();

  /**
   * Add a caller method and its callee methods to the call graph.
   */
  public void addCallerMethod(JMethod callerMethod, Collection<JMethod> calleeMethods) {
    callerCalleesPairs.putAll(callerMethod, calleeMethods);
    for (JMethod calleeMethod : calleeMethods) {
      calleeCallersPairs.put(calleeMethod, callerMethod);
    }
  }

  /**
   * Build the call graph of a JProgram.
   */
  public void buildCallGraph(JProgram program) {
    resetCallGraph();
    BuildCallGraphVisitor buildCallGraphVisitor = new BuildCallGraphVisitor();
    buildCallGraphVisitor.accept(program);
  }

  /**
   * Return all the callee methods in the call graph.
   */
  public Set<JMethod> getAllCallees() {
    return calleeCallersPairs.keySet();
  }

  /**
   * Return all the callees of a set of caller methods.
   */
  public Set<JMethod> getCallees(Collection<JMethod> callerMethods) {
    assert (callerMethods != null);
    Set<JMethod> calleeMethods = Sets.newLinkedHashSet();
    for (JMethod callerMethod : callerMethods) {
      calleeMethods.addAll(callerCalleesPairs.get(callerMethod));
    }
    return calleeMethods;
  }

  /**
   * Return all the callers of a set of callee methods.
   */
  public Set<JMethod> getCallers(Collection<JMethod> calleeMethods) {
    assert (calleeMethods != null);
    Set<JMethod> callerMethods = Sets.newLinkedHashSet();
    for (JMethod calleeMethod : calleeMethods) {
      callerMethods.addAll(calleeCallersPairs.get(calleeMethod));
    }
    return callerMethods;
  }

  /**
   * Remove a callee method and all its caller methods in both
   * {@code calleeCallersPairs} and {@code callerCalleesPairs}.
   * Return its caller methods.
   */
  public Set<JMethod> removeCalleeMethod(JMethod calleeMethod) {
    Set<JMethod> callerMethods = calleeCallersPairs.removeAll(calleeMethod);
    for (JMethod callerMethod : callerMethods) {
      callerCalleesPairs.remove(callerMethod, calleeMethod);
    }
    return callerMethods;
  }

  /**
   * Remove a caller method and all its callee methods in both
   * {@code callerCalleesPairs} and {@code calleeCallersPairs}.
   * Return its callee methods.
   */
  public Set<JMethod> removeCallerMethod(JMethod callerMethod) {
    Set<JMethod> calleeMethods = callerCalleesPairs.removeAll(callerMethod);
    for (JMethod calleeMethod : calleeMethods) {
      calleeCallersPairs.remove(calleeMethod, callerMethod);
    }
    return calleeMethods;
  }

  public void resetCallGraph() {
    calleeCallersPairs.clear();
    callerCalleesPairs.clear();
  }

  /**
   * Update call graph of a JMethod. Record the deleted and added sub graph after the update in
   * {@code deletedSubCallGraph} and {@code addedSubCallGraph} respectively.
   */
  public void updateCallGraphOfMethod(JMethod method, CallGraph deletedSubCallGraph,
      CallGraph addedSubCallGraph) {
    Set<JMethod> calleeMethods = removeCallerMethod(method);
    BuildCallGraphVisitor callSiteVisitor = new BuildCallGraphVisitor();
    callSiteVisitor.accept(method);
    deletedSubCallGraph.addCallerMethod(method,
        Sets.difference(calleeMethods, callerCalleesPairs.get(method)));
    addedSubCallGraph.addCallerMethod(method,
        Sets.difference(callerCalleesPairs.get(method), calleeMethods));
  }
}
