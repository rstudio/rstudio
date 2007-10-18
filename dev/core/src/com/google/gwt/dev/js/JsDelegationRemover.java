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
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitchMember;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collapses delegating method calls.
 */
public class JsDelegationRemover {
  /**
   * This is used to clean up duplication invocations of a clinit. Whenever
   * there is a possible branch in program flow, the remover will create a new
   * instance of itself to handle the possible outcomes.
   * 
   * We don't look at combining the clinits that are called in all branch
   * choices. This will not produce the most efficient elimination of clinit
   * calls, but it handles the general case and is simple to verify.
   */
  private class DuplicateClinitRemover extends JsModVisitor {
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
      (new DuplicateClinitRemover(called)).acceptWithInsertRemove(x
          .getStatements());
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
   * Examines a statement to determine if it matches the delegator pattern. In
   * order to be considered a delegator, all JsNameReferences must either be
   * references to global/root objects or to the parameters of the enclosing
   * function. References to strict parameters must occur exactly once and in
   * the order in which the parameters are defined. A flexible parameter (one
   * that may be evaluated multiple times without effect) may be evaluated any
   * number of times and in any order.
   */
  private class IsDelegatingVisitor extends JsVisitor {
    private final Set<JsName> parameterNames;
    private final List<JsName> strictParameters;
    private final List<JsName> flexibleParameters;
    private boolean delegating = true;

    public IsDelegatingVisitor(Set<JsName> parameterNames,
        List<JsName> strictParameters, List<JsName> flexibleParameters) {
      this.parameterNames = parameterNames;
      this.strictParameters = strictParameters;
      this.flexibleParameters = flexibleParameters;
    }

    @Override
    public void endVisit(JsNameRef x, JsContext<JsExpression> ctx) {
      // Short circuit
      if (!delegating) {
        return;
      }

      // Don't examine JsNameRefs that are intermediate elements in a chain
      if (x.getQualifier() != null) {
        return;
      }

      JsName name = x.getName();

      if (parameterNames.contains(name)) {
        // The name is a reference to a parameter defined in the function
        if (flexibleParameters.contains(name)) {
          // It doesn't matter how many times an optional parameter is used

        } else if (strictParameters.indexOf(name) != 0) {
          // We saw a required parameter being used out of declaration order
          delegating = false;

        } else {
          // Record that the required parameter was used.
          if (strictParameters.remove(0) != name) {
            throw new InternalCompilerException(
                "Unexpected name removed from strict parameter list.");
          }
        }
      } else {
        // See if the name refers to a global/static name.
        boolean isStatic =
            name.getEnclosing() == program.getScope()
                || name.getEnclosing() == program.getRootScope();
        delegating &= isStatic;
      }
    }

    public boolean isDelegating() {
      return delegating && (strictParameters.size() == 0);
    }
  }

  /**
   * Given a call site, replace with an equivalent expression, assuming that the
   * callee is a delegation-style function.
   */
  private class NameRefReplacerVisitor extends JsModVisitor {
    /**
     * Set up a map of parameter names back to the expressions that will be
     * passed in from the outer call site.
     */
    final Map<JsName, JsExpression> originalParameterExpressions =
        new HashMap<JsName, JsExpression>();

    /**
     * Constructor.
     * 
     * @param invocation The delegating JsInvocation (the inner delegation)
     * @param function The function that encloses the outer delegation
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
        originalParameterExpressions.put(p.getName(), e);
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

      JsExpression original = originalParameterExpressions.get(x.getName());

      if (original != null) {
        ctx.replaceMe(original);
      }
    }
  }

  /**
   * This looks for all invocations in the program. Any invoked JsFunctions
   * derived from Java functions are then examined for being possible delegation
   * patterns.
   */
  private class RemoveDelegationVisitor extends JsModVisitor {
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

      // Confirm that the statement conforms to the desired pattern
      if (!isDelegatingStatement(x, f, toHoist)) {
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

      // Perform the name replacement
      NameRefReplacerVisitor v = new NameRefReplacerVisitor(x, f);
      replacement = v.accept(replacement);

      // Assemble the (clinit(), hoisted) expression.
      if (statements.size() == 2) {
        replacement =
            new JsBinaryOperation(JsBinaryOperator.COMMA, ((JsExprStmt) clinit)
                .getExpression(), replacement);
      }

      ctx.replaceMe(replacement);
    }
  }

  /**
   * Static entry point used by JavaToJavaScriptCompiler.
   */
  public static boolean exec(JsProgram program) {
    return new JsDelegationRemover(program).execImpl();
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
   * Indicates if an expression can be repeated multiple times in a delegation
   * removal without side-effects.
   */
  private static boolean requiresSingleEvaluation(JsExpression e) {
    if (e instanceof JsNameRef) {
      JsNameRef ref = (JsNameRef) e;
      if (ref.getQualifier() != null) {
        return requiresSingleEvaluation(ref.getQualifier());
      } else {
        return false;
      }
    } else if (e instanceof JsBooleanLiteral) {
      return false;
    } else if (e instanceof JsDecimalLiteral) {
      return false;
    } else if (e instanceof JsIntegralLiteral) {
      return false;
    } else if (e instanceof JsNullLiteral) {
      return false;
    } else if (e instanceof JsStringLiteral) {
      return false;
    } else if (e instanceof JsThisRef) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * The intended victim for the optimizer.
   */
  private final JsProgram program;

  /**
   * Constructor.
   */
  private JsDelegationRemover(JsProgram program) {
    this.program = program;
  }

  /**
   * Instance execution method.
   */
  private boolean execImpl() {
    RemoveDelegationVisitor v = new RemoveDelegationVisitor();
    v.accept(program);

    DuplicateClinitRemover r = new DuplicateClinitRemover();
    r.accept(program);

    return v.didChange() || r.didChange();
  }

  /**
   * Determine if a statement matches the delegator pattern.
   */
  private boolean isDelegatingStatement(JsInvocation invocation,
      JsFunction enclosing, JsStatement statement) {

    // Build up a map of parameter names
    Set<JsName> parameterNames = new HashSet<JsName>();

    for (JsParameter param : enclosing.getParameters()) {
      parameterNames.add(param.getName());
    }

    if (invocation.getArguments().size() != enclosing.getParameters().size()) {
      /*
       * This will happen with varargs-style JavaScript functions that rely on
       * the "arguments" array. The reference to arguments would be detected in
       * IsDelegatingVisitor, but the bucketing code below assumes the same
       * number of parameters and arguments.
       */
      return false;
    }

    /*
     * Determine which function parameters can safely be duplicated or
     * re-ordered in the delegation removal. This will vary between call sites,
     * based on whether or not the invocation's arguments can be repeated
     * without ill effect.
     */
    List<JsName> strictParameters = new ArrayList<JsName>();
    List<JsName> flexibleParameters = new ArrayList<JsName>();
    for (int i = 0; i < invocation.getArguments().size(); i++) {
      JsExpression e = invocation.getArguments().get(i);
      if (requiresSingleEvaluation(e)) {
        strictParameters.add(enclosing.getParameters().get(i).getName());
      } else {
        flexibleParameters.add(enclosing.getParameters().get(i).getName());
      }
    }

    IsDelegatingVisitor v =
        new IsDelegatingVisitor(parameterNames, strictParameters,
            flexibleParameters);
    v.accept(statement);
    return v.isDelegating();
  }
}
