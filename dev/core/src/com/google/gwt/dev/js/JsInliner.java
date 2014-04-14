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
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.impl.OptimizerStats;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatchScope;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Perform inlining optimizations on the JavaScript AST.
 *
 * TODO(bobv): remove anything that's duplicating work with {@link JsStaticEval}
 * migrate other stuff to that class perhaps.
 */
public class JsInliner {
  private static final String NAME = JsInliner.class.getSimpleName();

  /**
   * Determines if the evaluation of a JsNode may be affected by side effects.
   */
  private static class AffectedBySideEffectsVisitor extends JsVisitor {
    private boolean affectedBySideEffects;
    private final JsScope safeScope;

    public AffectedBySideEffectsVisitor(JsScope safeScope) {
      this.safeScope = safeScope;
    }

    public boolean affectedBySideEffects() {
      return affectedBySideEffects;
    }

    @Override
    public void endVisit(JsArrayLiteral x, JsContext ctx) {
      affectedBySideEffects = true;
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      affectedBySideEffects = true;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      /*
       * We could make this more accurate by analyzing the function that's being
       * executed, but we'll bank on subsequent passes inlining simple function
       * invocations.
       */
      affectedBySideEffects = true;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x.getQualifier() == null && x.getName() != null) {
        // Special case the undefined literal.
        if (x.getName() == JsRootScope.INSTANCE.getUndefined()) {
          return;
        }
        // Locals in a safe scope are unaffected.
        if (x.getName().getEnclosing() == safeScope) {
          return;
        }
      }

      /*
       * We can make this more accurate if we had single-assignment information
       * (e.g. static final fields).
       */
      affectedBySideEffects = true;
    }

