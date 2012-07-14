/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * A visitor that visits every location in the AST where instrumentation is
 * desirable.
 */
public abstract class CoverageVisitor extends JsModVisitor {
  private int lastLine = -1;
  private String lastFile = "";
  private Set<String> instrumentedFiles;

  /**
   * Nodes in this set are used in a context that expects a reference, not
   * just an arbitrary expression. For example, <code>delete</code> takes a
   * reference. These are tracked because it wouldn't be safe to rewrite
   * <code>delete foo.bar</code> to <code>delete (line='123',foo).bar</code>.
   */
  private final Set<JsNode> nodesInRefContext = Sets.newHashSet();

  public CoverageVisitor(Set<String> instrumentedFiles) {
    this.instrumentedFiles = instrumentedFiles;
  }

  @Override public void endVisit(JsArrayAccess x, JsContext ctx) {
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsBinaryOperation x, JsContext ctx) {
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsInvocation x, JsContext ctx) {
    nodesInRefContext.remove(x.getQualifier());
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsNameRef x, JsContext ctx) {
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsNew x, JsContext ctx) {
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsPostfixOperation x, JsContext ctx) {
    visitExpression(x, ctx);
  }

  @Override public void endVisit(JsPrefixOperation x, JsContext ctx) {
    visitExpression(x, ctx);
    nodesInRefContext.remove(x.getArg());
  }

  /**
   * This is essentially a hacked-up version of JsFor.traverse to account for
   * flow control differing from visitation order. It resets lastFile and
   * lastLine before the condition and increment expressions in the for loop
   * so that location data will be recorded correctly.
   */
  @Override public boolean visit(JsFor x, JsContext ctx) {
    if (x.getInitExpr() != null) {
      x.setInitExpr(accept(x.getInitExpr()));
    } else if (x.getInitVars() != null) {
      x.setInitVars(accept(x.getInitVars()));
    }

    if (x.getCondition() != null) {
      resetPosition();
      x.setCondition(accept(x.getCondition()));
    }

    if (x.getIncrExpr() != null) {
      resetPosition();
      x.setIncrExpr(accept(x.getIncrExpr()));
    }
    accept(x.getBody());
    return false;
  }

  @Override public boolean visit(JsInvocation x, JsContext ctx) {
    nodesInRefContext.add(x.getQualifier());
    return true;
  }

  @Override public boolean visit(JsPrefixOperation x, JsContext ctx) {
    if (x.getOperator() == JsUnaryOperator.DELETE
        || x.getOperator() == JsUnaryOperator.TYPEOF) {
      nodesInRefContext.add(x.getArg());
    }
    return true;
  }

  /**
   * Similar to JsFor, this resets the current location information before
   * evaluating the condition.
   */
  @Override public boolean visit(JsWhile x, JsContext ctx) {
    resetPosition();
    x.setCondition(accept(x.getCondition()));
    accept(x.getBody());
    return false;
  }

  protected abstract void endVisit(JsExpression x, JsContext ctx);

  private void resetPosition() {
    lastFile = "";
    lastLine = -1;
  }

  private void visitExpression(JsExpression x, JsContext ctx) {
    if (ctx.isLvalue()) {
      // Assignments to comma expressions aren't legal
      return;
    } else if (nodesInRefContext.contains(x)) {
      // Don't modify references into non-references
      return;
    } else if (!instrumentedFiles.contains(x.getSourceInfo().getFileName())) {
      return;
    } else if (x.getSourceInfo().getStartLine() == lastLine
        && (x.getSourceInfo().getFileName().equals(lastFile))) {
      return;
    }
    lastLine = x.getSourceInfo().getStartLine();
    lastFile = x.getSourceInfo().getFileName();
    endVisit(x, ctx);
  }
}