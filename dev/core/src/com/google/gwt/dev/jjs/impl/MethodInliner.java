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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.util.collect.Stack;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Inline methods that can be inlined. The current implementation limits the
 * methods that can be inlined to those that are composed of at most two
 * top-level expressions.
 *
 * Future improvements will allow more complex methods to be inlined based on
 * the number of call sites, as well as adding support for more complex target
 * method expressions.
 */
public class MethodInliner {
  /**
   * Clones an expression, ensuring no local or this refs.
   */
  private static class CloneCalleeExpressionVisitor extends CloneExpressionVisitor {
    @Override
    public boolean visit(JThisRef x, Context ctx) {
      throw new InternalCompilerException("Should not encounter a JThisRef "
          + "within a static method");
    }
  }

  /**
   * Method inlining visitor.
   */
  private class InliningVisitor extends JChangeTrackingVisitor {

    public InliningVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    /**
     * Resets with each new visitor, which is good since things that couldn't be
     * inlined before might become inlinable.
     */
    private final Set<JMethod> cannotInline = Sets.newHashSet();
    private final Stack<JExpression> expressionsWhoseValuesAreIgnored = Stack.create();

    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      expressionsWhoseValuesAreIgnored.pop();
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (getCurrentMethod() == method) {
        // Never try to inline a recursive call!
        return;
      }

      if (cannotInline.contains(method)) {
        return;
      }

      if (tryInlineMethodCall(x, ctx) == InlineResult.BLACKLIST) {
        // Do not try to inline this method again
        cannotInline.add(method);
      }
    }

    @Override
    public void endVisit(JMultiExpression x, Context ctx) {
      for (int i = 0; i < x.getExpressions().size() - 1; i++) {
        expressionsWhoseValuesAreIgnored.pop();
      }
    }

    private InlineResult tryInlineMethodCall(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (!method.isStatic() || method.isJsniMethod() || method.canBeImplementedExternally()) {
        // Only inline static methods that are not native.
        return InlineResult.BLACKLIST;
      }

      if (!method.isInliningAllowed()) {
        return InlineResult.BLACKLIST;
      }

      JMethodBody body = (JMethodBody) method.getBody();
      List<JStatement> stmts = body.getStatements();

      if (method.getEnclosingType() != null
         && method.getEnclosingType().getClinitMethod() == method && !stmts.isEmpty()) {
          // clinit() calls cannot be inlined unless they are empty
        return InlineResult.BLACKLIST;
      }

      // try to inline
      List<JExpression> expressions = extractExpressionsFromBody(body);
      if (expressions == null) {
        // If it will never be possible to inline the method, add it to a
        // blacklist
        return InlineResult.BLACKLIST;
      }

      return tryInlineBody(x, ctx, expressions, expressionsWhoseValuesAreIgnored.contains(x));
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not inline new operations.
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      expressionsWhoseValuesAreIgnored.push(x.getExpr());
      return true;
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      if (program.getStaticImpl(x) != null) {
        /*
         * Never inline a static impl into the calling instance method. We used
         * to allow this, and it required all kinds of special logic in the
         * optimizers to keep the AST sane. This was because it was possible to
         * tighten an instance call to its static impl after the static impl had
         * already been inlined, this meant any "flow" type optimizer would have
         * to fake artificial flow from the instance method to the static impl.
         *
         * TODO: allow the inlining if we are the last remaining call site, and
         * prune the static impl? But it might tend to generate more code.
         */
        return false;
      }
      return true;
    }

    @Override
    public boolean visit(JMultiExpression x, Context ctx) {
      for (int i = 0; i < x.getExpressions().size() - 1; i++) {
        expressionsWhoseValuesAreIgnored.push(x.getExpression(i));
      }
      return true;
    }

