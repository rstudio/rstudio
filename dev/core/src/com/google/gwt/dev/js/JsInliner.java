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
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsDecimalLiteral;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsIntegralLiteral;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitchMember;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsUnaryOperation;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Perform inlining optimizations on the JavaScript AST.
 */
public class JsInliner {

  /**
   * Determines if the evaluation of a JsNode may be affected by side effects.
   */
  private static class AffectedBySideEffectsVisitor extends JsVisitor {
    private boolean affectedBySideEffects;
    private final JsProgram program;
    private final JsScope safeScope;

    public AffectedBySideEffectsVisitor(JsProgram program, JsScope safeScope) {
      this.program = program;
      this.safeScope = safeScope;
    }

    public boolean affectedBySideEffects() {
      return affectedBySideEffects;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      /*
       * We could make this more accurate by analyzing the function that's being
       * executed, but we'll bank on subsequent passes inlining simple function
       * invocations.
       */
      affectedBySideEffects = true;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      if (x.getQualifier() == null && x.getName() != null) {
        // Special case the undefined literal.
        if (x.getName() == program.getUndefinedLiteral().getName()) {
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

    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      if (isComma(x) == null) {
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
        if (inner != null && !hasSideEffects(inner.getArg2())) {
          x.setArg1(inner.getArg1());
          didChange = true;
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
      JsBinaryOperation newOp = new JsBinaryOperation(JsBinaryOperator.COMMA);
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
    public void endVisit(JsArrayAccess x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsArrayLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsBooleanLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsConditional x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsDecimalLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsIntegralLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNew x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsNullLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsObjectLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsRegExp x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsStringLiteral x, JsContext<JsExpression> ctx) {
      complexity++;
    }

    @Override
    public void endVisit(JsThisRef x, JsContext<JsExpression> ctx) {
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
     * Retains the the functions that we know have been called.
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
    public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
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
            ctx.replaceMe(program.getNullLiteral());
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
    public boolean visit(JsBlock x, JsContext<JsStatement> ctx) {
      branch(x.getStatements());
      return false;
    }

    @Override
    public boolean visit(JsCase x, JsContext<JsSwitchMember> ctx) {
      x.setCaseExpr(accept(x.getCaseExpr()));
      branch(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
      x.setTestExpression(accept(x.getTestExpression()));
      x.setThenExpression(branch(x.getThenExpression()));
      x.setElseExpression(branch(x.getElseExpression()));
      return false;
    }

    @Override
    public boolean visit(JsDefault x, JsContext<JsSwitchMember> ctx) {
      branch(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsExprStmt x, JsContext<JsStatement> ctx) {
      if (isDuplicateCall(x.getExpression())) {
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(program.getEmptyStmt());
        }
        return false;

      } else {
        return true;
      }
    }

    @Override
    public boolean visit(JsFor x, JsContext<JsStatement> ctx) {
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
    public boolean visit(JsForIn x, JsContext<JsStatement> ctx) {
      if (x.getIterExpr() != null) {
        x.setIterExpr(accept(x.getIterExpr()));
      }

      x.setObjExpr(accept(x.getObjExpr()));

      // The body is not guaranteed to be a JsBlock
      x.setBody(branch(x.getBody()));
      return false;
    }

    @Override
    public boolean visit(JsIf x, JsContext<JsStatement> ctx) {
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
    public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
      JsFunction func = isExecuteOnce(x);
      while (func != null) {
        called.add(func);
        func = func.getImpliedExecute();
      }
      return true;
    }

    @Override
    public boolean visit(JsWhile x, JsContext<JsStatement> ctx) {
      x.setCondition(accept(x.getCondition()));

      // The body is not guaranteed to be a JsBlock
      x.setBody(branch(x.getBody()));
      return false;
    }

    private <T extends JsNode<T>> void branch(List<T> x) {
      DuplicateXORemover dup = new DuplicateXORemover(program, called);
      dup.acceptWithInsertRemove(x);
      didChange |= dup.didChange();
    }

    private <T extends JsNode<T>> T branch(T x) {
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
    private boolean maintainsOrder = true;
    private final List<JsName> toEvaluate;
    private final List<JsName> unevaluated;

    public EvaluationOrderVisitor(List<JsName> toEvaluate) {
      this.toEvaluate = toEvaluate;
      this.unevaluated = new ArrayList<JsName>(toEvaluate);
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
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
    public void endVisit(JsConditional x, JsContext<JsExpression> ctx) {
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
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      maintainsOrder = false;
    }

    /**
     * The innermost invocation we see must consume all presently unevaluated
     * parameters to ensure that an exception does not prevent their evaluation.
     * 
     * In the case of a nested invocation, such as
     * <code>F(r1, r2, G(r3, r4), f1);</code> the evaluation order is
     * guaranteed to be maintained, provided that no required parameters occur
     * after the nested invocation.
     */
    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      /*
       * The check for isExecuteOnce() is potentially incorrect here, however
       * the original Java semantics of the clinit would have made the code
       * incorrect anyway.
       */
      if ((isExecuteOnce(x) == null) && unevaluated.size() > 0) {
        maintainsOrder = false;
      }
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      JsName name = x.getName();

      if (!toEvaluate.contains(name)) {
        return;
      }

      if (unevaluated.size() == 0 || !unevaluated.remove(0).equals(name)) {
        maintainsOrder = false;
      }
    }

    public boolean maintainsOrder() {
      return maintainsOrder && unevaluated.size() == 0;
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
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
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
    private final Stack<JsFunction> functionStack = new Stack<JsFunction>();
    private final JsProgram program;

    public InliningVisitor(JsProgram program) {
      this.program = program;
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
    public void endVisit(JsExprStmt x, JsContext<JsStatement> ctx) {
      JsExpression e = x.getExpression();

      // We will occasionally create JsExprStmts that have no side-effects.
      if (ctx.canRemove() && !hasSideEffects(x.getExpression())) {
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
        if (hasSideEffects(op.getArg2())) {
          statements.add(0, new JsExprStmt(op.getArg2()));
        }

        e = op.getArg1();
      }

      /*
       * We know the return value from the original invocation was ignored, so
       * it may be possible to ignore the final expressions as long as it has no
       * side-effects.
       */
      if (hasSideEffects(e)) {
        statements.add(0, new JsExprStmt(e));
      }

      if (statements.size() == 0) {
        // The expression contained no side effects at all.
        if (ctx.canRemove()) {
          ctx.removeMe();
        } else {
          ctx.replaceMe(program.getEmptyStmt());
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
          JsBlock b = new JsBlock();
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
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      /*
       * We only want to look at invocations of things that we statically know
       * to be functions. Otherwise, we can't know what statements the
       * invocation would actually invoke. The static reference would be null
       * when trying operate on references to external functions, or functions
       * as arguments to another function.
       */
      JsFunction f = isFunction(x.getQualifier());
      if (f == null) {
        return;
      }

      // Don't inline blacklisted functions
      if (blacklist.contains(f)) {
        return;
      }

      List<JsStatement> statements = f.getBody().getStatements();
      List<JsExpression> hoisted = new ArrayList<JsExpression>(
          statements.size());

      boolean sawReturnStatement = false;
      for (JsStatement statement : statements) {
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
          return;
        }

        hoisted.add(h);

        if (isReturnStatement(statement)) {
          sawReturnStatement = true;
          break;
        }
      }

      /*
       * If the inlined method has no return statement, synthesize an undefined
       * reference. It will be reclaimed if the method call is from a
       * JsExprStmt.
       */
      if (!sawReturnStatement) {
        hoisted.add(program.getUndefinedLiteral());
      }

      assert (hoisted.size() > 0);

      /*
       * Build up the new comma expression from right-to-left; building the
       * rightmost comma expressions first. Bootstrapping with i.previous()
       * ensures that this logic will function correctly in the case of a single
       * expression.
       */
      ListIterator<JsExpression> i = hoisted.listIterator(hoisted.size());
      JsExpression op = i.previous();
      while (i.hasPrevious()) {
        JsBinaryOperation outerOp = new JsBinaryOperation(
            JsBinaryOperator.COMMA);
        outerOp.setArg1(i.previous());
        outerOp.setArg2(op);
        op = outerOp;
      }

      // Confirm that the expression conforms to the desired heuristics
      if (!isInlinable(program, functionStack.peek(), x, f, op)) {
        return;
      }

      // Perform the name replacement
      NameRefReplacerVisitor v = new NameRefReplacerVisitor(x, f);
      op = v.accept(op);

      // Normalize any nested comma expressions that we may have generated.
      op = (new CommaNormalizer()).accept(op);

      /*
       * Compare the relative complexity of the original invocation versus the
       * inlined form.
       */
      int originalComplexity = complexity(x);
      int inlinedComplexity = complexity(op);
      if (((float) inlinedComplexity / originalComplexity) > MAX_COMPLEXITY_INCREASE) {
        return;
      }

      /*
       * See if any further inlining can be performed in the current context. By
       * attempting to maximize the level of inlining now, we can reduce the
       * total number of passes required to finalize the AST.
       */
      op = accept(op);

      ctx.replaceMe(op);
    }

    @Override
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
      functionStack.push(x);
      return true;
    }
  }

  /**
   * Replace references to JsNames with the inlined JsExpression.
   */
  private static class NameRefReplacerVisitor extends JsModVisitor {
    /**
     * Set up a map of parameter names back to the expressions that will be
     * passed in from the outer call site.
     */
    final Map<JsName, JsExpression> paramsToArgsMap = new HashMap<JsName, JsExpression>();

    /**
     * Constructor.
     * 
     * @param invocation The call site
     * @param function The function that encloses the inlined statement
     */
    public NameRefReplacerVisitor(JsInvocation invocation, JsFunction function) {
      List<JsParameter> parameters = function.getParameters();
      List<JsExpression> arguments = invocation.getArguments();

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
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      if (x.getQualifier() != null) {
        return;
      }

      /*
       * TODO if we ever allow mutable JsExpression types to be considered
       * always flexible, then it would be necessary to clone the expression.
       */
      JsExpression original = paramsToArgsMap.get(x.getName());

      if (original != null) {
        ctx.replaceMe(original);
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
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      containsNestedFunctions = true;
    }
  }

  /**
   * Detects uses of parameters that would produce incorrect results if inlined.
   * Generally speaking, we disallow the use of parameters as lvalues.
   */
  private static class ParameterUsageVisitor extends JsVisitor {
    private boolean lvalue = false;
    private final Set<JsName> parameterNames;

    public ParameterUsageVisitor(Set<JsName> parameterNames) {
      this.parameterNames = parameterNames;
    }

    /**
     * Disallow inlining if the left-hand side of an assignment is a parameter.
     */
    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      JsBinaryOperator op = x.getOperator();

      // Don't allow assignments to the left-hand side.
      if (op.isAssignment() && isParameter(x.getArg1())) {
        lvalue = true;
      }
    }

    /**
     * Delegates to {@link #checkUnaryOperation(JsUnaryOperation)}.
     */
    @Override
    public void endVisit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
      checkUnaryOperation(x);
    }

    /**
     * Delegates to {@link #checkUnaryOperation(JsUnaryOperation)}.
     */
    @Override
    public void endVisit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
      checkUnaryOperation(x);
    }

    public boolean parameterAsLValue() {
      return lvalue;
    }

    /**
     * Disallow modification of parameters via unary operations.
     */
    private void checkUnaryOperation(JsUnaryOperation x) {
      if (x.getOperator().isModifying() && isParameter(x.getArg())) {
        lvalue = true;
      }
    }

    /**
     * Determine if a JsExpression is a JsNameRef that refers to a parameter.
     */
    private boolean isParameter(JsExpression e) {
      if (!(e instanceof JsNameRef)) {
        return false;
      }

      JsNameRef ref = (JsNameRef) e;
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
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
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
    public boolean visit(JsFunction x, JsContext<JsExpression> ctx) {
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
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {

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
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
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
    private boolean refersToUnbound;

    public RefersToNameVisitor(Collection<JsName> names) {
      this.names = names;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      JsName name = x.getName();

      if (name == null) {
        refersToUnbound = true;
      } else {
        refersToName = refersToName || names.contains(name);
      }
    }

    public boolean refersToName() {
      return refersToName;
    }

    public boolean refersToUnbound() {
      return refersToUnbound;
    }
  }

  /**
   * Examine a node to determine if it might produce side effects.
   */
  private static class SideEffectsVisitor extends JsVisitor {
    private boolean hasSideEffects;

    @Override
    public void endVisit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      hasSideEffects |= (x.getOperator().isAssignment());
    }

    @Override
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      /*
       * Declaring a named function implicitly has an assignment side-effect.
       */
      hasSideEffects |= x.getName() != null;
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      /*
       * We don't actually need to drill-down into other functions to see if
       * they do or do not have side-effects. The simple, side-effect free
       * function invocations will naturally be inlined in subsequent
       * iterations.
       */
      hasSideEffects = true;
    }

    @Override
    public void endVisit(JsNew x, JsContext<JsExpression> ctx) {
      /*
       * The typical use of the new keyword in JavaScript generated by GWT is to
       * create a prototypical object, and then pass it into a Java-derived
       * constructor. Given that the majority of the uses of new would not
       * benefit from inlining, it's not worth the extra complexity of worrying
       * about yet another set of special cases.
       */
      hasSideEffects = true;
    }

    @Override
    public void endVisit(JsPostfixOperation x, JsContext<JsExpression> ctx) {
      hasSideEffects |= x.getOperator().isModifying();
    }

    @Override
    public void endVisit(JsPrefixOperation x, JsContext<JsExpression> ctx) {
      hasSideEffects |= x.getOperator().isModifying();
    }

    @Override
    public void endVisit(JsThrow x, JsContext<JsStatement> ctx) {
      hasSideEffects = true;
    }

    public boolean hasSideEffects() {
      return hasSideEffects;
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
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
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

      } else {
        stable = false;
      }
    }

    public boolean isStable() {
      return stable;
    }
  }

  /**
   * When attempting to inline an invocation, this constant determines the
   * maximum allowable ratio of potential inlined complexity to initial
   * complexity. This acts as a brake on very large expansions from bloating the
   * the generated output. Increasing this number will allow larger sections of
   * code to be inlined, but at a cost of larger JS output.
   */
  private static final int MAX_COMPLEXITY_INCREASE = 5;

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static boolean exec(JsProgram program) {
    RedefinedFunctionCollector d = new RedefinedFunctionCollector();
    d.accept(program);

    RecursionCollector rc = new RecursionCollector();
    rc.accept(program);

    InliningVisitor v = new InliningVisitor(program);
    v.blacklist(d.getRedefined());
    v.blacklist(rc.getRecursive());
    v.accept(program);

    DuplicateXORemover r = new DuplicateXORemover(program);
    r.accept(program);

    return v.didChange() || r.didChange();
  }

  /**
   * Determine whether or not a list of AST nodes are affected by side effects.
   * The context parameter provides a scope in which local (and therefore
   * immutable) variables are defined.
   */
  private static <T extends JsVisitable<T>> boolean affectedBySideEffects(
      JsProgram program, List<T> list, JsFunction context) {
    /*
     * If the caller contains no nested functions, none of its locals can
     * possibly be affected by side effects.
     */
    JsScope safeScope = null;
    if (context != null && !containsNestedFunctions(context)) {
      safeScope = context.getScope();
    }
    AffectedBySideEffectsVisitor v = new AffectedBySideEffectsVisitor(program,
        safeScope);
    v.acceptList(list);
    return v.affectedBySideEffects();
  }

  /**
   * Generate an exisimated measure of the syntactic complexity of a JsNode.
   */
  private static int complexity(JsNode<?> toEstimate) {
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

  /**
   * Check to see if the to-be-inlined statement shares any idents with the
   * call-side arguments. Two passes are made: the first one looks for qualified
   * names; the second pass looks for unqualified names, but ignores identifiers
   * that refer to function parameters.
   */
  private static boolean hasCommonIdents(List<JsExpression> arguments,
      JsNode<?> toInline, Collection<String> parameterIdents) {

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
   * Determine whether or not an AST node has side effects.
   */
  private static <T extends JsVisitable<T>> boolean hasSideEffects(T e) {
    return hasSideEffects(Collections.singletonList(e));
  }

  /**
   * Determine whether or not a list of AST nodes have side effects.
   */
  private static <T extends JsVisitable<T>> boolean hasSideEffects(List<T> list) {
    SideEffectsVisitor v = new SideEffectsVisitor();
    v.acceptList(list);
    return v.hasSideEffects();
  }

  /**
   * Given a delegated JsStatement, construct an expression to hoist into the
   * outer caller. This does not perform any name replacement, but simply
   * constructs a mutable copy of the expression that can be manipulated
   * at-will.
   */
  private static JsExpression hoistedExpression(JsStatement statement) {
    JsExpression expression;
    if (statement instanceof JsExprStmt) {
      JsExprStmt exprStmt = (JsExprStmt) statement;
      expression = exprStmt.getExpression();
    } else if (statement instanceof JsReturn) {
      JsReturn ret = (JsReturn) statement;
      expression = ret.getExpr();
    } else {
      return null;
    }

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
   * Given an expression, determine if it it is a JsNameRef that refers to a
   * statically-defined JsFunction.
   */
  // Javac 1.6.0_01 barfs on if staticRef is a JsNode<?>
  @SuppressWarnings("unchecked")
  private static JsFunction isFunction(JsExpression e) {
    if (e instanceof JsNameRef) {
      JsNameRef ref = (JsNameRef) e;

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
  private static boolean isInlinable(JsProgram program, JsFunction caller,
      JsInvocation invocation, JsFunction callee, JsNode<?> toInline) {
    List<JsExpression> arguments = invocation.getArguments();

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
     * 
     */
    if (hasCommonIdents(arguments, toInline, parameterIdents)) {
      return false;
    }

    /*
     * Determine if the evaluation of the invocation's arguments may create side
     * effects. This will determine how aggressively the parameters may be
     * reordered.
     */
    if (isVolatile(program, arguments, caller)) {
      /*
       * Determine the order in which the parameters must be evaluated. This
       * will vary between call sites, based on whether or not the invocation's
       * arguments can be repeated without ill effect.
       */
      List<JsName> requiredOrder = new ArrayList<JsName>();
      for (int i = 0; i < arguments.size(); i++) {
        JsExpression e = arguments.get(i);
        JsParameter p = callee.getParameters().get(i);

        if (isVolatile(program, e, callee)) {
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
          requiredOrder);
      orderVisitor.accept(toInline);
      if (!orderVisitor.maintainsOrder()) {
        return false;
      }
    }

    // Check that parameters aren't used in such a way as to prohibit inlining
    ParameterUsageVisitor v = new ParameterUsageVisitor(parameterNames);
    v.accept(toInline);
    if (v.parameterAsLValue()) {
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
  private static boolean isVolatile(JsProgram program, JsExpression e,
      JsFunction context) {
    return isVolatile(program, Collections.singletonList(e), context);
  }

  /**
   * Indicates if a list of expressions would create side effects or possibly be
   * affected by side effects when evaluated within a particular function
   * context.
   */
  private static <T extends JsVisitable<T>> boolean isVolatile(
      JsProgram program, List<T> list, JsFunction context) {
    return hasSideEffects(list)
        || affectedBySideEffects(program, list, context);
  }

  /**
   * Utility class.
   */
  private JsInliner() {
  }
}
