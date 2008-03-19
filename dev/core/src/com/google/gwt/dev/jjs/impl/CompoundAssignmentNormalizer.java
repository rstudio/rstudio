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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Replace any complex assignments that will cause problems down the road with
 * broken expressions; replace side-effect expressions in the lhs with temps to
 * prevent multiple evaluation.
 */
public class CompoundAssignmentNormalizer {

  /**
   * Breaks apart certain complex assignments.
   */
  private class BreakupAssignOpsVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      if (op.getNonAssignmentOf() == null) {
        return;
      }

      boolean doIt = false;
      if (x.getType() == program.getTypePrimitiveLong()) {
        doIt = true;
      }
      if (op == JBinaryOperator.ASG_DIV
          && x.getType() != program.getTypePrimitiveFloat()
          && x.getType() != program.getTypePrimitiveDouble()) {
        doIt = true;
      }

      if (!doIt) {
        return;
      }

      /*
       * Convert to an assignment and binary operation. Since the left hand size
       * must be computed twice, we have to replace any left-hand side
       * expressions that could have side effects with temporaries, so that they
       * are only run once.
       */
      final int pushLocalIndex = localIndex;
      ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(
          new JMultiExpression(program, x.getSourceInfo()));
      JExpression newLhs = replacer.accept(x.getLhs());
      localIndex = pushLocalIndex;

      JBinaryOperation operation = new JBinaryOperation(program,
          x.getSourceInfo(), newLhs.getType(), op.getNonAssignmentOf(), newLhs,
          x.getRhs());
      JBinaryOperation asg = new JBinaryOperation(program, x.getSourceInfo(),
          newLhs.getType(), JBinaryOperator.ASG, newLhs, operation);

