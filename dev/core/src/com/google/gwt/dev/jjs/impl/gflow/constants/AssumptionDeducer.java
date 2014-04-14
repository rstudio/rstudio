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
package com.google.gwt.dev.jjs.impl.gflow.constants;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JValueLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

/**
 * Assumption deducer analyzes the expression, knowing its value, and deduces
 * variables constant values assumptions from it.
 */
final class AssumptionDeducer extends JVisitor {
  /**
   * Deduce assumptions, knowing that <code>expression</code> evaluates to
   * <code>value</code> and stores individual variable assumptions in the
   * <code>assumption</code> parameter. It will never override any existing
   * constant assumptions. It will override top and bottom assumptions though.
   */
  static void deduceAssumption(
      JExpression expression, final JValueLiteral value,
      final ConstantsAssumption.Updater assumption) {
    new AssumptionDeducer(value, assumption).accept(expression);
  }
  private final ConstantsAssumption.Updater assumption;

  /**
   * Contains the value of evaluating expression we're currently visiting.
   * Is <code>null</code> if we do not know current expression value.
   */
  private JValueLiteral currentValue;

  AssumptionDeducer(JValueLiteral value, ConstantsAssumption.Updater assumption) {
    this.assumption = assumption;
    currentValue = value;
  }

  @SuppressWarnings("incomplete-switch")
  @Override
  public boolean visit(JBinaryOperation x, Context ctx) {
    switch (x.getOp()) {
      case EQ:
        if (isTrue(currentValue)) {
          if (x.getRhs() instanceof JValueLiteral &&
              isSubstitutableIfEquals(x.getRhs())) {
            currentValue = (JValueLiteral) x.getRhs();
            accept(x.getLhs());
            return false;
          } else if (x.getLhs() instanceof JValueLiteral &&
              isSubstitutableIfEquals(x.getLhs())) {
            currentValue = (JValueLiteral) x.getLhs();
            accept(x.getRhs());
            return false;
          }
        }
        break;

      case NEQ:
        if (isFalse(currentValue)) {
          if (x.getRhs() instanceof JValueLiteral &&
              isSubstitutableIfEquals(x.getRhs())) {
            currentValue = (JValueLiteral) x.getRhs();
            accept(x.getLhs());
            return false;
          } else if (x.getLhs() instanceof JValueLiteral &&
              isSubstitutableIfEquals(x.getLhs())) {
            currentValue = (JValueLiteral) x.getLhs();
            accept(x.getRhs());
            return false;
          }
        }
        break;

      case AND:
        if (isTrue(currentValue)) {
          accept(x.getLhs());
          currentValue = JBooleanLiteral.get(true);
          accept(x.getRhs());
          return false;
        }
        break;

      case OR:
        if (isFalse(currentValue)) {
          accept(x.getLhs());
          currentValue = JBooleanLiteral.FALSE;
          accept(x.getRhs());
          return false;
        }
        break;
    }
    currentValue = null;
    return true;
  }

  @Override
  public boolean visit(JExpression x, Context ctx) {
    // Unknown expression. Do not go inside.
    return false;
  }

  @Override
  public boolean visit(JLocalRef x, Context ctx) {
    if (assumption.hasAssumption(x.getTarget())) {
      // Expression evaluation can't change existing assumptions
      return false;
    }
    assumption.set(x.getTarget(), currentValue);
    return false;
  }

  @Override
  public boolean visit(JMultiExpression x, Context ctx) {
    // Knowing the value multi expression, we know the value of its last
    // expression only.
    accept(x.getExpression(x.getNumberOfExpressions() - 1));
    return false;
  }

  @Override
  public boolean visit(JParameterRef x, Context ctx) {
    if (assumption.hasAssumption(x.getTarget())) {
      // Expression evaluation shouldn't change existing assumptions
      return false;
    }
    assumption.set(x.getTarget(), currentValue);
    return false;
  }

  private boolean isFalse(JValueLiteral value) {
    return value instanceof JBooleanLiteral &&
        !((JBooleanLiteral) value).getValue();
  }

  /**
   * Checks that if some expression equals <code>e</code>, then we can freely
   * substitute it by e.
   */
  private boolean isSubstitutableIfEquals(JExpression e) {
    if (!(e instanceof JValueLiteral)) {
      return false;
    }

    if (e instanceof JFloatLiteral &&
        ((JFloatLiteral) e).getValue() == 0.0f) {
      // There are +0.0 and -0.0. And both of them are equal.
      // We can't substitute 0.0 instead of them.
      return false;
    }

    if (e instanceof JDoubleLiteral &&
        ((JDoubleLiteral) e).getValue() == 0.0d) {
      // There are +0.0 and -0.0. And both of them are equal.
      // We can't substitute 0.0 instead of them.
      return false;
    }

    return true;
  }

  private boolean isTrue(JValueLiteral value) {
    return value instanceof JBooleanLiteral &&
        ((JBooleanLiteral) value).getValue();
  }
}