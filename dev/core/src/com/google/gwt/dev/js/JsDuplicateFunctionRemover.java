/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Replace references to functions which have post-obfuscation duplicate bodies
 * by reference to a canonical one. Intended to run only when stack trace
 * stripping is enabled.
 */
public class JsDuplicateFunctionRemover {

  private class DuplicateFunctionBodyRecorder extends JsVisitor {

    private final Set<JsName> dontReplace = Sets.newIdentityHashSet();

    private final Map<JsName, JsName> duplicateOriginalMap = Maps.newIdentityHashMap();

    private final Map<JsFunction, JsFunction> duplicateMethodOriginalMap = Maps.newLinkedHashMap();

    private final Stack<JsNameRef> invocationQualifiers = new Stack<JsNameRef>();

    // static / global methods
    private final Map<String, JsName> uniqueBodies = Maps.newHashMap();

    // vtable methods
    private final Map<String, JsFunction> uniqueMethodBodies = Maps.newHashMap();

    public DuplicateFunctionBodyRecorder() {
      // Add sentinel to stop Stack.peek() from throwing exception.
      invocationQualifiers.push(null);
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      if (x.getQualifier() instanceof JsNameRef) {
        invocationQualifiers.pop();
      }
    }

    @Override
    public void endVisit(JsNameOf x, JsContext ctx) {
      dontReplace.add(x.getName());
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x != invocationQualifiers.peek()) {
        if (x.getName() != null) {
          dontReplace.add(x.getName());
        }
      }
    }

    public Set<JsName> getBlacklist() {
      return dontReplace;
    }

    public Map<JsName, JsName> getDuplicateMap() {
      return duplicateOriginalMap;
    }

    public Map<JsFunction, JsFunction> getDuplicateMethodMap() {
      return duplicateMethodOriginalMap;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
      String fnSource = x.toSource();
      String body = fnSource.substring(fnSource.indexOf("("));
      /*
       * Static function processed separate from virtual functions
       */
      if (x.getName() != null) {
        JsName original = uniqueBodies.get(body);
        if (original != null) {
          duplicateOriginalMap.put(x.getName(), original);
        } else {
          uniqueBodies.put(body, x.getName());
        }
      } else if (x.isFromJava()) {
         JsFunction original = uniqueMethodBodies.get(body);
         if (original != null) {
           duplicateMethodOriginalMap.put(x, original);
         } else {
           uniqueMethodBodies.put(body, x);
         }
      }
      return true;
    }

