/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.Stack;

/**
 * Resolves any unresolved JsNameRefs.
 */
public class JsSymbolResolver {

  /**
   * Resolves any unresolved JsNameRefs.
   */
  private class JsResolveSymbolsVisitor extends JsVisitor {

    private final Stack scopeStack = new Stack();

    public void endVisit(JsCatch x, JsContext ctx) {
      popScope();
    }

    public void endVisit(JsFunction x, JsContext ctx) {
      popScope();
    }

    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x.isResolved()) {
        return;
      }

      JsName name;
      String ident = x.getIdent();
      if (x.getQualifier() == null) {
        name = getScope().findExistingName(ident);
        if (name == null) {
          // No clue what this is; create a new unobfuscatable name
          name = program.getRootScope().declareName(ident);
          name.setObfuscatable(false);
        }
      } else {
        name = program.getObjectScope().findExistingName(ident);
        if (name == null) {
          // No clue what this is; create a new unobfuscatable name
          name = program.getObjectScope().declareName(ident);
          name.setObfuscatable(false);
        }
      }
      x.resolve(name);
    }

    public void endVisit(JsProgram x, JsContext ctx) {
      popScope();
    }

    public boolean visit(JsCatch x, JsContext ctx) {
      pushScope(x.getScope());
      return true;
    }

    public boolean visit(JsFunction x, JsContext ctx) {
      pushScope(x.getScope());
      return true;
    }

    public boolean visit(JsProgram x, JsContext ctx) {
      pushScope(x.getScope());
      return true;
    }

    private JsScope getScope() {
      return (JsScope) scopeStack.peek();
    }

    private void popScope() {
      scopeStack.pop();
    }

    private void pushScope(JsScope scope) {
      scopeStack.push(scope);
    }
  }

  public static void exec(JsProgram program) {
    new JsSymbolResolver(program).execImpl();
  }

  private final JsProgram program;

  private JsSymbolResolver(JsProgram program) {
    this.program = program;
  }

  private void execImpl() {
    JsResolveSymbolsVisitor resolver = new JsResolveSymbolsVisitor();
    resolver.accept(program);
  }
}