    private JMethodCall createClinitCall(JMethodCall x) {
      JDeclaredType targetType = x.getTarget().getEnclosingType().getClinitTarget();
      if (!getCurrentMethod().getEnclosingType().checkClinitTo(targetType)) {
        // Access from this class to the target class won't trigger a clinit
        return null;
      }
      if (program.isStaticImpl(x.getTarget()) &&
          !x.getTarget().getEnclosingType().isJsoType()) {
        // No clinit needed; target is really a non-jso instance method.
        return null;
      }
      if (JProgram.isClinit(x.getTarget())) {
        // This is a clinit call, doesn't need another clinit
        return null;
      }

      JMethod clinit = targetType.getClinitMethod();

      // If the clinit is a non-native, empty body we can optimize it out here
      if (!clinit.isJsniMethod() && (((JMethodBody) clinit.getBody())).getStatements().size() == 0) {
        return null;
      }

      return new JMethodCall(x.getSourceInfo(), null, clinit);
    }

    /**
     * Creates a JMultiExpression from a set of JExpressionStatements,
     * optionally terminated by a JReturnStatement. If the method doesn't match
     * this pattern, it returns <code>null</code>.
     *
     * If a method has a non-void return statement and can be represented as a
     * multi-expression, the output of the multi-expression will be the return
     * expression of the method. If the method is void, the output of the
     * multi-expression should be considered undefined.
     */
    private List<JExpression> extractExpressionsFromBody(JMethodBody body) {
      List<JExpression> expressions = Lists.newArrayList();
      CloneCalleeExpressionVisitor cloner = new CloneCalleeExpressionVisitor();

      for (JStatement stmt : body.getStatements()) {
        if (stmt instanceof JDeclarationStatement) {
          JDeclarationStatement declStatement = (JDeclarationStatement) stmt;
          if (!(declStatement.getVariableRef() instanceof JLocalRef)) {
            return null;
          }
          JExpression initializer = declStatement.getInitializer();
          if (initializer == null) {
            continue;
          }
          JLocal local = (JLocal) declStatement.getVariableRef().getTarget();
          JExpression clone = new JBinaryOperation(stmt.getSourceInfo(), local.getType(),
              JBinaryOperator.ASG,
              local.makeRef(declStatement.getVariableRef().getSourceInfo()),
              cloner.cloneExpression(initializer));
          expressions.add(clone);
        } else if (stmt instanceof JExpressionStatement) {
          JExpressionStatement exprStmt = (JExpressionStatement) stmt;
          JExpression expr = exprStmt.getExpr();
          JExpression clone = cloner.cloneExpression(expr);
          expressions.add(clone);
        } else if (stmt instanceof JReturnStatement) {
          JReturnStatement returnStatement = (JReturnStatement) stmt;
          JExpression expr = returnStatement.getExpr();
          if (expr != null) {
            JExpression clone = cloner.cloneExpression(expr);
            clone = maybeCast(clone, body.getMethod().getType());
            expressions.add(clone);
          }
          // We hit an unconditional return; no need to evaluate anything else.
          break;
        } else {
          // Any other kind of statement won't be inlinable.
          return null;
        }
      }

      return expressions;
    }

    /**
     * Creates a lists of expression for evaluating a method call instance,
     * possible clinit, and all arguments. This is a precursor for inlining the
     * remainder of a method that does not reference any parameters.
     */
    private List<JExpression> expressionsIncludingArgs(JMethodCall x) {
      List<JExpression> expressions = Lists.newArrayListWithCapacity(x.getArgs().size() + 2);
      expressions.add(x.getInstance());
      expressions.add(createClinitCall(x));

      for (int i = 0, c = x.getArgs().size(); i < c; ++i) {
        JExpression arg = x.getArgs().get(i);
        ExpressionAnalyzer analyzer = new ExpressionAnalyzer();
        analyzer.accept(arg);

        if (analyzer.hasAssignment() || analyzer.canThrowException()) {
          expressions.add(arg);
        }
      }
      return expressions;
    }

