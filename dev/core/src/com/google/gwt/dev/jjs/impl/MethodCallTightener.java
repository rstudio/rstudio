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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;

/**
 * Update polymorphic method calls to tighter bindings based on the type of the
 * qualifier. For a given polymorphic method call to a non-final target, see if
 * the static type of the qualifer would let us target an override instead.
 *
 * This is possible because the qualifier might have been tightened by
 * {@link com.google.gwt.dev.jjs.impl.TypeTightener}.
 *
 * For example, given the code:
 *
 * <pre>
 *   List foo = new ArrayList<String>();
 *   foo.add("bar");
 * </pre>
 *
 * The type of foo is tightened by TypeTightener from type List to be of type
 * ArrayList. This means that MethodCallTightener can analyze the polymorphic
 * call List.add() on foo and tighten it to the more specific ArrayList.add().
 */
public class MethodCallTightener {
  /**
   * Updates polymorphic method calls to tighter bindings based on the type of
   * the qualifier.
   */
  public class MethodCallTighteningVisitor extends JChangeTrackingVisitor {

    public MethodCallTighteningVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // The method call is already known statically
      if (x.isVolatile() || !x.canBePolymorphic()) {
        return;
      }

      JType instanceType = x.getInstance().getType().getUnderlyingType();
      if (!(instanceType instanceof JClassType)) {
        // Cannot tighten.
        return;
      }

      JMethodCall newCall = maybeUpgradeToNonPolymorphicCall(x);

      // If the call is still polymorphic, try tightening the method.
      if (newCall.canBePolymorphic()) {
        newCall = maybeTightenMethodCall(newCall);
      }

      if (newCall != x) {
        ctx.replaceMe(newCall);
      }
    }

    private JMethodCall maybeUpgradeToNonPolymorphicCall(JMethodCall x) {
      JReferenceType instanceType = (JReferenceType) x.getInstance().getType();

      if (!instanceType.canBeSubclass()) {
        // Mark a call as non-polymorphic if the targeted type is guaranteed to be not a subclass.
        x = maybeTightenMethodCall(x);
        x.setCannotBePolymorphic();
        madeChanges();
      } else if (!hasPotentialOverride(instanceType, x.getTarget())) {
        // Mark a call as non-polymorphic if the targeted method is the only possible dispatch.
        x.setCannotBePolymorphic();
        madeChanges();
       }

      return x;
    }

    private boolean hasPotentialOverride(JReferenceType instanceType, JMethod target) {
      if (target.isAbstract()) {
        return true;
      }

      for (JMethod override : target.getOverridingMethods()) {
        JReferenceType overrideType = override.getEnclosingType();
        if (!program.typeOracle.castFailsTrivially(instanceType, overrideType)) {
          // This call is truly polymorphic.
          return true;
        }
      }

      return false;
    }

    private JMethodCall maybeTightenMethodCall(final JMethodCall methodCall) {
      JMethod original = methodCall.getTarget();
      JClassType underlyingType =
          (JClassType) methodCall.getInstance().getType().getUnderlyingType();

      JMethod mostSpecificOverride =
          program.typeOracle.findMostSpecificOverride(underlyingType, original);

      if (mostSpecificOverride == original) {
        return methodCall;
      }
      JMethodCall newCall = new JMethodCall(
          methodCall.getSourceInfo(), methodCall.getInstance(), mostSpecificOverride);
      newCall.addArgs(methodCall.getArgs());
      return newCall;
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not tighten new operations.
    }

    @Override
    public boolean visit(JRunAsync x, Context ctx) {
      x.traverseOnSuccess(this);
      return super.visit(x, ctx);
    }
  }

  public static final String NAME = MethodCallTightener.class.getSimpleName();

  @VisibleForTesting
  static OptimizerStats exec(JProgram program) {
    return exec(program, OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
  }

  public static OptimizerStats exec(JProgram program, OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodCallTightener(program).execImpl(optimizerCtx);
    optimizerCtx.incOptimizationStep();
    optimizeEvent.end("didChange", "" + stats.didChange());
    JavaAstVerifier.assertProgramIsConsistent(program);
    return stats;
  }

  private final JProgram program;

  private MethodCallTightener(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    MethodCallTighteningVisitor tightener = new MethodCallTighteningVisitor(optimizerCtx);
    tightener.accept(program);
    return new OptimizerStats(NAME).recordModified(tightener.getNumMods());
  }
}