      JMultiExpression multiExpr = replacer.getMultiExpr();
      if (multiExpr.exprs.isEmpty()) {
        // just use the split assignment expression
        ctx.replaceMe(asg);
      } else {
        // add the assignment as the last item in the multi
        multiExpr.exprs.add(asg);
        ctx.replaceMe(multiExpr);
      }
    }

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      clearLocals();
      currentMethodBody = null;
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      JUnaryOperator op = x.getOp();
      if (!op.isModifying()) {
        return;
      }
      if (x.getType() != program.getTypePrimitiveLong()) {
        return;
      }

      // Convert into a comma operation, such as:
      // (t = x, x += 1, t)

      // First, replace the arg with a non-side-effect causing one.
      final int pushLocalIndex = localIndex;
      JMultiExpression multi = new JMultiExpression(program, x.getSourceInfo());
      ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(
          multi);
      JExpression newArg = replacer.accept(x.getArg());

      // Now generate the appropriate expressions.
      JLocal tempLocal = getTempLocal(newArg.getType());

      // t = x
      JLocalRef tempRef = new JLocalRef(program, x.getSourceInfo(), tempLocal);
      JBinaryOperation asg = new JBinaryOperation(program, x.getSourceInfo(),
          x.getType(), JBinaryOperator.ASG, tempRef, newArg);
      multi.exprs.add(asg);

      // x += 1
      asg = createAsgOpFromUnary(newArg, op);
      // Break the resulting asg op before adding to multi.
      multi.exprs.add(accept(asg));

      // t
      tempRef = new JLocalRef(program, x.getSourceInfo(), tempLocal);
      multi.exprs.add(tempRef);

      ctx.replaceMe(multi);
      localIndex = pushLocalIndex;
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      JUnaryOperator op = x.getOp();
      if (!op.isModifying()) {
        return;
      }
      if (x.getType() != program.getTypePrimitiveLong()) {
        return;
      }

      // Convert into the equivalent binary assignment operation, such as:
      // x += 1
      JBinaryOperation asg = createAsgOpFromUnary(x.getArg(), op);

      // Visit the result to break it up even more.
      ctx.replaceMe(accept(asg));
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody = x;
      clearLocals();
      return true;
    }

    private JBinaryOperation createAsgOpFromUnary(JExpression arg,
        JUnaryOperator op) {
      JBinaryOperator newOp;
      if (op == JUnaryOperator.INC) {
        newOp = JBinaryOperator.ASG_ADD;
      } else if (op == JUnaryOperator.DEC) {
        newOp = JBinaryOperator.ASG_SUB;
      } else {
        throw new InternalCompilerException(
            "Unexpected modifying unary operator: "
                + String.valueOf(op.getSymbol()));
      }

      JBinaryOperation asg = new JBinaryOperation(program, arg.getSourceInfo(),
          arg.getType(), newOp, arg, program.getLiteralLong(1));
      return asg;
    }
  }
  /**
   * Replaces side effects in lvalue.
   */
  private class ReplaceSideEffectsInLvalue extends JModVisitor {

    private final JMultiExpression multi;

    ReplaceSideEffectsInLvalue(JMultiExpression multi) {
      this.multi = multi;
    }

    public JMultiExpression getMultiExpr() {
      return multi;
    }

    @Override
    public boolean visit(JArrayRef x, Context ctx) {
      JExpression newInstance = possiblyReplace(x.getInstance());
      JExpression newIndexExpr = possiblyReplace(x.getIndexExpr());
      if (newInstance != x.getInstance() || newIndexExpr != x.getIndexExpr()) {
        JArrayRef newExpr = new JArrayRef(program, x.getSourceInfo(),
            newInstance, newIndexExpr);
        ctx.replaceMe(newExpr);
      }
      return false;
    }

    @Override
    public boolean visit(JFieldRef x, Context ctx) {
      if (x.getInstance() != null) {
        JExpression newInstance = possiblyReplace(x.getInstance());
        if (newInstance != x.getInstance()) {
          JFieldRef newExpr = new JFieldRef(program, x.getSourceInfo(),
              newInstance, x.getField(), x.getEnclosingType());
          ctx.replaceMe(newExpr);
        }
      }
      return false;
    }

    @Override
    public boolean visit(JLocalRef x, Context ctx) {
      return false;
    }

    @Override
    public boolean visit(JParameterRef x, Context ctx) {
      return false;
    }

    @Override
    public boolean visit(JThisRef x, Context ctx) {
      return false;
    }

    private JExpression possiblyReplace(JExpression x) {
      if (!x.hasSideEffects()) {
        return x;
      }

      // Create a temp local
      JLocal tempLocal = getTempLocal(x.getType());

      // Create an assignment for this temp and add it to multi.
      JLocalRef tempRef = new JLocalRef(program, x.getSourceInfo(), tempLocal);
      JBinaryOperation asg = new JBinaryOperation(program, x.getSourceInfo(),
          x.getType(), JBinaryOperator.ASG, tempRef, x);
      multi.exprs.add(asg);
      // Update me with the temp
      return tempRef;
    }
  }

  public static void exec(JProgram program) {
    new CompoundAssignmentNormalizer(program).execImpl();
  }

  private JMethodBody currentMethodBody;
  private int localIndex;
  private final JProgram program;
  private final List<JLocal> tempLocals = new ArrayList<JLocal>();

  private CompoundAssignmentNormalizer(JProgram program) {
    this.program = program;
  }

  private void clearLocals() {
    tempLocals.clear();
    localIndex = 0;
  }

  private void execImpl() {
    BreakupAssignOpsVisitor breaker = new BreakupAssignOpsVisitor();
    breaker.accept(program);
  }

  private JLocal getTempLocal(JType type) {
    if (localIndex < tempLocals.size()) {
      return tempLocals.get(localIndex++);
    }
    JLocal newTemp = program.createLocal(null,
        ("$t" + localIndex++).toCharArray(), type, false, currentMethodBody);
    tempLocals.add(newTemp);
    return newTemp;
  }

}
