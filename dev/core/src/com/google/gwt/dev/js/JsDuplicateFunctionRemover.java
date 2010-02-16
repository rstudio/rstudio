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
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Replace references to functions which have post-obfuscation duplicate bodies
 * by reference to a canonical one. Intended to run only when stack trace
 * stripping is enabled.
 */
public class JsDuplicateFunctionRemover {

  private class DuplicateFunctionBodyRecorder extends JsVisitor {

    Map<String, JsName> uniqueBodies = new HashMap<String, JsName>();
    Map<JsName, JsName> duplicateOriginalMap = new HashMap<JsName, JsName>();

    public Map<JsName, JsName> getDuplicateMap() {
      return duplicateOriginalMap;
    }

    @Override
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
      /*
       * At this point, unpruned zero-arg functions with empty 
       * bodies are Js constructor seed functions. 
       * If constructors are ever inlined into seed functions, revisit this.
       * Don't process anonymous functions.
       */
      if (x.getName() != null && x.getParameters().size() > 0 || 
          x.getBody().getStatements().size() > 0) {
        String fnSource = x.toSource();
        String body = fnSource.substring(fnSource.indexOf("("));
        JsName original = uniqueBodies.get(body);
        if (original != null) {
          duplicateOriginalMap.put(x.getName(), original);
        } else {
          uniqueBodies.put(body, x.getName());
        }
      }
      return false;
    }
  }

  private class ReplaceDuplicateInvocationNameRefs extends JsModVisitor {
    private Map<JsName, JsName> duplicateMap;

    public ReplaceDuplicateInvocationNameRefs(
        Map<JsName, JsName> duplicateMap) {
      this.duplicateMap = duplicateMap;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      JsName orig = duplicateMap.get(x.getName());
      if (orig != null && x.getName().getEnclosing() == program.getScope()) {
        ctx.replaceMe(orig.makeRef(x.getSourceInfo()));
      }
    }
  }

  public static boolean exec(JsProgram program, JsBlock fragment) {
    return new JsDuplicateFunctionRemover(program).execImpl(fragment);
  }

  private JsProgram program;

  public JsDuplicateFunctionRemover(JsProgram program) {
    this.program = program;
  }

  private boolean execImpl(JsBlock fragment) {
    DuplicateFunctionBodyRecorder dfbr = new DuplicateFunctionBodyRecorder();
    dfbr.accept(fragment);
    ReplaceDuplicateInvocationNameRefs rdup
        = new ReplaceDuplicateInvocationNameRefs(dfbr.getDuplicateMap());
    rdup.accept(fragment);
    return rdup.didChange();
  }
}
