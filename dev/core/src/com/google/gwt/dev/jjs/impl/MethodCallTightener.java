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
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
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
      JMethod method = x.getTarget();
      JExpression instance = x.getInstance();

      // The method call is already known statically
      if (!x.canBePolymorphic()) {
        return;
      }

      // Of the set of possible dispatches, see if there's one that subsumes the
      // rest.

      JReferenceType instanceType = ((JReferenceType) instance.getType()).getUnderlyingType();
      JReferenceType enclosingType = method.getEnclosingType();

      if (instanceType == enclosingType || !(instanceType instanceof JClassType)) {
        // Cannot tighten.
        return;
      }

      assert (instanceType instanceof JClassType);

      /*
       * Search myself and all my super types to find a tighter implementation
       * of the called method, if possible.
       */
      JMethod foundMethod = null;
      JClassType type;
      outer : for (type = (JClassType) instanceType; type != null && type != enclosingType; type =
          type.getSuperClass()) {
        for (JMethod methodIt : type.getMethods()) {
          if (methodIt.canBePolymorphic() && methodOverrides(methodIt, method)) {
            foundMethod = methodIt;
            break outer;
          }
        }
      }

      if (foundMethod == null) {
        return;
      }

      /*
       * Replace the call to the original method with a call to the same method
       * on the tighter type. Don't update the call's result type, however,
       * TypeTightener will handle that accurately.
       */
      JMethodCall call =
          new JMethodCall(x.getSourceInfo(), x.getInstance(), foundMethod, x.getType());
      call.addArgs(x.getArgs());
      ctx.replaceMe(call);
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not tighten new operations.
    }

    private boolean methodOverrides(JMethod subMethod, JMethod supMethod) {
      return subMethod.getSignature().equals(supMethod.getSignature());
    }
  }

  public static final String NAME = MethodCallTightener.class.getSimpleName();

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodCallTightener(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  /**
   * Tighten method calls that occur within <code>node</code> and its children.
   */
  public static void exec(JProgram program, JNode node) {
    new MethodCallTightener(program).execImpl(node);
  }

  private final JProgram program;

  private MethodCallTightener(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl() {
    MethodCallTighteningVisitor tightener = new MethodCallTighteningVisitor();
    tightener.accept(program);
    return new OptimizerStats(NAME).recordModified(tightener.getNumMods());
  }

  private void execImpl(JNode node) {
    MethodCallTighteningVisitor tightener = new MethodCallTighteningVisitor();
    tightener.accept(node);
  }
}
