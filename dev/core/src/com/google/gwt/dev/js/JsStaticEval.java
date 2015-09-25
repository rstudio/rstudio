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
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.rhino.ScriptRuntime;
import com.google.gwt.dev.util.Ieee754_64_Arithmetic;
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
      JsFunction func = JsUtils.isFunctionDeclaration(x);
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

      JsExpression result = x;

      if (op == JsBinaryOperator.AND) {
        result = shortCircuitAnd(x);
      } else if (op == JsBinaryOperator.OR) {
        result = shortCircuitOr(x);
      } else if (op == JsBinaryOperator.COMMA) {
        result = trySimplifyComma(x);
      } else if (op == JsBinaryOperator.EQ || op == JsBinaryOperator.REF_EQ) {
        result = simplifyEqAndRefEq(x);
      } else if (op == JsBinaryOperator.NEQ || op == JsBinaryOperator.REF_NEQ) {
        result = simplifyNeAndRefNe(x);
      } else if (arg1 instanceof JsValueLiteral && arg2 instanceof JsValueLiteral) {
         switch (op) {
           case ADD:
           case SUB:
           case MUL:
           case DIV:
           case MOD:
           case GT:
           case GTE:
           case LT:
           case LTE:
             result = simplifyOp(x);
             break;
           default:
             break;
         }
      }

      result = maybeReorderOperations(result);

      if (result != x) {
        ctx.replaceMe(result);
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

        if (!stmt.unconditionalControlBreak()) {
          continue;
        }

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
            ++j;
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
      boolean thenIsEmpty = JsUtils.isEmpty(thenStmt);
      boolean elseIsEmpty = JsUtils.isEmpty(elseStmt);
      JsExpression thenExpr = JsUtils.extractExpression(thenStmt);
      JsExpression elseExpr = JsUtils.extractExpression(elseStmt);

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
   * A set of the JS operators that are fully associative in nature; NOT included in this set are
   * operators that are only left-associative or right-associative or perform floating point
   * arithmetic.
   */
  private static final Set<JsBinaryOperator> REORDERABLE_OPERATORS = EnumSet.of(
      JsBinaryOperator.OR, JsBinaryOperator.AND, JsBinaryOperator.BIT_AND,
      JsBinaryOperator.BIT_OR, JsBinaryOperator.COMMA);

  public static OptimizerStats exec(JsProgram program) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "optimizer", NAME);
    OptimizerStats stats = new JsStaticEval(program).execImpl();
    optimizeJsEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  /**
   * Simplify short circuit AND expressions.
   *
   * <pre>
   * if (true && isWhatever()) -> if (isWhatever()), unless side effects
   * if (false() && isWhatever()) -> if (false())
   * </pre>
   */
  protected static JsExpression shortCircuitAnd(JsBinaryOperation expr) {
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();
    if (arg1 instanceof CanBooleanEval) {
      CanBooleanEval eval1 = (CanBooleanEval) arg1;
      if (eval1.isBooleanTrue() && !arg1.hasSideEffects()) {
        return arg2;
      } else if (eval1.isBooleanFalse()) {
        return arg1;
      }
    }
    return expr;
  }

  /**
   * Simplify short circuit OR expressions.
   *
   * <pre>
   * if (true() || isWhatever()) -> if (true())
   * if (false || isWhatever()) -> if (isWhatever()), unless side effects
   * </pre>
   */
  protected static JsExpression shortCircuitOr(JsBinaryOperation expr) {
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();
    if (arg1 instanceof CanBooleanEval) {
      CanBooleanEval eval1 = (CanBooleanEval) arg1;
      if (eval1.isBooleanTrue()) {
        return arg1;
      } else if (eval1.isBooleanFalse() && !arg1.hasSideEffects()) {
        return arg2;
      }
    }
    return expr;
  }

  protected static JsExpression trySimplifyComma(JsBinaryOperation expr) {
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();
    if (!arg1.hasSideEffects()) {
      return arg2;
    }
    return expr;
  }

  private static JsExpression simplifyEqAndRefEq(JsBinaryOperation expr) {
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();

    if (arg1 instanceof JsNullLiteral) {
      return simplifyNullEq(expr, arg2);
    }

    if (arg2 instanceof JsNullLiteral) {
      return simplifyNullEq(expr, arg1);
    }

    if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
      return JsBooleanLiteral.get(((JsNumberLiteral) arg1).getValue()
          == ((JsNumberLiteral) arg2).getValue());
    }

    if (arg1 instanceof JsStringLiteral && arg2 instanceof JsStringLiteral) {
      return JsBooleanLiteral.get(
          ((JsStringLiteral) arg1).getValue().equals(((JsStringLiteral) arg2).getValue()));
    }
    // no simplification made
    return expr;
  }

  /**
   * Simplify exp == null.
   */
  private static JsExpression simplifyNullEq(JsExpression original, JsExpression exp) {
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

  private static  JsExpression simplifyNeAndRefNe(JsBinaryOperation expr) {
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();

    JsExpression simplifiedEq = simplifyEqAndRefEq(expr);
    if (simplifiedEq == expr) {
      return expr;
    }

    assert simplifiedEq instanceof JsBooleanLiteral;

    return JsBooleanLiteral.get(!((JsBooleanLiteral) simplifiedEq).getValue());
  }

  /**
   * Simplify a op b.
   */
  private static JsExpression simplifyOp(JsBinaryOperation expr) {
    SourceInfo info = expr.getSourceInfo();
    JsExpression arg1 = expr.getArg1();
    JsExpression arg2 = expr.getArg2();
    JsBinaryOperator op = expr.getOperator();

    if (op == JsBinaryOperator.ADD &&
        (arg1 instanceof JsStringLiteral || arg2 instanceof JsStringLiteral)) {
      // cases: number + string or string + number
      StringBuilder result = new StringBuilder();
      if (appendLiteral(result, (JsValueLiteral) arg1)
          && appendLiteral(result, (JsValueLiteral) arg2)) {
        return new JsStringLiteral(info, result.toString());
      }
      return expr;
    }

    if (arg1 instanceof JsNumberLiteral && arg2 instanceof JsNumberLiteral) {
      double num1 = ((JsNumberLiteral) arg1).getValue();
      double num2 = ((JsNumberLiteral) arg2).getValue();
      Object result;

      switch (op) {
        case ADD:
          result = Ieee754_64_Arithmetic.add(num1, num2);
          break;
        case SUB:
          result = Ieee754_64_Arithmetic.subtract(num1, num2);
          break;
        case MUL:
          result = Ieee754_64_Arithmetic.multiply(num1, num2);
          break;
        case DIV:
          result = Ieee754_64_Arithmetic.divide(num1, num2);
          break;
        case MOD:
          result = Ieee754_64_Arithmetic.mod(num1, num2);
          break;
        case LT:
          result = Ieee754_64_Arithmetic.lt(num1, num2);
          break;
        case LTE:
          result = Ieee754_64_Arithmetic.le(num1, num2);
          break;
        case GT:
          result = Ieee754_64_Arithmetic.gt(num1, num2);
          break;
        case GTE:
          result = Ieee754_64_Arithmetic.ge(num1, num2);
          break;
        default:
          throw new InternalCompilerException("Can't handle simplify of op " + op);
      }
      return result instanceof Double ?
          new JsNumberLiteral(info, ((Double) result).doubleValue()) :
          JsBooleanLiteral.get(((Boolean) result).booleanValue());
    }
    return expr;
  }

  private static boolean appendLiteral(StringBuilder result, JsValueLiteral val) {
    if (val instanceof JsNumberLiteral) {
      double number = ((JsNumberLiteral) val).getValue();
      result.append(ScriptRuntime.numberToString(number, 10));
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
   * Makes expressions as expressions as left-normal as possible, i.e. prefers
   *
   *      o1                 o1
   *     /  \               /  \
   *    o2   e3     to     e1  o2
   *   /  \                   /  \
   *  e1  e2                e2   e3
   *
   *  when equivalent.
   */
  private static JsExpression maybeReorderOperations(JsExpression x) {
    if (!(x instanceof JsBinaryOperation)) {
      return x;
    }

    JsBinaryOperation expr = (JsBinaryOperation) x;
    JsBinaryOperator outerOp = expr.getOperator();

    if (!REORDERABLE_OPERATORS.contains(outerOp)) {
      return expr;
    }

    if (!(expr.getArg2() instanceof JsBinaryOperation) ||
        ((JsBinaryOperation) expr.getArg2()).getOperator() != outerOp) {
      return expr;
    }

    JsBinaryOperation leftExpr = (JsBinaryOperation) expr.getArg2();

    // Perform rotation.
    return new JsBinaryOperation(x.getSourceInfo(), leftExpr.getOperator(),
        maybeReorderOperations(
          new JsBinaryOperation(x.getSourceInfo(), outerOp, expr.getArg1(), leftExpr.getArg1())),
        leftExpr.getArg2());
  }

  private final JsProgram program;

  public JsStaticEval(JsProgram program) {
    this.program = program;
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
