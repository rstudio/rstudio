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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Attempts to remove dead code.
 */
public class DeadCodeElimination {

  /**
   * Eliminates dead or unreachable code when possible.
   */
  public class DeadCodeVisitor extends JModVisitor {

    /**
     * An expression whose result does not matter.
     */
    private JExpression ignoringExpressionOutput;

    /**
     * Short circuit binary operations.
     */
    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      JExpression lhs = x.getLhs();
      JExpression rhs = x.getRhs();
      if (op == JBinaryOperator.AND) {
        // simplify short circuit AND expressions
        if (lhs instanceof JBooleanLiteral) {
          // eg: if (false && isWhatever()) -> if (false)
          // eg: if (true && isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) lhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(rhs);
          } else {
            ctx.replaceMe(lhs);
          }

        } else if (rhs instanceof JBooleanLiteral) {
          // eg: if (isWhatever() && true) -> if (isWhatever())
          // eg: if (isWhatever() && false) -> if (false), unless side effects
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) rhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          } else if (!lhs.hasSideEffects()) {
            ctx.replaceMe(rhs);
          }
        }

      } else if (op == JBinaryOperator.OR) {
        // simplify short circuit OR expressions
        if (lhs instanceof JBooleanLiteral) {
          // eg: if (true || isWhatever()) -> if (true)
          // eg: if (false || isWhatever()) -> if (isWhatever())
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) lhs;
          if (booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          } else {
            ctx.replaceMe(rhs);
          }

        } else if (rhs instanceof JBooleanLiteral) {
          // eg: if (isWhatever() || false) -> if (isWhatever())
          // eg: if (isWhatever() && true) -> if (true), unless side effects
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) rhs;
          if (!booleanLiteral.getValue()) {
            ctx.replaceMe(lhs);
          } else if (!lhs.hasSideEffects()) {
            ctx.replaceMe(rhs);
          }
        }
      } else if (op == JBinaryOperator.EQ) {
        // simplify: null == null -> true
        if (lhs.getType() == program.getTypeNull()
            && rhs.getType() == program.getTypeNull()) {
          ctx.replaceMe(program.getLiteralBoolean(true));
        }
      } else if (op == JBinaryOperator.NEQ) {
        // simplify: null != null -> false
        if (lhs.getType() == program.getTypeNull()
            && rhs.getType() == program.getTypeNull()) {
          ctx.replaceMe(program.getLiteralBoolean(false));
        }
      } else if (op == JBinaryOperator.ADD
          && x.getType() == program.getTypeJavaLangString()) {
        // try to statically evaluate concatentation
        if (lhs instanceof JValueLiteral && rhs instanceof JValueLiteral) {
          Object lhsObj = ((JValueLiteral) lhs).getValueObj();
          Object rhsObj = ((JValueLiteral) rhs).getValueObj();
          ctx.replaceMe(program.getLiteralString(String.valueOf(lhsObj)
              + String.valueOf(rhsObj)));
        }
      }
    }

    /**
     * Prune empty blocks.
     */
    @Override
    public void endVisit(JBlock x, Context ctx) {
      if (x.statements.size() == 0) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        }
      }
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      JExpression condExpr = x.getIfTest();
      JExpression thenExpr = x.getThenExpr();
      JExpression elseExpr = x.getElseExpr();
      if (condExpr instanceof JBooleanLiteral) {
        if (((JBooleanLiteral) condExpr).getValue()) {
          // e.g. (true ? then : else) -> then
          ctx.replaceMe(thenExpr);
        } else {
          // e.g. (false ? then : else) -> else
          ctx.replaceMe(elseExpr);
        }
      } else if (thenExpr instanceof JBooleanLiteral) {
        if (((JBooleanLiteral) thenExpr).getValue()) {
          // e.g. (cond ? true : else) -> cond || else
          JBinaryOperation binOp = new JBinaryOperation(program,
              x.getSourceInfo(), x.getType(), JBinaryOperator.OR, condExpr,
              elseExpr);
          ctx.replaceMe(binOp);
        } else {
          // e.g. (cond ? false : else) -> !cond && else
          JPrefixOperation notCondExpr = new JPrefixOperation(program,
              condExpr.getSourceInfo(), JUnaryOperator.NOT, condExpr);
          JBinaryOperation binOp = new JBinaryOperation(program,
              x.getSourceInfo(), x.getType(), JBinaryOperator.AND, notCondExpr,
              elseExpr);
          ctx.replaceMe(binOp);
        }
      } else if (elseExpr instanceof JBooleanLiteral) {
        if (((JBooleanLiteral) elseExpr).getValue()) {
          // e.g. (cond ? then : true) -> !cond || then
          JPrefixOperation notCondExpr = new JPrefixOperation(program,
              condExpr.getSourceInfo(), JUnaryOperator.NOT, condExpr);
          JBinaryOperation binOp = new JBinaryOperation(program,
              x.getSourceInfo(), x.getType(), JBinaryOperator.OR, notCondExpr,
              thenExpr);
          ctx.replaceMe(binOp);
        } else {
          // e.g. (cond ? then : false) -> cond && then
          JBinaryOperation binOp = new JBinaryOperation(program,
              x.getSourceInfo(), x.getType(), JBinaryOperator.AND, condExpr,
              thenExpr);
          ctx.replaceMe(binOp);
        }
      }
    }

    /**
     * Convert do { } while (false); into a block.
     */
    @Override
    public void endVisit(JDoStatement x, Context ctx) {
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, replace do with do's body
        if (!booleanLiteral.getValue()) {
          // Unless it contains break/continue statements
          FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
          visitor.accept(x.getBody());
          if (!visitor.hasBreakContinueStatements()) {
            ctx.replaceMe(x.getBody());
          }
        }
      }
    }

    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      if (!x.getExpr().hasSideEffects()) {
        removeMe(x, ctx);
      }
    }

    /**
     * Prune for (X; false; Y) statements, but make sure X is run.
     */
    @Override
    public void endVisit(JForStatement x, Context ctx) {
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, replace the for statement with its initializers
        if (!booleanLiteral.getValue()) {
          JBlock block = new JBlock(program, x.getSourceInfo());
          block.statements.addAll(x.getInitializers());
          ctx.replaceMe(block);
        }
      }
    }

    /**
     * Simplify if statements.
     */
    @Override
    public void endVisit(JIfStatement x, Context ctx) {
      JExpression expr = x.getIfExpr();
      JStatement thenStmt = x.getThenStmt();
      JStatement elseStmt = x.getElseStmt();
      if (expr instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expr;
        boolean boolVal = booleanLiteral.getValue();
        if (boolVal && !isEmpty(thenStmt)) {
          // If true, replace myself with then statement
          ctx.replaceMe(thenStmt);
        } else if (!boolVal && !isEmpty(elseStmt)) {
          // If false, replace myself with else statement
          ctx.replaceMe(elseStmt);
        } else {
          // just prune me
          removeMe(x, ctx);
        }
      } else if (isEmpty(thenStmt) && isEmpty(elseStmt)) {
        ctx.replaceMe(expr.makeStatement());
      }
    }

    /**
     * Resolve method calls that can be computed statically.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method.getEnclosingType() == program.getTypeJavaLangString()) {
        tryOptimizeStringCall(x, ctx, method);
      }
    }

    /**
     * Remove any parts of JMultiExpression that have no side-effect.
     */
    @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      /*
       * If we're ignoring the output of this expression, we don't need to
       * maintain the final multi value.
       */
      for (int i = 0; i < numRemovableExpressions(x); i++) {
        JExpression expr = x.exprs.get(i);
        if (!expr.hasSideEffects()) {
          x.exprs.remove(i);
          i--;
          continue;
        }

        // Remove nested JMultiExpressions
        if (expr instanceof JMultiExpression) {
          x.exprs.remove(i);
          x.exprs.addAll(i, ((JMultiExpression) expr).exprs);
          i--;
          continue;
        }

        if (expr instanceof JFieldRef) {
          JFieldRef fieldRef = (JFieldRef) expr;
          JExpression instance = fieldRef.getInstance();
          if (instance == null || !instance.hasSideEffects()) {
            // If the instance doesn't have side-effects, but the field ref
            // does, it's because a clinit() needs to happen.
            JMethod clinit = fieldRef.getField().getEnclosingType().methods.get(0);
            assert (clinit.getName().equals("$clinit"));

            // Replace the field ref with a direct call to the clinit.
            JMethodCall methodCall = new JMethodCall(program,
                expr.getSourceInfo(), instance, clinit);
            x.exprs.set(i, methodCall);
          }
        }
      }

      if (x.exprs.size() == 1) {
        ctx.replaceMe(x.exprs.get(0));
      }
    }

    /**
     * Simplify the ! operator if possible.
     */
    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getOp() == JUnaryOperator.NOT) {
        JExpression arg = x.getArg();
        if (arg instanceof JBooleanLiteral) {
          // e.g. !true -> false; !false -> true
          JBooleanLiteral booleanLiteral = (JBooleanLiteral) arg;
          ctx.replaceMe(program.getLiteralBoolean(!booleanLiteral.getValue()));
        } else if (arg instanceof JBinaryOperation) {
          // try to invert the binary operator
          JBinaryOperation argOp = (JBinaryOperation) arg;
          JBinaryOperator op = argOp.getOp();
          JBinaryOperator newOp = null;
          if (op == JBinaryOperator.EQ) {
            // e.g. !(x == y) -> x != y
            newOp = JBinaryOperator.NEQ;
          } else if (op == JBinaryOperator.NEQ) {
            // e.g. !(x != y) -> x == y
            newOp = JBinaryOperator.EQ;
          } else if (op == JBinaryOperator.GT) {
            // e.g. !(x > y) -> x <= y
            newOp = JBinaryOperator.LTE;
          } else if (op == JBinaryOperator.LTE) {
            // e.g. !(x <= y) -> x > y
            newOp = JBinaryOperator.GT;
          } else if (op == JBinaryOperator.GTE) {
            // e.g. !(x >= y) -> x < y
            newOp = JBinaryOperator.LT;
          } else if (op == JBinaryOperator.LT) {
            // e.g. !(x < y) -> x >= y
            newOp = JBinaryOperator.GTE;
          }
          if (newOp != null) {
            JBinaryOperation newBinOp = new JBinaryOperation(program,
                argOp.getSourceInfo(), argOp.getType(), newOp, argOp.getLhs(),
                argOp.getRhs());
            ctx.replaceMe(newBinOp);
          }
        } else if (arg instanceof JPrefixOperation) {
          // try to invert the unary operator
          JPrefixOperation argOp = (JPrefixOperation) arg;
          JUnaryOperator op = argOp.getOp();
          // e.g. !!x -> x
          if (op == JUnaryOperator.NOT) {
            ctx.replaceMe(argOp.getArg());
          }
        }
      }
    }

    /**
     * Optimize switch statements.
     */
    public void endVisit(JSwitchStatement x, Context ctx) {
      if (hasNoDefaultCase(x)) {
        removeEmptyCases(x);
      }
      removeDoubleBreaks(x);
      tryRemoveSwitch(x, ctx);
    }

    /**
     * 1) Remove catch blocks whose exception type is not instantiable. 2) Prune
     * try statements with no body. 3) Hoist up try statements with no catches
     * and an empty finally.
     */
    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      // 1) Remove catch blocks whose exception type is not instantiable.
      List<JLocalRef> catchArgs = x.getCatchArgs();
      List<JBlock> catchBlocks = x.getCatchBlocks();
      Iterator<JLocalRef> itA = catchArgs.iterator();
      Iterator<JBlock> itB = catchBlocks.iterator();
      while (itA.hasNext()) {
        JLocalRef localRef = itA.next();
        itB.next();
        JReferenceType type = (JReferenceType) localRef.getType();
        if (!program.typeOracle.isInstantiatedType(type)
            || type == program.getTypeNull()) {
          itA.remove();
          itB.remove();
        }
      }

      // Compute properties regarding the state of this try statement
      boolean noTry = isEmpty(x.getTryBlock());
      boolean noCatch = catchArgs.size() == 0;
      boolean noFinally = isEmpty(x.getFinallyBlock());

      if (noTry) {
        // 2) Prune try statements with no body.
        if (noFinally) {
          // if there's no finally, prune the whole thing
          removeMe(x, ctx);
        } else {
          // replace the try statement with just the contents of the finally
          ctx.replaceMe(x.getFinallyBlock());
        }
      } else if (noCatch && noFinally) {
        // 3) Hoist up try statements with no catches and an empty finally.
        // If there's no catch or finally, there's no point in this even being
        // a try statement, replace myself with the try block
        ctx.replaceMe(x.getTryBlock());
      }
    }

    /**
     * Prune while (false) statements.
     */
    @Override
    public void endVisit(JWhileStatement x, Context ctx) {
      JExpression expression = x.getTestExpr();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, prune the while statement
        if (!booleanLiteral.getValue()) {
          removeMe(x, ctx);
        }
      }
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      ignoringExpressionOutput = x.getExpr();
      return true;
    }

    private boolean hasNoDefaultCase(JSwitchStatement x) {
      JBlock body = x.getBody();
      boolean inDefault = false;
      for (JStatement statement : body.statements) {
        if (statement instanceof JCaseStatement) {
          JCaseStatement caseStmt = (JCaseStatement) statement;
          if (caseStmt.getExpr() == null) {
            inDefault = true;
          }
        } else if (isUnconditionalBreak(statement)) {
          inDefault = false;
        } else {
          // We have some code to execute other than a break.
          if (inDefault) {
            // We have a default case with real code.
            return false;
          }
        }
      }
      // We found no default case that wasn't empty.
      return true;
    }

    /**
     * TODO: if the AST were normalized, we wouldn't need this.
     */
    private boolean isEmpty(JStatement stmt) {
      if (stmt == null) {
        return true;
      }
      return (stmt instanceof JBlock && ((JBlock) stmt).statements.isEmpty());
    }

    private boolean isUnconditionalBreak(JStatement statement) {
      if (statement instanceof JBreakStatement) {
        return true;
      } else if (statement instanceof JBlock) {
        JBlock block = (JBlock) statement;
        List<JStatement> blockStmts = block.statements;
        return blockStmts.size() > 0 && isUnconditionalBreak(blockStmts.get(0));
      } else {
        return false;
      }
    }

    private Class<?> mapType(JType type) {
      return typeClassMap.get(type);
    }

    private int numRemovableExpressions(JMultiExpression x) {
      if (x == ignoringExpressionOutput) {
        // All expressions can be removed.
        return x.exprs.size();
      } else {
        // The last expression cannot be removed.
        return x.exprs.size() - 1;
      }
    }

    /**
     * Removes any break statements that appear one after another.
     */
    private void removeDoubleBreaks(JSwitchStatement x) {
      JBlock body = x.getBody();
      boolean lastWasBreak = true;
      for (Iterator<JStatement> it = body.statements.iterator(); it.hasNext();) {
        JStatement statement = it.next();
        boolean isBreak = isUnconditionalBreak(statement);
        if (isBreak && lastWasBreak) {
          it.remove();
        }
        lastWasBreak = isBreak;
      }

      // Remove a trailing break statement from a case block
      if (lastWasBreak && body.statements.size() > 0) {
        body.statements.remove(body.statements.size() - 1);
      }
    }

    /**
     * A switch with no default case can have its empty cases pruned.
     */
    private void removeEmptyCases(JSwitchStatement x) {
      JBlock body = x.getBody();
      List<JStatement> noOpCaseStatements = new ArrayList<JStatement>();
      List<JStatement> potentialNoOpCaseStatements = new ArrayList<JStatement>();
      /*
       * A case statement has no effect if there is no code between it and
       * either an unconditional break or the end of the switch.
       */
      for (JStatement statement : body.statements) {
        if (statement instanceof JCaseStatement) {
          potentialNoOpCaseStatements.add(statement);
        } else if (isUnconditionalBreak(statement)) {
          // If we have any potential no-ops, they now become real no-ops.
          noOpCaseStatements.addAll(potentialNoOpCaseStatements);
          potentialNoOpCaseStatements.clear();
        } else {
          // Any other kind of statement makes these case statements are useful.
          potentialNoOpCaseStatements.clear();
        }
      }
      // None of the remaining case statements have any effect
      noOpCaseStatements.addAll(potentialNoOpCaseStatements);

      if (noOpCaseStatements.size() > 0) {
        for (JStatement statement : noOpCaseStatements) {
          body.statements.remove(statement);
        }
      }
    }

    private void removeMe(JStatement stmt, Context ctx) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        // empty block statement
        ctx.replaceMe(new JBlock(program, stmt.getSourceInfo()));
      }
    }

    /**
     * Replace String methods having literal args with the static result.
     */
    private void tryOptimizeStringCall(JMethodCall x, Context ctx,
        JMethod method) {

      if (method.getType() == program.getTypeVoid()) {
        return;
      }

      int skip = 0;
      Object instance;
      if (program.isStaticImpl(method)) {
        // is it static implementation for instance method?
        method = program.staticImplFor(method);
        instance = tryTranslateLiteral(x.getArgs().get(0), String.class);
        skip = 1;
      } else {
        // instance may be null
        instance = tryTranslateLiteral(x.getInstance(), String.class);
      }

      if (instance == null && !method.isStatic()) {
        return;
      }

      List<JType> params = method.getOriginalParamTypes();
      Class<?> paramTypes[] = new Class<?>[params.size()];
      Object paramValues[] = new Object[params.size()];
      ArrayList<JExpression> args = x.getArgs();
      for (int i = 0; i != params.size(); ++i) {
        paramTypes[i] = mapType(params.get(i));
        if (paramTypes[i] == null) {
          return;
        }
        paramValues[i] = tryTranslateLiteral(args.get(i + skip), paramTypes[i]);
        if (paramValues[i] == null) {
          return;
        }
      }

      try {
        Method actual = String.class.getMethod(method.getName(), paramTypes);
        if (actual == null) {
          return;
        }
        Object result = actual.invoke(instance, paramValues);
        if (result instanceof String) {
          ctx.replaceMe(program.getLiteralString((String) result));
        } else if (result instanceof Boolean) {
          ctx.replaceMe(program.getLiteralBoolean(((Boolean) result).booleanValue()));
        } else if (result instanceof Character) {
          ctx.replaceMe(program.getLiteralChar(((Character) result).charValue()));
        } else if (result instanceof Integer) {
          ctx.replaceMe(program.getLiteralInt(((Integer) result).intValue()));
        }
      } catch (Exception e) {
        // If the call threw an exception, just don't optimize
      }
    }

    private void tryRemoveSwitch(JSwitchStatement x, Context ctx) {
      JBlock body = x.getBody();
      if (body.statements.size() == 0) {
        // Empty switch; just run the switch condition.
        ctx.replaceMe(x.getExpr().makeStatement());
      } else if (body.statements.size() == 2) {
        /*
         * If there are only two statements, we know it's a case statement and
         * something with an effect.
         * 
         * TODO: make this more sophisticated; what we should really care about
         * is how many case statements it contains, not how many statements:
         * 
         * switch(i) { default: a(); b(); c(); }
         * 
         * becomes { a(); b(); c(); }
         * 
         * switch(i) { case 1: a(); b(); c(); }
         * 
         * becomes if (i == 1) { a(); b(); c(); }
         * 
         * switch(i) { case 1: a(); b(); break; default: c(); d(); }
         * 
         * becomes if (i == 1) { a(); b(); } else { c(); d(); }
         */
        JCaseStatement caseStatement = (JCaseStatement) body.statements.get(0);
        JStatement statement = body.statements.get(1);

        FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
        visitor.accept(statement);
        if (visitor.hasBreakContinueStatements()) {
          // Cannot optimize.
          return;
        }

        if (caseStatement.getExpr() != null) {
          // Create an if statement equivalent to the single-case switch.
          JBinaryOperation compareOperation = new JBinaryOperation(program,
              x.getSourceInfo(), program.getTypePrimitiveBoolean(),
              JBinaryOperator.EQ, x.getExpr(), caseStatement.getExpr());
          JBlock block = new JBlock(program, x.getSourceInfo());
          block.statements.add(statement);
          JIfStatement ifStatement = new JIfStatement(program,
              x.getSourceInfo(), compareOperation, block, null);
          ctx.replaceMe(ifStatement);
        } else {
          // All we have is a default case; convert to a JBlock.
          JBlock block = new JBlock(program, x.getSourceInfo());
          block.statements.add(x.getExpr().makeStatement());
          block.statements.add(statement);
          ctx.replaceMe(block);
        }
      }
    }

    private Object tryTranslateLiteral(JExpression maybeLit, Class<?> type) {
      if (!(maybeLit instanceof JValueLiteral)) {
        return null;
      }
      // TODO: make this way better by a mile
      if (type == boolean.class && maybeLit instanceof JBooleanLiteral) {
        return Boolean.valueOf(((JBooleanLiteral) maybeLit).getValue());
      }
      if (type == char.class && maybeLit instanceof JCharLiteral) {
        return new Character(((JCharLiteral) maybeLit).getValue());
      }
      if (type == double.class && maybeLit instanceof JDoubleLiteral) {
        return new Double(((JDoubleLiteral) maybeLit).getValue());
      }
      if (type == float.class && maybeLit instanceof JIntLiteral) {
        return new Float(((JIntLiteral) maybeLit).getValue());
      }
      if (type == int.class && maybeLit instanceof JIntLiteral) {
        return new Integer(((JIntLiteral) maybeLit).getValue());
      }
      if (type == long.class && maybeLit instanceof JLongLiteral) {
        return new Long(((JLongLiteral) maybeLit).getValue());
      }
      if (type == String.class && maybeLit instanceof JStringLiteral) {
        return ((JStringLiteral) maybeLit).getValue();
      }
      if (type == Object.class && maybeLit instanceof JValueLiteral) {
        return ((JValueLiteral) maybeLit).getValueObj();
      }
      return null;
    }
  }

  /**
   * Examines code to find out whether it contains any break or continue
   * statements.
   * 
   * TODO: We could be more sophisticated with this. A nested while loop with an
   * unlabeled break should not cause this visitor to return false. Nor should a
   * labeled break break to another context.
   */
  public static class FindBreakContinueStatementsVisitor extends JVisitor {
    private boolean hasBreakContinueStatements = false;

    @Override
    public void endVisit(JBreakStatement x, Context ctx) {
      hasBreakContinueStatements = true;
    }

    @Override
    public void endVisit(JContinueStatement x, Context ctx) {
      hasBreakContinueStatements = true;
    }

    protected boolean hasBreakContinueStatements() {
      return hasBreakContinueStatements;
    }
  }

  public static boolean exec(JProgram program) {
    return new DeadCodeElimination(program).execImpl(program);
  }

  public static boolean exec(JProgram program, JNode node) {
    return new DeadCodeElimination(program).execImpl(node);
  }

  private final JProgram program;

  private final Map<JType, Class<?>> typeClassMap = new IdentityHashMap<JType, Class<?>>();

  public DeadCodeElimination(JProgram program) {
    this.program = program;
    typeClassMap.put(program.getTypeJavaLangObject(), Object.class);
    typeClassMap.put(program.getTypeJavaLangString(), String.class);
    typeClassMap.put(program.getTypePrimitiveBoolean(), boolean.class);
    typeClassMap.put(program.getTypePrimitiveByte(), byte.class);
    typeClassMap.put(program.getTypePrimitiveChar(), char.class);
    typeClassMap.put(program.getTypePrimitiveDouble(), double.class);
    typeClassMap.put(program.getTypePrimitiveFloat(), float.class);
    typeClassMap.put(program.getTypePrimitiveInt(), int.class);
    typeClassMap.put(program.getTypePrimitiveLong(), long.class);
    typeClassMap.put(program.getTypePrimitiveShort(), short.class);
  }

  private boolean execImpl(JNode node) {
    boolean madeChanges = false;
    while (true) {
      DeadCodeVisitor deadCodeVisitor = new DeadCodeVisitor();
      deadCodeVisitor.accept(node);
      if (!deadCodeVisitor.didChange()) {
        break;
      }
      madeChanges = true;
    }
    return madeChanges;
  }
}
