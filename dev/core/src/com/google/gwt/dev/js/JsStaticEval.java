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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.CanBooleanEval;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

import java.util.List;

/**
 * Removes JsFunctions that are never referenced in the program.
 */
public class JsStaticEval {

  /**
   * Examines code to find out whether it contains any break or continue
   * statements.
   * 
   * TODO: We could be more sophisticated with this. A nested while loop with an
   * unlabeled break should not cause this visitor to return false. Nor should a
   * labeled break break to another context.
   */
  public static class FindBreakContinueStatementsVisitor extends JsVisitor {
    private boolean hasBreakContinueStatements = false;

    @Override
    public void endVisit(JsBreak x, JsContext<JsStatement> ctx) {
      hasBreakContinueStatements = true;
    }

    @Override
    public void endVisit(JsContinue x, JsContext<JsStatement> ctx) {
      hasBreakContinueStatements = true;
    }

    protected boolean hasBreakContinueStatements() {
      return hasBreakContinueStatements;
    }
  }

  /**
   * Does static evals.
   * 
   * TODO: borrow more concepts from
   * {@link com.google.gwt.dev.jjs.impl.DeadCodeElimination}, such as ignored
   * expression results.
   * 
   * TODO: remove silly stuff like !! coercion in a boolean context.
   */
  private class StaticEvalVisitor extends JsModVisitor {

    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      JsBinaryOperator op = x.getOperator();
      JsExpression arg1 = x.getArg1();
      JsExpression arg2 = x.getArg2();
      if (op == JsBinaryOperator.AND) {
        shortCircuitAnd(arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.OR) {
        shortCircuitOr(arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.COMMA) {
        trySimplifyComma(arg1, arg2, ctx);
      }
    }

    /**
     * Prune dead statements and empty blocks.
     */
    @Override
    public void endVisit(JsBlock x, JsContext<JsStatement> ctx) {
      /*
       * Remove any dead statements after an abrupt change in code flow and
       * promote safe statements within nested blocks to this block.
       */
      List<JsStatement> stmts = x.getStatements();
      for (int i = 0; i < stmts.size(); i++) {
        JsStatement stmt = stmts.get(i);

        if (stmt instanceof JsBlock) {
          // Promote a sub-block's children to the current block.
          JsBlock block = (JsBlock) stmt;
          stmts.remove(i);
          stmts.addAll(i, block.getStatements());
          i--;
          didChange = true;
          continue;
        }

        if (stmt.unconditionalControlBreak()) {
          // Abrupt change in flow, chop the remaining items from this block
          for (int j = i + 1; j < stmts.size();) {
            stmts.remove(j);
            didChange = true;
          }
        }
      }

      if (ctx.canRemove() && stmts.size() == 0) {
        // Remove blocks with no effect
        ctx.removeMe();
      }
    }

    @Override
    public void endVisit(JsConditional x, JsContext<JsExpression> ctx) {
      JsExpression condExpr = x.getTestExpression();
      JsExpression thenExpr = x.getThenExpression();
      JsExpression elseExpr = x.getElseExpression();
      if (condExpr instanceof CanBooleanEval) {
        CanBooleanEval condEval = (CanBooleanEval) condExpr;
        if (condEval.isBooleanTrue()) {
          // e.g. (true() ? then : else) -> true() && then
          JsBinaryOperation binOp = new JsBinaryOperation(JsBinaryOperator.AND,
              condExpr, thenExpr);
          ctx.replaceMe(accept(binOp));
        } else if (condEval.isBooleanFalse()) {
          // e.g. (false() ? then : else) -> false() || else
          JsBinaryOperation binOp = new JsBinaryOperation(JsBinaryOperator.OR,
              condExpr, elseExpr);
          ctx.replaceMe(accept(binOp));
        }
      }
    }

    /**
     * Convert do { } while (false); into a block.
     */
    @Override
    public void endVisit(JsDoWhile x, JsContext<JsStatement> ctx) {
      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace do with do's body
        if (cond.isBooleanFalse()) {
          // Unless it contains break/continue statements
          FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
          visitor.accept(x.getBody());
          if (!visitor.hasBreakContinueStatements()) {
            JsBlock block = new JsBlock();
            block.getStatements().add(x.getBody());
            block.getStatements().add(expr.makeStmt());
            ctx.replaceMe(block);
          }
        }
      }
    }

