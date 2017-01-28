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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.List;

/**
 * Methods that both construct and try to simplify AST nodes. If simplification
 * fails, then the methods will return an original, unmodified version of the
 * node if one is supplied. The routines do not recurse into their arguments;
 * the arguments are assumed to already be simplified as much as possible.
 */
public class Simplifier {

  /**
   * Negate the supplied expression if negating it makes the expression shorter.
   * Otherwise, return null.
   */
  private static JExpression maybeGetNegatedExpressionArgument(JExpression expression) {
    if (expression instanceof JPrefixOperation) {
      JPrefixOperation prefixOperation = (JPrefixOperation) expression;
      if (prefixOperation.getOp() == JUnaryOperator.NOT
          // Don't flip negations on floating point comparisons
          && !isFloatingPointComparison(prefixOperation.getArg())) {
        return prefixOperation.getArg();
      }
    }
    return null;
  }

  private static <T> List<T> allButLast(List<T> list) {
    return list.subList(0, list.size() - 1);
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }

  /**
   * This class provides only static methods. No instances will ever be created.
   */
  private Simplifier() {
  }

  /**
   * Simplify cast operations. Used when creating a cast in DeadCodeElimination. For simplifying
   * casts that are actually in the AST, cast(JCastOperation) is used instead.
   *
   * <pre>
   * (int) 1 -> 1
   * (A) (a,b) -> (a, (A) b)
   * </pre>
   *
   * @param type the Type to cast the expression <code>exp</code> to.
   * @param exp the current JExpression under the cast as it is being simplified.
   * @return the simplified expression.
   */
  public static JExpression cast(JType type, JExpression exp) {
    return simplifyCast(new JCastOperation(exp.getSourceInfo(), type, exp));
  }

  /**
   * Simplify cast operations.
   *
   * <pre>
   * (int) 1 -> 1
   * (A) (a,b) -> (a, (A) b)
   * </pre>
   *
   * @param castExpression a JCastOperation to be simplified.
   * @return the simplified expression if a simplification was possible; <code>exp</code> otherwise.
   */
  public static JExpression simplifyCast(JCastOperation castExpression) {
    SourceInfo info = castExpression.getSourceInfo();
    JType type = castExpression.getCastType();
    JExpression argument = castExpression.getExpr();

    if (type == argument.getType()) {
      return argument;
    }

    if (argument instanceof JMultiExpression) {
      // (T) (a, b, c) -> (a, b,(T) c)
      JMultiExpression argumentAsMultiExpression = (JMultiExpression) argument;
      JMultiExpression simplifiedExpression = new JMultiExpression(info);
      simplifiedExpression.addExpressions(allButLast(argumentAsMultiExpression.getExpressions()));
      simplifiedExpression.addExpressions(
          simplifyCast(
              new JCastOperation(info, type, last(argumentAsMultiExpression.getExpressions()))));
      return simplifiedExpression;
    }

    if (type.isPrimitiveType() && (argument instanceof JValueLiteral)) {
      // Statically evaluate casting literals.
      JPrimitiveType primitiveType = (JPrimitiveType) type;
      JValueLiteral valueLiteral = (JValueLiteral) argument;
      JValueLiteral simplifiedExpression = primitiveType.coerce(valueLiteral);
      if (simplifiedExpression != null) {
        return simplifiedExpression;
      }
    }

    /*
     * Discard casts from byte or short to int, because such casts are always
     * implicit anyway. Cannot coerce char since that would change the semantics
     * of concat.
     */
    if (type == JPrimitiveType.INT) {
      if ((argument.getType() == JPrimitiveType.SHORT)
          || (argument.getType() == JPrimitiveType.BYTE)) {
        return argument;
      }
    }

    // no simplification made
    return castExpression;
  }

