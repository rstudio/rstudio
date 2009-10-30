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

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
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
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsVars.JsVar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    public void endVisit(JsExprStmt x, JsContext<JsStatement> ctx) {
      JsFunction func = isFunctionDecl(x);
      if (func != null) {
        mustExec.add(x);
      }
    }

    @Override
    public void endVisit(JsVars x, JsContext<JsStatement> ctx) {
      JsVars strippedVars = new JsVars(x.getSourceInfo().makeChild(
          MustExecVisitor.class, "Simplified execution"));
      boolean mustReplace = false;
      for (JsVar var : x) {
        JsVar strippedVar = new JsVar(var.getSourceInfo().makeChild(
            MustExecVisitor.class, "Simplified execution"), var.getName());
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
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
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
      } else if (op == JsBinaryOperator.EQ) {
        trySimplifyEq(x, arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.NEQ) {
        trySimplifyNe(x, arg1, arg2, ctx);
      } else if (op == JsBinaryOperator.ADD) {
        trySimplifyAdd(x, arg1, arg2, ctx);
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
    public void endVisit(JsConditional x, JsContext<JsExpression> ctx) {
      evalBooleanContext.remove(x.getTestExpression());

      JsExpression condExpr = x.getTestExpression();
      JsExpression thenExpr = x.getThenExpression();
      JsExpression elseExpr = x.getElseExpression();
      if (condExpr instanceof CanBooleanEval) {
        CanBooleanEval condEval = (CanBooleanEval) condExpr;
        if (condEval.isBooleanTrue()) {
          JsBinaryOperation binOp = new JsBinaryOperation(makeSourceInfo(x,
              "Simplified always-true condition"), JsBinaryOperator.AND,
              condExpr, thenExpr);
          ctx.replaceMe(accept(binOp));
        } else if (condEval.isBooleanFalse()) {
          // e.g. (false() ? then : else) -> false() || else
          JsBinaryOperation binOp = new JsBinaryOperation(makeSourceInfo(x,
              "Simplified always-false condition"), JsBinaryOperator.OR,
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
            JsBlock block = new JsBlock(makeSourceInfo(x,
                "Simplified always-false condition"));
            block.getStatements().add(x.getBody());
            block.getStatements().add(expr.makeStmt());
            ctx.replaceMe(accept(block));
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
      evalBooleanContext.remove(x.getCondition());

      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with initializers and condition.
        if (cond.isBooleanFalse()) {
          JsBlock block = new JsBlock(makeSourceInfo(x,
              "Simplified always-false condition"));
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
    public void endVisit(JsIf x, JsContext<JsStatement> ctx) {
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
        JsConditional cond = new JsConditional(makeSourceInfo(x,
            "Replaced if statement with conditional"), x.getIfExpr(), thenExpr,
            elseExpr);
        ctx.replaceMe(accept(cond.makeStmt()));
      } else if (thenIsEmpty && elseExpr != null) {
        // Convert "if (a()) {} else {b()}" => a()||b().
        JsBinaryOperation op = new JsBinaryOperation(makeSourceInfo(x,
            "Replaced if statement with ||"), JsBinaryOperator.OR,
            x.getIfExpr(), elseExpr);
        ctx.replaceMe(accept(op.makeStmt()));
      } else if (thenIsEmpty && !elseIsEmpty) {
        // Convert "if (a()) {} else {stuff}" => "if (!a()) {stuff}".
        JsUnaryOperation negatedOperation = new JsPrefixOperation(
            makeSourceInfo(x, "Simplified if with empty then statement"),
            JsUnaryOperator.NOT, x.getIfExpr());
        JsIf newIf = new JsIf(makeSourceInfo(x,
            "Simplified if with empty then statement"), negatedOperation,
            elseStmt, null);
        ctx.replaceMe(accept(newIf));
      } else if (elseIsEmpty && thenExpr != null) {
        // Convert "if (a()) {b()}" => "a()&&b()".
        JsBinaryOperation op = new JsBinaryOperation(makeSourceInfo(x,
            "Replaced if statement with &&"), JsBinaryOperator.AND,
            x.getIfExpr(), thenExpr);
        ctx.replaceMe(accept(op.makeStmt()));
      } else if (elseIsEmpty && elseStmt != null) {
        // Convert "if (a()) {b()} else {}" => "if (a()) {b()}".
        JsIf newIf = new JsIf(makeSourceInfo(x, "Pruned empty else statement"),
            x.getIfExpr(), thenStmt, null);
        ctx.replaceMe(accept(newIf));
      }
    }

    /**
     * Change !!x to x in a boolean context.
     */
    @Override
    public void endVisit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
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
    public void endVisit(JsWhile x, JsContext<JsStatement> ctx) {
      evalBooleanContext.remove(x.getCondition());

      JsExpression expr = x.getCondition();
      if (expr instanceof CanBooleanEval) {
        CanBooleanEval cond = (CanBooleanEval) expr;

        // If false, replace with condition.
        if (cond.isBooleanFalse()) {
          JsBlock block = new JsBlock(makeSourceInfo(x,
              "Simplified always-false condition"));
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
    public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
      evalBooleanContext.add(x.getTestExpression());
      return true;
    }

    @Override
    public boolean visit(JsDoWhile x, JsContext<JsStatement> ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
    }

    @Override
    public boolean visit(JsFor x, JsContext<JsStatement> ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
    }

    @Override
    public boolean visit(JsIf x, JsContext<JsStatement> ctx) {
      evalBooleanContext.add(x.getIfExpr());
      return true;
    }

    @Override
    public boolean visit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
      if (x.getOperator() == JsUnaryOperator.NOT) {
        evalBooleanContext.add(x.getArg());
      }
      return true;
    }

    @Override
    public boolean visit(JsWhile x, JsContext<JsStatement> ctx) {
      evalBooleanContext.add(x.getCondition());
      return true;
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
        JsBlock jsBlock = new JsBlock(makeSourceInfo(stmt,
            "Ensuring declarations"));
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
        
    private SourceInfo makeSourceInfo(HasSourceInfo x, String m) {
      return x.getSourceInfo().makeChild(StaticEvalVisitor.class, m);
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
        return program.getBooleanLiteral(result);
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
        return program.getBooleanLiteral(result);
      }

      // no simplification made
      return original;
    }

    /**
     * Simplify a + b.
     */
    private void trySimplifyAdd(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext<JsExpression> ctx) {
      if (arg1 instanceof JsValueLiteral && arg2 instanceof JsValueLiteral) {
        // case: number + number
        if (arg1 instanceof JsNumberLiteral && 
            arg2 instanceof JsNumberLiteral) {
          ctx.replaceMe(program.getNumberLiteral(
              ((JsNumberLiteral) arg1).getValue() + 
              ((JsNumberLiteral) arg2).getValue()));
        } else {
          // cases: number + string or string + number
          StringBuilder result = new StringBuilder();
          if (appendLiteral(result, (JsValueLiteral) arg1) && 
              appendLiteral(result, (JsValueLiteral) arg2)) {
            ctx.replaceMe(program.getStringLiteral(original.getSourceInfo(),
                result.toString()));
          }
        }
      }
    }
  
    private void trySimplifyEq(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext<JsExpression> ctx) {
      JsExpression updated = simplifyEq(original, arg1, arg2);
      if (updated != original) {
        ctx.replaceMe(updated);
      }
    }

    private void trySimplifyNe(JsExpression original, JsExpression arg1,
        JsExpression arg2, JsContext<JsExpression> ctx) {
      JsExpression updated = simplifyNe(original, arg1, arg2);
      if (updated != original) {
        ctx.replaceMe(updated);
      }
    }

    private boolean tryStaticEvalIf(JsIf x, CanBooleanEval cond,
        JsContext<JsStatement> ctx) {
      JsStatement thenStmt = x.getThenStmt();
      JsStatement elseStmt = x.getElseStmt();
      if (cond.isBooleanTrue()) {
        JsBlock block = new JsBlock(makeSourceInfo(x,
            "Simplified always-true condition"));
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
        JsBlock block = new JsBlock(makeSourceInfo(x,
            "Simplified always-false condition"));
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

  public static boolean exec(JsProgram program) {
    return (new JsStaticEval(program)).execImpl();
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
    StaticEvalVisitor sev = new StaticEvalVisitor();
    sev.accept(program);

    return sev.didChange();
  }
}
