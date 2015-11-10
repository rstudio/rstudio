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
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

/**
 * <p>
 * Replace problematic compound assignments with a sequence of simpler
 * operations, all of which are either simple assignments or are non-assigning
 * operations. When doing so, be careful that side effects happen exactly once
 * and that the order of any side effects is preserved. The choice of which
 * assignments to replace is made in subclasses; they must override the three
 * <code>shouldBreakUp()</code> methods.
 * </p>
 *
 * <p>
 * Note that because AST nodes are mutable, they cannot be reused in different
 * parts of the same tree. Instead, the node must be cloned before each
 * insertion into a tree other than the first.
 * </p>
 */
public abstract class CompoundAssignmentNormalizer {
  /**
   * Breaks apart certain complex assignments.
   */
  private class BreakupAssignOpsVisitor extends JModVisitorWithTemporaryVariableCreation {

    public BreakupAssignOpsVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
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
          JArrayRef newExpr = new JArrayRef(x.getSourceInfo(), newInstance, newIndexExpr);
          ctx.replaceMe(newExpr);
        }
        return false;
      }

      @Override
      public boolean visit(JFieldRef x, Context ctx) {
        if (x.getInstance() != null) {
          JExpression newInstance = possiblyReplace(x.getInstance());
          if (newInstance != x.getInstance()) {
            JFieldRef newExpr =
                new JFieldRef(x.getSourceInfo(), newInstance, x.getField(), x.getEnclosingType());
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
        JLocal tempLocal = createTempLocal(x.getSourceInfo(), x.getType(), TEMP_LOCAL_NAME);

        // Create an assignment for this temp and add it to multi.
        JLocalRef tempRef = tempLocal.makeRef(x.getSourceInfo());
        JBinaryOperation asg =
            new JBinaryOperation(x.getSourceInfo(), x.getType(), JBinaryOperator.ASG, tempRef, x);
        multi.addExpressions(asg);
        // Update me with the temp
        return cloner.cloneExpression(tempRef);
      }
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      if (op.getNonAssignmentOf() == null) {
        return;
      }
      if (!shouldBreakUp(x)) {
        return;
      }

      /*
       * Convert to an assignment and binary operation. Since the left hand size
       * must be computed twice, we have to replace any left-hand side
       * expressions that could have side effects with temporaries, so that they
       * are only run once.
       */
      ReplaceSideEffectsInLvalue replacer =
          new ReplaceSideEffectsInLvalue(new JMultiExpression(x.getSourceInfo()));
      JExpression newLhs = replacer.accept(x.getLhs());

      JExpression operation =
          new JBinaryOperation(x.getSourceInfo(), newLhs.getType(), op.getNonAssignmentOf(),
              newLhs, x.getRhs());
      operation = modifyResultOperation((JBinaryOperation) operation);

      // newLhs is cloned below because it was used in operation
      JBinaryOperation asg =
          new JBinaryOperation(x.getSourceInfo(), newLhs.getType(), JBinaryOperator.ASG, cloner
              .cloneExpression(newLhs), operation);

      JMultiExpression multiExpr = replacer.getMultiExpr();
      if (multiExpr.isEmpty()) {
        // just use the split assignment expression
        ctx.replaceMe(asg);
      } else {
        // add the assignment as the last item in the multi
        multiExpr.addExpressions(asg);
        ctx.replaceMe(multiExpr);
      }
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      JUnaryOperator op = x.getOp();
      if (!op.isModifying()) {
        return;
      }
      if (!shouldBreakUp(x)) {
        return;
      }

      // Convert into a comma operation, such as:
      // (t = x, x += 1, t)

      // First, replace the arg with a non-side-effect causing one.
      JMultiExpression multi = new JMultiExpression(x.getSourceInfo());
      ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(multi);
      JExpression newArg = replacer.accept(x.getArg());

      JExpression expressionReturn = expressionToReturn(newArg);

      // Now generate the appropriate expressions.
      JLocal tempLocal =
          createTempLocal(x.getSourceInfo(), expressionReturn.getType(), TEMP_LOCAL_NAME);

      // t = x
      JLocalRef tempRef = tempLocal.makeRef(x.getSourceInfo());
      JBinaryOperation asg =
          new JBinaryOperation(x.getSourceInfo(), x.getType(), JBinaryOperator.ASG, tempRef,
              expressionReturn);
      multi.addExpressions(asg);

      // x += 1
      asg = createAsgOpFromUnary(newArg, op);
      // Break the resulting asg op before adding to multi.
      multi.addExpressions(accept(asg));

      // t
      tempRef = tempLocal.makeRef(x.getSourceInfo());
      multi.addExpressions(tempRef);

      ctx.replaceMe(multi);
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      JUnaryOperator op = x.getOp();
      if (!op.isModifying()) {
        return;
      }
      if (!shouldBreakUp(x)) {
        return;
      }

      // Convert into the equivalent binary assignment operation, such as:
      // x += 1
      JBinaryOperation asg = createAsgOpFromUnary(x.getArg(), op);

      // Visit the result to break it up even more.
      ctx.replaceMe(accept(asg));
    }

    private JBinaryOperation createAsgOpFromUnary(JExpression arg, JUnaryOperator op) {
      JBinaryOperator newOp;
      if (op == JUnaryOperator.INC) {
        newOp = JBinaryOperator.ASG_ADD;
      } else if (op == JUnaryOperator.DEC) {
        newOp = JBinaryOperator.ASG_SUB;
      } else {
        throw new InternalCompilerException("Unexpected modifying unary operator: "
            + String.valueOf(op.getSymbol()));
      }

      JExpression one;
      if (arg.getType() == JPrimitiveType.LONG) {
        // use an explicit long, so that LongEmulationNormalizer does not get
        // confused
        one = JLongLiteral.get(1);
      } else {
        // int is safe to add to all other types
        one = JIntLiteral.get(1);
      }
      // arg is cloned below because the caller is allowed to use it somewhere
      JBinaryOperation asg =
          new JBinaryOperation(arg.getSourceInfo(), arg.getType(), newOp, cloner
              .cloneExpression(arg), one);
      return asg;
    }
  }

  private final CloneExpressionVisitor cloner = new CloneExpressionVisitor();

  public void accept(JNode node) {
    BreakupAssignOpsVisitor breaker =
        new BreakupAssignOpsVisitor(OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
    breaker.accept(node);
  }

  // Name to assign to temporaries. All temporaries are created with the same name, which is
  // not a problem as they are referred to by reference.
  // {@link GenerateJavaScriptAst.FixNameClashesVisitor} will resolve into unique names when
  // needed.
  private static final String TEMP_LOCAL_NAME = "$tmp";

  /**
   * Decide what expression to return when breaking up a compound assignment of
   * the form <code>lhs op= rhs</code>. By default the <code>lhs</code> is
   * returned.
   */
  protected JExpression expressionToReturn(JExpression lhs) {
    return lhs;
  }

  /**
   * Decide what expression to return when breaking up a compound assignment of
   * the form <code>lhs op= rhs</code>. The breakup creates an expression of the
   * form <code>lhs = lhs op rhs</code>, and the right hand side of the newly
   * created expression is passed to this method.
   */
  protected JExpression modifyResultOperation(JBinaryOperation op) {
    return op;
  }

  protected abstract boolean shouldBreakUp(JBinaryOperation x);

  protected abstract boolean shouldBreakUp(JPostfixOperation x);

  protected abstract boolean shouldBreakUp(JPrefixOperation x);
}
