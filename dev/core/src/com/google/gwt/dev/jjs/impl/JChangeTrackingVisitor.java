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
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;

/**
 * A visitor for optimizing an AST.
 */
public abstract class JChangeTrackingVisitor extends JModVisitor {

  private JField currentField;
  private JMethod currentMethod;
  private final OptimizerContext optimizerCtx;
  private boolean fieldModified = false;
  private boolean methodModified = false;

  public JChangeTrackingVisitor(OptimizerContext optimizerCtx) {
    this.optimizerCtx = optimizerCtx;
  }

  public boolean enterField(JField f, Context ctx) {
    return true;
  }

  public boolean enterMethod(JMethod x, Context ctx) {
    return true;
  }

  public JField getCurrentField() {
    return currentField;
  }

  public JMethod getCurrentMethod() {
    return currentMethod;
  }

  public void exitField(JField f, Context ctx) {
    return;
  }

  public void exitMethod(JMethod x, Context ctx) {
    return;
  }

  @Override
  public final void endVisit(JField x, Context ctx) {
    exitField(x, ctx);
    if (fieldModified) {
      optimizerCtx.markModifiedField(x);
    }
    currentField = null;
  }

  @Override
  public final void endVisit(JMethod x, Context ctx) {
    exitMethod(x, ctx);
    if (methodModified) {
      optimizerCtx.markModifiedMethod(x);
    }
    currentMethod = null;
  }

  @Override
  public final boolean visit(JField x, Context ctx) {
    currentField = x;
    fieldModified = false;
    return enterField(x, ctx);
  }

  @Override
  public final boolean visit(JMethod x, Context ctx) {
    currentMethod = x;
    methodModified = false;
    return enterMethod(x, ctx);
  }

  @Override
  protected void madeChanges() {
    super.madeChanges();
    if (currentMethod != null) {
      methodModified = true;
    }
    if (currentField != null) {
      fieldModified = true;
    }
  }

  public void fieldWasRemoved(JField field) {
    optimizerCtx.removeField(field);
  }

  public void methodWasRemoved(JMethod method) {
    optimizerCtx.removeMethod(method);
  }
}
