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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.CanBeSetFinal;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Set;

/**
 * Finds all items are effectively final. That is, methods that are never  overridden and variables
 * that are never reassigned.
 * <p>
 * NOTE: Classes should not be marked final, since the analysis for classes is done through
 * {@link JAnalysisDecoratedType}.
 */
public class Finalizer {
  /**
   * Any items that weren't marked during MarkVisitor can be set final.
   *
   * Open question: What does it mean if an interface/abstract method becomes
   * final? Is this possible after Pruning? I guess it means that someone tried
   * to make a call to method that wasn't actually implemented anywhere in the
   * program. But if it wasn't implemented, then the enclosing class should have
   * come up as not instantiated and been culled. So I think it's not possible.
   */
  private class FinalizeVisitor extends JChangeTrackingVisitor {

    public FinalizeVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JConstructor x, Context ctx) {
      // Not applicable.
    }

    @Override
    public void exit(JField x, Context ctx) {
      if (x.isVolatile() || x.canBeImplementedExternally() || x.canBeReferencedExternally()) {
        return;
      }
      maybeFinalize(x);
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      maybeFinalize(x);
    }

    @Override
    public void exit(JMethod x, Context ctx) {
      if (!x.isFinal() && x.getOverridingMethods().isEmpty()) {
        setFinal(x);
      }
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      maybeFinalize(x);
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // Don't visit external types, because we can't change their final
      // specifiers.
      return !x.isExternal();
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      for (JLocal local : x.getLocals()) {
        maybeFinalize(local);
      }
      return false;
    }

    private void maybeFinalize(JVariable x) {
      if (!x.isFinal() && !isReassigned.contains(x)) {
        setFinal(x);
      }
    }

    private void setFinal(CanBeSetFinal x) {
      x.setFinal();
      assert x.isFinal();
      madeChanges();
    }
  }

  /**
   * Find all items that ARE overridden/subclassed/reassigned.
   */
  private class MarkVisitor extends JVisitor {
    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp().isAssignment()) {
        recordAssignment(x.getLhs());
      }
    }

    @Override
    public void endVisit(JConstructor x, Context ctx) {
      // Never overridden.
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      // This is should only be considered a reassignment if the uninitialized value for this field
      // is observable.
      // TODO(rluble): do static analysis to improve this pass.
      if (x.getVariableRef() instanceof JFieldRef) {
        JField field = ((JFieldRef) x.getVariableRef()).getField();
        if (field.getLiteralInitializer() != null &&
            !field.getLiteralInitializer().equals(field.getType().getDefaultValue())) {
          recordAssignment(x.getVariableRef());
        }
      }
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        recordAssignment(x.getArg());
      }
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        recordAssignment(x.getArg());
      }
    }

    @Override
    public void endVisit(JsniFieldRef x, Context ctx) {
      if (x.isLvalue()) {
        recordAssignment(x);
      }
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // Don't visit external types, because we can't change their final
      // specifiers.
      return !x.isExternal();
    }

    private void recordAssignment(JExpression lhs) {
      if (lhs instanceof JVariableRef) {
        JVariableRef variableRef = (JVariableRef) lhs;
        isReassigned.add(variableRef.getTarget());
      }
    }
  }

  private static final String NAME = Finalizer.class.getSimpleName();

  private Finalizer(JProgram program) {
    this.program = program;
  }

  @VisibleForTesting
  static OptimizerStats exec(JProgram program) {
    return exec(program, OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
  }

  public static OptimizerStats exec(JProgram program, OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new Finalizer(program).execImpl(optimizerCtx);
    optimizerCtx.incOptimizationStep();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private final Set<JVariable> isReassigned = Sets.newHashSet();

  private final JProgram program;

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    MarkVisitor marker = new MarkVisitor();
    marker.accept(program);

    FinalizeVisitor finalizer = new FinalizeVisitor(optimizerCtx);
    finalizer.accept(program);

    JavaAstVerifier.assertProgramIsConsistent(program);

    return new OptimizerStats(NAME).recordModified(finalizer.getNumMods());
  }
}
