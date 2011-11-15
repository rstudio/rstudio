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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.js.ast.CanBooleanEval;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsValueLiteral;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void endVisit(JsBreak x, JsContext ctx) {
      hasBreakContinueStatements = true;
    }

    @Override
    public void endVisit(JsContinue x, JsContext ctx) {
      hasBreakContinueStatements = true;
    }

    protected boolean hasBreakContinueStatements() {
      return hasBreakContinueStatements;
    }
  }

  /**
   * Creates a minimalist list of statements that must be run in order to
   * achieve the same declaration effect as the visited statements.
   * 
   * For example, a JsFunction declaration should be run as a JsExprStmt. JsVars
   * should be run without any initializers.
   * 
   * This visitor is called from
   * {@link StaticEvalVisitor#ensureDeclarations(JsStatement)} on any statements
   * that are removed from a function.
   */
  private static class MustExecVisitor extends JsVisitor {

    private final List<JsStatement> mustExec = new ArrayList<JsStatement>();

    public MustExecVisitor() {
    }

    @Override
    public void endVisit(JsExprStmt x, JsContext ctx) {
      JsFunction func = isFunctionDecl(x);
      if (func != null) {
        mustExec.add(x);
      }
    }

    @Override
    public void endVisit(JsVars x, JsContext ctx) {
      JsVars strippedVars = new JsVars(x.getSourceInfo());
      boolean mustReplace = false;
      for (JsVar var : x) {
        JsVar strippedVar = new JsVar(var.getSourceInfo(), var.getName());
        strippedVars.add(strippedVar);
        if (var.getInitExpr() != null) {
          mustReplace = true;
        }
      }
      if (mustReplace) {
        mustExec.add(strippedVars);
      } else {
        mustExec.add(x);
      }
    }

    public List<JsStatement> getStatements() {
      return mustExec;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
      // Don't dive into nested functions.
      return false;
    }
  }

  /**
   * Does static evals.
   * 
   * TODO: borrow more concepts from
   * {@link com.google.gwt.dev.jjs.impl.DeadCodeElimination}, such as ignored
   * expression results.
   */
  private class StaticEvalVisitor extends JsModVisitor {

    private Set<JsExpression> evalBooleanContext = new HashSet<JsExpression>();

    /**
     * This is used by {@link #additionCoercesToString}.
     */
    private Map<JsExpression, Boolean> coercesToStringMap = new IdentityHashMap<JsExpression, Boolean>();

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      JsBinaryOperator op = x.getOperator();
      JsExpression arg1 = x.getArg1();
      JsExpression arg2 = x.getArg2();

      if (MATH_ASSOCIATIVE.contains(op)
          && trySimplifyAssociativeExpression(x, ctx)) {
        // Nothing else to do
      } else if (op == JsBinaryOperator.AND) {
        shortCircuitAnd(arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.OR) {
        shortCircuitOr(arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.COMMA) {
        trySimplifyComma(arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.EQ) {
        trySimplifyEq(x, arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.NEQ) {
        trySimplifyNe(x, arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.ADD) {
        trySimplifyAdd(x, arg1, arg2, ctx);
      } else  {
       switch (op) {
         case GT:
         case GTE:
         case LT:
         case LTE:
           trySimplifyCompare(x, arg1, arg2, op, ctx);
           break;
         default:
           break;
       }
      }
    }

    /**
     * Prune dead statements and empty blocks.
     */
    @Override
    public void endVisit(JsBlock x, JsContext ctx) {
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
            JsStatement toRemove = stmts.get(j);
            JsStatement toReplace = ensureDeclarations(toRemove);
            if (toReplace == null) {
              stmts.remove(j);
              didChange = true;
            } else if (toReplace == toRemove) {
              ++j;
            } else {
              stmts.set(j, toReplace);
              didChange = true;
            }
          }
        }
      }

      if (ctx.canRemove() && stmts.size() == 0) {
        // Remove blocks with no effect
        ctx.removeMe();
      }
    }

    @Override
    public void endVisit(JsConditional x, JsContext ctx) {
      evalBooleanContext.remove(x.getTestExpression());

      JsExpression condExpr = x.getTestExpression();
      JsExpression thenExpr = x.getThenExpression();
      JsExpression elseExpr = x.getElseExpression();
      if (condExpr instanceof CanBooleanEval) {
        CanBooleanEval condEval = (CanBooleanEval) condExpr;
        if (condEval.isBooleanTrue()) {
          JsBinaryOperation binOp = new JsBinaryOperation(x.getSourceInfo(),
              JsBinaryOperator.AND, condExpr, thenExpr);
          ctx.replaceMe(accept(binOp));
        } else if (condEval.isBooleanFalse()) {
          // e.g. (false() ? then : else) -> false() || else
          JsBinaryOperation binOp = new JsBinaryOperation(x.getSourceInfo(),
              JsBinaryOperator.OR, condExpr, elseExpr);
          ctx.replaceMe(accept(binOp));
        }
      }
    }

    /**
     * Convert do { } while (false); into a block.
     */
    @Override
    public void endVisit(JsDoWhile x, JsContext ctx) {
      evalBooleanContext.remove(x.getCondition());

      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace do with do's body
        if (cond.isBooleanFalse()) {
          // Unless it contains break/continue statements
          FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
          visitor.accept(x.getBody());
          if (!visitor.hasBreakContinueStatements()) {
            JsBlock block = new JsBlock(x.getSourceInfo());
            block.getStatements().add(x.getBody());
            block.getStatements().add(expr.makeStmt());
            ctx.replaceMe(accept(block));
          }
        }
      }
    }

    @Override
    public void endVisit(JsExprStmt x, JsContext ctx) {
      if (!x.getExpression().hasSideEffects()) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(new JsEmpty(x.getSourceInfo()));
        }
      }
    }

    /**
     * Prune for (X; false(); Y) statements, make sure X and false() are run.
     */
    @Override
    public void endVisit(JsFor x, JsContext ctx) {
      evalBooleanContext.remove(x.getCondition());

      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with initializers and condition.
        if (cond.isBooleanFalse()) {
          JsBlock block = new JsBlock(x.getSourceInfo());
          if (x.getInitExpr() != null) {
            block.getStatements().add(x.getInitExpr().makeStmt());
          }
          if (x.getInitVars() != null) {
            block.getStatements().add(x.getInitVars());
          }
          block.getStatements().add(expr.makeStmt());
          JsStatement decls = ensureDeclarations(x.getBody());
          if (decls != null) {
            block.getStatements().add(decls);
          }
          ctx.replaceMe(accept(block));
        }
      }
    }

    /**
     * Simplify if statements.
     */
    @Override
    public void endVisit(JsIf x, JsContext ctx) {
      evalBooleanContext.remove(x.getIfExpr());

      JsExpression condExpr = x.getIfExpr();
      if (condExpr instanceof CanBooleanEval) {
        if (tryStaticEvalIf(x, (CanBooleanEval) condExpr, ctx)) {
          return;
        }
      }

      JsStatement thenStmt = x.getThenStmt();
      JsStatement elseStmt = x.getElseStmt();
      boolean thenIsEmpty = isEmpty(thenStmt);
      boolean elseIsEmpty = isEmpty(elseStmt);
      JsExpression thenExpr = extractExpression(thenStmt);
      JsExpression elseExpr = extractExpression(elseStmt);

      if (thenIsEmpty && elseIsEmpty) {
        // Convert "if (a()) {}" => "a()".
        ctx.replaceMe(condExpr.makeStmt());
      } else if (thenExpr != null && elseExpr != null) {
        // Convert "if (a()) {b()} else {c()}" => "a()?b():c()".
        JsConditional cond = new JsConditional(x.getSourceInfo(),
            x.getIfExpr(), thenExpr, elseExpr);
        ctx.replaceMe(accept(cond.makeStmt()));
      } else if (thenIsEmpty && elseExpr != null) {
        // Convert "if (a()) {} else {b()}" => a()||b().
        JsBinaryOperation op = new JsBinaryOperation(x.getSourceInfo(),
            JsBinaryOperator.OR, x.getIfExpr(), elseExpr);
        ctx.replaceMe(accept(op.makeStmt()));
      } else if (thenIsEmpty && !elseIsEmpty) {
        // Convert "if (a()) {} else {stuff}" => "if (!a()) {stuff}".
        JsUnaryOperation negatedOperation = new JsPrefixOperation(
            x.getSourceInfo(), JsUnaryOperator.NOT, x.getIfExpr());
        JsIf newIf = new JsIf(x.getSourceInfo(), negatedOperation, elseStmt,
            null);
        ctx.replaceMe(accept(newIf));
      } else if (elseIsEmpty && thenExpr != null) {
        // Convert "if (a()) {b()}" => "a()&&b()".
        JsBinaryOperation op = new JsBinaryOperation(x.getSourceInfo(),
            JsBinaryOperator.AND, x.getIfExpr(), thenExpr);
        ctx.replaceMe(accept(op.makeStmt()));
      } else if (elseIsEmpty && elseStmt != null) {
        // Convert "if (a()) {b()} else {}" => "if (a()) {b()}".
        JsIf newIf = new JsIf(x.getSourceInfo(), x.getIfExpr(), thenStmt, null);
        ctx.replaceMe(accept(newIf));
      }
    }

    /**
     * Change !!x to x in a boolean context.
     */
    @Override
    public void endVisit(JsPrefixOperation x, JsContext ctx) {
      if (x.getOperator() == JsUnaryOperator.NOT) {
        evalBooleanContext.remove(x.getArg());
      }

      if (evalBooleanContext.contains(x)) {
        if ((x.getOperator() == JsUnaryOperator.NOT)
            && (x.getArg() instanceof JsPrefixOperation)) {
          JsPrefixOperation arg = (JsPrefixOperation) x.getArg();
          if (arg.getOperator() == JsUnaryOperator.NOT) {
            ctx.replaceMe(arg.getArg());
            return;
          }
        }
      }
    }

    /**
     * Prune while (false) statements.
     */
    @Override
    public void endVisit(JsWhile x, JsContext ctx) {
      evalBooleanContext.remove(x.getCondition());

      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with condition.
        if (cond.isBooleanFalse()) {
          JsBlock block = new JsBlock(x.getSourceInfo());
          block.getStatements().add(expr.makeStmt());
          JsStatement decls = ensureDeclarations(x.getBody());
          if (decls != null) {
            block.getStatements().add(decls);
          }
          ctx.replaceMe(accept(block));
        }
      }
    }

    @Override
    public boolean visit(JsConditional x, JsContext ctx) {
      evalBooleanContext.add(x.getTestExpression());
      return true;
    }

    @Override
    public boolean visit(JsDoWhile x, JsContext ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
    }

    @Override
    public boolean visit(JsFor x, JsContext ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
    }

    @Override
    public boolean visit(JsIf x, JsContext ctx) {
      evalBooleanContext.add(x.getIfExpr());
      return true;
    }

    @Override
    public boolean visit(JsPrefixOperation x, JsContext ctx) {
      if (x.getOperator() == JsUnaryOperator.NOT) {
        evalBooleanContext.add(x.getArg());
      }
      return true;
    }

    @Override
    public boolean visit(JsWhile x, JsContext ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
    }

    /**
     * Given an expression, determine if the addition operator would cause a
     * string coercion to happen.
     */
    private boolean additionCoercesToString(JsExpression expr) {
      if (expr instanceof JsStringLiteral) {
        return true;
      }

      /*
       * Because the nodes passed into this method are visited on exit, it is
       * worthwile to memoize the result for this function.
       */
      Boolean toReturn = coercesToStringMap.get(expr);
      if (toReturn != null) {
        return toReturn;
      }
      toReturn = false;

      if (expr instanceof JsBinaryOperation) {
        JsBinaryOperation op = (JsBinaryOperation) expr;
        switch (op.getOperator()) {
          case ADD:
            toReturn = additionCoercesToString(op.getArg1())
                || additionCoercesToString(op.getArg2());
            break;
          case COMMA:
            toReturn = additionCoercesToString(op.getArg2());
            break;
        }

        if (op.getOperator().isAssignment()) {
          toReturn = additionCoercesToString(op.getArg2());
        }
      }

      /*
       * TODO: Consider adding heuristics to detect String(foo), typeof(foo),
       * and foo.toString(). The latter is debatable, since an implementation
       * might not actually return a string.
       */

      coercesToStringMap.put(expr, toReturn);
      return toReturn;
    }

    private boolean appendLiteral(StringBuilder result, JsValueLiteral val) {
      if (val instanceof JsNumberLiteral) {
        double number = ((JsNumberLiteral) val).getValue();
        result.append(fixTrailingZeroes(String.valueOf(number)));
      } else if (val instanceof JsStringLiteral) {
        result.append(((JsStringLiteral) val).getValue());
      } else if (val instanceof JsBooleanLiteral) {
        result.append(((JsBooleanLiteral) val).getValue());
      } else if (val instanceof JsNullLiteral) {
        result.append("null");
      } else {
        return false;
      }
      return true;
    }

    /**
     * This method MUST be called whenever any statements are removed from a
     * function. This is because some statements, such as JsVars or JsFunction
     * have the effect of defining local variables, no matter WHERE they are in
     * the function. The returned statement (if any), must be executed. It is
     * also possible for stmt to be directly returned, in which case the caller
     * should not perform AST changes that would cause an infinite optimization
     * loop.
     * 
     * Note: EvalFunctionsAtTopScope will have changed any JsFunction
     * declarations into statements before this visitor runs.
     */
    private JsStatement ensureDeclarations(JsStatement stmt) {
      if (stmt == null) {
        return null;
      }
      MustExecVisitor mev = new MustExecVisitor();
      mev.accept(stmt);
      List<JsStatement> stmts = mev.getStatements();
      if (stmts.isEmpty()) {
        return null;
      } else if (stmts.size() == 1) {
        return stmts.get(0);
      } else {
        JsBlock jsBlock = new JsBlock(stmt.getSourceInfo());
        jsBlock.getStatements().addAll(stmts);
        return jsBlock;
      }
    }

    /*
     * String.valueOf(Double) produces trailing .0 on integers which is
     * incorrect for Javascript which produces conversions to string without
     * trailing zeroes. Without this, int + String will turn out wrong.
     */
    private String fixTrailingZeroes(String num) {
      if (num.endsWith(".0")) {
        String fixNum = num.substring(0, num.length() - 2);
        assert Double.parseDouble(fixNum) == Double.parseDouble(num);
        num = fixNum;
      }
      return num;
    }

    private JsExpression simplifyCompare(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsBinaryOperator op) {
      assert (original != null);

      // TODO(cromwellian) handle all types
      if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
          double num1 = ((JsNumberLiteral) arg1).getValue();
          double num2 = ((JsNumberLiteral) arg2).getValue();
          boolean result = false;
          switch(op) {
            case LT:
              result = num1 < num2;
              break;
            case LTE:
              result = num1 <= num2;
              break;
            case GT:
              result = num1 > num2;
              break;
            case GTE:
              result = num1 >= num2;
              break;
            default:
              throw new InternalCompilerException("Can't handle simplify of op " + op);
          }
        return JsBooleanLiteral.get(result);
      }
      // no simplification made
      return original;
    }

    private JsExpression simplifyEq(JsExpression original, JsExpression arg1,
        JsExpression arg2) {
      assert (original != null);

      if (arg1 instanceof JsNullLiteral) {
        return simplifyNullEq(original, arg2);
      }

      if (arg2 instanceof JsNullLiteral) {
        return simplifyNullEq(original, arg1);
      }

      if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
          return JsBooleanLiteral.get(((JsNumberLiteral) arg1).getValue()
           == ((JsNumberLiteral) arg2).getValue());
      }
      // no simplification made
      return original;
    }

    private JsExpression simplifyNe(JsExpression original, JsExpression arg1,
        JsExpression arg2) {
      assert (original != null);

      if (arg1 instanceof JsNullLiteral) {
        return simplifyNullNe(original, arg2);
      }

      if (arg2 instanceof JsNullLiteral) {
        return simplifyNullNe(original, arg1);
      }

      if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
        return JsBooleanLiteral.get(((JsNumberLiteral) arg1).getValue()
            != ((JsNumberLiteral) arg2).getValue());
      }
      // no simplification made
      return original;
    }

    /**
     * Simplify exp == null.
     */
    private JsExpression simplifyNullEq(JsExpression original, JsExpression exp) {
      assert (original != null);

      if (exp instanceof JsValueLiteral) {
        // "undefined" is not a JsValueLiteral, so the only way
        // the result can be true is if exp is itself a JsNullLiteral
        boolean result = exp instanceof JsNullLiteral;
        return JsBooleanLiteral.get(result);
      }

      // no simplification made
      return original;
    }

    /**
     * Simplify exp != null.
     */
    private JsExpression simplifyNullNe(JsExpression original, JsExpression exp) {
      assert (original != null);

      if (exp instanceof JsValueLiteral) {
        // "undefined" is not a JsValueLiteral, so the only way
        // the result can be false is if exp is itself a JsNullLiteral
        boolean result = !(exp instanceof JsNullLiteral);
        return JsBooleanLiteral.get(result);
      }

      // no simplification made
      return original;
    }

    /**
     * Simplify a + b.
     */
    private void trySimplifyAdd(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext ctx) {
      if (arg1 instanceof JsValueLiteral && arg2 instanceof JsValueLiteral) {
        SourceInfo info = original.getSourceInfo();
        // case: number + number
        if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
          double value = ((JsNumberLiteral) arg1).getValue()
              + ((JsNumberLiteral) arg2).getValue();
          ctx.replaceMe(new JsNumberLiteral(info, value));
        } else {
          // cases: number + string or string + number
          StringBuilder result = new StringBuilder();
          if (appendLiteral(result, (JsValueLiteral) arg1)
              && appendLiteral(result, (JsValueLiteral) arg2)) {
            ctx.replaceMe(new JsStringLiteral(info, result.toString()));
          }
        }
      }
    }

    /**
     * Attempts to simplify adjoining binary expressions with mathematically
     * associative operators. This pass also tries to make these binary
     * expressions as left-normal as possible.
     */
    private boolean trySimplifyAssociativeExpression(JsBinaryOperation x,
        JsContext ctx) {
      boolean toReturn = false;
      JsBinaryOperator op = x.getOperator();
      JsExpression arg1 = x.getArg1();
      JsExpression arg2 = x.getArg2();

      /*
       * First, we'll try to normalize the nesting of any binary expressions
       * that we encounter. If we do this correctly,it will help to cut down on
       * the number of unnecessary parens in the emitted JS.
       */
      // (X) O (c O d) ==> ((X) O c) O d
      {
        JsBinaryOperation rightOp = null;
        if (arg2 instanceof JsBinaryOperation) {
          rightOp = (JsBinaryOperation) arg2;
        }
        if (rightOp != null && !rightOp.getOperator().isAssignment()
            && op == rightOp.getOperator()) {

          if (op == JsBinaryOperator.ADD) {
            /*
             * JS type coercion is a problem if we don't know for certain that
             * the right-hand expression will definitely be evaluated in a
             * string context.
             */
            boolean mustBeString = additionCoercesToString(rightOp.getArg1())
                || (additionCoercesToString(arg1) && additionCoercesToString(rightOp.getArg2()));
            if (!mustBeString) {
              return toReturn;
            }
          }

          // (X) O c --> Try to reduce this
          JsExpression newLeft = new JsBinaryOperation(x.getSourceInfo(), op,
              arg1, rightOp.getArg1());

          // Reset local vars with new state
          op = rightOp.getOperator();
          arg1 = accept(newLeft);
          arg2 = rightOp.getArg2();
          x = new JsBinaryOperation(x.getSourceInfo(), op, arg1, arg2);

          ctx.replaceMe(x);
          toReturn = didChange = true;
        }
      }

      /*
       * Now that we know that our AST is as left-normal as we can make it
       * (because this method is called from endVisit), we now try to simplify
       * the left-right node and the right node.
       */
      // (a O b) O c ==> a O s
      {
        JsBinaryOperation leftOp = null;
        JsExpression leftLeft = null;
        JsExpression leftRight = null;

        if (arg1 instanceof JsBinaryOperation) {
          leftOp = (JsBinaryOperation) arg1;
          if (op.getPrecedence() == leftOp.getOperator().getPrecedence()) {
            leftLeft = leftOp.getArg1();
            leftRight = leftOp.getArg2();
          }
        }

        if (leftRight != null) {
          if (op == JsBinaryOperator.ADD) {
            // Behavior as described above
            boolean mustBeString = additionCoercesToString(leftRight)
                || (additionCoercesToString(leftLeft) && additionCoercesToString(arg2));
            if (!mustBeString) {
              return toReturn;
            }
          }

          // (b O c)
          JsBinaryOperation middle = new JsBinaryOperation(x.getSourceInfo(),
              op, leftRight, arg2);
          StaticEvalVisitor v = new StaticEvalVisitor();
          JsExpression maybeSimplified = v.accept(middle);

          if (v.didChange()) {
            x.setArg1(leftLeft);
            x.setArg2(maybeSimplified);
            toReturn = didChange = true;
          }
        }
      }
      return toReturn;
    }

    private void trySimplifyCompare(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsBinaryOperator op, JsContext ctx) {
      JsExpression updated = simplifyCompare(original, arg1, arg2, op);
      if (updated != original) {
        ctx.replaceMe(updated);
      }
    }

    private void trySimplifyEq(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext ctx) {
      JsExpression updated = simplifyEq(original, arg1, arg2);
      if (updated != original) {
        ctx.replaceMe(updated);
      }
    }

    private void trySimplifyNe(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext ctx) {
      JsExpression updated = simplifyNe(original, arg1, arg2);
      if (updated != original) {
        ctx.replaceMe(updated);
      }
    }

    private boolean tryStaticEvalIf(JsIf x, CanBooleanEval cond, JsContext ctx) {
      JsStatement thenStmt = x.getThenStmt();
      JsStatement elseStmt = x.getElseStmt();
      if (cond.isBooleanTrue()) {
        JsBlock block = new JsBlock(x.getSourceInfo());
        block.getStatements().add(x.getIfExpr().makeStmt());
        if (thenStmt != null) {
          block.getStatements().add(thenStmt);
        }
        JsStatement decls = ensureDeclarations(elseStmt);
        if (decls != null) {
          block.getStatements().add(decls);
        }
        ctx.replaceMe(accept(block));
        return true;
      } else if (cond.isBooleanFalse()) {
        JsBlock block = new JsBlock(x.getSourceInfo());
        block.getStatements().add(x.getIfExpr().makeStmt());
        if (elseStmt != null) {
          block.getStatements().add(elseStmt);
        }
        JsStatement decls = ensureDeclarations(thenStmt);
        if (decls != null) {
          block.getStatements().add(decls);
        }
        ctx.replaceMe(accept(block));
        return true;
      } else {
        return false;
      }
    }
  }

  private static final String NAME = JsStaticEval.class.getSimpleName();

  /**
   * A set of the JS operators that are mathematically associative in nature.
   */
  private static final Set<JsBinaryOperator> MATH_ASSOCIATIVE = EnumSet.of(
      JsBinaryOperator.ADD, JsBinaryOperator.AND, JsBinaryOperator.BIT_AND,
      JsBinaryOperator.BIT_OR, JsBinaryOperator.BIT_XOR,
      JsBinaryOperator.COMMA, JsBinaryOperator.MUL, JsBinaryOperator.OR);

  public static <T extends JsVisitable> T exec(JsProgram program, T node) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "optimizer", NAME);
    T result = new JsStaticEval(program).execImpl(node);
    optimizeJsEvent.end();
    return result;
  }

  public static OptimizerStats exec(JsProgram program) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "optimizer", NAME);
    OptimizerStats stats = new JsStaticEval(program).execImpl();
    optimizeJsEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  /**
   * Attempts to extract a single expression from a given statement and returns
   * it. If no such expression exists, returns <code>null</code>.
   */
  protected static JsExpression extractExpression(JsStatement stmt) {
    if (stmt == null) {
      return null;
    }

    if (stmt instanceof JsExprStmt) {
      return ((JsExprStmt) stmt).getExpression();
    }

    if (stmt instanceof JsBlock && ((JsBlock) stmt).getStatements().size() == 1) {
      return extractExpression(((JsBlock) stmt).getStatements().get(0));
    }

    return null;
  }

  protected static boolean isEmpty(JsStatement stmt) {
    if (stmt == null) {
      return true;
    }
    return (stmt instanceof JsBlock && ((JsBlock) stmt).getStatements().isEmpty());
  }

  /**
   * If the statement is a JsExprStmt that declares a function with no other
   * side effects, returns that function; otherwise <code>null</code>.
   */
  protected static JsFunction isFunctionDecl(JsStatement stmt) {
    if (stmt instanceof JsExprStmt) {
      JsExprStmt exprStmt = (JsExprStmt) stmt;
      JsExpression expr = exprStmt.getExpression();
      if (expr instanceof JsFunction) {
        JsFunction func = (JsFunction) expr;
        if (func.getName() != null) {
          return func;
        }
      }
    }
    return null;
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
      JsContext ctx) {
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
      JsContext ctx) {
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
      JsContext ctx) {
    if (!arg1.hasSideEffects()) {
      ctx.replaceMe(arg2);
    }
  }

  private final JsProgram program;

  public JsStaticEval(JsProgram program) {
    this.program = program;
  }

  public <T extends JsVisitable> T execImpl(T node) {
    return new StaticEvalVisitor().accept(node);
  }

  public OptimizerStats execImpl() {
    StaticEvalVisitor sev = new StaticEvalVisitor();
    sev.accept(program);
    OptimizerStats stats = new OptimizerStats(NAME);
    if (sev.didChange()) {
      stats.recordModified();
    }
    return stats;
  }
}
