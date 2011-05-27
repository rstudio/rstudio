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
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

/**
 * Update polymorphic method calls to tighter bindings based on the type of the
 * qualifier. For a given polymorphic method call to a non-final target, see if
 * the static type of the qualifer would let us target an override instead.
 * 
 * This is possible because the qualifier might have been tightened by
 * {@link com.google.gwt.dev.jjs.impl.TypeTightener}.
 */
public class MethodCallTightener {
  /**
   * Updates polymorphic method calls to tighter bindings based on the type of
   * the qualifier.
   */
  public class MethodCallTighteningVisitor extends JModVisitor {

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // The method call is already known statically
      if (!x.canBePolymorphic()) {
        return;
      }

      JReferenceType instanceType =
          ((JReferenceType) x.getInstance().getType()).getUnderlyingType();
      if (!(instanceType instanceof JClassType)) {
        // Cannot tighten.
        return;
      }

      JMethod method = x.getTarget();
      if (method == runAsyncOnsuccess) {
        // Do not defeat code splitting.
        return;
      }
      if (instanceType == method.getEnclosingType()) {
        // Cannot tighten.
        return;
      }

      JMethod foundMethod =
          program.typeOracle.getPolyMethod((JClassType) instanceType, method.getSignature());
      if (foundMethod == null) {
        // The declared instance type is abstract and doesn't have the method.
        return;
      }
      if (foundMethod == method) {
        // The instance type doesn't override the method.
        return;
      }
      assert foundMethod.canBePolymorphic();

      /*
       * Replace the call to the original method with a call to the same method
       * on the tighter type.
       */
      JMethodCall call = new JMethodCall(x.getSourceInfo(), x.getInstance(), foundMethod);
      call.addArgs(x.getArgs());
      ctx.replaceMe(call);
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not tighten new operations.
    }
  }

  public static final String NAME = MethodCallTightener.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodCallTightener(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private final JProgram program;
  private final JMethod runAsyncOnsuccess;

  private MethodCallTightener(JProgram program) {
    this.program = program;
    runAsyncOnsuccess = program.getIndexedMethod("RunAsyncCallback.onSuccess");
  }

  private OptimizerStats execImpl() {
    MethodCallTighteningVisitor tightener = new MethodCallTighteningVisitor();
    tightener.accept(program);
    return new OptimizerStats(NAME).recordModified(tightener.getNumMods());
  }
}