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

import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

/**
 * Optimizer that will remove all trivially computable casts and instanceof
 * operations.
 */
public class CastOptimizer {

  private class ReplaceTrivialCastsVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Replace all trivially computable casts and instanceof operations.");

    // @Override
    public void endVisit(JCastOperation x, Mutator m) {
      JType argType = x.getExpression().getType();
      if (!(x.castType instanceof JReferenceType)
          || !(argType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = (JReferenceType) x.castType;
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
        changeList.replaceExpression(m, x.expr);
      } else if (triviallyFalse) {
        // throw a ClassCastException unless the argument is null
        JMethod method = program.getSpecialMethod("Cast.throwClassCastExceptionUnlessNull");
        /*
         * Override the type of the called method with the null type. Null flow
         * will proceedeth forth from this cast operation. Assuredly, if the
         * call completes normally it will return null.
         */
        JMethodCall call = new JMethodCall(program, null, method,
            program.getTypeNull());
        ChangeList myChangeList = new ChangeList("Replace '" + x
            + "' with a call to throwClassCastExceptionUnlessNull().");
        myChangeList.addExpression(x.expr, call.args);
        myChangeList.replaceExpression(m, call);
        changeList.add(myChangeList);
      }
    }

    // @Override
    public void endVisit(JInstanceOf x, Mutator m) {
      JType argType = x.getExpression().getType();
      if (!(argType instanceof JReferenceType)) {
        return;
      }

      JReferenceType toType = x.testType;
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
        if (fromType == program.getTypeNull()) {
          // replace with a true literal
          changeList.replaceExpression(m, program.getLiteralBoolean(true));
        } else {
          // replace with a simple null test
          JNullLiteral nullLit = program.getLiteralNull();
          JBinaryOperation eq = new JBinaryOperation(program,
              program.getTypePrimitiveBoolean(), JBinaryOperator.NEQ, nullLit,
              nullLit);
          ChangeList myChangeList = new ChangeList("Replace '" + x
              + "' with a simple null test.");
          myChangeList.replaceExpression(eq.lhs, x.expr);
          myChangeList.replaceExpression(m, eq);
          changeList.add(myChangeList);
        }
      } else if (triviallyFalse) {
        // replace with a false literal
        changeList.replaceExpression(m, program.getLiteralBoolean(false));
      }
    }

    public ChangeList getChangeList() {
      return changeList;
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
    program.traverse(replacer);
    ChangeList changes = replacer.getChangeList();
    if (changes.empty()) {
      return false;
    }
    changes.apply();
    return true;
  }
}
