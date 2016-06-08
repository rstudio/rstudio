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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JBreakStatement;
import com.google.gwt.dev.jjs.ast.JCaseStatement;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JContinueStatement;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JDoStatement;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JEnumField;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JForStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JSwitchStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVariableRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JWhileStatement;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.util.Ieee754_64_Arithmetic;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes certain kinds of dead code, and simplifies certain expressions. This
 * pass focuses on intraprocedural optimizations, that is, optimizations that
 * can be performed inside of a single method body (and usually a single
 * expression) rather than global transformations. Global optimizations done in
 * other passes will feed into this, however.
 */
public class DeadCodeElimination {

  /**
   * Eliminates dead or unreachable code when possible, and makes local
   * simplifications like changing "<code>x || true</code>" to "<code>x</code>".
   *
   * This visitor should perform all of its optimizations in a single pass.
   * Except in rare cases, running this pass multiple times should produce no
   * changes after the first pass. The only currently known exception to this
   * rule is in {@link #endVisit(JNewInstance, Context)}, where the target
   * constructor may be non-empty at the beginning of DCE and become empty
   * during the run, which potentially unlocks optimizations at call sites.
   *
   * TODO: leverage ignoring expression output more to remove intermediary
   * operations in favor of pure side effects.
   *
   * TODO: move more simplifications into methods like
   * {@link #cast(JExpression, SourceInfo, JType, JExpression) simplifyCast}, so
   * that more simplifications can be made on a single pass through a tree.
   */
  public class DeadCodeVisitor extends JChangeTrackingVisitor {

    public DeadCodeVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    /**
     * Expressions whose result does not matter. A parent node should add any
     * children whose result does not matter to this set during the parent's
     * <code>visit()</code> method. It should then remove those children during
     * its own <code>endVisit()</code>.
     *
     * TODO: there's a latent bug here: some immutable nodes (such as literals)
     * can be multiply referenced in the AST. In theory, one reference to that
     * node could be put into this set while another reference actually contains
     * a result that is needed. In practice this is okay at the moment since the
     * existing uses of <code>ignoringExpressionOutput</code> are with mutable
     * nodes.
     */
    private final Set<JExpression> ignoringExpressionOutput = Sets.newHashSet();

    /**
     * Expressions being used as lvalues.
     */
    private final Set<JExpression> lvalues = Sets.newHashSet();

    private final Set<JBlock> switchBlocks = Sets.newHashSet();

    private final JMethod isScriptMethod = program.getIndexedMethod(RuntimeConstants.GWT_IS_SCRIPT);
    private final JMethod enumOrdinalMethod =
        program.getIndexedMethod(RuntimeConstants.ENUM_ORDINAL);
    private final JField enumOrdinalField =
        program.getIndexedField(RuntimeConstants.ENUM_ORDINAL);

    /**
     * Short circuit binary operations.
     */
    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      JExpression lhs = x.getLhs();
      JExpression rhs = x.getRhs();

      // If either parameter is a multiexpression, restructure if possible.
      if (isNonEmptyMultiExpression(lhs)) {
        // Push the operation inside the multiexpression. This exposes other optimization
        // opportunities for latter passes e.g:
        //
        // (a(), b(), 1) + 2 ==> (a(), b(), 1 + 2) ==> (a(), b(), 3)
        //
        // There is no need to consider the symmetric case as it requires that all expression in the
        // rhs (except the last one) to be side effect free, in which case they will end up being
        // removed anyway.

        List<JExpression> expressions = ((JMultiExpression) lhs).getExpressions();
        JMultiExpression result = new JMultiExpression(lhs.getSourceInfo(),
            expressions.subList(0, expressions.size() - 1));
        result.addExpressions(new JBinaryOperation(x.getSourceInfo(), x.getType(), x.getOp(),
            expressions.get(expressions.size() - 1), rhs));
        ctx.replaceMe(result);
        return;
      }

      if (isNonEmptyMultiExpression(rhs) &&  lhs instanceof JValueLiteral &&
          op != JBinaryOperator.AND && op != JBinaryOperator.OR) {
        // Push the operation inside the multiexpression if the lhs is a value literal.
        // This exposes other optimization opportunities for latter passes e.g:
        //
        // 2 + (a(), b(), 1) ==> (a(), b(), 2 + 1) ==> (a(), b(), 3)
        //
        // And exception must be made for || and &&, as these operations might not evaluate the
        // rhs due to shortcutting.
        List<JExpression> expressions = ((JMultiExpression) rhs).getExpressions();
        JMultiExpression result = new JMultiExpression(rhs.getSourceInfo(),
            expressions.subList(0, expressions.size() - 1));
        result.addExpressions(new JBinaryOperation(x.getSourceInfo(), x.getType(), x.getOp(),
            lhs, expressions.get(expressions.size() - 1)));
        ctx.replaceMe(result);
        return;
      }

      if ((lhs instanceof JValueLiteral) && (rhs instanceof JValueLiteral)) {
        if (evalOpOnLiterals(op, (JValueLiteral) lhs, (JValueLiteral) rhs, ctx)) {
          return;
        }
      }

      switch (op) {
        case AND:
          maybeReplaceMe(x, Simplifier.and(x), ctx);
          break;
        case OR:
          maybeReplaceMe(x, Simplifier.or(x), ctx);
          break;
        case BIT_XOR:
          simplifyXor(lhs, rhs, ctx);
          break;
        case EQ:
          simplifyEq(lhs, rhs, ctx, false);
          break;
        case NEQ:
          simplifyEq(lhs, rhs, ctx, true);
          break;
        case ADD:
          simplifyAdd(lhs, rhs, ctx, x.getType());
          break;
        case CONCAT:
          evalConcat(x.getSourceInfo(), lhs, rhs, ctx);
          break;
        case SUB:
          simplifySub(lhs, rhs, ctx, x.getType());
          break;
        case MUL:
          simplifyMul(lhs, rhs, ctx, x.getType());
          break;
        case DIV:
          simplifyDiv(lhs, rhs, ctx, x.getType());
          break;
        case SHL:
        case SHR:
        case SHRU:
          if (isLiteralZero(rhs)) {
            ctx.replaceMe(lhs);
          }
          break;
        default:
          if (op.isAssignment()) {
            lvalues.remove(lhs);
          }
          break;
      }
    }

    private boolean isNonEmptyMultiExpression(JExpression expression) {
      return expression instanceof JMultiExpression &&
          !((JMultiExpression) expression).getExpressions().isEmpty();
    }