  /**
   * Simplify conditional expressions.
   *
   * <pre>
   * (a,b,c)?d:e -> a,b,(c?d:e)
   * true ? then : else -> then
   * false ? then : else -> else
   * cond ? true : else) -> cond || else
   * cond ? false : else -> !cond && else
   * cond ? then : true -> !cond || then
   * cond ? then : false -> cond && then
   * !cond ? then : else -> cond ? else : then
   * </pre>
   *
   * @param expression a JCondintional to be simplified.
   * @return the simplified expression if a simplification was possible; <code>exp</code> otherwise.
   */
  public static JExpression simplifyConditional(JConditional expression) {
    SourceInfo info = expression.getSourceInfo();
    JType type = expression.getType();
    JExpression conditionExpression = expression.getIfTest();
    JExpression thenExpression = expression.getThenExpr();
    JExpression elseExpression = expression.getElseExpr();
    if (conditionExpression instanceof JMultiExpression) {
      // (a,b,c)?d:e -> a,b,(c?d:e)
      JMultiExpression conditionMultiExpression = (JMultiExpression) conditionExpression;
      JMultiExpression simplifiedExpression = new JMultiExpression(info);
      simplifiedExpression.addExpressions(allButLast(conditionMultiExpression.getExpressions()));
      simplifiedExpression.addExpressions(
          simplifyConditional(
              new JConditional(info, type, last(conditionMultiExpression.getExpressions()),
                  thenExpression, elseExpression)));
      return simplifiedExpression;
    }
    if (conditionExpression instanceof JBooleanLiteral) {
      return ((JBooleanLiteral) conditionExpression).getValue()
        // e.g. (true ? then : else) -> then
        ? thenExpression
        // e.g. (false ? then : else) -> else
        : elseExpression;
   }

   if (thenExpression instanceof JBooleanLiteral) {
     return ((JBooleanLiteral) thenExpression).getValue()
        // e.g. (cond ? true : else) -> cond || else
        ? or(info, conditionExpression, elseExpression)
        // e.g. (cond ? false : else) -> !cond && else
        : and(info,
            negate(conditionExpression.getSourceInfo(), conditionExpression),
            elseExpression);
    }

    if (elseExpression instanceof JBooleanLiteral) {
      return ((JBooleanLiteral) elseExpression).getValue()
        // e.g. (cond ? then : true) -> !cond || then
        ? or(info, negate(conditionExpression.getSourceInfo(), conditionExpression), thenExpression)
        // e.g. (cond ? then : false) -> cond && then
        : and(info, conditionExpression, thenExpression);
    }

      // e.g. (!cond ? then : else) -> (cond ? else : then)
    JExpression negatedExpressionArgument = maybeGetNegatedExpressionArgument(conditionExpression);
    if (negatedExpressionArgument != null) {
      return simplifyConditional(
          new JConditional(info, type, negatedExpressionArgument, elseExpression, thenExpression));
    }

    // Not simplified.
    return expression;
  }

