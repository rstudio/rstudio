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
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVariable;

import java.util.Collection;

/**
 * A visitor for optimizing an AST.
 * <p>
 * Subclasses of JChangeTrackingVisitor should override enter/exit instead of visit/endVisit for
 * JMethod, JConstructor, JVariable and JField
 * <p>
 * Passes that modify the Java AST while an OptimizerContext is in effect should subclass this
 * class (JChangeTrackingVisitor) instead of {@link JModVisitor} or interact with
 * {@link OptimizerContext} directly.
 * <p>
 * Passes that would like to act only on parts of the tree related to the modification that happened
 * since the pass was last run should do something like:
 *   {@code
 *     String passName = "SomeUniqueNameForThisPass";
 *     int lastSeenOptmizationStep = optimizerContext.getLastStepFor(passName);
 *     Set<JMethod> modifiedMethods =
 *       optimizerContext.getModifiedMethodsSince(lastSeenOptmizationStep)
 *     Set<JField> modifiedFields =
 *       optimizerContext.getModifiedFieldsSince(lastSeenOptmizationStep)
 *     // compute potentially affected fields and methods and run the optimizer just on those
 *     ....
 *     //
 *     optimizerContext.setLastStepFor(passName, optimizerContext.getCurrentOptimizationStep());
 *     optimizerContext.incOptimizationStep;
 *   }
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

  @Override
  public final void endVisit(JConstructor x, Context ctx) {
    exit(x, ctx);
    if (methodModified) {
      optimizerCtx.markModified(x);
    }
    currentMethod = null;
  }

  @Override
  public final void endVisit(JField x, Context ctx) {
    exit(x, ctx);
    if (fieldModified) {
      optimizerCtx.markModified(x);
    }
    currentField = null;
  }

  @Override
  public final void endVisit(JMethod x, Context ctx) {
    exit(x, ctx);
    currentMethod = null;

    if (!methodModified) {
      return;
    }

    optimizerCtx.markModified(x);
    if (JProgram.isClinit(x) || JProgram.isInit(x)) {
      // Mark all class static fields as modified when a class clinit is modified to reflect
      // that when inline declaration statements are modified the corresponding field must
      // be considered modified.
      for (JField potentiallyModifiedField: x.getEnclosingType().getFields()) {
        if (potentiallyModifiedField.isStatic() && JProgram.isClinit(x)
            || !potentiallyModifiedField.isStatic() && !JProgram.isClinit(x)) {
          optimizerCtx.markModified(potentiallyModifiedField);
        }
      }
    }
  }

  @Override
  public final void endVisit(JVariable x, Context ctx) {
    exit(x, ctx);
  }

  public boolean enter(JConstructor x, Context ctx) {
    return enter((JMethod) x, ctx);
  }

  public boolean enter(JField x, Context ctx) {
    return enter((JVariable) x, ctx);
  }

  public boolean enter(JMethod x, Context ctx) {
    return true;
  }

  public boolean enter(JVariable x, Context ctx) {
    return true;
  }

  public void exit(JConstructor x, Context ctx) {
    exit((JMethod) x, ctx);
  }

  public void exit(JField x, Context ctx) {
    exit((JVariable) x, ctx);
  }

  public void exit(JMethod x, Context ctx) {
    return;
  }

  public void exit(JVariable x, Context ctx) {
    return;
  }

  public JField getCurrentField() {
    return currentField;
  }

  public JMethod getCurrentMethod() {
    return currentMethod;
  }

  @Override
  public final boolean visit(JConstructor x, Context ctx) {
    currentMethod = x;
    methodModified = false;
    return enter(x, ctx);
  }

  @Override
  public final boolean visit(JField x, Context ctx) {
    currentField = x;
    fieldModified = false;
    return enter(x, ctx);
  }

  @Override
  public final boolean visit(JMethod x, Context ctx) {
    currentMethod = x;
    methodModified = false;
    return enter(x, ctx);
  }

  @Override
  public final boolean visit(JVariable x, Context ctx) {
    return enter(x, ctx);
  }

  public final void wasRemoved(JField field) {
    optimizerCtx.remove(field);
  }

  public final void wasRemoved(JMethod method) {
    optimizerCtx.remove(method);
  }

  public final void methodsWereRemoved(Collection<JMethod> methods) {
    optimizerCtx.removeMethods(methods);
  }

  public final void fieldsWereRemoved(Collection<JField> fields) {
    optimizerCtx.removeFields(fields);
  }

  @Override
  protected final void madeChanges() {
    super.madeChanges();
    if (currentMethod != null) {
      methodModified = true;
    }
    if (currentField != null) {
      fieldModified = true;
    }
  }
}
