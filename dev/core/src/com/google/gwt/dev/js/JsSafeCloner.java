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
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsNameOf;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsNumericEntry;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.collect.Stack;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to clone JsExpression AST members. <b>Not all expressions are necessarily
 * cloned </b>, only those expressions that are safe to hoist into outer call sites.
 */
public final class JsSafeCloner {
  /**
   * Implements actual cloning logic. We rely on the JsExpressions to provide
   * traversal logic. The {@link #stack} field is used to accumulate
   * already-cloned JsExpression instances. One gotcha that falls out of this is
   * that argument lists are on the stack in reverse order, so lists should be
   * constructed via inserts, rather than appends.
   */
  public static class Cloner extends JsVisitor {
    protected final Stack<JsExpression> stack = new Stack<JsExpression>();
    private boolean successful = true;

    @Override
    public void endVisit(JsArrayAccess x, JsContext ctx) {
      JsArrayAccess newExpression = new JsArrayAccess(x.getSourceInfo());
      newExpression.setIndexExpr(stack.pop());
      newExpression.setArrayExpr(stack.pop());
      stack.push(newExpression);
    }

    @Override
    public void endVisit(JsArrayLiteral x, JsContext ctx) {
      JsArrayLiteral toReturn = new JsArrayLiteral(x.getSourceInfo());
      List<JsExpression> expressions = toReturn.getExpressions();
      int size = x.getExpressions().size();
      while (size-- > 0) {
        expressions.add(0, stack.pop());
      }
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsBinaryOperation x, JsContext ctx) {
      JsBinaryOperation toReturn = new JsBinaryOperation(x.getSourceInfo(),
          x.getOperator());
      toReturn.setArg2(stack.pop());
      toReturn.setArg1(stack.pop());
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsBooleanLiteral x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public void endVisit(JsConditional x, JsContext ctx) {
      JsConditional toReturn = new JsConditional(x.getSourceInfo());
      toReturn.setElseExpression(stack.pop());
      toReturn.setThenExpression(stack.pop());
      toReturn.setTestExpression(stack.pop());
      stack.push(toReturn);
    }

    /**
     * The only functions that would get be visited are those being used as
     * first-class objects.
     */
    @Override
    public void endVisit(JsFunction x, JsContext ctx) {
      // Set a flag to indicate that we cannot continue, and push a null so
      // we don't run out of elements on the stack.
      successful = false;
      stack.push(null);
    }

    /**
     * Cloning the invocation allows us to modify it without damaging other call
     * sites.
     */
    @Override
    public void endVisit(JsInvocation x, JsContext ctx) {
      JsInvocation toReturn = new JsInvocation(x.getSourceInfo());
      List<JsExpression> params = toReturn.getArguments();
      int size = x.getArguments().size();
      while (size-- > 0) {
        params.add(0, stack.pop());
      }
      toReturn.setQualifier(stack.pop());
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsNameOf x, JsContext ctx) {
      JsNameOf toReturn = new JsNameOf(x.getSourceInfo(), x.getName());
      stack.push(toReturn);
    }

    /**
     * Do a deep clone of a JsNameRef. Because JsNameRef chains are shared
     * throughout the AST, you can't just go and change their qualifiers when
     * re-writing an invocation.
     */
    @Override
    public void endVisit(JsNameRef x, JsContext ctx) {
      if (x.getQualifier() == null && x.getIdent() == "arguments") {
        // References to the arguments object can not be hoisted.
        successful = false;
        stack.push(null);
      }
      JsNameRef toReturn = new JsNameRef(x.getSourceInfo(), x.getName());

      if (x.getQualifier() != null) {
        toReturn.setQualifier(stack.pop());
      }
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsNew x, JsContext ctx) {
      int size = x.getArguments().size();
      List<JsExpression> arguments = new ArrayList<JsExpression>(size);
      while (size-- > 0) {
        arguments.add(0, stack.pop());
      }
      JsNew toReturn = new JsNew(x.getSourceInfo(), stack.pop());
      toReturn.getArguments().addAll(arguments);
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsNullLiteral x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public void endVisit(JsNumberLiteral x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public void endVisit(JsNumericEntry x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public boolean visit(JsObjectLiteral x, JsContext ctx) {
      JsObjectLiteral.Builder builder = JsObjectLiteral.builder(x.getSourceInfo());

      if (x.isInternable()) {
        builder.setInternable();
      }

      for (JsPropertyInitializer propertyInitializer : x.getPropertyInitializers()) {
        /*
         * JsPropertyInitializers are the only non-JsExpression objects that we
         * care about, so we just go ahead and create the objects in the loop,
         * rather than expecting it to be on the stack and having to perform
         * narrowing casts at all stack.pop() invocations.
         */
        accept(propertyInitializer.getLabelExpr());
        JsExpression label = stack.pop();
        accept(propertyInitializer.getValueExpr());
        JsExpression value = stack.pop();
        builder.add(propertyInitializer.getSourceInfo(), label, value);
      }
      stack.push(builder.build());
      return false;
    }

    @Override
    public void endVisit(JsPostfixOperation x, JsContext ctx) {
      JsPostfixOperation toReturn = new JsPostfixOperation(x.getSourceInfo(),
          x.getOperator());
      toReturn.setArg(stack.pop());
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsPrefixOperation x, JsContext ctx) {
      JsPrefixOperation toReturn = new JsPrefixOperation(x.getSourceInfo(),
          x.getOperator());
      toReturn.setArg(stack.pop());
      stack.push(toReturn);
    }

    @Override
    public void endVisit(JsRegExp x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public void endVisit(JsStringLiteral x, JsContext ctx) {
      stack.push(x);
    }

    @Override
    public void endVisit(JsThisRef x, JsContext ctx) {
      stack.push(new JsThisRef(x.getSourceInfo()));
    }

    public JsExpression getExpression() {
      return (successful && checkStack()) ? stack.peek() : null;
    }

    private boolean checkStack() {
      if (stack.size() > 1) {
        throw new InternalCompilerException("Too many expressions on stack");
      }

      return stack.size() == 1;
    }
  }

  /**
   * Given a JsStatement, construct an expression to clone into the outer
   * caller. This does not perform any name replacement, nor does it verify the
   * scope of referenced elements, but simply constructs a mutable copy of the
   * expression that can be manipulated at-will.
   *
   * @return A copy of the original expression, or <code>null</code> if the
   *         expression cannot be hoisted.
   */
  public static JsExpression clone(JsExpression expression) {
    if (expression == null) {
      return null;
    }

    Cloner c = new Cloner();
    c.accept(expression);
    return c.getExpression();
  }

  private JsSafeCloner() {
  }
}