    /**
     * Inline a call to an expression. Returns {@code InlineResult.BLACKLIST} if the method is
     * deemed not inlineable regardless of call site; {@code InlineResult.DO_NOT_BLACKLIST}
     * otherwise.
     */
    private InlineResult tryInlineBody(JMethodCall x, Context ctx,
        List<JExpression> bodyAsExpressionList, boolean ignoringReturn) {

      if (isTooComplexToInline(bodyAsExpressionList, ignoringReturn)) {
        return InlineResult.BLACKLIST;
      }

      // Do not inline anything that modifies one of its params.
      ExpressionAnalyzer targetAnalyzer = new ExpressionAnalyzer();
      targetAnalyzer.accept(bodyAsExpressionList);
      if (targetAnalyzer.hasAssignmentToParameter()) {
        return InlineResult.BLACKLIST;
      }

      // Make sure the expression we're about to inline doesn't include a call
      // to the target method!
      RecursionCheckVisitor recursionCheckVisitor = new RecursionCheckVisitor(x.getTarget());
      recursionCheckVisitor.accept(bodyAsExpressionList);
      if (recursionCheckVisitor.isRecursive()) {
        return InlineResult.BLACKLIST;
      }

      /*
       * After this point, it's possible that the method might be inlinable at
       * some call sites, depending on its arguments. From here on return 'true'
       * as the method might be inlinable elsewhere.
       */

      /*
       * There are a different number of parameters than args - this is likely a
       * result of parameter pruning. Don't consider this call site a candidate.
       *
       * TODO: would this be possible in the trivial delegation case?
       */
      if (x.getTarget().getParams().size() != x.getArgs().size()) {
        // Could not inline this call but the method might be inlineable at a different call site.
        return InlineResult.DO_NOT_BLACKLIST;
      }

      // Run the order check. This verifies that all the parameters are
      // referenced once and only once, not within a conditionally-executing
      // expression and before any tricky target expressions, such as:
      // - assignments to any variable
      // - expressions that throw exceptions
      // - field references

      /*
       * Ensure correct evaluation order or params relative to each other and to
       * other expressions.
       */
      OrderVisitor orderVisitor = new OrderVisitor(x.getTarget().getParams());
      orderVisitor.accept(bodyAsExpressionList);

      switch (orderVisitor.checkResults()) {
        case NO_REFERENCES:
          /*
           * A method that doesn't touch any parameters is trivially inlinable (this
           * covers the empty method case)
           */
          if (!x.hasSideEffects()) {
            markCallsAsSideEffectFree(bodyAsExpressionList);
          }
          new LocalVariableExtruder(getCurrentMethod()).accept(bodyAsExpressionList);
          List<JExpression> expressions = expressionsIncludingArgs(x);
          expressions.addAll(bodyAsExpressionList);
          ctx.replaceMe(JjsUtils.createOptimizedMultiExpression(ignoringReturn, expressions));
          return InlineResult.DO_NOT_BLACKLIST;
        case FAILS:
          /*
           * We can still inline in the case where all of the actual arguments are
           * "safe". They must have no side effects, and also have values which
           * could not be affected by the execution of any code within the callee.
           */
          for (JExpression arg : x.getArgs()) {
            ExpressionAnalyzer argAnalyzer = new ExpressionAnalyzer();
            argAnalyzer.accept(arg);

            if (argAnalyzer.hasAssignment() || argAnalyzer.accessesField()
                || argAnalyzer.createsObject() || argAnalyzer.canThrowException()) {

              /*
               * This argument evaluation could affect or be affected by the
               * callee so we cannot inline here.
               */
              // Could not inline this call but the method is potentially inlineable.
              return InlineResult.DO_NOT_BLACKLIST;
            }
          }
          // Fall through!
        case CORRECT_ORDER:
        default:
          if (!x.hasSideEffects()) {
            markCallsAsSideEffectFree(bodyAsExpressionList);
          }
          new LocalVariableExtruder(getCurrentMethod()).accept(bodyAsExpressionList);
          // Replace all params in the target expression with the actual arguments.
          ParameterReplacer replacer = new ParameterReplacer(x);
          replacer.accept(bodyAsExpressionList);
          bodyAsExpressionList.add(0, x.getInstance());
          bodyAsExpressionList.add(1, createClinitCall(x));
          ctx.replaceMe(JjsUtils.createOptimizedMultiExpression(ignoringReturn,
              bodyAsExpressionList));
          return InlineResult.DO_NOT_BLACKLIST;
      }
    }
  }

  private static void markCallsAsSideEffectFree(List<JExpression> expressions) {
    // Propagate side effect information to the inlined body due to @HasNoSideEffects annotation
    // in the method.
    new JModVisitor() {
      @Override
      public void endVisit(JMethodCall x, Context ctx) {
        x.markSideEffectFree();
      }

    }.accept(expressions);
  }

  private static boolean isTooComplexToInline(List<JExpression> bodyAsExpressionList,
      boolean ignoringReturn) {
    /*
     * Limit inlined methods to multiexpressions of length 2 for now. This
     * handles the simple { return JVariableRef; } or { expression; return
     * something; } cases.
     *
     * TODO: add an expression complexity analyzer.
     */
    if (bodyAsExpressionList.size() > 3) {
      return true;
    }

    if (bodyAsExpressionList.size() == 3
        && (!ignoringReturn || bodyAsExpressionList.get(2).hasSideEffects())) {
      return true;
    }

    // The expression is effectively of size 2, hence not too complex to inline.
    return false;
  }

  /**
   * Verifies that all the parameters are referenced once and only once, not
   * within a conditionally-executing expression, and any before trouble some
   * expressions evaluate. Examples of troublesome expressions include:
   *
   * <ul>
   * <li>assignments to any variable</li>
   * <li>expressions that throw exceptions</li>
   * <li>field references</li>
   * </ul>
   */
  private static class OrderVisitor extends ExpressionAnalyzer {
    private int currentIndex = 0;
    private final List<JParameter> parameters;
    private boolean succeeded = true;

    public OrderVisitor(List<JParameter> parameters) {
      this.parameters = parameters;
    }

    public SideEffectCheck checkResults() {
      if (succeeded && currentIndex == parameters.size()) {
        return SideEffectCheck.CORRECT_ORDER;
      }

      if (succeeded && currentIndex == 0) {
        return SideEffectCheck.NO_REFERENCES;
      }

      return SideEffectCheck.FAILS;
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      JParameter param = x.getParameter();

      // If the expression has side-effects before a parameter reference, fail
      if (hasAssignment() || accessesField() || canThrowException()) {
        succeeded = false;
      }

      // If this parameter reference won't always execute, fail
      if (isInConditional()) {
        succeeded = false;
      }

      // Ensure this parameter is evaluated in the correct order relative to
      // other parameters.
      if (parameters.indexOf(param) == currentIndex) {
        currentIndex++;
      } else {
        succeeded = false;
      }

      super.endVisit(x, ctx);
    }
  }

  /**
   * Replace parameters inside an inlined expression with arguments to the
   * inlined method.
   */
  private class ParameterReplacer extends JModVisitor {
    private final JMethodCall methodCall;

    public ParameterReplacer(JMethodCall methodCall) {
      this.methodCall = methodCall;
    }

    @Override
    public void endVisit(JParameterRef x, Context ctx) {
      int paramIndex = methodCall.getTarget().getParams().indexOf(x.getParameter());
      assert paramIndex != -1;

      // Replace with a cloned call argument.
      CloneExpressionVisitor cloner = new CloneExpressionVisitor();
      JExpression arg = methodCall.getArgs().get(paramIndex);
      JExpression clone = cloner.cloneExpression(arg);

      clone = maybeCast(clone, x.getType());
      ctx.replaceMe(clone);
    }
  }

  /**
   * Extrudes local variables from the body into the currect method.
   */
  private class LocalVariableExtruder extends JModVisitor {
    private final Map<JLocal, JLocal> newLocalsByOriginalLocal = Maps.newLinkedHashMap();
    private final JMethodBody methodBody;

    public LocalVariableExtruder(JMethod method) {
      methodBody = (JMethodBody) method.getBody();
    }

    @Override
    public void endVisit(JLocalRef x, Context ctx) {
      JLocal originalLocal = x.getLocal();
      JLocal newLocal = newLocalsByOriginalLocal.get(originalLocal);
      if (newLocal == null) {
        newLocal = JProgram.createLocal(originalLocal.getSourceInfo(), originalLocal.getName(), originalLocal.getType(), originalLocal.isFinal(), methodBody);
        newLocalsByOriginalLocal.put(originalLocal, newLocal);
      }

      ctx.replaceMe(newLocal.makeRef(x.getSourceInfo()));
    }
  }

  private static class RecursionCheckVisitor extends JVisitor {
    private boolean isRecursive = false;
    private final JMethod method;

    public RecursionCheckVisitor(JMethod method) {
      this.method = method;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      if (x.getTarget() == method) {
        isRecursive = true;
      }
    }

    public boolean isRecursive() {
      return isRecursive;
    }
  }

  /**
   * Results of a side-effect and order check.
   */
  private enum SideEffectCheck {
    CORRECT_ORDER, FAILS, NO_REFERENCES
  }

  public static String NAME = MethodInliner.class.getSimpleName();

  public static OptimizerStats exec(JProgram program, OptimizerContext optimizerCtx) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodInliner(program).execImpl(optimizerCtx);
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  public static OptimizerStats exec(JProgram program) {
    return exec(program, new FullOptimizerContext(program));
  }

  private final JProgram program;

  private MethodInliner(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl(OptimizerContext optimizerCtx) {
    OptimizerStats stats = new OptimizerStats(NAME);
    while (true) {
      InliningVisitor inliner = new InliningVisitor(optimizerCtx);

      Set<JMethod> modifiedMethods =
          optimizerCtx.getModifiedMethodsSince(optimizerCtx.getLastStepFor(NAME));
      Set<JMethod> affectedMethods = affectedMethods(modifiedMethods, optimizerCtx);
      optimizerCtx.traverse(inliner, affectedMethods);

      stats.recordModified(inliner.getNumMods());
      optimizerCtx.setLastStepFor(NAME, optimizerCtx.getOptimizationStep());

      optimizerCtx.incOptimizationStep();

      if (!inliner.didChange()) {
        break;
      }

      // Run a cleanup on the methods we just modified
      OptimizerStats dceStats = DeadCodeElimination.exec(program, optimizerCtx);
      stats.recordModified(dceStats.getNumMods());
    }
    JavaAstVerifier.assertProgramIsConsistent(program);
    return stats;
  }

  /**
   * Return the set of methods affected (because they are or callers of) by the modifications to the
   * given set functions.
   */
  private Set<JMethod> affectedMethods(Set<JMethod> modifiedMethods,
      OptimizerContext optimizerCtx) {
    assert (modifiedMethods != null);
    Set<JMethod> affectedMethods = Sets.newLinkedHashSet();
    affectedMethods.addAll(modifiedMethods);
    affectedMethods.addAll(optimizerCtx.getCallers(modifiedMethods));
    return affectedMethods;
  }

  /**
   * Insert an implicit cast if the types differ; it might get optimized out
   * later, but in some cases it will force correct math evaluation.
   */
  private JExpression maybeCast(JExpression exp, JType targetType) {
    if (targetType instanceof JReferenceType) {
      assert exp.getType() instanceof JReferenceType;
      targetType = merge((JReferenceType) exp.getType(), (JReferenceType) targetType);
    }
    if (!program.typeOracle.castSucceedsTrivially(exp.getType(), targetType)) {
      exp = new JCastOperation(exp.getSourceInfo(), targetType, exp);
    }
    return exp;
  }

  private JReferenceType merge(JReferenceType source, JReferenceType target) {
    JReferenceType result;
    if (program.typeOracle.castSucceedsTrivially(
        source.getUnderlyingType(), target.getUnderlyingType())) {
      result = source;
    } else {
      result = target;
    }
    return result;
  }

  private enum InlineResult { BLACKLIST, DO_NOT_BLACKLIST}
}
