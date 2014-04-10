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
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.List;

/**
 * For each JMethodCall that contains a specialization, retarget the method
 * to the specialized call if possible. Depends on call tightening,
 * staticizing, and finalizing having been successful on the
 * method of interest.
 */
public class MethodCallSpecializer {

  private class MethodCallSpecializingVisitor extends JModVisitor {

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // Don't do anything if the method binding isn't statically known
      if (x.isVolatile() || x.canBePolymorphic() || x.getTarget().isAbstract()
          || !x.getTarget().isFinal() || !x.getTarget().isStatic()) {
        return;
      }

      JMethod method = x.getTarget();
      JMethod.Specialization specialization = method.getSpecialization();

      if (specialization == null) {
        return;
      }

      List<JType> params = specialization.getParams();
      if (params.size() == x.getArgs().size()) {
        for (int i = 0; i < params.size(); i++) {
          JType argType = x.getArgs().get(i).getType();
          if (argType instanceof JNonNullType) {
            argType = ((JNonNullType) argType).getUnderlyingType();
          }
          if (argType instanceof JNullType) {
            return;
          }
          // see if the args passed to the function can be cast to the
          // specialization pattern
          JType qType = params.get(i);
          if (qType instanceof JNonNullType) {
            qType = ((JNonNullType) qType).getUnderlyingType();
          }
          if (!program.typeOracle.canTriviallyCast(argType, qType)) {
            // params don't match
            return;
          }
        }

        JMethod targetMethod = specialization.getTargetMethod();
        if (targetMethod != null) {
          JMethodCall call = new JMethodCall(x.getSourceInfo(),
              x.getInstance(), targetMethod);
          call.addArgs(x.getArgs());
          ctx.replaceMe(call);
        }
      }
    }
  }

  public static final String NAME = MethodCallSpecializer.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodCallSpecializer(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private final JProgram program;

  private MethodCallSpecializer(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl() {
    MethodCallSpecializingVisitor specializer =
        new MethodCallSpecializingVisitor();
    specializer.accept(program);
    return new OptimizerStats(NAME).recordModified(specializer.getNumMods());
  }
}