/*
 * Copyright 2008 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

/**
 * Most autoboxing is handled by {@link GenerateJavaAST}. The only cases it does
 * not handle are <code>++</code>, <code>--</code>, and compound assignment
 * operations (<code>+=</code>, etc.) when applied to a boxed type. This class
 * fixes such cases in two steps. First, an internal subclass of
 * {@link CompoundAssignmentNormalizer} simplifies such expressions to a simple
 * assignment expression. Second, this visitor replaces an assignment to an
 * unboxing method (<code>unbox(x) = unbox(x) + 1</code>) with an assignment to
 * the underlying box (<code>x = box(unbox(x) + 1)</code>).
 * 
 * <p>
 * Update: GenerateJavaAST can also leave invalid AST structures of the form
 * <code>(Foo) x = foo</code> due to the way generics are handled. This can
 * happen when assigning into a field of a generic type. We'll go ahead and
 * resolve that case here as well.
 * </p>
 */
public class FixAssignmentToUnbox extends JModVisitor {
  /**
   * Normalize compound assignments where the lhs is an unbox operation.
   */
  private static class CompoundAssignmentToUnboxNormalizer extends CompoundAssignmentNormalizer {
    private final AutoboxUtils autoboxUtils;

    protected CompoundAssignmentToUnboxNormalizer(JProgram program) {
      autoboxUtils = new AutoboxUtils(program);
    }

    /**
     * If the lhs is an unbox operation, then return the box rather than the
     * original value.
     */
    @Override
    protected JExpression expressionToReturn(JExpression lhs) {
      JExpression boxed = autoboxUtils.undoUnbox(lhs);
      if (boxed != null) {
        return boxed;
      }
      return lhs;
    }

    @Override
    protected boolean shouldBreakUp(JBinaryOperation x) {
      return isUnboxExpression(x.getLhs());
    }

    @Override
    protected boolean shouldBreakUp(JPostfixOperation x) {
      return isUnboxExpression(x.getArg());
    }

    @Override
    protected boolean shouldBreakUp(JPrefixOperation x) {
      return isUnboxExpression(x.getArg());
    }

    private boolean isUnboxExpression(JExpression x) {
      return (autoboxUtils.undoUnbox(x) != null);
    }
  }

  public static void exec(JProgram program) {
    Event fixAssignmentToUnboxEvent =
        SpeedTracerLogger.start(CompilerEventType.FIX_ASSIGNMENT_TO_UNBOX);
    new CompoundAssignmentToUnboxNormalizer(program).accept(program);
    new FixAssignmentToUnbox(program).accept(program);
    fixAssignmentToUnboxEvent.end();
  }

  private final AutoboxUtils autoboxUtils;

  private FixAssignmentToUnbox(JProgram program) {
    this.autoboxUtils = new AutoboxUtils(program);
  }

  @Override
  public void endVisit(JBinaryOperation x, Context ctx) {
    if (x.getOp() != JBinaryOperator.ASG) {
      return;
    }

    JExpression lhs = x.getLhs();
    JExpression boxed = autoboxUtils.undoUnbox(lhs);
    if (boxed != null) {
      // Assignment-to-unbox, e.g.
      // unbox(x) = foo -> x = box(foo)
      JClassType boxedType = (JClassType) boxed.getType();
      ctx.replaceMe(new JBinaryOperation(x.getSourceInfo(), boxedType, JBinaryOperator.ASG, boxed,
          autoboxUtils.box(x.getRhs(), (JPrimitiveType) lhs.getType())));
      return;
    }

    if (lhs instanceof JCastOperation) {
      // Assignment-to-cast-operation, e.g.
      // (Foo) x = foo -> x = foo
      JCastOperation cast = (JCastOperation) lhs;
      JBinaryOperation newAsg =
          new JBinaryOperation(x.getSourceInfo(), x.getType(), JBinaryOperator.ASG, cast.getExpr(),
              x.getRhs());
      ctx.replaceMe(newAsg);
    }
  }
}
