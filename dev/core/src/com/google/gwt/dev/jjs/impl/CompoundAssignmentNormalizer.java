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
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
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

    // @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      /*
       * Convert to a normal divide operation so we can cast the result. Since
       * the left hand size must be computed twice, we have to replace any
       * left-hand side expressions that could have side effects with
       * temporaries, so that they are only run once.
       */
      if (x.getOp() == JBinaryOperator.ASG_DIV
          && x.getType() != program.getTypePrimitiveFloat()
          && x.getType() != program.getTypePrimitiveDouble()) {

        /*
         * Convert to a normal divide operation so we can cast the result. Since
         * the left hand size must be computed twice, we have to replace any
         * left-hand side expressions that could have side effects with
         * temporaries, so that they are only run once.
         */
        final int pushUsedLocals = localIndex;
        JMultiExpression multi = new JMultiExpression(program,
            x.getSourceInfo());
        ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(
            multi);
        JExpression newLhs = replacer.accept(x.getLhs());
        localIndex = pushUsedLocals;

        JNullLiteral litNull = program.getLiteralNull();
        JBinaryOperation operation = new JBinaryOperation(program,
            x.getSourceInfo(), newLhs.getType(), JBinaryOperator.DIV, newLhs,
            x.getRhs());
        JBinaryOperation asg = new JBinaryOperation(program, x.getSourceInfo(),
            newLhs.getType(), JBinaryOperator.ASG, newLhs, operation);

        JMultiExpression multiExpr = replacer.getMultiExpr();
        if (multiExpr.exprs.isEmpty()) {
          // just use the split assignment expression
          ctx.replaceMe(asg);
        } else {
          // add the assignment as the last item in the multi
          multi.exprs.add(asg);
          ctx.replaceMe(multi);
        }
      }
    }

    // @Override
    public void endVisit(JMethodBody x, Context ctx) {
      clearLocals();
      currentMethodBody = null;
    }

    // @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody = x;
      clearLocals();
      return true;
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

    // @Override
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

    // @Override
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

    // @Override
    public boolean visit(JLocalRef x, Context ctx) {
      return false;
    }

    // @Override
    public boolean visit(JParameterRef x, Context ctx) {
      return false;
    }

    // @Override
    public boolean visit(JThisRef x, Context ctx) {
      return false;
    }

    private JExpression possiblyReplace(JExpression x) {
      if (!x.hasSideEffects()) {
        return x;
      }

      // Create a temp local
      JLocal tempLocal = getTempLocal();

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
  private final List/* <JLocal> */tempLocals = new ArrayList/* <JLocal> */();

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

  private JLocal getTempLocal() {
    if (localIndex < tempLocals.size()) {
      return (JLocal) tempLocals.get(localIndex++);
    }
    JLocal newTemp = program.createLocal(null,
        ("$t" + localIndex++).toCharArray(), program.getTypeVoid(), false,
        currentMethodBody);
    tempLocals.add(newTemp);
    return newTemp;
  }

}