  /**
   * Simplifies an if then else statement.
   *
   * <pre>
   * if(a,b,c) d [else e] -> {a; b; if(c) d [else e]; }
   * if(true) a [else b] -> a
   * if(false) a else b -> b
   * if(notImpl(c)) a else b -> if(c) b else a
   * if(true) ; else b -> true
   * if(false) a [else ;] -> false
   * if(c) ; [else ;] -> c
   *</pre>
   *
   * @param ifStatement the statement to simplify.
   * @param methodReturnType the return type of the method where the statement resides if any
   * @return the simplified statement if a simplification could be done and <code>ifStatement</code>
   *         otherwise.
   */
  public static JStatement simplifyIfStatement(JIfStatement ifStatement, JType methodReturnType) {
    SourceInfo info = ifStatement.getSourceInfo();
    JExpression conditionExpression = ifStatement.getIfExpr();
    JStatement thenStmt = ifStatement.getThenStmt();
    JStatement elseStmt = ifStatement.getElseStmt();
    if (conditionExpression instanceof JMultiExpression) {
      // if(a,b,c) d else e -> {a; b; if(c) d else e; }
      JMultiExpression condMulti = (JMultiExpression) conditionExpression;
      JBlock simplifiedStatement = new JBlock(info);
      for (JExpression expr : allButLast(condMulti.getExpressions())) {
        simplifiedStatement.addStmt(expr.makeStatement());
      }
      simplifiedStatement.addStmt(
          simplifyIfStatement(
              new JIfStatement(info, last(condMulti.getExpressions()), thenStmt, elseStmt),
              methodReturnType));
      return simplifiedStatement;
    }

    if (conditionExpression instanceof JBooleanLiteral) {
      boolean conditionValue = ((JBooleanLiteral) conditionExpression).getValue();
      if (conditionValue && !JjsUtils.isEmptyBlock(thenStmt)) {
        // If true, replace myself with then statement
        return thenStmt;
      } else if (!conditionValue && !JjsUtils.isEmptyBlock(elseStmt)) {
        // If false, replace myself with else statement
        return elseStmt;
      } else {
        // just prune me.
        return conditionExpression.makeStatement();
      }
    }

    if (JjsUtils.isEmptyBlock(thenStmt) && JjsUtils.isEmptyBlock(elseStmt)) {
      return conditionExpression.makeStatement();
    }

    if (!JjsUtils.isEmptyBlock(elseStmt)) {
      // if (!cond) foo else bar -> if (cond) bar else foo
      JExpression negationArugment =
          Simplifier.maybeGetNegatedExpressionArgument(conditionExpression);
      if (negationArugment != null) {
        // Force sub-parts to blocks, otherwise we break else-if chains.
        // TODO: this goes away when we normalize the Java AST properly.
        thenStmt = ensureBlock(thenStmt);
        elseStmt = ensureBlock(elseStmt);
        return simplifyIfStatement(
            new JIfStatement(info, negationArugment, elseStmt, thenStmt), methodReturnType);
      }
    }

    JStatement rewritenStatement =
        rewriteIfStatementAsExpression(
            info, conditionExpression, thenStmt, elseStmt, methodReturnType);
    if (rewritenStatement != null) {
      return rewritenStatement;
    }

    // no simplification made
    return ifStatement;
  }

