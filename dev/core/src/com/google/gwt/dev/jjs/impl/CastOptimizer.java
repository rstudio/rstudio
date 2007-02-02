/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;

/**
 * Optimizer that will remove all trivially computable casts and instanceof
 * operations.
 */
public class CastOptimizer {

  /**
   * Replaces all trivially computable casts and instanceof operations.
   */
  private class ReplaceTrivialCastsVisitor extends JModVisitor {

    // @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JType argType = x.getExpr().getType();
      if (!(x.getCastType() instanceof JReferenceType)
          || !(argType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = (JReferenceType) x.getCastType();
      JReferenceType fromType = (JReferenceType) argType;

      boolean triviallyTrue = false;
      boolean triviallyFalse = false;

      JTypeOracle typeOracle = program.typeOracle;
      if (typeOracle.canTriviallyCast(fromType, toType)) {
        triviallyTrue = true;
      } else if (!typeOracle.isInstantiatedType(toType)) {
        triviallyFalse = true;
      } else if (!typeOracle.canTheoreticallyCast(fromType, toType)) {
        triviallyFalse = true;
      }

      if (triviallyTrue) {
        // remove the cast operation
        ctx.replaceMe(x.getExpr());
      } else if (triviallyFalse) {
        // throw a ClassCastException unless the argument is null
        JMethod method = program.getSpecialMethod("Cast.throwClassCastExceptionUnlessNull");
        /*
         * Override the type of the called method with the null type. Null flow
         * will proceedeth forth from this cast operation. Assuredly, if the
         * call completes normally it will return null.
         */
        JMethodCall call = new JMethodCall(program, x.getSourceInfo(), null,
            method, program.getTypeNull());
        call.getArgs().add(x.getExpr());
        ctx.replaceMe(call);
      }
    }

    // @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JType argType = x.getExpr().getType();
      if (!(argType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = x.getTestType();
      JReferenceType fromType = (JReferenceType) argType;

      boolean triviallyTrue = false;
      boolean triviallyFalse = false;

      JTypeOracle typeOracle = program.typeOracle;
      if (fromType == program.getTypeNull()) {
        // null is never instanceOf anything
        triviallyFalse = true;
      } else if (typeOracle.canTriviallyCast(fromType, toType)) {
        triviallyTrue = true;
      } else if (!typeOracle.isInstantiatedType(toType)) {
        triviallyFalse = true;
      } else if (!typeOracle.canTheoreticallyCast(fromType, toType)) {
        triviallyFalse = true;
      }

      if (triviallyTrue) {
        // replace with a simple null test
        JNullLiteral nullLit = program.getLiteralNull();
        JBinaryOperation neq = new JBinaryOperation(program, x.getSourceInfo(),
            program.getTypePrimitiveBoolean(), JBinaryOperator.NEQ,
            x.getExpr(), nullLit);
        ctx.replaceMe(neq);
      } else if (triviallyFalse) {
        // replace with a false literal
        ctx.replaceMe(program.getLiteralBoolean(false));
      }
    }
  }

  public static boolean exec(JProgram program) {
    return new CastOptimizer(program).execImpl();
  }

  private final JProgram program;

  private CastOptimizer(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    ReplaceTrivialCastsVisitor replacer = new ReplaceTrivialCastsVisitor();
    replacer.accept(program);
    return replacer.didChange();
  }
}