    @Override
    public boolean visit(JsInvocation x, JsContext ctx) {
      if (x.getQualifier() instanceof JsNameRef) {
        invocationQualifiers.push((JsNameRef) x.getQualifier());
      }
      return true;
    }
  }

  private class ReplaceDuplicateInvocationNameRefs extends JsModVisitor {

    private final Set<JsName> blacklist;
    private final Map<JsFunction, JsFunction> dupMethodMap;
    private final Map<JsFunction, JsName> hoistMap;

    private final Map<JsName, JsName> duplicateMap;

    public ReplaceDuplicateInvocationNameRefs(Map<JsName, JsName> duplicateMap,
        Set<JsName> blacklist, Map<JsFunction, JsFunction> dupMethodMap,
        Map<JsFunction, JsName> hoistMap) {
      this.duplicateMap = duplicateMap;
      this.blacklist = blacklist;
      this.dupMethodMap = dupMethodMap;
      this.hoistMap = hoistMap;
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (dupMethodMap.containsKey(x)) {
        ctx.replaceMe(hoistMap.get(dupMethodMap.get(x)).makeRef(x.getSourceInfo()));
      } else if (hoistMap.containsKey(x)) {
        ctx.replaceMe(hoistMap.get(x).makeRef(x.getSourceInfo()));
      }
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      JsName orig = duplicateMap.get(x.getName());
      if (orig != null && x.getName() != null
          && x.getName().getEnclosing() == program.getScope()
          && !blacklist.contains(x.getName()) && !blacklist.contains(orig)) {
        ctx.replaceMe(orig.makeRef(x.getSourceInfo()));
      }
    }
  }

  /**
   * Entry point for the removeDuplicateFunctions optimization.
   *
   * This optimization will collapse functions whose JavaScript (output) code is identical. After
   * collapsing duplicate functions it will remove functions that become unreferenced as a result.
   *
   * This pass is safe only for JavaScript functions generated from Java where references to
   * local function variables can not be extruded by returning a function. E,g. in the next example
   *
   * function f1() {return a;}
   *
   * funcion f2() { var a; return function() {return a;}}
   *
   * f1() and the return of f2() are not duplicates even though the have a syntacticaly identical
   * parameters and body. The reason is that a in f1() refers to some globally scoped variable a,
   * whereas a in the return of f2() refers to the local variable a. It would be not correct to
   * move the return of f2() to the global scope.
   *
   * This situation does NOT arise from functions that where generated from Java sources (non
   * native)
   *
   * IMPORTANT NOTE: It is NOT safe to rename JsNames after this pass is performed. E.g.
   *
   * Consider an output  JavaScript for two unrelated classes:
   * defineClass(...) //class A
   * _.a
   * _.m1 = function() { return this.a; }
   *
   * defineClass(...) // class B
   * _.a
   * _.m2 = function() { return this.a; }
   *
   * Here m1() in class A and m2 in class B have identical parameters and bodies; hence the result
   * will be
   *
   * defineClass(...) //class A
   * _.a
   * _.m1 = g1
   *
   * defineClass(...) // class B
   * _.a
   * _.m2 = g1
   *
   * function g1() { return this.a; }
   *
   * The reference to this.a in g1 will be to either A.a or B.a and as long as those names remain
   * the same the removal was correct. However if A.a gets renamed then A.m1() and B.m2() would
   * no longer have been identical hence the dedup that is already done is incorrect.
   *
   * @param program the program to optimize
   * @param nameGenerator a freshNameGenerator to assign fresh names to deduped functions that are
   *                      lifted to the global scope
   * @return {@code true} if it made any changes; {@code false} otherwise.
   */
  public static boolean exec(JsProgram program, FreshNameGenerator nameGenerator) {
    return new JsDuplicateFunctionRemover(program, nameGenerator).execImpl();
  }

  private final JsProgram program;

  /**
   * A FreshNameGenerator instance to obtain fresh top scope names consistent with the
   * naming strategy used.
   */
  private FreshNameGenerator freshNameGenerator;


  public JsDuplicateFunctionRemover(JsProgram program, FreshNameGenerator freshNameGenerator) {
    this.program = program;
    this.freshNameGenerator = freshNameGenerator;
  }

  private boolean execImpl() {
    boolean changed = false;
    for (int i = 0; i < program.getFragmentCount(); i++) {
      JsBlock fragment = program.getFragmentBlock(i);

      DuplicateFunctionBodyRecorder dfbr = new DuplicateFunctionBodyRecorder();
      dfbr.accept(fragment);
      Map<JsFunction, JsName> newNamesByHoistedFunction = Maps.newHashMap();
      // Hoist all anonymous duplicate functions.
      Map<JsFunction, JsFunction> dupMethodMap = dfbr.getDuplicateMethodMap();
      for (JsFunction dupMethod : dupMethodMap.values()) {
        if (newNamesByHoistedFunction.containsKey(dupMethod)) {
          continue;
        }
        // move function to top scope and re-declaring it with a unique name
        JsName newName = program.getScope().declareName(freshNameGenerator.getFreshName());
        JsFunction newFunc = new JsFunction(dupMethod.getSourceInfo(),
            program.getScope(), newName, dupMethod.isFromJava());
        // we're not using the old function anymore, we can use reuse the body
        // instead of cloning it
        newFunc.setBody(dupMethod.getBody());
        // also copy the parameters from the old function
        newFunc.getParameters().addAll(dupMethod.getParameters());
        // add the new function to the top level list of statements
        fragment.getStatements().add(newFunc.makeStmt());
        newNamesByHoistedFunction.put(dupMethod, newName);
      }

      ReplaceDuplicateInvocationNameRefs rdup = new ReplaceDuplicateInvocationNameRefs(
          dfbr.getDuplicateMap(), dfbr.getBlacklist(), dupMethodMap, newNamesByHoistedFunction);
      rdup.accept(fragment);
      changed = changed || rdup.didChange();
    }

    if (changed) {
      JsUnusedFunctionRemover.exec(program);
    }
    return changed;
  }
}
