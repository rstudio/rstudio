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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.InternalCompilerException;
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
import com.google.gwt.dev.js.ast.JsNullLiteral;
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
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Perform inlining optimizations on the JavaScript AST.
 */
public class JsInliner {
  /**
   * This is used to clean up duplication invocations of a clinit. Whenever
   * there is a possible branch in program flow, the remover will create a new
   * instance of itself to handle the possible outcomes.
   * 
   * We don't look at combining the clinits that are called in all branch
   * choices. This will not produce the most efficient elimination of clinit
   * calls, but it handles the general case and is simple to verify.
   */
  private static class DuplicateClinitRemover extends JsModVisitor {
    /*
     * TODO: Most of the special casing below can be removed if complex
     * statements always use blocks, rather than plain statements.
     */

    /**
     * Retains the names of the clinit functions that we know have been called.
     */
    private final Set<JsName> called;

    public DuplicateClinitRemover() {
      called = new HashSet<JsName>();
    }

    public DuplicateClinitRemover(Set<JsName> alreadyCalled) {
      called = new HashSet<JsName>(alreadyCalled);
    }

    @Override
    public boolean visit(JsBinaryOperation x, JsContext<JsExpression> ctx) {
      if ((x.getOperator() == JsBinaryOperator.COMMA)
          && (x.getArg1() instanceof JsInvocation)) {

        JsName clinit = getClinitFromInvocation((JsInvocation) x.getArg1());
        if ((clinit != null) && called.contains(clinit)) {

          // Replace the binary operation with the RHS
          ctx.replaceMe(x.getArg2());

          // Manually call accept on the RHS to eliminate nested comma
          // expressions.
          accept(x.getArg2());

          // Don't continue traversing the original binary operation
          return false;
        }
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
      (new DuplicateClinitRemover(called)).acceptWithInsertRemove(x.getStatements());
      return false;
    }

    @Override
    public boolean visit(JsCase x, JsContext<JsSwitchMember> ctx) {
      accept(x.getCaseExpr());
      (new DuplicateClinitRemover(called)).acceptWithInsertRemove(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsConditional x, JsContext<JsExpression> ctx) {
      accept(x.getTestExpression());
      (new DuplicateClinitRemover(called)).accept(x.getThenExpression());
      (new DuplicateClinitRemover(called)).accept(x.getElseExpression());
      return false;
    }

    @Override
    public boolean visit(JsDefault x, JsContext<JsSwitchMember> ctx) {
      (new DuplicateClinitRemover(called)).acceptWithInsertRemove(x.getStmts());
      return false;
    }

    @Override
    public boolean visit(JsFor x, JsContext<JsStatement> ctx) {
      // The JsFor may have an expression xor a variable declaration.
      if (x.getInitExpr() != null) {
        accept(x.getInitExpr());
      } else if (x.getInitVars() != null) {
        accept(x.getInitVars());
      }

      // The condition is optional
      if (x.getCondition() != null) {
        accept(x.getCondition());
      }

      // We don't check the increment expression because even if it exists, it
      // is not guaranteed to be called at all

      // The body is not guaranteed to be a JsBlock
      (new DuplicateClinitRemover(called)).accept(x.getBody());
      return false;
    }

    @Override
    public boolean visit(JsForIn x, JsContext<JsStatement> ctx) {
      if (x.getIterExpr() != null) {
        accept(x.getIterExpr());
      }

      accept(x.getObjExpr());

      // The body is not guaranteed to be a JsBlock
      (new DuplicateClinitRemover(called)).accept(x.getBody());
      return false;
    }

    @Override
    public boolean visit(JsIf x, JsContext<JsStatement> ctx) {
      accept(x.getIfExpr());

      (new DuplicateClinitRemover(called)).accept(x.getThenStmt());
      if (x.getElseStmt() != null) {
        (new DuplicateClinitRemover(called)).accept(x.getElseStmt());
      }

      return false;
    }

    /**
     * Possibly record that we've seen a clinit in the current context.
     */
    @Override
    public boolean visit(JsInvocation x, JsContext<JsExpression> ctx) {
      JsName name = getClinitFromInvocation(x);
      if (name != null) {
        called.add(name);
      }
      return true;
    }

    @Override
    public boolean visit(JsWhile x, JsContext<JsStatement> ctx) {
      accept(x.getCondition());

      // The body is not guaranteed to be a JsBlock
      (new DuplicateClinitRemover(called)).accept(x.getBody());
      return false;
    }
  }

  /**
   * Determines if a list of names is guaranteed to be evaluated in a particular
   * order.
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

      // We don't care about the left-hand expression, because it is guaranteed
      // to be evaluated.
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
      if (unevaluated.size() > 0) {
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
    private final Set<String> idents = new HashSet<String>();
    private final boolean collectQualified;

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
   * This class looks for function invocations that can be inlined and perform
   * the replacement.
   */
  private static class InliningVisitor extends JsModVisitor {
    final Stack<JsFunction> functionStack = new Stack<JsFunction>();

    @Override
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      if (!functionStack.pop().equals(x)) {
        throw new InternalCompilerException("Unexpected function popped");
      }
    }

    @Override
    public void endVisit(JsInvocation x, JsContext<JsExpression> ctx) {
      // Start by determining what is being invoked.
      if (!(x.getQualifier() instanceof JsNameRef)) {
        return;
      }
      JsNameRef ref = (JsNameRef) x.getQualifier();

      /*
       * We only want to look at invocations of things that we statically know
       * to be functions. Otherwise, we can't know what statements the
       * invocation would actually invoke. The static reference would be null
       * when trying operate on references to external functions, or functions
       * as arguments to another function.
       */
      if (!(ref.getName().getStaticRef() instanceof JsFunction)) {
        return;
      }

      JsFunction f = (JsFunction) ref.getName().getStaticRef();
      List<JsStatement> statements = f.getBody().getStatements();
      JsStatement clinit;
      JsStatement toHoist;

      if (statements.size() == 1) {
        // The simple case.
        clinit = null;
        toHoist = statements.get(0);

      } else if (statements.size() == 2) {
        /*
         * In the case of DOM, or similarly-structured classes that use a static
         * "impl" field, we need to account for the static initializer. What
         * we'll do is to create a JS comma expression to encapsulate the
         * invocation of the static initializer as well as the delegated
         * function. As a subsequent optimization, we can go through all
         * functions and look for repeated invocations of a clinit, removing the
         * JsBinaryOperation and replacing it with its rhs.
         */
        clinit = statements.get(0);
        toHoist = statements.get(1);
        if (!isStaticInitializer(clinit)) {
          return;
        }

      } else {
        // The expression is too complicated for this optimization
        return;
      }

      /*
       * Create a replacement expression to use in place of the invocation. It
       * is important that the replacement is newly-minted and therefore not
       * referenced by any other AST nodes. Consider the case of a common,
       * delegating function. If the hoisted expressions were not distinct
       * objects, it would not be possible to substitute different JsNameRefs at
       * different call sites.
       */
      JsExpression replacement = hoistedExpression(toHoist);
      if (replacement == null) {
        return;
      }

      // Confirm that the statement conforms to the desired pattern
      if (!isInlinableStatement(functionStack.peek(), x.getArguments(), f,
          toHoist)) {
        return;
      }

      // Perform the name replacement
      NameRefReplacerVisitor v = new NameRefReplacerVisitor(x, f);
      replacement = v.accept(replacement);

      // Assemble the (clinit(), hoisted) expression.
      if (statements.size() == 2) {
        replacement = new JsBinaryOperation(JsBinaryOperator.COMMA,
            ((JsExprStmt) clinit).getExpression(), replacement);
      }

      // Replace the original invocation with the inlined statement
      ctx.replaceMe(replacement);
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
   * Detects uses of parameters that would produce incorrect results if inlined.
   * Generally speaking, we disallow the use of parameters as lvalues.
   */
  private static class ParameterUsageVisitor extends JsVisitor {
    private final Set<JsName> parameterNames;
    private boolean lvalue = false;

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
    private final JsScope callerScope;
    private final JsScope calleeScope;
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
      // We can ignore qualified reference, since their scope is always that
      // of the qualifier.
      if (x.getQualifier() != null) {
        return;
      }

      // Attempt to resolve the ident in both scopes
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
   * A List of expression types that are known to never be affected by
   * side-effects. Used by {@link #alwaysFlexible(JsExpression)}.
   */
  private static final List<Class<?>> ALWAYS_FLEXIBLE = Arrays.asList(new Class<?>[] {
      JsBooleanLiteral.class, JsDecimalLiteral.class, JsIntegralLiteral.class,
      JsNullLiteral.class, JsRegExp.class, JsStringLiteral.class,
      JsThisRef.class});

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static boolean exec(JsProgram program) {
    InliningVisitor v = new InliningVisitor();
    v.accept(program);

    DuplicateClinitRemover r = new DuplicateClinitRemover();
    r.accept(program);

    return v.didChange() || r.didChange();
  }

  /**
   * Indicates if an expression can be repeated multiple times in a delegation
   * removal without side-effects.
   */
  private static boolean alwaysFlexible(JsExpression e) {
    if (e instanceof JsNameRef) {
      return false;
    } else {
      return ALWAYS_FLEXIBLE.contains(e.getClass());
    }
  }

  /**
   * Given a JsInvocation, determine if it is invoking a class's static
   * initializer. This just looks for an invocation of a function whose short
   * identifier is "$clinit". Not fancy, but it works.
   */
  private static JsName getClinitFromInvocation(JsInvocation invocation) {
    if (invocation.getQualifier() instanceof JsNameRef) {
      JsNameRef nameRef = (JsNameRef) invocation.getQualifier();
      JsName name = nameRef.getName();
      if (name.getShortIdent().equals("$clinit")) {
        return name;
      }
    }

    return null;
  }

  /**
   * Check to see if the to-be-inlined statement shares any idents with the
   * call-side arguments. Two passes are made: the first one looks for qualified
   * names; the second pass looks for unqualified names, but ignores identifiers
   * that refer to function parameters.
   */
  private static boolean hasCommonIdents(List<JsExpression> arguments,
      JsStatement toInline, Collection<String> parameterIdents) {

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
   * Determine if a statement can be inlined into a call site.
   */
  private static boolean isInlinableStatement(JsFunction caller,
      List<JsExpression> arguments, JsFunction callee, JsStatement toInline) {
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
    SideEffectsVisitor sideEffects = new SideEffectsVisitor();
    sideEffects.acceptList(arguments);
    boolean maintainOrder = sideEffects.hasSideEffects();

    if (maintainOrder) {
      /*
       * Determine the order in which the parameters must be evaluated. This
       * will vary between call sites, based on whether or not the invocation's
       * arguments can be repeated without ill effect.
       */
      List<JsName> requiredOrder = new ArrayList<JsName>();
      for (int i = 0; i < arguments.size(); i++) {
        JsExpression e = arguments.get(i);
        JsParameter p = callee.getParameters().get(i);

        if (!alwaysFlexible(e)) {
          requiredOrder.add(p.getName());
        }
      }

      /*
       * Verify that the non-reorderable arguments are evaluated in the right
       * order.
       */
      if (requiredOrder.size() > 0) {
        EvaluationOrderVisitor orderVisitor = new EvaluationOrderVisitor(
            requiredOrder);
        orderVisitor.accept(toInline);
        if (!orderVisitor.maintainsOrder()) {
          return false;
        }
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
   * Determines if a statement is an invocation of a static initializer.
   */
  private static boolean isStaticInitializer(JsStatement statement) {
    if (!(statement instanceof JsExprStmt)) {
      return false;
    }

    JsExprStmt exprStmt = (JsExprStmt) statement;
    JsExpression expression = exprStmt.getExpression();

    if (!(expression instanceof JsInvocation)) {
      return false;
    }

    JsInvocation invocation = (JsInvocation) expression;

    if (!(invocation.getQualifier() instanceof JsNameRef)) {
      return false;
    }

    return getClinitFromInvocation(invocation) != null;
  }

  /**
   * Utility class.
   */
  private JsInliner() {
  }
}
