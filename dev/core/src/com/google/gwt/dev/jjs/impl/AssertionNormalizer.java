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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

/**
 * Removes all assertion statements from the AST.
 */
public class AssertionNormalizer {

  /**
   * Normalizes all asserts.
   */
  private class AssertNormalizeVisitor extends JModVisitor {

    @Override
    public void endVisit(JAssertStatement x, Context ctx) {
      JExpression lhs = x.getTestExpr();
      String methodName = "Exceptions.throwAssertionError" + getAssertMethodSuffix(x.getArg());
      JMethod method = program.getIndexedMethod(methodName);
      JMethodCall rhs = new JMethodCall(x.getSourceInfo(), null, method);
      if (x.getArg() != null) {
        rhs.addArg(x.getArg());
      }
      JBinaryOperation binOp =
          new JBinaryOperation(x.getSourceInfo(), program.getTypePrimitiveBoolean(),
              JBinaryOperator.OR, lhs, rhs);
      ctx.replaceMe(binOp.makeStatement());
    }
  }

  public static void exec(JProgram program) {
    Event assertionNormalizerEvent =
        SpeedTracerLogger.start(CompilerEventType.ASSERTION_NORMALIZER);
    new AssertionNormalizer(program).execImpl();
    assertionNormalizerEvent.end();
  }

  private static String getAssertMethodSuffix(JExpression arg) {
    if (arg == null) {
      return "";
    }
    JType argType = arg.getType();
    if (argType instanceof JReferenceType) {
      return "_Object";
    }

    assert (argType instanceof JPrimitiveType);
    return "_" + argType.getName();
  }

  private final JProgram program;

  public AssertionNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    AssertNormalizeVisitor assertNormalizer = new AssertNormalizeVisitor();
    assertNormalizer.accept(program);
  }
}
