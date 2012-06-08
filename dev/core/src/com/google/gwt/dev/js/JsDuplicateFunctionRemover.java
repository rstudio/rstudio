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
import com.google.gwt.dev.util.collect.IdentityHashSet;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Replace references to functions which have post-obfuscation duplicate bodies
 * by reference to a canonical one. Intended to run only when stack trace
 * stripping is enabled.
 */
public class JsDuplicateFunctionRemover {

  private class DuplicateFunctionBodyRecorder extends JsVisitor {

    private final Set<JsName> dontReplace = new IdentityHashSet<JsName>();

    private final Map<JsName, JsName> duplicateOriginalMap = new IdentityHashMap<JsName, JsName>();

    private final Map<JsFunction, JsFunction> duplicateMethodOriginalMap = new IdentityHashMap<JsFunction, JsFunction>();


    private final Stack<JsNameRef> invocationQualifiers = new Stack<JsNameRef>();

    // static / global methods
    private final Map<String, JsName> uniqueBodies = new HashMap<String, JsName>();

    // vtable methods
    private final Map<String, JsFunction> uniqueMethodBodies = new HashMap<String, JsFunction>();

    public DuplicateFunctionBodyRecorder() {
      // Add sentinel to stop Stack.peek() from throwing exception.
      invocationQualifiers.add(null);
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

  // Needed for OptimizerTestBase
  public static boolean exec(JsProgram program) {
    return new JsDuplicateFunctionRemover(program).execImpl(program.getFragmentBlock(0));
  }

  public static boolean exec(JsProgram program, JsBlock fragment) {
    return new JsDuplicateFunctionRemover(program).execImpl(fragment);
  }

  private final JsProgram program;

  public JsDuplicateFunctionRemover(JsProgram program) {
    this.program = program;
  }

  private boolean execImpl(JsBlock fragment) {
    DuplicateFunctionBodyRecorder dfbr = new DuplicateFunctionBodyRecorder();
    dfbr.accept(fragment);
    int count = 0;
    Map<JsFunction, JsName> hoistMap = new HashMap<JsFunction, JsName>();
    // Hoist all anonymous versions
    Map<JsFunction, JsFunction> dupMethodMap = dfbr.getDuplicateMethodMap();
    for (JsFunction x : dupMethodMap.values()) {
      if (!hoistMap.containsKey(x)) {
        // move function to top scope and re-declaring it with a unique name
        JsName newName = program.getScope().declareName("_DUP" + count++);
        JsFunction newFunc = new JsFunction(x.getSourceInfo(),
            program.getScope(), newName, x.isFromJava());
        // we're not using the old function anymore, we can use reuse the body instead of cloning it
        newFunc.setBody(x.getBody());
        // also copy the parameters from the old function
        newFunc.getParameters().addAll(x.getParameters());
        // add the new function to the top level list of statements
        fragment.getStatements().add(newFunc.makeStmt());
        hoistMap.put(x, newName);
      }
    }

    ReplaceDuplicateInvocationNameRefs rdup = new ReplaceDuplicateInvocationNameRefs(
        dfbr.getDuplicateMap(), dfbr.getBlacklist(), dupMethodMap, hoistMap);
    rdup.accept(fragment);
    return rdup.didChange();
  }
}