    @Override
    public void endVisit(JsExprStmt x, JsContext<JsStatement> ctx) {
      if (!x.getExpression().hasSideEffects()) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(program.getEmptyStmt());
        }
      }
    }

    /**
     * Prune for (X; false(); Y) statements, make sure X and false() are run.
     */
    @Override
    public void endVisit(JsFor x, JsContext<JsStatement> ctx) {
      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with initializers and condition.
        if (cond.isBooleanFalse()) {
          JsBlock block = new JsBlock();
          if (x.getInitExpr() != null) {
            block.getStatements().add(x.getInitExpr().makeStmt());
          }
          if (x.getInitVars() != null) {
            block.getStatements().add(x.getInitVars());
          }
          block.getStatements().add(expr.makeStmt());
          ctx.replaceMe(block);
        }
      }
    }

    /**
     * Simplify if statements.
     */
    @Override
    public void endVisit(JsIf x, JsContext<JsStatement> ctx) {
      JsExpression expr = x.getIfExpr();
      JsStatement thenStmt = x.getThenStmt();
      JsStatement elseStmt = x.getElseStmt();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;
        JsStatement onlyStmtToExecute;
        if (cond.isBooleanTrue()) {
          onlyStmtToExecute = thenStmt;
        } else if (cond.isBooleanFalse()) {
          onlyStmtToExecute = elseStmt;
        } else {
          return;
        }
        JsBlock block = new JsBlock();
        block.getStatements().add(expr.makeStmt());
        block.getStatements().add(onlyStmtToExecute);
        ctx.replaceMe(block);
      } else if (isEmpty(thenStmt) && isEmpty(elseStmt)) {
        ctx.replaceMe(expr.makeStmt());
      }
    }

    /**
     * Prune while (false) statements.
     */
    @Override
    public void endVisit(JsWhile x, JsContext<JsStatement> ctx) {
      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with condition.
        if (cond.isBooleanFalse()) {
          ctx.replaceMe(expr.makeStmt());
        }
      }
    }
  }

  public static boolean exec(JsProgram program) {
    return (new JsStaticEval(program)).execImpl();
  }

  protected static boolean isEmpty(JsStatement stmt) {
    if (stmt == null) {
      return true;
    }
    return (stmt instanceof JsBlock && ((JsBlock) stmt).getStatements().isEmpty());
  }

  /**
   * Simplify short circuit AND expressions.
   * 
   * <pre>
   * if (true && isWhatever()) -> if (isWhatever()), unless side effects
   * if (false() && isWhatever()) -> if (false())
   * </pre>
   */
  protected static void shortCircuitAnd(JsExpression arg1, JsExpression arg2,
      JsContext<JsExpression> ctx) {
    if (arg1 instanceof CanBooleanEval) {
      CanBooleanEval eval1 = (CanBooleanEval) arg1;
      if (eval1.isBooleanTrue() && !arg1.hasSideEffects()) {
        ctx.replaceMe(arg2);
      } else if (eval1.isBooleanFalse()) {
        ctx.replaceMe(arg1);
      }
    }
  }

  /**
   * Simplify short circuit OR expressions.
   * 
   * <pre>
   * if (true() || isWhatever()) -> if (true())
   * if (false || isWhatever()) -> if (isWhatever()), unless side effects
   * </pre>
   */
  protected static void shortCircuitOr(JsExpression arg1, JsExpression arg2,
      JsContext<JsExpression> ctx) {
    if (arg1 instanceof CanBooleanEval) {
      CanBooleanEval eval1 = (CanBooleanEval) arg1;
      if (eval1.isBooleanTrue()) {
        ctx.replaceMe(arg1);
      } else if (eval1.isBooleanFalse() && !arg1.hasSideEffects()) {
        ctx.replaceMe(arg2);
      }
    }
  }

  protected static void trySimplifyComma(JsExpression arg1, JsExpression arg2,
      JsContext<JsExpression> ctx) {
    if (!arg1.hasSideEffects()) {
      ctx.replaceMe(arg2);
    }
  }

  private final JsProgram program;

  public JsStaticEval(JsProgram program) {
    this.program = program;
  }

  public boolean execImpl() {
    // Remove the unused functions from the JsProgram
    StaticEvalVisitor evalVisitor = new StaticEvalVisitor();
    evalVisitor.accept(program);

    return evalVisitor.didChange();
  }
}