  /**
   * Simplifies an negation expression.
   *
   * !(a > b) => a <= b
   *
   * @param expression the expression to simplify.
   * @return the simplified expression if a simplification could be done and <code>expr</code>
   *         otherwise.
   */
  public static JExpression simplifyNot(JPrefixOperation expression) {
    JExpression argument = expression.getArg();
    if (isFloatingPointComparison(argument)) {
      // Don't negate floating point expression because it changes the values when NaNs are
      // involved. E.g. !(Nan > 3) is not equivalent to (Nan <= 3).
      return expression;
    }
    SourceInfo info = expression.getSourceInfo();
    if (argument instanceof JMultiExpression) {
      // !(a,b,c) -> (a,b,!c)
      JMultiExpression multiExpression = (JMultiExpression) argument;
      JMultiExpression simplifiedExpression = new JMultiExpression(info);
      simplifiedExpression.addExpressions(allButLast(multiExpression.getExpressions()));
      simplifiedExpression.addExpressions(negate(info, last(multiExpression.getExpressions())));
      return simplifiedExpression;
    }

    if (argument instanceof JBinaryOperation) {
      // try to invert the binary operator
      JBinaryOperation binaryExpression = (JBinaryOperation) argument;
      switch (binaryExpression.getOp()) {
        case EQ:
          // e.g. !(x == y) -> x != y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.NEQ,
              binaryExpression.getLhs(), binaryExpression.getRhs());
        case NEQ:
          // e.g. !(x != y) -> x == y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.EQ,
              binaryExpression.getLhs(), binaryExpression.getRhs());
        case GT:
          // e.g. !(x > y) -> x <= y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.LTE,
              binaryExpression.getLhs(), binaryExpression.getRhs());
        case LTE:
          // e.g. !(x <= y) -> x > y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.GT,
              binaryExpression.getLhs(), binaryExpression.getRhs());
        case GTE:
          // e.g. !(x >= y) -> x < y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.LT,
              binaryExpression.getLhs(), binaryExpression.getRhs());
        case LT:
          // e.g. !(x < y) -> x >= y
          return new JBinaryOperation(info, binaryExpression.getType(), JBinaryOperator.GTE,
              binaryExpression.getLhs(), binaryExpression.getRhs());
      }
    }

    if (argument instanceof JPrefixOperation) {
      // try to invert the unary operator
      JPrefixOperation prefixExpression = (JPrefixOperation) argument;
      // e.g. !!x -> x
      if (prefixExpression.getOp() == JUnaryOperator.NOT) {
        return prefixExpression.getArg();
      }
    }

    if (argument instanceof JBooleanLiteral) {
      JBooleanLiteral booleanLiteral = (JBooleanLiteral) argument;
      return JBooleanLiteral.get(!booleanLiteral.getValue());
    }
    // no simplification made.
    return expression;
  }

  private static boolean isFloatingPointComparison(JExpression expr) {
    if (expr instanceof JBinaryOperation) {
      JBinaryOperation binaryOperation = (JBinaryOperation) expr;
      return
          binaryOperation.getType() == JPrimitiveType.BOOLEAN
          && (binaryOperation.getLhs().getType() == JPrimitiveType.FLOAT
              || binaryOperation.getLhs().getType() == JPrimitiveType.DOUBLE
              || binaryOperation.getRhs().getType() == JPrimitiveType.FLOAT
              || binaryOperation.getRhs().getType() == JPrimitiveType.DOUBLE);
    }
    return false;
  }

  private static JExpression negate(SourceInfo info, JExpression argument) {
    return simplifyNot(new JPrefixOperation(info, JUnaryOperator.NOT, argument));
  }

  /**
   * Simplify short circuit AND expressions.
   *
   * <pre>
   * true && isWhatever() -> isWhatever()
   * false && isWhatever() -> false
   *
   * isWhatever() && true -> isWhatever()
   * isWhatever() && false -> false, unless side effects
   *
   * (a, b) && c -> (a, b && c)
   * </pre>
   *
   * @param expression an AND JBinaryExpression to be simplified.
   * @return the simplified expression if a simplification was possible; <code>exp</code> otherwise.
   *
   */
  public static JExpression simplifyAnd(JBinaryOperation expression) {
    assert expression.getOp() == JBinaryOperator.AND
        : "Simplifier.and was called with " + expression;
    JExpression lhs = expression.getLhs();
    JExpression rhs = expression.getRhs();
    SourceInfo info = expression.getSourceInfo();
    if (lhs instanceof JMultiExpression) {
      // (a,b,c)&&d -> a,b,(c&&d)
      JMultiExpression lhsMultiExpression = (JMultiExpression) lhs;
      JMultiExpression simplifiedExpression = new JMultiExpression(info);
      simplifiedExpression.addExpressions(allButLast(lhsMultiExpression.getExpressions()));
      simplifiedExpression.addExpressions(
          and(info, last(lhsMultiExpression.getExpressions()), rhs));
      return simplifiedExpression;
    }
    if (lhs instanceof JBooleanLiteral) {
      return (((JBooleanLiteral) lhs).getValue()) ? rhs : lhs;
    }

    if (rhs instanceof JBooleanLiteral) {
      if (((JBooleanLiteral) rhs).getValue()) {
        return lhs;
      } else if (!lhs.hasSideEffects()) {
        // Do not remove lhs if it had side effects
        return rhs;
      }
    }

    // no simplification made.
    return expression;
  }

  private static JExpression and(SourceInfo info, JExpression lhs, JExpression rhs) {
    return simplifyAnd(
        new JBinaryOperation(info, JPrimitiveType.BOOLEAN, JBinaryOperator.AND, lhs, rhs));
  }

  /**
   * Simplify short circuit OR expressions.
   *
   * <pre>
   * true || isWhatever() -> true
   * false || isWhatever() -> isWhatever()
   *
   * isWhatever() || false isWhatever()
   * isWhatever() || true -> true, unless side effects
   *
   * (a, b) || c -> (a, b || c)
   * </pre>
   *
   * @param expression an OR JBinaryExpression to be simplified.
   * @return the simplified expression if a simplification was possible; <code>exp</code> otherwise.
   *
   */
  public static JExpression simplifyOr(JBinaryOperation expression) {
    assert expression.getOp() == JBinaryOperator.OR
        : "Simplifier.or was called with " + expression;
    JExpression lhs = expression.getLhs();
    JExpression rhs = expression.getRhs();
    SourceInfo info = expression.getSourceInfo();
    if (lhs instanceof JMultiExpression) {
      // (a,b,c)|| d -> a,b,(c||d)
      JMultiExpression lhsMultiExpression = (JMultiExpression) lhs;
      JMultiExpression simplifiedExpression = new JMultiExpression(info);
      simplifiedExpression.addExpressions(allButLast(lhsMultiExpression.getExpressions()));
      simplifiedExpression.addExpressions(or(info, last(lhsMultiExpression.getExpressions()), rhs));
      return simplifiedExpression;
    }
    if (lhs instanceof JBooleanLiteral) {
      return ((JBooleanLiteral) lhs).getValue() ? lhs : rhs;
    }

    if (rhs instanceof JBooleanLiteral) {
      if (!((JBooleanLiteral) rhs).getValue()) {
        return lhs;
      } else if (!lhs.hasSideEffects()) {
        return rhs;
      }
    }
    return expression;
  }

  private static JExpression or(SourceInfo info, JExpression lhs, JExpression rhs) {
    return simplifyOr(
        new JBinaryOperation(info, JPrimitiveType.BOOLEAN, JBinaryOperator.OR, lhs, rhs));
  }

  private static JStatement ensureBlock(JStatement statement) {
    if (statement == null) {
      return null;
    }
    if (statement instanceof JBlock) {
      return statement;
    }

    return new JBlock(statement.getSourceInfo(), statement);
  }

  private static JExpression extractExpression(JStatement statement) {
    if (statement instanceof JExpressionStatement) {
      return ((JExpressionStatement) statement).getExpr();
    }

    return null;
  }

  private static JStatement extractSingleStatement(JStatement statement) {
    if (statement instanceof JBlock) {
      JBlock block = (JBlock) statement;
      if (block.getStatements().size() == 1) {
        return extractSingleStatement(block.getStatements().get(0));
      }
    }

    return statement;
  }

  private static JStatement rewriteIfStatementAsExpression(SourceInfo sourceInfo,
      JExpression conditionExpression, JStatement thenStmt, JStatement elseStmt,
      JType methodReturnType) {
    thenStmt = extractSingleStatement(thenStmt);
    elseStmt = extractSingleStatement(elseStmt);

    if (thenStmt instanceof JReturnStatement && elseStmt instanceof JReturnStatement
        && methodReturnType != null) {
      // Special case
      // if () { return ..; } else { return ..; } =>
      // return ... ? ... : ...;
      JExpression thenExpression = ((JReturnStatement) thenStmt).getExpr();
      JExpression elseExpression = ((JReturnStatement) elseStmt).getExpr();
      if (thenExpression == null || elseExpression == null) {
        // empty returns are not supported.
        return null;
      }

      return
          new JConditional(
              sourceInfo, methodReturnType, conditionExpression, thenExpression, elseExpression
          ).makeReturnStatement();
    }

    if (elseStmt != null) {
      // if () { } else { } -> ... ? ... : ... ;
      JExpression thenExpression = extractExpression(thenStmt);
      JExpression elseExpression = extractExpression(elseStmt);

      if (thenExpression != null && elseExpression != null) {
        return
            new JConditional(
                sourceInfo, JPrimitiveType.VOID, conditionExpression, thenExpression, elseExpression
            ).makeStatement();
      }
    } else {
      // if () { } -> ... && ...;
      JExpression thenExpression = extractExpression(thenStmt);

      if (thenExpression != null) {
        JBinaryOperator binaryOperator = JBinaryOperator.AND;

        JExpression negationArgument = maybeGetNegatedExpressionArgument(conditionExpression);
        if (negationArgument != null) {
          conditionExpression = negationArgument;
          binaryOperator = JBinaryOperator.OR;
        }

        return
            new JBinaryOperation(
                sourceInfo, JPrimitiveType.VOID, binaryOperator, conditionExpression, thenExpression
            ).makeStatement();
      }
    }

    return null;
  }
}
