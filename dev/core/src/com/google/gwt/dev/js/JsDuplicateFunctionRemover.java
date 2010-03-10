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
import com.google.gwt.dev.js.ast.JsExpression;
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

    private final Stack<JsNameRef> invocationQualifiers = new Stack<JsNameRef>();

    private final Map<String, JsName> uniqueBodies = new HashMap<String, JsName>();

    public DuplicateFunctionBodyRecorder() {
      // Add sentinel to stop Stack.peek() from throwing exception.
      invocationQualifiers.add(null);
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      if (x.getQualifier() instanceof JsNameRef) {
        invocationQualifiers.pop();
      }
    }

    @Override
    public void endVisit(JsNameOf x, JsContext<JsExpression> ctx) {
      dontReplace.add(x.getName());
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
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

    @Override
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
      /*
       * Don't process anonymous functions.
       */
      if (x.getName() != null) {
        String fnSource = x.toSource();
        String body = fnSource.substring(fnSource.indexOf("("));
        JsName original = uniqueBodies.get(body);
        if (original != null) {
          duplicateOriginalMap.put(x.getName(), original);
        } else {
          uniqueBodies.put(body, x.getName());
        }
      }
      return true;
    }

    @Override
    public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
      if (x.getQualifier() instanceof JsNameRef) {
        invocationQualifiers.push((JsNameRef) x.getQualifier());
      }
      return true;
    }
  }

  private class ReplaceDuplicateInvocationNameRefs extends JsModVisitor {

    private final Set<JsName> blacklist;

    private final Map<JsName, JsName> duplicateMap;

    public ReplaceDuplicateInvocationNameRefs(Map<JsName, JsName> duplicateMap,
        Set<JsName> blacklist) {
      this.duplicateMap = duplicateMap;
      this.blacklist = blacklist;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
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
    ReplaceDuplicateInvocationNameRefs rdup = new ReplaceDuplicateInvocationNameRefs(
        dfbr.getDuplicateMap(), dfbr.getBlacklist());
    rdup.accept(fragment);
    return rdup.didChange();
  }
}
