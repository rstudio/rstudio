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
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
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
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.LinkedHashSet;
import java.util.List;
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
    public boolean visit(JLocalRef x, Context ctx) {
      throw new InternalCompilerException(
          "Unable to clone a local reference in a function being inlined");
    }

    @Override
    public boolean visit(JThisRef x, Context ctx) {
      throw new InternalCompilerException("Should not encounter a JThisRef "
          + "within a static method");
    }
  }

  /**
   * Method inlining visitor.
   */
  private class InliningVisitor extends JModVisitor {
    protected final Set<JMethod> modifiedMethods = new LinkedHashSet<JMethod>();

    /**
     * Resets with each new visitor, which is good since things that couldn't be
     * inlined before might become inlinable.
     */
    private final Set<JMethod> cannotInline = Sets.newHashSet();
    private JExpression ignoringReturnValueFor;

    @Override
    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();

      if (currentMethod == method) {
        // Never try to inline a recursive call!
        return;
      }

      if (cannotInline.contains(method)) {
        return;
      }

      if (!tryInlineMethodCall(x, ctx)) {
        // Do not try to inline this method again
        cannotInline.add(method);
      }
    }

    private boolean tryInlineMethodCall(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (JProgram.isJsInterfacePrototype(method.getEnclosingType())) {
         // don't inline calls to JsInterface Prototype methods, since these are merely stubs to
         // preserve super calls
        return false;
      }

      if (!method.isStatic() || method.isNative()) {
        // Only inline static methods that are not native.
        return false;
      }

      if (!program.isInliningAllowed(method)) {
        return false;
      }

      JMethodBody body = (JMethodBody) method.getBody();
      List<JStatement> stmts = body.getStatements();

      if (method.getEnclosingType() != null
         && method.getEnclosingType().getClinitMethod() == method && !stmts.isEmpty()) {
          // clinit() calls cannot be inlined unless they are empty
        return false;
      }

      if (!body.getLocals().isEmpty()) {
          // methods with local variables cannot be inlined
        return false;
      }

      // try to inline
      JMultiExpression multi = createMultiExpressionFromBody(body, ignoringReturnValueFor == x);
      if (multi == null || !tryInlineExpression(x, ctx, multi)) {
        // If it will never be possible to inline the method, add it to a
        // blacklist
        return false;
      }

      // Successfully inlined.
      return true;
    }

    @Override
    public void endVisit(JNewInstance x, Context ctx) {
      // Do not inline new operations.
    }

    @Override
    public boolean visit(JExpressionStatement x, Context ctx) {
      ignoringReturnValueFor = x.getExpr();
      return true;
    }

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      if (program.getStaticImpl(x) != null) {
        /*
         * Never inline a static impl into the calling instance method. We used
         * to allow this, and it required all kinds of special logic in the
         * optimizers to keep the AST sane. This was because it was possible to
         * tighten an instance call to its static impl after the static impl had
         * already been inlined, this meant any "flow" type optimizer would have
         * to fake artifical flow from the instance method to the static impl.
         *
         * TODO: allow the inlining if we are the last remaining call site, and
         * prune the static impl? But it might tend to generate more code.
         */
        return false;
      }
      return true;
    }

    private JMethodCall createClinitCall(JMethodCall x) {
      JDeclaredType targetType = x.getTarget().getEnclosingType().getClinitTarget();
      if (!currentMethod.getEnclosingType().checkClinitTo(targetType)) {
        // Access from this class to the target class won't trigger a clinit
        return null;
      }
      if (program.isStaticImpl(x.getTarget())) {
        // No clinit needed; target is really an instance method.
        return null;
      }
      if (JProgram.isClinit(x.getTarget())) {
        // This is a clinit call, doesn't need another clinit
        return null;
      }

      JMethod clinit = targetType.getClinitMethod();

      // If the clinit is a non-native, empty body we can optimize it out here
      if (!clinit.isNative() && (((JMethodBody) clinit.getBody())).getStatements().size() == 0) {
        return null;
      }

      return new JMethodCall(x.getSourceInfo(), null, clinit);
    }

    /**
     * Creates a multi expression for evaluating a method call instance and
     * possible clinit. This is a precursor for inlining the remainder of a
     * method.
     */
    private JMultiExpression createMultiExpressionForInstanceAndClinit(JMethodCall x) {
      JMultiExpression multi = new JMultiExpression(x.getSourceInfo());

      // Any instance expression goes first (this can happen even with statics).
      if (x.getInstance() != null) {
        multi.addExpressions(x.getInstance());
      }

      // If we need a clinit call, add it first
      JMethodCall clinit = createClinitCall(x);
      if (clinit != null) {
        multi.addExpressions(clinit);
      }
      return multi;
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
    private JMultiExpression createMultiExpressionFromBody(JMethodBody body,
        boolean ignoringReturnValue) {
      JMultiExpression multi = new JMultiExpression(body.getSourceInfo());
      CloneCalleeExpressionVisitor cloner = new CloneCalleeExpressionVisitor();

      for (JStatement stmt : body.getStatements()) {
        if (stmt instanceof JExpressionStatement) {
          JExpressionStatement exprStmt = (JExpressionStatement) stmt;
          JExpression expr = exprStmt.getExpr();
          JExpression clone = cloner.cloneExpression(expr);
          multi.addExpressions(clone);
        } else if (stmt instanceof JReturnStatement) {
          JReturnStatement returnStatement = (JReturnStatement) stmt;
          JExpression expr = returnStatement.getExpr();
          if (expr != null) {
            if (!ignoringReturnValue || expr.hasSideEffects()) {
              JExpression clone = cloner.cloneExpression(expr);
              clone = maybeCast(clone, body.getMethod().getType());
              multi.addExpressions(clone);
            }
          }
          // We hit an unconditional return; no need to evaluate anything else.
          break;
        } else {
          // Any other kind of statement won't be inlinable.
          return null;
        }
      }

      return multi;
    }

    /**
     * Creates a multi expression for evaluating a method call instance,
     * possible clinit, and all arguments. This is a precursor for inlining the
     * remainder of a method that does not reference any parameters.
     */
    private JMultiExpression createMultiExpressionIncludingArgs(JMethodCall x) {
      JMultiExpression multi = createMultiExpressionForInstanceAndClinit(x);

      for (int i = 0, c = x.getArgs().size(); i < c; ++i) {
        JExpression arg = x.getArgs().get(i);
        ExpressionAnalyzer analyzer = new ExpressionAnalyzer();
        analyzer.accept(arg);

        if (analyzer.hasAssignment() || analyzer.canThrowException()) {
          multi.addExpressions(arg);
        }
      }
      return multi;
    }

    /**
     * Replace the current expression with a given multi-expression and mark the
     * method as modified. The dead-code elimination pass will optimize this if
     * necessary.
     */
    private void replaceWithMulti(Context ctx, JMultiExpression multi) {
      ctx.replaceMe(multi);
      modifiedMethods.add(currentMethod);
    }

    /**
     * Inline a call to an expression.
     */
    private boolean tryInlineExpression(JMethodCall x, Context ctx, JMultiExpression targetExpr) {
      /*
       * Limit inlined methods to multiexpressions of length 2 for now. This
       * handles the simple { return JVariableRef; } or { expression; return
       * something; } cases.
       *
       * TODO: add an expression complexity analyzer.
       */
      if (targetExpr.getNumberOfExpressions() > 2) {
        return false;
      }

      // Do not inline anything that modifies one of its params.
      ExpressionAnalyzer targetAnalyzer = new ExpressionAnalyzer();
      targetAnalyzer.accept(targetExpr);
      if (targetAnalyzer.hasAssignmentToParameter()) {
        return false;
      }

      // Make sure the expression we're about to inline doesn't include a call
      // to the target method!
      RecursionCheckVisitor recursionCheckVisitor = new RecursionCheckVisitor(x.getTarget());
      recursionCheckVisitor.accept(targetExpr);
      if (recursionCheckVisitor.isRecursive()) {
        return false;
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
        return true;
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
      orderVisitor.accept(targetExpr);

      /*
       * A method that doesn't touch any parameters is trivially inlinable (this
       * covers the empty method case)
       */
      if (orderVisitor.checkResults() == SideEffectCheck.NO_REFERENCES) {
        JMultiExpression multi = createMultiExpressionIncludingArgs(x);
        multi.addExpressions(targetExpr);
        replaceWithMulti(ctx, multi);
        return true;
      }

      /*
       * We can still inline in the case where all of the actual arguments are
       * "safe". They must have no side effects, and also have values which
       * could not be affected by the execution of any code within the callee.
       */
      if (orderVisitor.checkResults() == SideEffectCheck.FAILS) {
        for (JExpression arg : x.getArgs()) {
          ExpressionAnalyzer argAnalyzer = new ExpressionAnalyzer();
          argAnalyzer.accept(arg);

          if (argAnalyzer.hasAssignment() || argAnalyzer.accessesField()
              || argAnalyzer.createsObject() || argAnalyzer.canThrowException()) {

            /*
             * This argument evaluation could affect or be affected by the
             * callee so we cannot inline here.
             */
            return true;
          }
        }
      }

      // We're safe to inline.
      JMultiExpression multi = createMultiExpressionForInstanceAndClinit(x);

      // Replace all params in the target expression with the actual arguments.
      ParameterReplacer replacer = new ParameterReplacer(x);
      replacer.accept(targetExpr);

      multi.addExpressions(targetExpr);
      replaceWithMulti(ctx, multi);
      return true;
    }
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

  public static OptimizerStats exec(JProgram program) {
    Event optimizeEvent = SpeedTracerLogger.start(CompilerEventType.OPTIMIZE, "optimizer", NAME);
    OptimizerStats stats = new MethodInliner(program).execImpl();
    optimizeEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  private JMethod currentMethod;

  private final JProgram program;

  private MethodInliner(JProgram program) {
    this.program = program;
  }

  private OptimizerStats execImpl() {
    OptimizerStats stats = new OptimizerStats(NAME);
    while (true) {
      InliningVisitor inliner = new InliningVisitor();
      inliner.accept(program);
      stats.recordModified(inliner.getNumMods());
      if (!inliner.didChange()) {
        break;
      }

      // Run a cleanup on the methods we just modified
      for (JMethod method : inliner.modifiedMethods) {
        OptimizerStats innerStats = DeadCodeElimination.exec(program, method);
        stats.recordModified(innerStats.getNumMods());
      }
    }
    return stats;
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
    if (!program.typeOracle.canTriviallyCast(exp.getType(), targetType)) {
      exp = new JCastOperation(exp.getSourceInfo(), targetType, exp);
    }
    return exp;
  }

  private JReferenceType merge(JReferenceType source, JReferenceType target) {
    JReferenceType result;
    if (program.typeOracle.canTriviallyCast(source.getUnderlyingType(), target.getUnderlyingType())) {
      result = source;
    } else {
      result = target;
    }
    return result;
  }
}
