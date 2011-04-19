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
import com.google.gwt.dev.jjs.ast.JArrayLength;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * Analyzes an expression and make a number of static analysis flags available
 * based on the information available solely through the expression.
 * 
 * TODO: make this even smarter when we have real null analysis.
 */
public class ExpressionAnalyzer extends JVisitor {
  private boolean accessesField;
  private boolean accessesFieldNonFinal;
  private boolean accessesLocal;
  private boolean accessesParameter;
  private boolean assignmentToField;
  private boolean assignmentToLocal;
  private boolean assignmentToParameter;
  private boolean canThrowException;
  private boolean createsObject;
  private int inConditional;

  /**
   * Does this expression read or write fields within the scope of the
   * expression?
   */
  public boolean accessesField() {
    return accessesField;
  }

  /**
   * Does this expression read or write non-final fields within the scope of the
   * expression?
   */
  public boolean accessesFieldNonFinal() {
    return accessesFieldNonFinal;
  }

  /**
   * Does this expression read or write locals within the scope of the
   * expression?
   */
  public boolean accessesLocal() {
    return accessesLocal;
  }

  /**
   * Does this expression read or write parameters within the scope of the
   * expression?
   */
  public boolean accessesParameter() {
    return accessesParameter;
  }

  public boolean canThrowException() {
    return canThrowException;
  }

  public boolean createsObject() {
    return createsObject;
  }

  @Override
  public void endVisit(JArrayLength x, Context ctx) {
    // TODO: Is setting accessesField necessary for array.length access?
    accessesField = true;
    // Can throw an NPE when the array instance is null at runtime.
    JReferenceType refType = (JReferenceType) x.getInstance().getType();
    canThrowException = refType.canBeNull();
  }

  @Override
  public void endVisit(JArrayRef x, Context ctx) {
    /*
     * In Java, array references can throw IndexOutOfBoundsExceptions, but this
     * isn't the case for current GWT generated code. If we add a strict array
     * bounds check later, this flag would need to reflect it.
     */

    // If JArrayRef is null, this can throw a NullPointerException.
    canThrowException = true;
  }

  @Override
  public void endVisit(JBinaryOperation x, Context ctx) {
    if (x.isAssignment()) {
      JExpression lhs = x.getLhs();
      if (lhs instanceof JArrayRef) {
        // Array store operations can throw ArrayStoreExceptions
        canThrowException = true;
      } else {
        analyzeStore(lhs);
      }
    }
  }

  @Override
  public void endVisit(JCastOperation x, Context ctx) {
    // Can throw ClassCastException
    canThrowException = true;
  }

  @Override
  public void endVisit(JFieldRef x, Context ctx) {
    accessesField = true;
    if (!x.getTarget().isFinal()) {
      accessesFieldNonFinal = true;
    }

    if (x.hasClinit()) {
      recordMethodCall();
    }

    JExpression instance = x.getInstance();
    if (instance == null) {
      return;
    }

    // Field references using this are always safe
    if (instance instanceof JThisRef) {
      return;
    }

    if (x.getField().isStatic()) {
      // Can throw exceptions IFF a clinit is triggered.
      canThrowException = x.hasClinit();
    } else {
      // Can throw exceptions IFF the instance is null.
      JReferenceType refType = (JReferenceType) instance.getType();
      canThrowException = refType.canBeNull();
    }
  }

  @Override
  public void endVisit(JLocalRef x, Context ctx) {
    accessesLocal = true;
  }

  @Override
  public void endVisit(JMethodCall x, Context ctx) {
    recordMethodCall();
  }

  @Override
  public void endVisit(JNewArray x, Context ctx) {
    /*
     * If no array bounds, the new array is being automatically initialized. If
     * there are side-effects, they'll show up when we visit the initializers.
     */
    if (x.dims == null) {
      return;
    }

    /*
     * Can throw NegativeArraySizeException if we initialize an array with
     * negative dimensions.
     */
    for (JExpression expression : x.dims) {
      if (expression instanceof JIntLiteral) {
        int value = ((JIntLiteral) expression).getValue();
        if (value >= 0) {
          continue;
        }
      }
      canThrowException = true;
    }
  }

  @Override
  public void endVisit(JNewInstance x, Context ctx) {
    createsObject = true;
    endVisit((JMethodCall) x, ctx);
  }

  @Override
  public void endVisit(JParameterRef x, Context ctx) {
    accessesParameter = true;
  }

  @Override
  public void endVisit(JPostfixOperation x, Context ctx) {
    // Unary operations that are modifying cause assignment side-effects.
    if (x.getOp().isModifying()) {
      analyzeStore(x.getArg());
    }
  }

  @Override
  public void endVisit(JPrefixOperation x, Context ctx) {
    // Unary operations that are modifying cause assignment side-effects.
    if (x.getOp().isModifying()) {
      analyzeStore(x.getArg());
    }
  }

  /**
   * Does this expression make assignments to variables within the scope of the
   * expression?
   */
  public boolean hasAssignment() {
    return assignmentToField || assignmentToLocal || assignmentToParameter;
  }

  /**
   * Does this expression make assignments to fields within the scope of the
   * expression?
   */
  public boolean hasAssignmentToField() {
    return assignmentToField;
  }

  /**
   * Does this expression make assignments to locals within the scope of the
   * expression?
   */
  public boolean hasAssignmentToLocal() {
    return assignmentToLocal;
  }

  /**
   * Does this expression make assignments to parameters within the scope of the
   * expression?
   */
  public boolean hasAssignmentToParameter() {
    return assignmentToParameter;
  }

  @Override
  public boolean visit(JBinaryOperation x, Context ctx) {
    if (x.getOp() == JBinaryOperator.AND || x.getOp() == JBinaryOperator.OR) {
      accept(x.getLhs());
      inConditional++;
      accept(x.getRhs());
      inConditional--;
      return false;
    }
    return true;
  }

  @Override
  public boolean visit(JConditional x, Context ctx) {
    accept(x.getIfTest());
    inConditional++;
    accept(x.getThenExpr());
    accept(x.getElseExpr());
    inConditional--;

    return false;
  }

  /**
   * Determined if the current expression conditionally executes, based on its
   * parent expressions.
   */
  protected boolean isInConditional() {
    return inConditional > 0;
  }

  private void analyzeStore(JExpression expr) {
    if (expr instanceof JFieldRef) {
      assignmentToField = true;
    } else if (expr instanceof JParameterRef) {
      assignmentToParameter = true;
    } else if (expr instanceof JLocalRef) {
      assignmentToLocal = true;
    }
  }

  /**
   * We can't assume anything about method calls right now, except that it can't
   * access any of our locals or parameters.
   * 
   * TODO: what about accessing arrays? Should be treated like field refs I
   * guess.
   */
  private void recordMethodCall() {
    assignmentToField = true;
    accessesField = true;
    accessesFieldNonFinal = true;
    canThrowException = true;
    createsObject = true;
  }
}