    @Override
    public void endVisit(JsObjectLiteral x, JsContext ctx) {
      affectedBySideEffects = true;
    }
  }

  /**
   * Make comma binary operations left-nested since commas are naturally
   * left-associative. We will define the comma-normal form such that a comma
   * expression should never have a comma expression as its RHS and contains no
   * side-effect-free expressions save for the outer, right-hand expression.
   * This form has a nice side-effect of minimizing the number of generated
   * parentheses.
   *
   * <pre>
   * (X, b) is unchanged
   * (X, (b, c) becomes ((X, b), c); b is guaranteed to have a side-effect
   * (X, ((b, c), d)) becomes (((X, b), c), d)
   * </pre>
   */
  private static class CommaNormalizer extends JsModVisitor {

    /**
     * Returns an expression as a JsBinaryOperation if it is a comma expression.
     */
    private static JsBinaryOperation isComma(JsExpression x) {
      if (!(x instanceof JsBinaryOperation)) {
        return null;
      }
      JsBinaryOperation op = (JsBinaryOperation) x;

      return op.getOperator().equals(JsBinaryOperator.COMMA) ? op : null;
    }

    private final List<JsName> localVariableNames;

    public CommaNormalizer(List<JsName> localVariableNames) {
      this.localVariableNames = localVariableNames;
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      if (isComma(x) == null) {
        return;
      }

      // If (X, a) and X has no side effects, replace with a
      if (!x.getArg1().hasSideEffects()) {
        ctx.replaceMe(x.getArg2());
        return;
      }

      JsBinaryOperation toUpdate = isComma(x.getArg2());
      if (toUpdate == null) {
        /*
         * We have a JsBinaryOperation that's structurally normal: (X, a). Now
         * it may be the case that the inner expression X is a comma expression
         * (Y, b). If b creates no side-effects, we can remove it, leaving (Y,
         * a) as the expression.
         */
        JsBinaryOperation inner = isComma(x.getArg1());
        if (inner != null && !inner.getArg2().hasSideEffects()) {
          x.setArg1(inner.getArg1());
          didChange = true;
        }

        /*
         * Eliminate the pattern (localVar = expr, localVar). This tends to
         * occur when a method interacted with pruned fields or had statements
         * removed.
         */
        JsName assignmentRef = null;
        JsExpression expr = null;
        JsName returnRef = null;

        if (x.getArg1() instanceof JsBinaryOperation) {
          JsBinaryOperation op = (JsBinaryOperation) x.getArg1();
          if (op.getOperator() == JsBinaryOperator.ASG
              && op.getArg1() instanceof JsNameRef) {
            JsNameRef nameRef = (JsNameRef) op.getArg1();
            if (nameRef.getQualifier() == null) {
              assignmentRef = nameRef.getName();
              expr = op.getArg2();
            }
          }
        }

        if (x.getArg2() instanceof JsNameRef) {
          JsNameRef nameRef = (JsNameRef) x.getArg2();
          if (nameRef.getQualifier() == null) {
            returnRef = nameRef.getName();
          }
        }

        if (assignmentRef != null && assignmentRef.equals(returnRef)
            && localVariableNames.contains(assignmentRef)) {
          assert expr != null;
          localVariableNames.remove(assignmentRef);
          ctx.replaceMe(expr);
        }
        return;
      }

      // Find the left-most, nested comma expression
      while (isComma(toUpdate.getArg1()) != null) {
        toUpdate = (JsBinaryOperation) toUpdate.getArg1();
      }

      /*
       * Create a new comma expression with the original LHS and the LHS of the
       * nested comma expression.
       */
      JsBinaryOperation newOp = new JsBinaryOperation(x.getSourceInfo(),
          JsBinaryOperator.COMMA);
      newOp.setArg1(x.getArg1());
      newOp.setArg2(toUpdate.getArg1());

      // Set the LHS of the nested comma expression to the new comma expression
      toUpdate.setArg1(newOp);

      // Replace the original node with its updated RHS
      ctx.replaceMe(x.getArg2());
    }
  }

  /**
   * Provides a relative metric by which the syntactic complexity of a
   * JsExpression can be gauged.
   */
  private static class ComplexityEstimator extends JsVisitor {
    /**
     * The current measure of complexity. This measures the number of
     * expressions that have been encountered by the visitor.
     */
    private int complexity = 0;

    @Override
    public void endVisit(JsArrayAccess x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsArrayLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsBooleanLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsConditional x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNew x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNullLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNumberLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsObjectLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsPostfixOperation x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsPrefixOperation x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsRegExp x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsStringLiteral x, JsContext ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsThisRef x, JsContext ctx) {
      complexity++;
    }

    public int getComplexity() {
      return complexity;
    }
  }

  /**
   * This is used to clean up duplication invocations of functions that should
   * only be executed once, such as clinit functions. Whenever there is a
   * possible branch in program flow, the remover will create a new instance of
   * itself to handle the possible branches.
   *
   * We don't look at combining branch choices. This will not produce the most
   * efficient elimination of duplicated calls, but it handles the general case
   * and is simple to verify.
   */
  private static class DuplicateXORemover extends JsModVisitor {
    /*
     * TODO: Most of the special casing below can be removed if complex
     * statements always use blocks, rather than plain statements.
     */

    /**
     * Retains the functions that we know have been called.
     */
    private final Set<JsFunction> called;
    private final JsProgram program;

    public DuplicateXORemover(JsProgram program) {
      this.program = program;
      called = new HashSet<JsFunction>();
    }

    public DuplicateXORemover(JsProgram program, Set<JsFunction> alreadyCalled) {
      this.program = program;
      called = new HashSet<JsFunction>(alreadyCalled);
    }

    /**
     * Look for comma expressions that contain duplicate calls and handle the
     * conditional-evaluation case of logical and/or operations.
     */
    @Override
    public boolean visit(JsBinaryOperation x, JsContext ctx) {
      if (x.getOperator() == JsBinaryOperator.COMMA) {

        boolean left = isDuplicateCall(x.getArg1());
        boolean right = isDuplicateCall(x.getArg2());

        if (left && right) {
          /*
           * (clinit(), clinit()) --> delete or null.
           *
           * This construct is very unlikely since the InliningVisitor builds
           * the comma expressions in a right-nested manner.
           */
          if (ctx.canRemove()) {
            ctx.removeMe();
            return false;
          } else {
            // The return value from an XO function is never used
            ctx.replaceMe(JsNullLiteral.INSTANCE);
            return false;
          }

        } else if (left) {
          // (clinit(), xyz) --> xyz
          // This is the common case
          ctx.replaceMe(accept(x.getArg2()));
          return false;

        } else if (right) {
          // (xyz, clinit()) --> xyz
          // Possible if a clinit() were the last element
          ctx.replaceMe(accept(x.getArg1()));
          return false;
        }

      } else if (x.getOperator().equals(JsBinaryOperator.AND)
          || x.getOperator().equals(JsBinaryOperator.OR)) {
        x.setArg1(accept(x.getArg1()));
        // Possibility of conditional evaluation of second parameter
        x.setArg2(branch(x.getArg2()));
        return false;
      }

      return true;
    }

    /**
     * Most of the branching statements (as well as JsFunctions) will visit with
     * a JsBlock, so we don't need to explicitly enumerate all JsStatement
     * subtypes.
     */
    @Override
    public boolean visit(JsBlock x, JsContext ctx) {
      branch(x.getStatements());
      return false;
    }

    @Override
    public boolean visit(JsCase x, JsContext ctx) {
      x.setCaseExpr(accept(x.getCaseExpr()));
      branch(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsConditional x, JsContext ctx) {
      x.setTestExpression(accept(x.getTestExpression()));
      x.setThenExpression(branch(x.getThenExpression()));
      x.setElseExpression(branch(x.getElseExpression()));
      return false;
    }

    @Override
    public boolean visit(JsDefault x, JsContext ctx) {
      branch(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsExprStmt x, JsContext ctx) {
      if (isDuplicateCall(x.getExpression())) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(new JsEmpty(x.getSourceInfo()));
        }
        return false;

      } else {
        return true;
      }
    }

    @Override
    public boolean visit(JsFor x, JsContext ctx) {
      // The JsFor may have an expression xor a variable declaration.
      if (x.getInitExpr() != null) {
        x.setInitExpr(accept(x.getInitExpr()));
      } else if (x.getInitVars() != null) {
        x.setInitVars(accept(x.getInitVars()));
      }

      // The condition is optional
      if (x.getCondition() != null) {
        x.setCondition(accept(x.getCondition()));
      }

      // The increment expression is optional
      if (x.getIncrExpr() != null) {
        x.setIncrExpr(branch(x.getIncrExpr()));
      }

      // The body is not guaranteed to be a JsBlock
      x.setBody(branch(x.getBody()));
      return false;
    }

    @Override
    public boolean visit(JsForIn x, JsContext ctx) {
      if (x.getIterExpr() != null) {
        x.setIterExpr(accept(x.getIterExpr()));
      }

      x.setObjExpr(accept(x.getObjExpr()));

      // The body is not guaranteed to be a JsBlock
      x.setBody(branch(x.getBody()));
      return false;
    }

    @Override
    public boolean visit(JsIf x, JsContext ctx) {
      x.setIfExpr(accept(x.getIfExpr()));

      x.setThenStmt(branch(x.getThenStmt()));
      if (x.getElseStmt() != null) {
        x.setElseStmt(branch(x.getElseStmt()));
      }

      return false;
    }

    /**
     * Possibly record that we've seen a call in the current context.
     */
    @Override
    public boolean visit(JsInvocation x, JsContext ctx) {
      JsFunction func = isExecuteOnce(x);
      while (func != null) {
        called.add(func);
        func = func.getImpliedExecute();
      }
      return true;
    }

    @Override
    public boolean visit(JsWhile x, JsContext ctx) {
      x.setCondition(accept(x.getCondition()));

      // The body is not guaranteed to be a JsBlock
      x.setBody(branch(x.getBody()));
      return false;
    }

    private <T extends JsNode> void branch(List<T> x) {
      DuplicateXORemover dup = new DuplicateXORemover(program, called);
      dup.acceptWithInsertRemove(x);
      didChange |= dup.didChange();
    }

    private <T extends JsNode> T branch(T x) {
      DuplicateXORemover dup = new DuplicateXORemover(program, called);
      T toReturn = dup.accept(x);

      if ((toReturn != x) && !dup.didChange()) {
        throw new InternalCompilerException(
            "node replacement should imply didChange()");
      }

      didChange |= dup.didChange();
      return toReturn;
    }

    private boolean isDuplicateCall(JsExpression x) {
      if (!(x instanceof JsInvocation)) {
        return false;
      }

      JsFunction func = isExecuteOnce((JsInvocation) x);
      return (func != null && called.contains(func));
    }
  }

  /**
   * Determines that a list of names is guaranteed to be evaluated in a
   * particular order. Also ensures that all names are evaluated before any
   * invocations occur.
   */
  private static class EvaluationOrderVisitor extends JsVisitor {
    /**
     * A dummy name to represent 'this' refs.
     */
    public static final JsName THIS_NAME = new JsCatchScope(
        JsRootScope.INSTANCE, "this").getAllNames().iterator().next();

    private boolean maintainsOrder = true;
    private final List<JsName> toEvaluate;
    private final List<JsName> unevaluated;
    private final Set<JsName> paramsOrLocals = new HashSet<JsName>();

    public EvaluationOrderVisitor(List<JsName> toEvaluate, JsFunction callee) {
      this.toEvaluate = toEvaluate;
      this.unevaluated = new ArrayList<JsName>(toEvaluate);
      // collect params and locals from callee function
      new JsVisitor() {
        @Override
        public void endVisit(JsParameter x, JsContext ctx) {
          paramsOrLocals.add(x.getName());
        }

        @Override
        public boolean visit(JsVar x, JsContext ctx) {
          // record this before visiting initializer
          paramsOrLocals.add(x.getName());
          return true;
        }
      }.accept(callee);
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      JsBinaryOperator op = x.getOperator();

      /*
       * We don't care about the left-hand expression, because it is guaranteed
       * to be evaluated.
       */
      boolean rightStrict = refersToRequiredName(x.getArg2());
      boolean conditionalEvaluation = JsBinaryOperator.AND.equals(op)
          || JsBinaryOperator.OR.equals(op);

      if (rightStrict && conditionalEvaluation) {
        maintainsOrder = false;
      }
    }

    /**
     * If the condition would cause conditional evaluation of strict parameters,
     * don't allow inlining.
     */
    @Override
    public void endVisit(JsConditional x, JsContext ctx) {
      boolean thenStrict = refersToRequiredName(x.getThenExpression());
      boolean elseStrict = refersToRequiredName(x.getElseExpression());

      if (thenStrict || elseStrict) {
        maintainsOrder = false;
      }
    }

    /**
     * The statement declares a function closure. This makes actual evaluation
     * order of the parameters difficult or impossible to determine, so we'll
     * just ignore them.
     */
    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      maintainsOrder = false;
    }

    /**
     * The innermost invocation we see must consume all presently unevaluated
     * parameters to ensure that an exception does not prevent their evaluation.
     *
     * In the case of a nested invocation, such as
     * <code>F(r1, r2, G(r3, r4), f1);</code> the evaluation order is guaranteed
     * to be maintained, provided that no required parameters occur after the
     * nested invocation.
     */
    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      if (unevaluated.size() > 0) {
        maintainsOrder = false;
      }
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      checkName(x.getName());
    }

    @Override
    public void endVisit(JsNew x, JsContext ctx) {
      /*
       * Unless all arguments have already been evaluated, assume that invoking
       * the new expression might interfere with the evaluation of the argument.
       *
       * It would be possible to allow this if the invoked function either does
       * nothing or does nothing that affects the remaining arguments. However,
       * currently there is no analysis of the invoked function.
       */
      if (unevaluated.size() > 0) {
        maintainsOrder = false;
      }
    }

    @Override
    public void endVisit(JsThisRef x, JsContext ctx) {
      checkName(THIS_NAME);
    }

    public boolean maintainsOrder() {
      return maintainsOrder && unevaluated.size() == 0;
    }

    /**
     * Check to see if the evaluation of this JsName will break program order assumptions given
     * the parameters left to be substituted.
     *
     * The cases are as follows:
     * 1) JsName is a function parameter name which has side effects or is affected by side effects
     * (hereafter called 'volatile'), so it will be in 'toEvaluate'
     * 2) JsName is a function parameter which is not volatile (not in toEvaluate)
     * 3) JsName is a reference to a global variable
     * 4) JsName is a reference to a local variable
     *
     * A reference to a global while there are still parameters left to evaluate / substitute
     * implies an order violation.
     *
     * A reference to a volatile parameter is ok if it is the next parameter in sequence to
     * be evaluated (beginning of unevaluated list). Else, it is either being evaluated out of
     * order with respect to other parameters, or it is being evaluated more than once.
     */
    private void checkName(JsName name) {
      if (!toEvaluate.contains(name)) {
        // if the name is a non-local/non-parameter (e.g. global) and there are params left to eval
        if (!paramsOrLocals.contains(name) && unevaluated.size() > 0) {
          maintainsOrder = false;
        }
        // else this may be a local, or all volatile params have already been evaluated, so it's ok.
        return;
      }

      // either this param is being evaled twice, or out of order
      if (unevaluated.size() == 0 || !unevaluated.remove(0).equals(name)) {
        maintainsOrder = false;
      }
    }

    /**
     * Determine if an expression contains a reference to a strict parameter.
     */
    private boolean refersToRequiredName(JsExpression e) {
      RefersToNameVisitor v = new RefersToNameVisitor(toEvaluate);
      v.accept(e);
      return v.refersToName();
    }
  }

  /**
   * Collect names in a hoisted statement that are local to the original
   * scope.  These names will need to be copied to the destination scope
   * once the statement becomes hoisted.
   */
  private static class HoistedNameVisitor extends JsVisitor {
    private final JsScope toScope;
    private final JsScope fromScope;
    private final List<JsName> hoistedNames;

    public HoistedNameVisitor(JsScope toScope, JsScope fromScope) {
      this.toScope = toScope;
      this.fromScope = fromScope;
      this.hoistedNames = new ArrayList<JsName>();
    }

    public List<JsName> getHoistedNames() {
      return hoistedNames;
    }

    /*
     * We need to hoist names that are only visible in fromScope, but not in
     * toScope (i.e. we don't want to hoist names that are visible to both
     * scopes, such as a global). Also, we don't want to hoist names that have a
     * staticRef, which indicates a formal parameter, or a function name.
     */
    @Override
    public boolean visit(JsNameRef nameRef, JsContext ctx) {
      JsName name = nameRef.getName();
      JsName fromScopeName = fromScope.findExistingName(name.getIdent());
      JsName toScopeName = toScope.findExistingName(name.getIdent());
      if (name.getStaticRef() == null
          && name == fromScopeName
          && name != toScopeName
          && !hoistedNames.contains(name)) {
        hoistedNames.add(name);
      }
      return true;
    }
  }

  /**
   * Collect all of the idents used in an AST node. The collector can be
   * configured to collect idents from qualified xor unqualified JsNameRefs.
   */
  private static class IdentCollector extends JsVisitor {
    private final boolean collectQualified;
    private final Set<String> idents = new HashSet<String>();

    public IdentCollector(boolean collectQualified) {
      this.collectQualified = collectQualified;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      boolean hasQualifier = x.getQualifier() != null;

      if ((collectQualified && !hasQualifier)
          || (!collectQualified && hasQualifier)) {
        return;
      }

      assert x.getIdent() != null;
      idents.add(x.getIdent());
    }

    public Set<String> getIdents() {
      return idents;
    }
  }

  /**
   * This class looks for function invocations that can be inlined and performs
   * the replacement by replacing the JsInvocation with a comma expression
   * consisting of the expressions evaluated by the target function. A second
   * step may convert the expressions in the comma expression back to multiple
   * statements if the context of the invocation would allow this.
   */
  private static class InliningVisitor extends JsModVisitor {
    private final Set<JsFunction> blacklist = new HashSet<JsFunction>();
    private final Set<JsNode> whitelist;
    /**
     * This reflects the functions that are currently being inlined to prevent
     * infinite expansion.
     */
    private final Stack<JsFunction> inlining = new Stack<JsFunction>();
    /**
     * This reflects which function the visitor is currently visiting.
     */
    private final Stack<JsFunction> functionStack = new Stack<JsFunction>();
    private final InvocationCountingVisitor invocationCountingVisitor = new InvocationCountingVisitor();
    private final Stack<List<JsName>> newLocalVariableStack = new Stack<List<JsName>>();

    /**
     * A map containing the next integer to try as an identifier suffix for a
     * given JsScope.
     */
    private IdentityHashMap<JsScope, HashMap<String, Integer>> startIdentForScope = new IdentityHashMap<JsScope, HashMap<String, Integer>>();

    /**
     * Not a stack because program fragments aren't nested.
     */
    private JsFunction programFunction;

    public InliningVisitor(JsProgram program, Set<JsNode> whitelist) {
      invocationCountingVisitor.accept(program);
      this.whitelist = whitelist;
    }

    /**
     * Add to the list of JsFunctions that should not be inlined, regardless of
     * whether or not they would normally be inlinable.
     */
    public void blacklist(Collection<JsFunction> functions) {
      blacklist.addAll(functions);
    }

    /**
     * This normalizes the comma expressions into multiple statements and
     * removes statements with no side-effects.
     */
    @Override
    public void endVisit(JsExprStmt x, JsContext ctx) {
      JsExpression e = x.getExpression();

      // We will occasionally create JsExprStmts that have no side-effects.
      if (ctx.canRemove() && !x.getExpression().hasSideEffects()) {
        ctx.removeMe();
        return;
      }

      List<JsExprStmt> statements = new ArrayList<JsExprStmt>();

      /*
       * Assemble the expressions back into a list of JsExprStmts. We will
       * iteratively disassemble the nested comma expressions, stopping when the
       * LHS is not a comma expression.
       */
      while (e instanceof JsBinaryOperation) {
        JsBinaryOperation op = (JsBinaryOperation) e;

        if (!op.getOperator().equals(JsBinaryOperator.COMMA)) {
          break;
        }

        /*
         * We can ignore intermediate expressions as long as they have no
         * side-effects.
         */
        if (op.getArg2().hasSideEffects()) {
          statements.add(0, op.getArg2().makeStmt());
        }

        e = op.getArg1();
      }

      /*
       * We know the return value from the original invocation was ignored, so
       * it may be possible to ignore the final expressions as long as it has no
       * side-effects.
       */
      if (e.hasSideEffects()) {
        statements.add(0, e.makeStmt());
      }

      if (statements.size() == 0) {
        // The expression contained no side effects at all.
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(new JsEmpty(x.getSourceInfo()));
        }

      } else if (x.getExpression() != statements.get(0).getExpression()) {
        // Something has changed

        if (!ctx.canInsert()) {
          /*
           * This indicates that the function was attached to a clause of a
           * control function and not into an existing block. We'll replace the
           * single JsExprStmt with a JsBlock that contains all of the
           * statements.
           */
          JsBlock b = new JsBlock(x.getSourceInfo());
          b.getStatements().addAll(statements);
          ctx.replaceMe(b);
          return;

        } else {
          // Insert the new statements into the original context
          for (JsStatement s : statements) {
            ctx.insertBefore(s);
          }
          ctx.removeMe();
        }
      }
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }

      JsBlock body = x.getBody();
      List<JsName> newLocalVariables = newLocalVariableStack.pop();

      addVars(x, body, newLocalVariables);
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      if (functionStack.isEmpty()) {
        return;
      }
      JsFunction callerFunction = functionStack.peek();

      if (!whitelist.contains(callerFunction)) {
        // Only look at functions that are in the white list
        return;
      }

      /*
       * We only want to look at invocations of things that we statically know
       * to be functions. Otherwise, we can't know what statements the
       * invocation would actually invoke. The static reference would be null
       * when trying operate on references to external functions, or functions
       * as arguments to another function.
       */
      JsFunction invokedFunction = isFunction(x.getQualifier());
      if (invokedFunction == null) {
        return;
      }

      /*
       * Don't inline huge functions into huge multi-expressions. Some JS
       * engines will blow up.
       */
      if (invokedFunction.getBody().getStatements().size() > MAX_INLINE_FN_SIZE) {
        return;
      }

      // Don't inline blacklisted functions
      if (blacklist.contains(invokedFunction)) {
        return;
      }

      /*
       * The current function has been mutated so as to be self-recursive. Ban
       * it from any future inlining to prevent infinite expansion.
       */
      if (invokedFunction == callerFunction) {
        blacklist.add(invokedFunction);
        return;
      }

      /*
       * We are already in the middle of attempting to inline a call to this
       * function. This check prevents infinite expansion across
       * mutually-recursive, inlinable functions. Any invocation skipped by this
       * logic will be re-visited in the <code>op = accept(op)</code> call in
       * the outermost JsInvocation.
       */
      if (inlining.contains(invokedFunction)) {
        return;
      }

      inlining.push(invokedFunction);
      x = tryToUnravelExplicitCall(x);
      JsExpression op = process(x, callerFunction, invokedFunction);

      if (x != op) {
        /*
         * See if any further inlining can be performed in the current context.
         * By attempting to maximize the level of inlining now, we can reduce
         * the total number of passes required to finalize the AST.
         */
        op = accept(op);
        ctx.replaceMe(op);
      }

      if (inlining.pop() != invokedFunction) {
        throw new RuntimeException("Unexpected function popped");
      }
    }

    @Override
    public void endVisit(JsProgram x, JsContext ctx) {
      if (!functionStack.pop().equals(programFunction)) {
        throw new InternalCompilerException("Unexpected function popped");
      }

      assert programFunction.getBody().getStatements().size() == 0 : "Should not have moved statements into program";

      List<JsName> newLocalVariables = newLocalVariableStack.pop();
      assert newLocalVariables.size() == 0 : "Should not have tried to create variables in program";
    }

    @Override
    public boolean visit(JsExprStmt x, JsContext ctx) {
      if (functionStack.peek() == programFunction) {
        /* Don't inline top-level invocations. */
        if (x.getExpression() instanceof JsInvocation) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
      functionStack.push(x);
      newLocalVariableStack.push(new ArrayList<JsName>());
      return true;
    }

    /**
     * Create a synthetic context to attempt to simplify statements in the
     * top-level of the program.
     */
    @Override
    public boolean visit(JsProgram x, JsContext ctx) {
      programFunction = new JsFunction(x.getSourceInfo(), x.getScope());
      programFunction.setBody(new JsBlock(x.getSourceInfo()));
      functionStack.push(programFunction);
      newLocalVariableStack.push(new ArrayList<JsName>());
      return true;
    }

    private void addVars(HasSourceInfo x, JsBlock body,
        List<JsName> newLocalVariables) {
      // Nothing to do
      if (newLocalVariables.isEmpty()) {
        return;
      }

      List<JsStatement> statements = body.getStatements();

      // The body can't be empty if we have local variables to create
      assert !statements.isEmpty();

      // Find or create the JsVars as the first statement
      SourceInfo sourceInfo = x.getSourceInfo();
      JsVars vars;
      if (statements.get(0) instanceof JsVars) {
        vars = (JsVars) statements.get(0);
      } else {
        vars = new JsVars(sourceInfo);
        statements.add(0, vars);
      }

      // Add all variables
      for (JsName name : newLocalVariables) {
        vars.add(new JsVar(sourceInfo, name));
      }
    }

    private boolean isInvokedMoreThanOnce(JsFunction f) {
      Integer count = invocationCountingVisitor.invocationCount(f);
      return count == null || count > 1;
    }

    /**
     * Determine if <code>invokedFunction</code> can be inlined into
     * <code>callerFunction</code> at callsite <code>x</code>.
     *
     * @return An expression equivalent to <code>x</code>
     */
    private JsExpression process(JsInvocation x, JsFunction callerFunction,
        JsFunction invokedFunction) {
      List<JsStatement> statements;
      if (invokedFunction.getBody() != null) {
        statements = new ArrayList<JsStatement>(
            invokedFunction.getBody().getStatements());
      } else {
        /*
         * Will see this with certain classes whose clinits are folded into the
         * main JsProgram body.
         */
        statements = Collections.emptyList();
      }

      List<JsExpression> hoisted = new ArrayList<JsExpression>(statements.size());
      JsExpression thisExpr = ((JsNameRef) x.getQualifier()).getQualifier();
      HoistedNameVisitor hoistedNameVisitor =
          new HoistedNameVisitor(callerFunction.getScope(), invokedFunction.getScope());

      boolean sawReturnStatement = false;

      for (JsStatement statement : statements) {
        if (sawReturnStatement) {
          /*
           * We've already seen a return statement, but there are still more
           * statements. The target is unsafe to inline, so bail. Note: in most
           * cases JsStaticEval will have removed any statements following a
           * return statement.
           *
           * The reason we have to bail is that the return statement's
           * expression MUST be the last thing evaluated.
           *
           * TODO(bobv): maybe it could still be inlined with smart
           * transformation?
           */
          return x;
        }

        /*
         * Create replacement expressions to use in place of the original
         * statements. It is important that the replacement is newly-minted and
         * therefore not referenced by any other AST nodes. Consider the case of
         * a common, delegating function. If the hoisted expressions were not
         * distinct objects, it would not be possible to substitute different
         * JsNameRefs at different call sites.
         */
        JsExpression h = hoistedExpression(statement);
        if (h == null) {
          return x;
        }

        /*
         * Visit the statement to find names that will be moved to the caller's
         * scope from the invoked function.
         */
        hoistedNameVisitor.accept(statement);

        if (isReturnStatement(statement)) {
          sawReturnStatement = true;
          hoisted.add(h);
        } else if (hasSideEffects(Collections.singletonList(h))) {
          hoisted.add(h);
        }
      }

      /*
       * Get the referenced names that need to be copied to the caller's scope.
       */
      List<JsName> hoistedNames = hoistedNameVisitor.getHoistedNames();

      /*
       * If the inlined method has no return statement, synthesize an undefined
       * reference. It will be reclaimed if the method call is from a
       * JsExprStmt.
       */
      if (!sawReturnStatement) {
        hoisted.add(new JsNameRef(x.getSourceInfo(),
            JsRootScope.INSTANCE.getUndefined()));
      }

      assert (hoisted.size() > 0);

      /*
       * Build up the new comma expression from right-to-left; building the
       * rightmost comma expressions first. Bootstrapping with i.previous()
       * ensures that this logic will function correctly in the case of a single
       * expression.
       */
      SourceInfo sourceInfo = x.getSourceInfo();
      ListIterator<JsExpression> i = hoisted.listIterator(hoisted.size());
      JsExpression op = i.previous();
      while (i.hasPrevious()) {
        JsBinaryOperation outerOp = new JsBinaryOperation(sourceInfo,
            JsBinaryOperator.COMMA);
        outerOp.setArg1(i.previous());
        outerOp.setArg2(op);
        op = outerOp;
      }

      // Confirm that the expression conforms to the desired heuristics
      if (!isInlinable(callerFunction, invokedFunction, thisExpr,
          x.getArguments(), op)) {
        return x;
      }

      // Perform the name replacement
      NameRefReplacerVisitor v = new NameRefReplacerVisitor(thisExpr,
          x.getArguments(), invokedFunction.getParameters());
      for (ListIterator<JsName> nameIterator = hoistedNames.listIterator(); nameIterator.hasNext();) {
        JsName name = nameIterator.next();

        /*
         * Find an unused identifier in the caller's scope. It's possible that
         * the same function has been inlined in multiple places within the
         * function so we'll use a counter for disambiguation.
         */
        String ident;
        String base = invokedFunction.getName() + "_" + name.getIdent();
        JsScope scope = callerFunction.getScope();
        HashMap<String, Integer> startIdent = startIdentForScope.get(scope);
        if (startIdent == null) {
          startIdent = new HashMap<String, Integer>();
          startIdentForScope.put(scope, startIdent);
        }

        Integer s = startIdent.get(base);
        int suffix = (s == null) ? 0 : s.intValue();
        do {
          ident = base + "_" + suffix++;
        } while (scope.findExistingName(ident) != null);
        startIdent.put(base, suffix);

        JsName newName = scope.declareName(ident, name.getShortIdent());
        v.setReplacementName(name, newName);
        nameIterator.set(newName);
      }
      op = v.accept(op);

      // Normalize any nested comma expressions that we may have generated.
      op = (new CommaNormalizer(hoistedNames)).accept(op);

      /*
       * Compare the relative complexity of the original invocation versus the
       * inlined form.
       */
      int originalComplexity = complexity(x);
      int inlinedComplexity = complexity(op);
      double ratio = ((double) inlinedComplexity) / originalComplexity;
      if (ratio > MAX_COMPLEXITY_INCREASE
          && isInvokedMoreThanOnce(invokedFunction)) {
        return x;
      }

      if (callerFunction == programFunction && hoistedNames.size() > 0) {
        // Don't add additional variables to the top-level program.
        return x;
      }

      // We've committed to the inlining, ensure the vars are created
      newLocalVariableStack.peek().addAll(hoistedNames);

      // update invocation counts according to this inlining
      invocationCountingVisitor.removeCountsFor(x);
      invocationCountingVisitor.accept(op);
      return op;
    }
  }

  /**
   * Counts the number of times a function is invoked. Functions that only have
   * a single call site in the whole program are inlined, regardless of
   * complexity.
   */
  private static class InvocationCountingVisitor extends JsVisitor {
    private boolean removingCounts = false;
    private final Map<JsFunction, Integer> invocationCount = new IdentityHashMap<JsFunction, Integer>();

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      checkFunctionCall(x.getQualifier());
    }

    @Override
    public void endVisit(JsNew x, JsContext ctx) {
      checkFunctionCall(x.getConstructorExpression());
    }

    public Integer invocationCount(JsFunction f) {
      return invocationCount.get(f);
    }

    /**
     * Like accept(), but remove counts for all invocations in expr.
     */
    public void removeCountsFor(JsExpression expr) {
      assert (!removingCounts);
      removingCounts = true;
      accept(expr);
      removingCounts = false;
    }

    private void checkFunctionCall(JsExpression qualifier) {
      JsFunction function = isFunction(qualifier);
      if (function != null) {
        Integer count = invocationCount.get(function);
        if (count == null) {
          assert (!removingCounts);
          count = 1;
        } else {
          if (removingCounts) {
            count -= 1;
          } else {
            count += 1;
          }
        }
        invocationCount.put(function, count);
      }
    }
  }

  /**
   * Finds functions that are only invoked at a single invocation site.
   */
  private static class SingleInvocationVisitor extends JsVisitor {
    // Keep track of functions that are invoked once.
    // Invariant: singleInvokations(fn) = null => calls to fn have not been seen
    //            singleInvokations(fn) = MULTIPLE =>  mutiple callsites to fn have been seen.
    //            singleInvokations(fn) = caller =>  one callsite has been seen an occurs in caller.
    private final Map<JsFunction, JsFunction> singleInvocations =
        new IdentityHashMap<JsFunction, JsFunction>();

    // Indicates multiple invocations were found (only identity is used).
    private static final JsFunction MULTIPLE = JsFunction.createSentinel();

    private final Stack<JsFunction> functionStack = new Stack<JsFunction>();

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }
    }

    public Collection<JsNode> inliningCandidates() {
      Collection<JsNode> set = new LinkedHashSet<JsNode>();
      for (Map.Entry<JsFunction, JsFunction> entry : singleInvocations.entrySet()) {
        if (entry.getValue() != MULTIPLE) {
          set.add(entry.getValue());
        }
      }
      return set;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
      functionStack.push(x);
      return true;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      checkFunctionCall(x.getQualifier());
    }

    @Override
    public void endVisit(JsNew x, JsContext ctx) {
      checkFunctionCall(x.getConstructorExpression());
    }

    private void checkFunctionCall(JsExpression qualifier) {
      JsFunction function = isFunction(qualifier);
      if (function != null && !functionStack.isEmpty()) {
        // Keep track if function is only invoked at a single callsite.
        JsFunction recordedInvoker = singleInvocations.get(function);
        // Mark self recursive functions as if they had multiple call sites.
        if (recordedInvoker == null && functionStack.peek() != function) {
          // This is the first invocation, register it.
          singleInvocations.put(function, functionStack.peek());
        } else if (recordedInvoker != MULTIPLE) {
          singleInvocations.put(function, MULTIPLE);
        }
      }
    }
  }

  /**
   * Replace references to JsNames with the inlined JsExpression.
   */
  private static class NameRefReplacerVisitor extends JsModVisitor {
    /**
     * Set up a map to record name replacements to perform.
     */
    final Map<JsName, JsName> nameReplacements = new IdentityHashMap<JsName, JsName>();

    /**
     * Set up a map of parameter names back to the expressions that will be
     * passed in from the outer call site.
     */
    final Map<JsName, JsExpression> paramsToArgsMap = new IdentityHashMap<JsName, JsExpression>();

    /**
     * A replacement expression for this references.
     */
    private JsExpression thisExpr;

    public NameRefReplacerVisitor(JsExpression thisExpr,
        List<JsExpression> arguments, List<JsParameter> parameters) {
      this.thisExpr = thisExpr;
      if (parameters.size() != arguments.size()) {
        // This shouldn't happen if the cloned JsInvocation has been properly
        // configured
        throw new InternalCompilerException(
            "Mismatch on parameters and arguments");
      }

      for (int i = 0; i < parameters.size(); i++) {
        JsParameter p = parameters.get(i);
        JsExpression e = arguments.get(i);
        paramsToArgsMap.put(p.getName(), e);
      }
    }

    /**
     * Replace JsNameRefs that refer to parameters with the expression passed
     * into the function invocation.
     */
    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x.getQualifier() != null) {
        return;
      }

      JsExpression replacement = tryGetReplacementExpression(x.getSourceInfo(),
          x.getName());

      if (replacement != null) {
        ctx.replaceMe(replacement);
      }
    }

    @Override
    public void endVisit(JsThisRef x, JsContext ctx) {
      assert thisExpr != null;
      ctx.replaceMe(thisExpr);
    }

    /**
     * Set a replacement JsName for all references to a JsName.
     *
     * @param name the name to replace
     * @param newName the new name that should be used in place of references to
     *          <code>name</code>
     * @return the previous JsName the name would have been replaced with or
     *         <code>null</code> if one was not previously set
     */
    public JsName setReplacementName(JsName name, JsName newName) {
      return nameReplacements.put(name, newName);
    }

    /**
     * Determine the replacement expression to use in place of a reference to a
     * given name. Returns <code>null</code> if no replacement has been set for
     * the name.
     */
    private JsExpression tryGetReplacementExpression(SourceInfo sourceInfo,
        JsName name) {
      if (paramsToArgsMap.containsKey(name)) {
        /*
         * TODO if we ever allow mutable JsExpression types to be considered
         * always flexible, then it would be necessary to clone the expression.
         */
        return paramsToArgsMap.get(name);

      } else if (nameReplacements.containsKey(name)) {
        return nameReplacements.get(name).makeRef(sourceInfo);

      } else {
        return null;
      }
    }
  }

  /**
   * Detects function declarations.
   */
  private static class NestedFunctionVisitor extends JsVisitor {

    private boolean containsNestedFunctions = false;

    public boolean containsNestedFunctions() {
      return containsNestedFunctions;
    }

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      containsNestedFunctions = true;
    }
  }

  /**
   * Detects uses of parameters that would produce incorrect results if inlined.
   * Generally speaking, we disallow the use of parameters as lvalues. Also
   * detects trying to inline a method which references 'this' where the call
   * site has no qualifier.
   */
  private static class ParameterUsageVisitor extends JsVisitor {
    private final boolean hasThisExpr;
    private final Set<JsName> parameterNames;
    private boolean violation = false;

    public ParameterUsageVisitor(boolean hasThisExpr, Set<JsName> parameterNames) {
      this.hasThisExpr = hasThisExpr;
      this.parameterNames = parameterNames;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (ctx.isLvalue() && isParameter(x)) {
        violation = true;
      }
    }

    @Override
    public void endVisit(JsThisRef x, JsContext ctx) {
      if (!hasThisExpr) {
        violation = true;
      }
    }

    public boolean hasViolation() {
      return violation;
    }

    /**
     * Determine if a JsExpression is a JsNameRef that refers to a parameter.
     */
    private boolean isParameter(JsNameRef ref) {
      if (ref.getQualifier() != null) {
        return false;
      }

      JsName name = ref.getName();
      return parameterNames.contains(name);
    }
  }

  /**
   * Collect self-recursive functions. This visitor does not look for
   * mutually-recursive functions because inlining one of the functions into the
   * other would make the single resultant function self-recursive and not
   * eligible for inlining in a subsequent pass.
   */
  private static class RecursionCollector extends JsVisitor {
    private final Stack<JsFunction> functionStack = new Stack<JsFunction>();
    private final Set<JsFunction> recursive = new HashSet<JsFunction>();

    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }
    }

    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      /*
       * Because functions can encapsulate other functions, we look at the
       * entire stack and not just the top element. This would prevent inlining
       *
       * function a() { function b() { a(); } b(); }
       *
       * in the case that we generally allow nested functions to be inlinable.
       */
      JsFunction f = isFunction(x.getQualifier());
      if (functionStack.contains(f)) {
        recursive.add(f);
      }
    }

    public Set<JsFunction> getRecursive() {
      return recursive;
    }

    @Override
    public boolean visit(JsFunction x, JsContext ctx) {
      functionStack.push(x);
      return true;
    }
  }

  /**
   * Determine which functions should not be inlined because they are redefined
   * during program execution. This would violate the assumption that the
   * statements to be executed by any given function invocation are stable over
   * the lifetime of the program.
   */
  private static class RedefinedFunctionCollector extends JsVisitor {
    private final Map<JsName, JsFunction> nameMap = new IdentityHashMap<JsName, JsFunction>();
    private final Set<JsFunction> redefined = new HashSet<JsFunction>();

    /**
     * Look for assignments to JsNames whose static references are JsFunctions.
     */
    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {

      if (!x.getOperator().equals(JsBinaryOperator.ASG)) {
        return;
      }

      JsFunction f = isFunction(x.getArg1());
      if (f != null) {
        redefined.add(f);
      }
    }

    /**
     * Look for the case where a function is declared with the same name as an
     * existing function.
     */
    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      JsName name = x.getName();

      if (name == null) {
        // Ignore anonymous functions
        return;

      } else if (nameMap.containsKey(name)) {
        /*
         * We have to add the current function as well as the original
         * JsFunction that was declared to use that name.
         */
        redefined.add(nameMap.get(name));
        redefined.add(x);
      } else {
        nameMap.put(name, x);
      }
    }

    public Collection<JsFunction> getRedefined() {
      return redefined;
    }
  }

  /**
   * Given a collection of JsNames, determine if an AST node refers to any of
   * those names.
   */
  private static class RefersToNameVisitor extends JsVisitor {
    private final Collection<JsName> names;
    private boolean refersToName;

    public RefersToNameVisitor(Collection<JsName> names) {
      this.names = names;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      JsName name = x.getName();

      if (name != null) {
        refersToName = refersToName || names.contains(name);
      }
    }

    public boolean refersToName() {
      return refersToName;
    }
  }

  /**
   * This ensures that changing the scope of an expression from its enclosing
   * function into the scope of the call site will not cause unqualified
   * identifiers to resolve to different values.
   */
  private static class StableNameChecker extends JsVisitor {
    private final JsScope calleeScope;
    private final JsScope callerScope;
    private final Collection<JsName> parameterNames;
    private boolean stable = true;

    public StableNameChecker(JsScope callerScope, JsScope calleeScope,
        Collection<JsName> parameterNames) {
      this.callerScope = callerScope;
      this.calleeScope = calleeScope;
      this.parameterNames = parameterNames;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      /*
       * We can ignore qualified reference, since their scope is always that of
       * the qualifier.
       */
      if (x.getQualifier() != null) {
        return;
      }

      /*
       * Attempt to resolve the ident in both scopes
       */
      JsName callerName = callerScope.findExistingName(x.getIdent());
      JsName calleeName = calleeScope.findExistingName(x.getIdent());

      if (callerName == null && calleeName == null) {
        // They both reference out-of-module names

      } else if (parameterNames.contains(calleeName)) {
        // A reference to a parameter, which will be replaced by an argument

      } else if (callerName != null && callerName.equals(calleeName)) {
        // The names are known to us and are the same

      } else if (calleeName.getEnclosing().equals(calleeScope)) {
        // It's a local variable in the callee

      } else {
        stable = false;
      }
    }

    public boolean isStable() {
      return stable;
    }
  }

  /**
   * The maximum number of statements a function can have to be actually considered for inlining.
   *
   * Setting gwt.jsinlinerMaxFnSize = 50 and gwt.jsinlinerRatio = 1.7 (as was originally)
   * increases compile time by 5% and decreases code size by 0.4%.
   */
  public static final int MAX_INLINE_FN_SIZE = Integer.parseInt(System.getProperty(
      "gwt.jsinlinerMaxFnSize", "23"));

  /**
   * When attempting to inline an invocation, this constant determines the
   * maximum allowable ratio of potential inlined complexity to initial
   * complexity. This acts as a brake on very large expansions from bloating the
   * generated output. Increasing this number will allow larger sections of
   * code to be inlined, but at a cost of larger JS output.
   */
  private static final double MAX_COMPLEXITY_INCREASE = Double.parseDouble(System.getProperty(
      "gwt.jsinlinerRatio", "1.2"));

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static OptimizerStats exec(JsProgram program, Collection<JsNode> toInline) {
    Event optimizeJsEvent = SpeedTracerLogger.start(
        CompilerEventType.OPTIMIZE_JS, "optimizer", NAME);
    OptimizerStats stats = execImpl(program, toInline);
    optimizeJsEvent.end("didChange", "" + stats.didChange());
    return stats;
  }

  /**
   * Determine whether or not a list of AST nodes are affected by side effects.
   * The context parameter provides a scope in which local (and therefore
   * immutable) variables are defined.
   */
  private static boolean affectedBySideEffects(List<JsExpression> list,
      JsFunction context) {
    /*
     * If the caller contains no nested functions, none of its locals can
     * possibly be affected by side effects.
     */
    JsScope safeScope = null;
    if (context != null && !containsNestedFunctions(context)) {
      safeScope = context.getScope();
    }
    AffectedBySideEffectsVisitor v = new AffectedBySideEffectsVisitor(safeScope);
    v.acceptList(list);
    return v.affectedBySideEffects();
  }

  /**
   * Generate an estimated measure of the syntactic complexity of a JsNode.
   */
  private static int complexity(JsNode toEstimate) {
    ComplexityEstimator e = new ComplexityEstimator();
    e.accept(toEstimate);
    return e.getComplexity();
  }

  /**
   * Examine a JsFunction to determine if it contains nested functions.
   */
  private static boolean containsNestedFunctions(JsFunction func) {
    NestedFunctionVisitor v = new NestedFunctionVisitor();
    v.accept(func.getBody());
    return v.containsNestedFunctions();
  }


  private static OptimizerStats execImpl(JsProgram program, Collection<JsNode> toInline) {
    OptimizerStats stats = new OptimizerStats(NAME);

    // We are not covering the whole AST, hence we will try to inline functions with a single call
    // site as well as those produced by native methods and their callers.
    SingleInvocationVisitor s = new SingleInvocationVisitor();
    s.accept(program);
    Set<JsNode> candidates = new LinkedHashSet<JsNode>(toInline);
    candidates.addAll(s.inliningCandidates());

    RedefinedFunctionCollector d = new RedefinedFunctionCollector();
    d.accept(program);

    RecursionCollector rc = new RecursionCollector();
    for (JsNode fn : candidates) {
      rc.accept(fn);
    }

    InliningVisitor v = new InliningVisitor(program, candidates);
    v.blacklist(d.getRedefined());
    v.blacklist(rc.getRecursive());
    // Do not accept among candidates as the list might get stale and contain nodes that are not
    // reachable from the AST. Instead filter within InliningVisitor.
    v.accept(program);

    if (v.didChange()) {
      stats.recordModified();
    }

    DuplicateXORemover r = new DuplicateXORemover(program);
    r.accept(program);
    if (r.didChange()) {
      stats.recordModified();
    }
    return stats;
  }

  /**
   * Check to see if the to-be-inlined statement shares any idents with the
   * call-side arguments. Two passes are made: the first one looks for qualified
   * names; the second pass looks for unqualified names, but ignores identifiers
   * that refer to function parameters.
   */
  private static boolean hasCommonIdents(List<JsExpression> arguments,
      JsNode toInline, Collection<String> parameterIdents) {

    // This is a fire-twice loop
    boolean checkQualified = false;
    do {
      checkQualified = !checkQualified;

      // Collect the idents used in the arguments and the statement
      IdentCollector argCollector = new IdentCollector(checkQualified);
      argCollector.acceptList(arguments);
      IdentCollector statementCollector = new IdentCollector(checkQualified);
      statementCollector.accept(toInline);

      Set<String> idents = argCollector.getIdents();

      // Unqualified idents may be references to parameters, thus ignored
      if (!checkQualified) {
        idents.removeAll(parameterIdents);
      }

      // Perform the set difference
      idents.retainAll(statementCollector.getIdents());

      if (idents.size() > 0) {
        return true;
      }
    } while (checkQualified);

    return false;
  }

  /**
   * Determine whether or not a list of AST nodes have side effects.
   */
  private static boolean hasSideEffects(List<JsExpression> list) {
    for (JsExpression expr : list) {
      if (expr.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given a delegated JsStatement, construct an expression to hoist into the
   * outer caller. This does not perform any name replacement, but simply
   * constructs a mutable copy of the expression that can be manipulated
   * at-will.
   *
   * @param statement the statement from which to extract the expressions
   * @return a JsExpression representing all expressions that would have been
   *         evaluated by the statement
   */
  private static JsExpression hoistedExpression(JsStatement statement) {

    JsExpression expression;
    if (statement instanceof JsExprStmt) {
      // Extract the expression
      JsExprStmt exprStmt = (JsExprStmt) statement;
      expression = exprStmt.getExpression();

    } else if (statement instanceof JsReturn) {
      // Extract the return value
      JsReturn ret = (JsReturn) statement;
      expression = ret.getExpr();
      if (expression == null) {
        expression = new JsNameRef(ret.getSourceInfo(),
            JsRootScope.INSTANCE.getUndefined());
      }

    } else if (statement instanceof JsVars) {
      // Create a comma expression for variable initializers
      JsVars vars = (JsVars) statement;

      // Rely on comma expression cleanup to remove this later.
      expression = JsNullLiteral.INSTANCE;

      for (JsVar var : vars) {
        // Extract the initialization expression
        JsExpression init = var.getInitExpr();
        if (init != null) {
          SourceInfo sourceInfo = var.getSourceInfo();
          JsBinaryOperation assignment = new JsBinaryOperation(sourceInfo,
              JsBinaryOperator.ASG);
          assignment.setArg1(var.getName().makeRef(sourceInfo));
          assignment.setArg2(init);

          // Multiple initializers go into a comma expression
          JsBinaryOperation comma = new JsBinaryOperation(sourceInfo,
              JsBinaryOperator.COMMA);
          comma.setArg1(expression);
          comma.setArg2(assignment);
          expression = comma;
        }
      }
    } else {
      return null;
    }

    assert expression != null;
    return JsHoister.hoist(expression);
  }

  /**
   * Given a JsInvocation, determine if it is invoking a JsFunction that is
   * specified to be executed only once during the program's lifetime.
   */
  private static JsFunction isExecuteOnce(JsInvocation invocation) {
    JsFunction f = isFunction(invocation.getQualifier());
    if (f != null && f.getExecuteOnce()) {
      return f;
    }
    return null;
  }

  /**
   * Given an expression, determine if it is a JsNameRef that refers to a
   * statically-defined JsFunction.
   */
  private static JsFunction isFunction(JsExpression e) {
    if (e instanceof JsNameRef) {
      JsNameRef ref = (JsNameRef) e;

      // Unravel foo.call(...).
      if (!ref.getName().isObfuscatable() && "call".equals(ref.getIdent())) {
        if (ref.getQualifier() instanceof JsNameRef) {
          ref = (JsNameRef) ref.getQualifier();
        }
      }

      JsNode staticRef = ref.getName().getStaticRef();
      if (staticRef instanceof JsFunction) {
        return (JsFunction) staticRef;
      }
    }

    return null;
  }

  /**
   * Determine if a statement can be inlined into a call site.
   */
  private static boolean isInlinable(JsFunction caller, JsFunction callee,
      JsExpression thisExpr, List<JsExpression> arguments, JsNode toInline) {

    /*
     * This will happen with varargs-style JavaScript functions that rely on the
     * "arguments" array. The reference to arguments would be detected in
     * BoundedScopeVisitor, but the code below assumes the same number of
     * parameters and arguments.
     */
    if (arguments.size() != callee.getParameters().size()) {
      return false;
    }

    // Build up a list of all parameter names
    Set<JsName> parameterNames = new HashSet<JsName>();
    Set<String> parameterIdents = new HashSet<String>();
    for (JsParameter param : callee.getParameters()) {
      parameterNames.add(param.getName());
      parameterIdents.add(param.getName().getIdent());
    }

    /*
     * Make sure that inlining won't change the final name of non-parameter
     * idents due to the change of scope. The most likely cause would be the use
     * of an unqualified variable reference in a JSNI block that happened to
     * conflict with a Java-derived identifier.
     */
    StableNameChecker detector = new StableNameChecker(caller.getScope(),
        callee.getScope(), parameterNames);
    detector.accept(toInline);
    if (!detector.isStable()) {
      return false;
    }

    /*
     * Ensure that the names referred to by the argument list and the statement
     * are disjoint. This prevents inlining of the following:
     *
     * static int i; public void add(int a) { i += a; }; add(i++);
     */
    if (hasCommonIdents(arguments, toInline, parameterIdents)) {
      return false;
    }

    List<JsExpression> evalArgs;
    if (thisExpr == null) {
      evalArgs = arguments;
    } else {
      evalArgs = new ArrayList<JsExpression>(1 + arguments.size());
      evalArgs.add(thisExpr);
      evalArgs.addAll(arguments);
    }

    /*
     * Determine if the evaluation of the invocation's arguments may create side
     * effects. This will determine how aggressively the parameters may be
     * reordered.
     */
    if (isVolatile(evalArgs, caller)) {
      /*
       * Determine the order in which the parameters must be evaluated. This
       * will vary between call sites, based on whether or not the invocation's
       * arguments can be repeated without ill effect.
       */
      List<JsName> requiredOrder = new ArrayList<JsName>();
      if (thisExpr != null && isVolatile(thisExpr, callee)) {
        requiredOrder.add(EvaluationOrderVisitor.THIS_NAME);
      }
      for (int i = 0; i < arguments.size(); i++) {
        JsExpression e = arguments.get(i);
        JsParameter p = callee.getParameters().get(i);

        if (isVolatile(e, callee)) {
          requiredOrder.add(p.getName());
        }
      }

      // This would indicate that isVolatile changed its output between
      // the if statement and the loop.
      assert requiredOrder.size() > 0;

      /*
       * Verify that the non-reorderable arguments are evaluated in the right
       * order.
       */
      EvaluationOrderVisitor orderVisitor = new EvaluationOrderVisitor(
          requiredOrder, callee);
      orderVisitor.accept(toInline);
      if (!orderVisitor.maintainsOrder()) {
        return false;
      }
    }

    // Check that parameters aren't used in such a way as to prohibit inlining
    ParameterUsageVisitor v = new ParameterUsageVisitor(thisExpr != null,
        parameterNames);
    v.accept(toInline);
    if (v.hasViolation()) {
      return false;
    }

    // Hooray!
    return true;
  }

  /**
   * This is used in combination with {@link #hoistedExpression(JsStatement)} to
   * indicate if a given statement would terminate the list of hoisted
   * expressions.
   */
  private static boolean isReturnStatement(JsStatement statement) {
    return statement instanceof JsReturn;
  }

  /**
   * Indicates if an expression would create side effects or possibly be
   * affected by side effects when evaluated within a particular function
   * context.
   */
  private static boolean isVolatile(JsExpression e, JsFunction context) {
    return isVolatile(Collections.singletonList(e), context);
  }

  /**
   * Indicates if a list of expressions would create side effects or possibly be
   * affected by side effects when evaluated within a particular function
   * context.
   */
  private static boolean isVolatile(List<JsExpression> list, JsFunction context) {
    return hasSideEffects(list) || affectedBySideEffects(list, context);
  }

  /**
   * Transforms any <code>foo.call(this)</code> into <code>this.foo()</code> to
   * be compatible with our inlining algorithm.
   */
  private static JsInvocation tryToUnravelExplicitCall(JsInvocation x) {
    if (!(x.getQualifier() instanceof JsNameRef)) {
      return x;
    }
    JsNameRef ref = (JsNameRef) x.getQualifier();
    if (ref.getName().isObfuscatable() || !"call".equals(ref.getIdent())) {
      return x;
    }
    List<JsExpression> oldArgs = x.getArguments();
    if (oldArgs.size() < 1) {
      return x;
    }

    JsNameRef oldTarget = (JsNameRef) ref.getQualifier();
    JsNameRef newTarget = new JsNameRef(oldTarget.getSourceInfo(),
        oldTarget.getName());
    newTarget.setQualifier(oldArgs.get(0));
    JsInvocation newCall = new JsInvocation(x.getSourceInfo());
    newCall.setQualifier(newTarget);
    // Don't have to clone because the returned invocation is transient.
    newCall.getArguments().addAll(oldArgs.subList(1, oldArgs.size()));
    return newCall;
  }

  /**
   * Utility class.
   */
  private JsInliner() {
  }
}
