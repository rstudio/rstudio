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
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.collect.Stack;

/**
 * Base class for any recursive resolver classes.
 */
public abstract class JsAbstractSymbolResolver extends JsVisitor {

  private final Stack<JsScope> scopeStack = new Stack<JsScope>();

  @Override
  public void endVisit(JsCatch x, JsContext ctx) {
    popScope();
  }

  @Override
  public void endVisit(JsFunction x, JsContext ctx) {
    popScope();
  }

  @Override
  public void endVisit(JsNameRef x, JsContext ctx) {
    if (x.isResolved()) {
      return;
    }

    if (x.getQualifier() != null) {
      resolveQualifiedName(x);
    } else {
      resolveUnqualifiedName(x);
    }
  }

  @Override
  public void endVisit(JsProgram x, JsContext ctx) {
    popScope();
  }

  @Override
  public boolean visit(JsCatch x, JsContext ctx) {
    pushScope(x.getScope());
    return true;
  }

  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    pushScope(x.getScope());
    return true;
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    pushScope(x.getScope());
    return true;
  }

  @Override
  public boolean visit(JsPropertyInitializer x, JsContext ctx) {
    if (x.getLabelExpr() instanceof JsNameRef) {
      // JsNameRefs in labels of object literals are considered qualified names even though they
      // are created with no qualifier, matching their use as regular object property names.
      resolveQualifiedName((JsNameRef) x.getLabelExpr());
    }
    accept(x.getValueExpr());
    return false;
  }

  protected JsScope getScope() {
    return scopeStack.peek();
  }

  protected abstract void resolveQualifiedName(JsNameRef x);

  protected abstract void resolveUnqualifiedName(JsNameRef x);

  private void popScope() {
    scopeStack.pop();
  }

  private void pushScope(JsScope scope) {
    scopeStack.push(scope);
  }
}