    /**
     * Prune dead statements and empty blocks.
     */
    @Override
    public void endVisit(JBlock x, Context ctx) {
      // Switch blocks require special optimization code
      if (switchBlocks.contains(x)) {
        return;
      }

      /*
       * Remove any dead statements after an abrupt change in code flow and
       * promote safe statements within nested blocks to this block.
       */
      for (int i = 0; i < x.getStatements().size(); i++) {
        JStatement stmt = x.getStatements().get(i);

        if (stmt instanceof JBlock) {
          /*
           * Promote a sub-block's children to the current block, unless the
           * sub-block contains local declarations as children.
           */
          JBlock block = (JBlock) stmt;
          if (canPromoteBlock(block)) {
            x.removeStmt(i);
            x.addStmts(i, block.getStatements());
            i--;
            madeChanges();
            continue;
          }
        }

        if (stmt instanceof JExpressionStatement) {
          JExpressionStatement stmtExpr = (JExpressionStatement) stmt;
          if (stmtExpr.getExpr() instanceof JMultiExpression) {
            // Promote a multi's expressions to the current block
            x.removeStmt(i);
            int start = i;
            JMultiExpression multi = ((JMultiExpression) stmtExpr.getExpr());
            for (JExpression expr : multi.getExpressions()) {
              x.addStmt(i++, expr.makeStatement());
            }
            i = start - 1;
            continue;
          }
        }

        if (stmt.unconditionalControlBreak()) {
          // Abrupt change in flow, chop the remaining items from this block
          for (int j = i + 1; j < x.getStatements().size();) {
            x.removeStmt(j);
            madeChanges();
          }
        }
      }

      if (ctx.canRemove() && x.getStatements().size() == 0) {
        // Remove blocks with no effect
        ctx.removeMe();
      }
    }

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      maybeReplaceMe(x, Simplifier.cast(x), ctx);
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      maybeReplaceMe(x, Simplifier.conditional(x), ctx);
    }

    @Override
    public void endVisit(JDeclarationStatement x, Context ctx) {
      JVariableRef variableRef = x.getVariableRef();
      lvalues.remove(variableRef);
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
          if (Simplifier.isEmpty(x.getBody())) {
            ctx.removeMe();
          } else { // Unless it contains break/continue statements
            FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
            visitor.accept(x.getBody());
            if (!visitor.hasBreakContinueStatements()) {
              ctx.replaceMe(x.getBody());
            }
          }
        }
      }
    }

    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      ignoringExpressionOutput.remove(x.getExpr());
      if (!x.getExpr().hasSideEffects()) {
        removeMe(x, ctx);
      }
    }

    @Override
    public void endVisit(JFieldRef x, Context ctx) {

      if (x.getField() == enumOrdinalField) {
        maybeReplaceWithOrdinalValue(x.getInstance(), ctx);
        return;
      }

      JLiteral literal = tryGetConstant(x);
      if (literal == null && !ignoringExpressionOutput.contains(x)) {
        return;
      }
      /*
       * At this point, either we have a constant replacement, or our value is
       * irrelevant. We can inline the constant, if any, but we might also need
       * to evaluate an instance and run a clinit.
       */
      // We can inline the constant, but we might also need to evaluate an
      // instance and run a clinit.
      List<JExpression> exprs = Lists.newArrayList();

      JExpression instance = x.getInstance();
      if (instance != null) {
        exprs.add(instance);
      }

      if (x.hasClinit() && !x.getField().isCompileTimeConstant()) {
        // Access to compile time constants do not trigger class initialization (JLS 12.4.1).
        exprs.add(createClinitCall(x.getSourceInfo(), x.getField().getEnclosingType()));
      }

      if (literal != null) {
        exprs.add(literal);
      }

      JExpression replacement;
      if (exprs.size() == 1) {
        replacement = exprs.get(0);
      } else {
        replacement = new JMultiExpression(x.getSourceInfo(), exprs);
      }

      ctx.replaceMe(this.accept(replacement));
    }

    /**
     * Prune for (X; false; Y) statements, but make sure X is run.
     */
    @Override
    public void endVisit(JForStatement x, Context ctx) {
      JExpression expression = x.getCondition();
      if (expression instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) expression;

        // If false, replace the for statement with its initializers
        if (!booleanLiteral.getValue()) {
          JBlock block = new JBlock(x.getSourceInfo());
          block.addStmts(x.getInitializers());
          replaceMe(block, ctx);
        }
      }
    }

    /**
     * Simplify if statements.
     */
    @Override
    public void endVisit(JIfStatement x, Context ctx) {
      maybeReplaceMe(x, Simplifier.ifStatement(x, getCurrentMethod()), ctx);
    }

    /**
     * Simplify JInstanceOf expression whose output is ignored.
     */
    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      if (ignoringExpressionOutput.contains(x)) {
        ctx.replaceMe(x.getExpr());
        ignoringExpressionOutput.remove(x);
        return;
      }

      if (!(x.getExpr().getType() instanceof JReferenceType)) {
        return;
      }

      AnalysisResult analysisResult =
          staticallyEvaluateInstanceOf((JReferenceType) x.getExpr().getType(), x.getTestType());
      switch (analysisResult) {
        case TRUE:
          // replace with a simple null test:  (expr != null).
          ctx.replaceMe(
              JjsUtils.createOptimizedNotNullComparison(program, x.getSourceInfo(), x.getExpr()));
          break;
        case FALSE:
          // replace with a false literal
          ctx.replaceMe(JjsUtils.createOptimizedMultiExpression(x.getExpr(),
              program.getLiteralBoolean(false)));
          break;
        case UNKNOWN:
        default:
      }
    }

    @Override
    public void endVisit(JLocalRef x, Context ctx) {
      JLiteral literal = tryGetConstant(x);
      if (literal != null) {
        assert (!x.hasSideEffects());
        ctx.replaceMe(literal);
      }
    }

    /**
     * Resolve method calls that can be computed statically.
     */
    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // Restore ignored expressions.
      JMethod target = x.getTarget();
      JExpression instance = x.getInstance();
      if (target.isStatic() && instance != null) {
        ignoringExpressionOutput.remove(instance);
      }

      // Normal optimizations.
      JDeclaredType targetType = target.getEnclosingType();
      if (targetType == program.getTypeJavaLangString() ||
          (instance != null &&
              instance.getType().getUnderlyingType() == program.getTypeJavaLangString())) {
        tryOptimizeStringCall(x, ctx, target);
      } else if (JProgram.isClinit(target)) {
        // Eliminate the call if the target is now empty.
        if (!targetType.hasClinit()) {
          ctx.replaceMe(program.getLiteralNull());
        } else if (targetType != targetType.getClinitTarget()) {
          // Tighten the target.
          ctx.replaceMe(createClinitCall(x.getSourceInfo(), targetType.getClinitTarget()));
        }
      } else if (target == isScriptMethod) {
        // optimize out in draftCompiles that don't do inlining
        ctx.replaceMe(JBooleanLiteral.TRUE);
      } else if (target == enumOrdinalMethod) {
        maybeReplaceWithOrdinalValue(instance, ctx);
      }
    }

    /**
     * Remove any parts of JMultiExpression that have no side-effect.
     */
    @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      List<JExpression> exprs = x.getExpressions();
      if (exprs.size() > 0) {
        if (ignoringExpressionOutput.contains(x)) {
          // Remove all my children we previously added.
          ignoringExpressionOutput.removeAll(exprs);
        } else {
          // Remove the non-final children we previously added.
          List<JExpression> nonFinalChildren = exprs.subList(0, exprs.size() - 1);
          ignoringExpressionOutput.removeAll(nonFinalChildren);
        }
      }

      HashSet<JDeclaredType> clinitsCalled = new HashSet<JDeclaredType>();
      for (int i = 0; i < numRemovableExpressions(x); ++i) {
        JExpression expr = x.getExpression(i);
        if (!expr.hasSideEffects()) {
          x.removeExpression(i);
          --i;
          madeChanges();
          continue;
        }

        // Remove nested JMultiExpressions
        if (expr instanceof JMultiExpression) {
          x.removeExpression(i);
          x.addExpressions(i, ((JMultiExpression) expr).getExpressions());
          i--;
          madeChanges();
          continue;
        }

        // Remove redundant clinits
        if (expr instanceof JMethodCall && JProgram.isClinit(((JMethodCall) expr).getTarget())) {
          JDeclaredType enclosingType = ((JMethodCall) expr).getTarget().getEnclosingType();
          // If a clinit of enclosingType or a subclass of enclosingType has already been
          // called as part of this JMultiExpression then this clinit call is noop at runtime
          // and can be statically removed.
          if (enclosingType.findSubtype(clinitsCalled) != null) {
            x.removeExpression(i);
            --i;
            madeChanges();
            continue;
          } else {
            clinitsCalled.add(enclosingType);
          }
        }
      }

      if (x.getNumberOfExpressions() == 1) {
        maybeReplaceMe(x, x.getExpressions().get(0), ctx);
      }
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      super.endVisit(x, ctx);
      /*
       * If the result of a new operation is ignored, we can remove it, provided
       * it has no side effects.
       */
      if (ignoringExpressionOutput.contains(x)) {
        if (!x.getTarget().isEmpty()) {
          return;
        }
        JMultiExpression multi = new JMultiExpression(x.getSourceInfo());
        multi.addExpressions(x.getArgs());
        // Determine whether a new Blah() in this method needs a clinit.
        if (getCurrentMethod().getEnclosingType().checkClinitTo(x.getTarget().getEnclosingType())) {
          multi.addExpressions(
              createClinitCall(x.getSourceInfo(), x.getTarget().getEnclosingType()));
        }
        ignoringExpressionOutput.add(multi);
        ctx.replaceMe(this.accept(multi));
        ignoringExpressionOutput.remove(multi);
      }
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      JLiteral literal = tryGetConstant(x);
      if (literal != null) {
        assert (!x.hasSideEffects());
        ctx.replaceMe(literal);
      }
    }

    /**
     * Replace post-inc/dec with pre-inc/dec if the result doesn't matter.
     */
    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        lvalues.remove(x.getArg());
      }

      if (isNonEmptyMultiExpression(x.getArg())) {
        List<JExpression> expressions = ((JMultiExpression) x.getArg()).getExpressions();
        JMultiExpression result = new JMultiExpression(x.getArg().getSourceInfo(),
            expressions.subList(0, expressions.size() - 1));
        result.addExpressions(new JPostfixOperation(x.getSourceInfo(), x.getOp(),
            expressions.get(expressions.size() - 1)));
        ctx.replaceMe(result);
        return;
      }

      if (ignoringExpressionOutput.contains(x)) {
        JPrefixOperation newOp = new JPrefixOperation(x.getSourceInfo(), x.getOp(), x.getArg());
        ctx.replaceMe(newOp);
      }
    }

    /**
     * Simplify the ! operator if possible.
     */
    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        lvalues.remove(x.getArg());
      }

      // If the argument is a multiexpression restructure it if possible.
      if (isNonEmptyMultiExpression(x.getArg())) {
        List<JExpression> expressions = ((JMultiExpression) x.getArg()).getExpressions();
        JMultiExpression result = new JMultiExpression(x.getArg().getSourceInfo(),
            expressions.subList(0, expressions.size() - 1));
        result.addExpressions(new JPrefixOperation(x.getSourceInfo(), x.getOp(),
            expressions.get(expressions.size() - 1)));
        ctx.replaceMe(result);
        return;
      }

      if (x.getArg() instanceof JValueLiteral) {
        if (evalOpOnLiteral(x.getOp(), (JValueLiteral) x.getArg(), ctx)) {
          return;
        }
      }

      if (x.getOp() == JUnaryOperator.NOT) {
        maybeReplaceMe(x, Simplifier.not(x), ctx);
        return;
      } else if (x.getOp() == JUnaryOperator.NEG) {
        maybeReplaceMe(x, simplifyNegate(x, x.getArg()), ctx);
      }
    }

    /**
     * Optimize switch statements.
     */
    @Override
    public void endVisit(JSwitchStatement x, Context ctx) {
      switchBlocks.remove(x.getBody());

      // If it returns true, it was reduced to nothing
      if (tryReduceSwitchWithConstantInput(x, ctx)) {
        return;
      }

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
      List<JTryStatement.CatchClause> catchClauses = x.getCatchClauses();

      Iterator<JTryStatement.CatchClause> itClauses = catchClauses.iterator();
      while (itClauses.hasNext()) {
        JTryStatement.CatchClause clause = itClauses.next();
        // Go over the types in the multiexception and remove the ones that are not instantiable.
        Iterator<JType> itTypes = clause.getTypes().iterator();
        while (itTypes.hasNext()) {
          JReferenceType type = (JReferenceType) itTypes.next();
          if (!program.typeOracle.isInstantiatedType(type) || type.isNullType()) {
            itTypes.remove();
            madeChanges();
          }
        }

        // if all exception types are gone then remove whole clause.
        if (clause.getTypes().isEmpty()) {
          itClauses.remove();
          madeChanges();
        }
      }

      // Compute properties regarding the state of this try statement
      boolean noTry = Simplifier.isEmpty(x.getTryBlock());
      boolean noCatch = catchClauses.size() == 0;
      boolean noFinally = Simplifier.isEmpty(x.getFinallyBlock());

      if (noTry) {
        // 2) Prune try statements with no body.
        if (noFinally) {
          // if there's no finally, prune the whole thing
          removeMe(x, ctx);
        } else {
          // replace the try statement with just the contents of the finally
          replaceMe(x.getFinallyBlock(), ctx);
        }
      } else if (noCatch && noFinally) {
        // 3) Hoist up try statements with no catches and an empty finally.
        // If there's no catch or finally, there's no point in this even being
        // a try statement, replace myself with the try block
        replaceMe(x.getTryBlock(), ctx);
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
    public boolean visit(JBinaryOperation x, Context ctx) {
      if (x.getOp().isAssignment()) {
        lvalues.add(x.getLhs());
      }
      return true;
    }

    @Override
    public boolean visit(JClassType x, Context ctx) {
      // We can't eliminate code from an external type
      return !x.isExternal();
    }

    @Override
    public boolean visit(JDeclarationStatement x, Context ctx) {
      lvalues.add(x.getVariableRef());
      return true;
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      ignoringExpressionOutput.add(x.getExpr());
      return true;
    }

    @Override
    public boolean visit(JMethodCall x, Context ctx) {
      JMethod target = x.getTarget();
      if (target.isStatic() && x.getInstance() != null) {
        ignoringExpressionOutput.add(x.getInstance());
      }
      return true;
    }

    @Override
    public boolean visit(JMultiExpression x, Context ctx) {
      List<JExpression> exprs = x.getExpressions();
      if (exprs.size() > 0) {
        if (ignoringExpressionOutput.contains(x)) {
          // None of my children matter.
          ignoringExpressionOutput.addAll(exprs);
        } else {
          // Only my final child matters.
          List<JExpression> nonFinalChildren = exprs.subList(0, exprs.size() - 1);
          ignoringExpressionOutput.addAll(nonFinalChildren);
        }
      }
      return true;
    }

    @Override
    public boolean visit(JPostfixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        lvalues.add(x.getArg());
      }
      return true;
    }

    @Override
    public boolean visit(JPrefixOperation x, Context ctx) {
      if (x.getOp().isModifying()) {
        lvalues.add(x.getArg());
      }
      return true;
    }

    @Override
    public boolean visit(JSwitchStatement x, Context ctx) {
      switchBlocks.add(x.getBody());
      return true;
    }

    /**
     * Returns true if a block can be merged into its parent block. This is true
     * when the block contains no local declarations.
     */
    private boolean canPromoteBlock(JBlock block) {
      for (JStatement nestedStmt : block.getStatements()) {
        if (nestedStmt instanceof JDeclarationStatement) {
          JDeclarationStatement decl = (JDeclarationStatement) nestedStmt;
          if (decl.getVariableRef() instanceof JLocalRef) {
            return false;
          }
        }
      }
      return true;
    }

    private JMethodCall createClinitCall(SourceInfo sourceInfo, JDeclaredType targetType) {
      JMethod clinit = targetType.getClinitTarget().getClinitMethod();
      assert (JProgram.isClinit(clinit));
      return new JMethodCall(sourceInfo, null, clinit);
    }

    private void evalConcat(SourceInfo info, JExpression lhs, JExpression rhs, Context ctx) {
      if (lhs instanceof JValueLiteral && rhs instanceof JValueLiteral) {
        Object lhsObj = ((JValueLiteral) lhs).getValueObj();
        Object rhsObj = ((JValueLiteral) rhs).getValueObj();
        ctx.replaceMe(program.getStringLiteral(info, String.valueOf(lhsObj)
            + String.valueOf(rhsObj)));
      }
    }

    /**
     * Evaluate <code>lhs == rhs</code>.
     *
     * @param lhs Any literal.
     * @param rhs Any literal.
     * @return Whether <code>lhs == rhs</code> will evaluate to
     *         <code>true</code> at run time; string literal comparisons use .equals() semantics.
     */
    private boolean evalEq(JValueLiteral lhs, JValueLiteral rhs) {
      if (isTypeNull(lhs)) {
        return isTypeNull(rhs);
      }
      if (isTypeString(lhs)) {
        return isTypeString(rhs) &&
            ((JStringLiteral) lhs).getValue().equals(((JStringLiteral) rhs).getValue());
      }
      if (isTypeBoolean(lhs)) {
        return toBoolean(lhs) == toBoolean(rhs);
      }
      if (isTypeFloatingPoint(lhs) || isTypeFloatingPoint(rhs)) {
        return Ieee754_64_Arithmetic.eq(toDouble(lhs), toDouble(rhs));
      }
      if (isTypeLong(lhs) || isTypeLong(rhs)) {
        return toLong(lhs) == toLong(rhs);
      }
      return toInt(lhs) == toInt(rhs);
    }

    /**
     * Static evaluation of a unary operation on a literal.
     *
     * @return Whether a change was made
     */
    private boolean evalOpOnLiteral(JUnaryOperator op, JValueLiteral exp, Context ctx) {
      switch (op) {
        case BIT_NOT: {
          long value = toLong(exp);
          long res = ~value;
          if (isTypeLong(exp)) {
            ctx.replaceMe(program.getLiteralLong(res));
          } else {
            ctx.replaceMe(program.getLiteralInt((int) res));
          }
          return true;
        }

        case NEG:
          if (isTypeLong(exp)) {
            ctx.replaceMe(program.getLiteralLong(-toLong(exp)));
            return true;
          }
          if (isTypeIntegral(exp)) {
            ctx.replaceMe(program.getLiteralInt(-toInt(exp)));
            return true;
          }
          if (isTypeDouble(exp)) {
            ctx.replaceMe(program.getLiteralDouble(Ieee754_64_Arithmetic.neg(toDouble(exp))));
            return true;
          }
          if (isTypeFloat(exp)) {
            ctx.replaceMe(program.getLiteralFloat(Ieee754_64_Arithmetic.neg(toDouble(exp))));
            return true;
          }
          return false;

        case NOT: {
          JBooleanLiteral booleanLit = (JBooleanLiteral) exp;
          ctx.replaceMe(program.getLiteralBoolean(!booleanLit.getValue()));
          return true;
        }

        default:
          return false;
      }
    }

    /**
     * Static evaluation of a binary operation on two literals.
     *
     * @return Whether a change was made
     */
    private boolean evalOpOnLiterals(JBinaryOperator op, JValueLiteral lhs, JValueLiteral rhs,
        Context ctx) {
      switch (op) {
        case EQ: {
          ctx.replaceMe(program.getLiteralBoolean(evalEq(lhs, rhs)));
          return true;
        }

        case NEQ: {
          ctx.replaceMe(program.getLiteralBoolean(!evalEq(lhs, rhs)));
          return true;
        }

        case ADD:
        case SUB:
        case MUL:
        case DIV:
        case MOD: {
          if (isTypeFloatingPoint(lhs) || isTypeFloatingPoint(rhs)) {
            // do the op on doubles and cast back
            double left = toDouble(lhs);
            double right = toDouble(rhs);
            double res;
            switch (op) {
              case ADD:
                res = Ieee754_64_Arithmetic.add(left, right);
                break;
              case SUB:
                res = Ieee754_64_Arithmetic.subtract(left, right);
                break;
              case MUL:
                res = Ieee754_64_Arithmetic.multiply(left, right);
                break;
              case DIV:
                res = Ieee754_64_Arithmetic.divide(left, right);
                break;
              case MOD:
                res = Ieee754_64_Arithmetic.mod(left, right);
                break;
              default:
                assert false;
                return false;
            }
            if (isTypeDouble(lhs) || isTypeDouble(rhs)) {
              ctx.replaceMe(program.getLiteralDouble(res));
            } else {
              ctx.replaceMe(program.getLiteralFloat(res));
            }
            return true;
          } else if (isTypeIntegral(lhs) && isTypeIntegral(rhs)) {
            // do the op on longs and cast to the correct
            // result type at the end
            long left = toLong(lhs);
            long right = toLong(rhs);

            long res;
            switch (op) {
              case ADD:
                res = left + right;
                break;
              case SUB:
                res = left - right;
                break;
              case MUL:
                res = left * right;
                break;
              case DIV:
                if (right == 0) {
                  // Don't divide by zero
                  return false;
                }
                res = left / right;
                break;
              case MOD:
                if (right == 0) {
                  // Don't divide by zero
                  return false;
                }
                res = left % right;
                break;
              default:
                assert false;
                return false;
            }
            if (isTypeLong(lhs) || isTypeLong(rhs)) {
              ctx.replaceMe(program.getLiteralLong(res));
            } else {
              ctx.replaceMe(program.getLiteralInt((int) res));
            }
            return true;
          }
          return false;
        }
        case LT:
        case LTE:
        case GT:
        case GTE: {
          if (isTypeFloatingPoint(lhs) ||  isTypeFloatingPoint(lhs)) {
            // operate on doubles
            double left = toDouble(lhs);
            double right = toDouble(rhs);
            boolean res;
            switch (op) {
              case LT:
                res = Ieee754_64_Arithmetic.lt(left, right);
                break;
              case LTE:
                res = Ieee754_64_Arithmetic.le(left, right);
                break;
              case GT:
                res = Ieee754_64_Arithmetic.gt(left, right);
                break;
              case GTE:
                res = Ieee754_64_Arithmetic.ge(left, right);
                break;
              default:
                assert false;
                return false;
            }
            ctx.replaceMe(program.getLiteralBoolean(res));
            return true;
          } else if (isTypeIntegral(lhs) && isTypeIntegral(rhs)) {
            // operate on longs
            long left = toLong(lhs);
            long right = toLong(rhs);
            boolean res;
            switch (op) {
              case LT:
                res = left < right;
                break;
              case LTE:
                res = left <= right;
                break;
              case GT:
                res = left > right;
                break;
              case GTE:
                res = left >= right;
                break;
              default:
                assert false;
                return false;
            }
            ctx.replaceMe(program.getLiteralBoolean(res));
            return true;
          }
          return false;
        }
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
          if (isTypeBoolean(lhs)) {
            // TODO: maybe eval non-short-circuit boolean operators.
            return false;
          } else if (isTypeIntegral(lhs) && isTypeIntegral(rhs)) {
            // operate on longs and then cast down
            long left = toLong(lhs);
            long right = toLong(rhs);
            long res;
            switch (op) {
              case BIT_AND:
                res = left & right;
                break;

              case BIT_OR:
                res = left | right;
                break;

              case BIT_XOR:
                res = left ^ right;
                break;

              default:
                assert false;
                return false;
            }
            if (isTypeLong(lhs) || isTypeLong(rhs)) {
              ctx.replaceMe(program.getLiteralLong(res));
            } else {
              ctx.replaceMe(program.getLiteralInt((int) res));
            }
            return true;
          }
          return false;
        case SHL:
        case SHR:
        case SHRU: {
          if (isTypeLong(lhs)) {
            long left = toLong(lhs);
            int right = toInt(rhs);
            long res;
            switch (op) {
              case SHL:
                res = left << right;
                break;

              case SHR:
                res = left >> right;
                break;

              case SHRU:
                res = left >>> right;
                break;

              default:
                assert false;
                return false;
            }

            ctx.replaceMe(program.getLiteralLong(res));
            return true;
          } else if (isTypeIntegral(lhs) && isTypeIntegral(rhs)) {
            int left = toInt(lhs);
            int right = toInt(rhs);
            int res;
            switch (op) {
              case SHL:
                res = left << right;
                break;

              case SHR:
                res = left >> right;
                break;

              case SHRU:
                res = left >>> right;
                break;

              default:
                assert false;
                return false;
            }

            ctx.replaceMe(program.getLiteralInt(res));
            return true;
          }
          return false;
        }
        default:
          return false;
      }
    }

    /**
     * If the effect of <code>statement</code> is to immediately do a break,
     * then return the {@link JBreakStatement} corresponding to that break.
     */
    private JBreakStatement findUnconditionalBreak(JStatement statement) {
      if (statement instanceof JBreakStatement) {
        return (JBreakStatement) statement;
      } else if (statement instanceof JBlock) {
        JBlock block = (JBlock) statement;
        List<JStatement> blockStmts = block.getStatements();
        if (blockStmts.size() > 0 && isUnconditionalBreak(blockStmts.get(0))) {
          return (JBreakStatement) blockStmts.get(0);
        }
      }
      return null;
    }

    /**
     * Tries to removes cases and statements from switches whose expression is a
     * constant value.
     *
     * @return true, if the switch was completely eliminated
     */
    private boolean tryReduceSwitchWithConstantInput(JSwitchStatement s, Context ctx) {
      if (!(s.getExpr() instanceof JValueLiteral)) {
        // the input is not a constant
        return false;
      }
      JValueLiteral targetValue = (JValueLiteral) s.getExpr();

      // Find the matching case
      JCaseStatement matchingCase = null;
      for (JStatement subStatement : s.getBody().getStatements()) {
        if (subStatement instanceof JCaseStatement) {
          JCaseStatement caseStatement = (JCaseStatement) subStatement;
          if (caseStatement.getExpr() == null) {
            // speculatively put the default case into the matching case
            matchingCase = caseStatement;
          } else if (caseStatement.getExpr() instanceof JValueLiteral) {
            JValueLiteral caseValue = (JValueLiteral) caseStatement.getExpr();
            if (caseValue.getValueObj().equals(targetValue.getValueObj())) {
              matchingCase = caseStatement;
              break;
            }
          }
        }
      }

      if (matchingCase == null) {
        // the switch has no default and no matching cases
        // the expression is a value literal, so it can go away completely
        removeMe(s, ctx);
        return true;
      }

      Iterator<JStatement> it = s.getBody().getStatements().iterator();

      // Remove things until we find the matching case
      while (it.hasNext() && (it.next() != matchingCase)) {
        it.remove();
        madeChanges();
      }

      // Until an unconditional control break, preserve everything that isn't a case
      // or default.
      while (it.hasNext()) {
        JStatement statement = it.next();
        if (statement.unconditionalControlBreak()) {
          break;
        } else if (statement instanceof JCaseStatement) {
          it.remove();
        }
      }

      // having found a break or return (or reached the end), remove all remaining
      while (it.hasNext()) {
        it.next();
        it.remove();
        madeChanges();
      }

      return false;
    }

    private boolean hasNoDefaultCase(JSwitchStatement x) {
      JBlock body = x.getBody();
      boolean inDefault = false;
      for (JStatement statement : body.getStatements()) {
        if (statement instanceof JCaseStatement) {
          JCaseStatement caseStmt = (JCaseStatement) statement;
          if (caseStmt.getExpr() == null) {
            inDefault = true;
          }
        } else if (isUnconditionalUnlabeledBreak(statement)) {
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

    private boolean isLiteralNegativeOne(JExpression exp) {
      if (!(exp instanceof JValueLiteral)) {
        return false;
      }
      JValueLiteral lit = (JValueLiteral) exp;
      if (isTypeIntegral(lit) && toLong(lit) == -1) {
        return true;
      }
      if (isTypeFloatingPoint(lit) && toDouble(lit) == -1.0) {
        return true;
      }
      return false;
    }

    private boolean isLiteralOne(JExpression exp) {
      if (!(exp instanceof JValueLiteral)) {
        return false;
      }
      JValueLiteral lit = (JValueLiteral) exp;
      if (isTypeIntegral(lit) && toLong(lit) == 1) {
        return true;
      }
      return isTypeFloatingPoint(lit) && toDouble(lit) == 1.0;
    }

    private boolean isLiteralZero(JExpression exp) {
      if (exp instanceof JValueLiteral) {
        JValueLiteral lit = (JValueLiteral) exp;
        if (toDouble(lit) == 0.0) {
          // Using toDouble only is safe even for integer types. All types but
          // long will keep full precision. Longs will lose precision, but
          // it will not affect whether the resulting double is zero or not.
          return true;
        }
      }
      return false;
    }

    private boolean isTypeBoolean(JExpression lhs) {
      return lhs.getType() == program.getTypePrimitiveBoolean();
    }

    private boolean isTypeDouble(JExpression exp) {
      return isTypeDouble(exp.getType());
    }

    private boolean isTypeDouble(JType type) {
      return type == program.getTypePrimitiveDouble();
    }

    private boolean isTypeFloat(JExpression exp) {
      return isTypeFloat(exp.getType());
    }

    private boolean isTypeFloat(JType type) {
      return type == program.getTypePrimitiveFloat();
    }

    /**
     * Return whether the type of the expression is float or double.
     */
    private boolean isTypeFloatingPoint(JExpression exp) {
      return isTypeFloatingPoint(exp.getType());
    }

    private boolean isTypeFloatingPoint(JType type) {
      return ((type == program.getTypePrimitiveDouble()) || (type == program
          .getTypePrimitiveFloat()));
    }

    /**
     * Return whether the type of the expression is byte, char, short, int, or
     * long.
     */
    private boolean isTypeIntegral(JExpression exp) {
      return isTypeIntegral(exp.getType());
    }

    private boolean isTypeIntegral(JType type) {
      return ((type == program.getTypePrimitiveInt()) || (type == program.getTypePrimitiveLong())
          || (type == program.getTypePrimitiveChar()) || (type == program.getTypePrimitiveByte())
          || (type == program.getTypePrimitiveShort()));
    }

    private boolean isTypeLong(JExpression exp) {
      return isTypeLong(exp.getType());
    }

    private boolean isTypeLong(JType type) {
      return type == program.getTypePrimitiveLong();
    }

    private boolean isTypeNull(JExpression exp) {
      return isTypeNull(exp.getType());
    }

    private boolean isTypeNull(JType type) {
      return type.isNullType();
    }

    private boolean isTypeString(JExpression exp) {
      return program.isJavaLangString(exp.getType());
    }

    private boolean isUnconditionalBreak(JStatement statement) {
      return findUnconditionalBreak(statement) != null;
    }

    private boolean isUnconditionalUnlabeledBreak(JStatement statement) {
      JBreakStatement breakStat = findUnconditionalBreak(statement);
      return (breakStat != null) && (breakStat.getLabel() == null);
    }

    private <T> T last(List<T> statements) {
      return statements.get(statements.size() - 1);
    }

    private Class<?> classObjectForType(JType type) {
      return classObjectsByType.get(type);
    }

    /**
     * Replace me only if the updated expression is different.
     */
    private void maybeReplaceMe(JExpression x, JExpression updated, Context ctx) {
      if (updated != x) {
        ctx.replaceMe(updated);
      }
    }

    private void maybeReplaceMe(JStatement x, JStatement updated, Context ctx) {
      if (updated != x) {
        replaceMe(updated, ctx);
      }
    }

    private void maybeReplaceWithOrdinalValue(JExpression instanceExpr, Context ctx) {
      if (!(instanceExpr instanceof JFieldRef)) {
        return;
      }

      JFieldRef fieldRef = (JFieldRef) instanceExpr;
      if (!(fieldRef.getField() instanceof JEnumField)) {
        return;
      }

      assert fieldRef.getInstance() == null;
      JEnumField enumField = (JEnumField) fieldRef.getField();
      ctx.replaceMe(program.getLiteralInt(enumField.ordinal()));
    }

    private int numRemovableExpressions(JMultiExpression x) {
      if (ignoringExpressionOutput.contains(x)) {
        // The result doesn't matter: all expressions can be removed.
        return x.getNumberOfExpressions();
      } else {
        // The last expression cannot be removed.
        return x.getNumberOfExpressions() - 1;
      }
    }

    /**
     * Removes any break statements that appear one after another.
     */
    private void removeDoubleBreaks(JSwitchStatement x) {
      JBlock body = x.getBody();
      boolean lastWasBreak = true;
      for (int i = 0; i < body.getStatements().size(); ++i) {
        JStatement statement = body.getStatements().get(i);
        boolean isBreak = isUnconditionalBreak(statement);
        if (isBreak && lastWasBreak) {
          body.removeStmt(i--);
          madeChanges();
        }
        lastWasBreak = isBreak;
      }

      // Remove a trailing break statement from a case block
      if (body.getStatements().size() > 0
          && isUnconditionalUnlabeledBreak(last(body.getStatements()))) {
        body.removeStmt(body.getStatements().size() - 1);
        madeChanges();
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
      for (JStatement statement : body.getStatements()) {
        if (statement instanceof JCaseStatement) {
          potentialNoOpCaseStatements.add(statement);
        } else if (isUnconditionalUnlabeledBreak(statement)) {
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
          body.removeStmt(body.getStatements().indexOf(statement));
          madeChanges();
        }
      }
    }

    /**
     * Call this instead of directly calling <code>ctx.removeMe()</code> for
     * Statements.
     */
    private void removeMe(JStatement stmt, Context ctx) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        // empty block statement
        ctx.replaceMe(new JBlock(stmt.getSourceInfo()));
      }
    }

    /**
     *
     * Call this instead of directly calling <code>ctx.replaceMe()</code> for
     * Statements.
     */
    private void replaceMe(JStatement stmt, Context ctx) {
      stmt = this.accept(stmt, ctx.canRemove());
      if (stmt == null) {
        ctx.removeMe();
      } else {
        ctx.replaceMe(stmt);
      }
    }

    private boolean simplifyAdd(JExpression lhs, JExpression rhs, Context ctx, JType type) {
      if (isLiteralZero(rhs)) {
        ctx.replaceMe(Simplifier.cast(type, lhs));
        return true;
      }
      if (isLiteralZero(lhs)) {
        ctx.replaceMe(Simplifier.cast(type, rhs));
        return true;
      }

      return false;
    }

    /**
     * Simplify <code>exp == bool</code>, where <code>bool</code> is a boolean
     * literal.
     */
    private void simplifyBooleanEq(JExpression exp, boolean bool, Context ctx) {
      if (bool) {
        ctx.replaceMe(exp);
      } else {
        ctx.replaceMe(new JPrefixOperation(exp.getSourceInfo(), JUnaryOperator.NOT, exp));
      }
    }

    /**
     * Simplify <code>lhs == rhs</code>, where <code>lhs</code> and
     * <code>rhs</code> are known to be boolean. If <code>negate</code> is
     * <code>true</code>, then treat it as <code>lhs != rhs</code> instead of
     * <code>lhs == rhs</code>. Assumes that the case where both sides are
     * literals has already been checked.
     */
    private void simplifyBooleanEq(JExpression lhs, JExpression rhs, Context ctx, boolean negate) {
      if (lhs instanceof JBooleanLiteral) {
        boolean left = ((JBooleanLiteral) lhs).getValue();
        simplifyBooleanEq(rhs, left ^ negate, ctx);
        return;
      }
      if (rhs instanceof JBooleanLiteral) {
        boolean right = ((JBooleanLiteral) rhs).getValue();
        simplifyBooleanEq(lhs, right ^ negate, ctx);
        return;
      }
    }

    private boolean simplifyDiv(JExpression lhs, JExpression rhs, Context ctx, JType type) {
      if (isLiteralOne(rhs)) {
        ctx.replaceMe(Simplifier.cast(type, lhs));
        return true;
      }
      if (isLiteralNegativeOne(rhs)) {
        ctx.replaceMe(simplifyNegate(Simplifier.cast(type, lhs)));
        return true;
      }

      return false;
    }

    private AnalysisResult staticallyEvaluateEq(JExpression lhs, JExpression rhs) {
      JType lhsType = lhs.getType();
      JType rhsType = rhs.getType();
      if (lhsType.isNullType() && rhsType.isNullType()) {
        return AnalysisResult.TRUE;
      }
      if (lhsType.isNullType() && !rhsType.canBeNull() ||
          rhsType.isNullType() && !lhsType.canBeNull()) {
        return AnalysisResult.FALSE;
      }
      if (!lhsType.canBeSubclass() && !rhsType.canBeSubclass() &&
          !lhsType.canBeNull() && !rhsType.canBeNull() && lhsType instanceof JReferenceType &&
          lhsType.getUnderlyingType() != rhsType.getUnderlyingType()) {
        // We can conclude that lhs != rhs via type compatibility. If both lhs and rhs are typed
        // as exact and not null then if the underlying types are different the comparison clearly
        // fails.
        return AnalysisResult.FALSE;
      }
      return AnalysisResult.UNKNOWN;
    }

    /**
     * Tries to statically evaluate the instanceof operation. Returning TRUE if it can be determined
     * statically that it is true when not null, FALSE if it can be determined that is false and
     * UNKNOWN if the* result can not be determined statically.
     */
    private AnalysisResult staticallyEvaluateInstanceOf(JReferenceType fromType,
        JReferenceType toType) {
      if (fromType.isNullType()) {
        // null is never instanceof anything
        return AnalysisResult.FALSE;
      } else if (program.typeOracle.castSucceedsTrivially(fromType, toType)) {
        return AnalysisResult.TRUE;
      } else if (!program.typeOracle.isInstantiatedType(toType)) {
        return AnalysisResult.FALSE;
      } else if (program.typeOracle.castFailsTrivially(fromType, toType)) {
        return AnalysisResult.FALSE;
      }
      return AnalysisResult.UNKNOWN;
    }

    /**
     * Simplify <code>lhs == rhs</code>. If <code>negate</code> is true, then
     * it's actually static evaluation of <code>lhs != rhs</code>.
     */
    private void simplifyEq(JExpression lhs, JExpression rhs, Context ctx, boolean negate) {
      // simplify: null == null -> true
      AnalysisResult analysisResult = staticallyEvaluateEq(lhs, rhs);
      if (analysisResult != AnalysisResult.UNKNOWN) {
        ctx.replaceMe(JjsUtils.createOptimizedMultiExpression(
            lhs, rhs, program.getLiteralBoolean(negate ^ (analysisResult == AnalysisResult.TRUE))));
        return;
      }

      if (isTypeBoolean(lhs) && isTypeBoolean(rhs)) {
        simplifyBooleanEq(lhs, rhs, ctx, negate);
        return;
      }
    }

    private boolean simplifyMul(JExpression lhs, JExpression rhs, Context ctx, JType type) {
      if (isLiteralOne(rhs)) {
        ctx.replaceMe(Simplifier.cast(type, lhs));
        return true;
      }
      if (isLiteralOne(lhs)) {
        ctx.replaceMe(Simplifier.cast(type, rhs));
        return true;
      }
      if (isLiteralNegativeOne(rhs)) {
        ctx.replaceMe(simplifyNegate(Simplifier.cast(type, lhs)));
        return true;
      }
      if (isLiteralNegativeOne(lhs)) {
        ctx.replaceMe(simplifyNegate(Simplifier.cast(type, rhs)));
        return true;
      }
      if (isLiteralZero(rhs) && !lhs.hasSideEffects()) {
        ctx.replaceMe(Simplifier.cast(type, rhs));
        return true;
      }
      if (isLiteralZero(lhs) && !rhs.hasSideEffects()) {
        ctx.replaceMe(Simplifier.cast(type, lhs));
        return true;
      }
      return false;
    }

    private JExpression simplifyNegate(JExpression exp) {
      return simplifyNegate(null, exp);
    }

    /**
     * Simplify the expression <code>-exp</code>.
     *
     * @param original An expression equivalent to <code>-exp</code>.
     * @param exp The expression to negate.
     *
     * @return A simplified expression equivalent to <code>- exp</code>.
     */
    private JExpression simplifyNegate(JExpression original, JExpression exp) {
      // - -x -> x
      if (exp instanceof JPrefixOperation) {
        JPrefixOperation prefarg = (JPrefixOperation) exp;
        if (prefarg.getOp() == JUnaryOperator.NEG) {
          return prefarg.getArg();
        }
      }

      // no change
      if (original != null) {
        return original;
      }
      return new JPrefixOperation(exp.getSourceInfo(), JUnaryOperator.NEG, exp);
    }

    private boolean simplifySub(JExpression lhs, JExpression rhs, Context ctx, JType type) {
      if (isLiteralZero(rhs)) {
        ctx.replaceMe(Simplifier.cast(type, lhs));
        return true;
      }
      if (isLiteralZero(lhs) && !isTypeFloatingPoint(type)) {
        ctx.replaceMe(simplifyNegate(Simplifier.cast(type, rhs)));
        return true;
      }
      return false;
    }

    private void simplifyXor(JExpression lhs, JBooleanLiteral rhs, Context ctx) {
      if (rhs.getValue()) {
        ctx.replaceMe(new JPrefixOperation(lhs.getSourceInfo(), JUnaryOperator.NOT, lhs));
      } else {
        ctx.replaceMe(lhs);
      }
    }

    /**
     * Simplify XOR expressions.
     *
     * <pre>
     * true ^ x     -> !x
     * false ^ x    ->  x
     * y ^ true     -> !y
     * y ^ false    -> y
     * </pre>
     */
    private void simplifyXor(JExpression lhs, JExpression rhs, Context ctx) {
      if (lhs instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) lhs;
        simplifyXor(rhs, booleanLiteral, ctx);
      } else if (rhs instanceof JBooleanLiteral) {
        JBooleanLiteral booleanLiteral = (JBooleanLiteral) rhs;
        simplifyXor(lhs, booleanLiteral, ctx);
      }
    }

    private boolean toBoolean(JValueLiteral x) {
      return ((JBooleanLiteral) x).getValue();
    }

    /**
     * Cast a Java wrapper class (Integer, Double, Float, etc.) to a double.
     */
    private double toDouble(JValueLiteral literal) {
      Object valueObj = literal.getValueObj();
      if (valueObj instanceof Number) {
        return ((Number) valueObj).doubleValue();
      } else {
        return ((Character) valueObj).charValue();
      }
    }

    /**
     * Cast a Java wrapper class (Integer, Double, Float, etc.) to a long.
     */
    private int toInt(JValueLiteral literal) {
      Object valueObj = literal.getValueObj();
      if (valueObj instanceof Number) {
        return ((Number) valueObj).intValue();
      } else {
        return ((Character) valueObj).charValue();
      }
    }

    /**
     * Cast a Java wrapper class (Integer, Double, Float, etc.) to a long.
     */
    private long toLong(JValueLiteral literal) {
      Object valueObj = literal.getValueObj();
      if (valueObj instanceof Number) {
        return ((Number) valueObj).longValue();
      } else {
        return ((Character) valueObj).charValue();
      }
    }

    private JLiteral tryGetConstant(JVariableRef x) {
      if (!lvalues.contains(x)) {
        JLiteral lit = x.getTarget().getConstInitializer();
        if (lit != null) {
          /*
           * Upcast the initializer so that the semantics of any arithmetic on
           * this value is not changed.
           */
          // TODO(spoon): use Simplifier.cast to shorten this
          if (x.getType().isPrimitiveType() && (lit instanceof JValueLiteral)) {
            JPrimitiveType xTypePrim = (JPrimitiveType) x.getType();
            lit = xTypePrim.coerce((JValueLiteral) lit);
          }
          return lit;
        }
      }
      return null;
    }

    /**
     * Replace String methods having literal args with the static result.
     */
    private void tryOptimizeStringCall(JMethodCall x, Context ctx, JMethod method) {

      if (method.getType() == program.getTypeVoid()) {
        return;
      }

      if (method.getOriginalParamTypes().size() != method.getParams().size()) {
        // One or more parameters were pruned, abort.
        return;
      }

      if (method.getName().endsWith("hashCode")) {
        // This cannot be computed at compile time because our implementation
        // differs from the JRE.
        // TODO: actually, I think it DOES match now, so we can remove this?
        return;
      }

      boolean isStaticImplMethod = program.isStaticImpl(method);
      JExpression instance = isStaticImplMethod ? x.getArgs().get(0) : x.getInstance();
      // drop the instance argument if the call was devirtualized as the compile time evaluation
      // will use the real Java String class.
      List<JExpression> arguments = isStaticImplMethod
          ? x.getArgs().subList(1, x.getArgs().size())
          : x.getArgs();

      // Handle toString specially to make sure it is a noop on non null string objects.
      if (method.getName().endsWith("toString")) {
        // replaces s.toString() with s
        if (!instance.getType().canBeNull()) {
          // Only replace when it known to be non null, otherwise it should follow the normal path
          // and throw an NPE if null at runtime.
          ctx.replaceMe(instance);
        }
        return;
      }

      Object instanceLiteral = tryTranslateLiteral(instance, String.class);
      method = isStaticImplMethod ? program.instanceMethodForStaticImpl(method) : method;

      if (instanceLiteral == null && !method.isStatic()) {
        // Instance method on an instance that was not a literal.
        return;
      }

      // Extract constant values from arguments or bail out if not possible, and also extract the
      // declared parameter types that will be used to get the correct override.
      int numberOfParameters = method.getOriginalParamTypes().size();
      Object[] argumentConstantValues = new Object[numberOfParameters];
      Class<?>[] parametersClasses = new Class<?>[numberOfParameters];
      for (int i = 0; i < numberOfParameters; i++) {
        parametersClasses[i] = classObjectForType(method.getOriginalParamTypes().get(i));
        argumentConstantValues[i] = tryTranslateLiteral(arguments.get(i), parametersClasses[i]);
        if (parametersClasses[i] == null || argumentConstantValues[i] == null) {
          //  not a literal, cannot evaluate statically.
          return;
        }
      }

      // Finally invoke the method statically.
      JLiteral resultValue = staticallyInvokeStringMethod(
          method.getName(), instanceLiteral, parametersClasses, argumentConstantValues);
      if (resultValue != null) {
        ctx.replaceMe(resultValue);
      }
    }

    private JLiteral staticallyInvokeStringMethod(
        String methodName, Object instance, Class<?>[] parameterClasses, Object[] argumentValues) {
      Method actualMethod = getMethod(methodName, String.class, parameterClasses);
      if (actualMethod == null) {
        // Convert all parameters types to Object to find a more generic applicable method.
        Arrays.fill(parameterClasses, Object.class);
        actualMethod = getMethod(methodName, String.class, parameterClasses);
      }
      if (actualMethod == null) {
        return null;
      }

      try {
        return program.getLiteral(actualMethod.invoke(instance, argumentValues));
      } catch (Exception e) {
        // couldn't evaluate statically or convert the result to a JLiteral
        return  null;
      }
    }

    private Method getMethod(String name, Class<?> enclosingClass, Class<?>[] parameterTypes) {
      try {
        return enclosingClass.getMethod(name, parameterTypes);
      } catch (NoSuchMethodException e) {
        return null;
      }
    }

    private void tryRemoveSwitch(JSwitchStatement x, Context ctx) {
      JBlock body = x.getBody();
      if (body.getStatements().size() == 0) {
        // Empty switch; just run the switch condition.
        replaceMe(x.getExpr().makeStatement(), ctx);
      } else if (body.getStatements().size() == 2) {
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
        JCaseStatement caseStatement = (JCaseStatement) body.getStatements().get(0);
        JStatement statement = body.getStatements().get(1);

        FindBreakContinueStatementsVisitor visitor = new FindBreakContinueStatementsVisitor();
        visitor.accept(statement);
        if (visitor.hasBreakContinueStatements()) {
          // Cannot optimize.
          return;
        }

        if (caseStatement.getExpr() != null) {
          // Create an if statement equivalent to the single-case switch.
          JBinaryOperation compareOperation =
              new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
                  JBinaryOperator.EQ, x.getExpr(), caseStatement.getExpr());
          JBlock block = new JBlock(x.getSourceInfo());
          block.addStmt(statement);
          JIfStatement ifStatement =
              new JIfStatement(x.getSourceInfo(), compareOperation, block, null);
          replaceMe(ifStatement, ctx);
        } else {
          // All we have is a default case; convert to a JBlock.
          JBlock block = new JBlock(x.getSourceInfo());
          block.addStmt(x.getExpr().makeStatement());
          block.addStmt(statement);
          replaceMe(block, ctx);
        }
      }
    }

    private Object tryTranslateLiteral(JExpression maybeLiteral, Class<?> type) {
      if (!(maybeLiteral instanceof JValueLiteral)) {
        return null;
      }
      // TODO: make this way better by a mile
      if (type == boolean.class && maybeLiteral instanceof JBooleanLiteral) {
        return Boolean.valueOf(((JBooleanLiteral) maybeLiteral).getValue());
      }
      if (type == char.class && maybeLiteral instanceof JCharLiteral) {
        return Character.valueOf(((JCharLiteral) maybeLiteral).getValue());
      }
      if (type == double.class && maybeLiteral instanceof JFloatLiteral) {
        return new Double(((JFloatLiteral) maybeLiteral).getValue());
      }
      if (type == double.class && maybeLiteral instanceof JDoubleLiteral) {
        return new Double(((JDoubleLiteral) maybeLiteral).getValue());
      }
      if (type == int.class && maybeLiteral instanceof JIntLiteral) {
        return Integer.valueOf(((JIntLiteral) maybeLiteral).getValue());
      }
      if (type == long.class && maybeLiteral instanceof JLongLiteral) {
        return Long.valueOf(((JLongLiteral) maybeLiteral).getValue());
      }
      if (type == String.class && maybeLiteral instanceof JStringLiteral) {
        return ((JStringLiteral) maybeLiteral).getValue();
      }
      if (type == Object.class) {
        // We already know it is a JValueLiteral instance
        return ((JValueLiteral) maybeLiteral).getValueObj();
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

  public static final String NAME = DeadCodeElimination.class.getSimpleName();

  @VisibleForTesting
  public static OptimizerStats exec(JProgram program) {
    return new DeadCodeElimination(program).execImpl(Collections.singleton(program),
        OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
  }

  // TODO(leafwang): Mark this as @VisibleForTesting eventually; this code path is also used
  // by DataflowOptimizer for now.
  public static OptimizerStats exec(JProgram program, JMethod method) {
    return new DeadCodeElimination(program).execImpl(Collections.singleton(method),
        OptimizerContext.NULL_OPTIMIZATION_CONTEXT);
  }

  /**
   * Apply DeadCodeElimination on the set of newly modified methods (obtained from the optimzer
   * context).
   */
  public static OptimizerStats exec(JProgram program, OptimizerContext optimizerCtx) {
    Set<JMethod> affectedMethods =
        optimizerCtx.getModifiedMethodsSince(optimizerCtx.getLastStepFor(NAME));
    affectedMethods.addAll(optimizerCtx.getMethodsByReferencedFields(
        optimizerCtx.getModifiedFieldsSince(optimizerCtx.getLastStepFor(NAME))));
    OptimizerStats stats = new DeadCodeElimination(program).execImpl(affectedMethods, optimizerCtx);
    optimizerCtx.setLastStepFor(NAME, optimizerCtx.getOptimizationStep());
    optimizerCtx.incOptimizationStep();
    JavaAstVerifier.assertProgramIsConsistent(program);
    return stats;
  }

  private final JProgram program;

  private final Map<JType, Class<?>> classObjectsByType;

  public DeadCodeElimination(JProgram program) {
    this.program = program;
    classObjectsByType = new ImmutableMap.Builder()
        .put(program.getTypeJavaLangObject(), Object.class)
        .put(program.getTypeJavaLangString(), String.class)
        .put(program.getTypePrimitiveBoolean(), boolean.class)
        .put(program.getTypePrimitiveByte(), byte.class)
        .put(program.getTypePrimitiveChar(), char.class)
        .put(program.getTypePrimitiveDouble(), double.class)
        .put(program.getTypePrimitiveFloat(), float.class)
        .put(program.getTypePrimitiveInt(), int.class)
        .put(program.getTypePrimitiveLong(), long.class)
        .put(program.getTypePrimitiveShort(), short.class)
        .build();
  }

  private OptimizerStats execImpl(Iterable<? extends JNode> nodes, OptimizerContext optimizerCtx) {
    OptimizerStats stats = new OptimizerStats(NAME);
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);

    DeadCodeVisitor deadCodeVisitor = new DeadCodeVisitor(optimizerCtx);
    for (JNode node : nodes) {
      deadCodeVisitor.accept(node);
    }
    stats.recordModified(deadCodeVisitor.getNumMods());
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private enum AnalysisResult { TRUE, FALSE, UNKNOWN }
}
