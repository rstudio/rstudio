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

import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsProgramFragment;
import com.google.gwt.dev.js.ast.JsStatement;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;

/**
 * Force all functions to be evaluated at the top of the lexical scope in which
 * they reside. This makes {@link StaticEvalVisitor} simpler in that we no
 * longer have to worry about function declarations within expressions. After
 * this runs, only statements can contain declarations. Moved functions will end
 * up just before the statement in which they presently reside.
 */
public class EvalFunctionsAtTopScope extends JsModVisitor {

  public static void exec(JsProgram jsProgram, JavaToJavaScriptMap map) {
    EvalFunctionsAtTopScope fev = new EvalFunctionsAtTopScope(map);
    fev.accept(jsProgram);
  }

  private JsStatement currentStatement;

  private final Set<JsFunction> dontMove = new HashSet<JsFunction>();

  private final Stack<ListIterator<JsStatement>> itrStack = new Stack<ListIterator<JsStatement>>();

  private JavaToJavaScriptMap java2jsMap;

  private final Stack<JsBlock> scopeStack = new Stack<JsBlock>();

  public EvalFunctionsAtTopScope(JavaToJavaScriptMap java2jsMap) {
    this.java2jsMap = java2jsMap;
  }

  @Override
  public void endVisit(JsExprStmt x, JsContext ctx) {
    currentStatement = null;
  }

  @Override
  public void endVisit(JsFunction x, JsContext ctx) {
    scopeStack.pop();
  }

  @Override
  public void endVisit(JsProgram x, JsContext ctx) {
    scopeStack.pop();
  }

  @Override
  public void endVisit(JsProgramFragment x, JsContext ctx) {
    scopeStack.pop();
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    if (x == scopeStack.peek()) {
      ListIterator<JsStatement> itr = x.getStatements().listIterator();
      itrStack.push(itr);
      while (itr.hasNext()) {
        JsStatement stmt = itr.next();
        JsFunction func = JsStaticEval.isFunctionDecl(stmt);
        // Already at the top level.
        if (func != null) {
          dontMove.add(func);
        }
        accept(stmt);
        if (func != null) {
          dontMove.remove(func);
        }
      }
      itrStack.pop();
      // Already visited.
      return false;
    } else {
      // Just do normal visitation.
      return true;
    }
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    currentStatement = x;
    return true;
  }

  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    JsStaticEval.isFunctionDecl(currentStatement);

    /*
     * We do this during visit() to preserve first-to-last evaluation order. We
     * check if this function is a vtable declaration and don't move functions
     * used in other expressions or are in vtable assignments.
     */
    if (x.getName() != null && x.getName().getNamespace() == null && !dontMove.contains(x)
        && !isVtableDeclaration(currentStatement)) {
      /*
       * Reinsert this function into the statement immediately before the
       * current statement. The current statement will have already been
       * returned from the current iterator's next(), so we have to backshuffle
       * one step to get in front of it.
       */
      ListIterator<JsStatement> itr = itrStack.peek();
      itr.previous();
      itr.add(x.makeStmt());
      itr.next();
      ctx.replaceMe(x.getName().makeRef(x.getSourceInfo().makeChild()));
    }

    // Dive into the function itself.
    scopeStack.push(x.getBody());
    return true;
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    scopeStack.push(x.getGlobalBlock());
    return true;
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext ctx) {
    scopeStack.push(x.getGlobalBlock());
    return true;
  }

  private boolean isVtableDeclaration(JsStatement currentStatement) {
    return java2jsMap.vtableInitToMethod(currentStatement) != null;
  }
}
