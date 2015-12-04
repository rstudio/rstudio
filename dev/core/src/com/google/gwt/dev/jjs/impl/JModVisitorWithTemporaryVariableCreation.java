/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.thirdparty.guava.common.collect.Queues;

import java.util.Deque;

/**
 * A JModVisitor capable of creating temporary local variables and placing their declarations in an
 * appropriate preceding place.
 */
public abstract class JModVisitorWithTemporaryVariableCreation extends JChangeTrackingVisitor {

  /**
   * Stack to keep track of where to insert the new variable declaration.
   * The top of the stack is the statement where declarations will be inserted.
   */
  private final Deque<Context> currentDeclarationInsertionPoint = Queues.newArrayDeque();

  public JModVisitorWithTemporaryVariableCreation(OptimizerContext optimizerCtx) {
    super(optimizerCtx);
  }

  @Override
  public final void endVisit(JStatement x, Context ctx) {
    if (ctx.canInsert()) {
      Context popped = currentDeclarationInsertionPoint.pop();
      assert popped == ctx;
    }
    super.endVisit(x, ctx);
  }

  @Override
  public final boolean visit(JStatement x, Context ctx) {
    if (ctx.canInsert()) {
      currentDeclarationInsertionPoint.push(ctx);
    }
    return super.visit(x, ctx);
  }

  protected JLocal createTempLocal(SourceInfo info, JType type) {
    assert !getCurrentMethod().isJsniMethod();
    JMethodBody currentMethodBody = (JMethodBody) getCurrentMethod().getBody();
    String temporaryLocalName = newTemporaryLocalName(info, type, currentMethodBody);
    JLocal local = JProgram.createLocal(info, temporaryLocalName, type, false, currentMethodBody);
    JDeclarationStatement declarationStatement =
        new JDeclarationStatement(info, local.makeRef(info), null);
    currentDeclarationInsertionPoint.peek().insertBefore(declarationStatement);
    return local;
  }

  protected abstract String newTemporaryLocalName(SourceInfo info, JType type,
      JMethodBody methodBody);
}
