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
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperation;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;
import com.google.gwt.dev.jjs.ast.js.JDebuggerStatement;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

/**
 * Replaces all assertion statements in the AST with if statements.
 *
 * <p>
 * The code will look something like:
 * <pre>
 * if (!x) {
 *   GWT.debugger();
 *   throw Exceptions.makeAssertionError();
 * }
 * </pre>
 *
 * If the assertion has a message, it will be passed in the call to the Exceptions method.
 */
public class AssertionNormalizer {

  /**
   * Normalizes all asserts.
   */
  private class AssertNormalizeVisitor extends JModVisitor {

    @Override
    public void endVisit(JAssertStatement x, Context ctx) {
      JBlock then = new JBlock(x.getSourceInfo());

      then.addStmt(new JDebuggerStatement(x.getSourceInfo()));

      String methodName =
          RuntimeConstants.EXCEPTIONS_MAKE_ASSERTION_ERROR_ + getAssertMethodSuffix(x.getArg());
      JMethod method = program.getIndexedMethod(methodName);
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method);
      if (x.getArg() != null) {
        call.addArg(x.getArg());
      }
      then.addStmt(new JThrowStatement(x.getSourceInfo(), call));

      JUnaryOperation notX =
          new JPrefixOperation(x.getSourceInfo(), JUnaryOperator.NOT, x.getTestExpr());
      JIfStatement cond =
          new JIfStatement(x.getSourceInfo(), notX, then, null);
      ctx.replaceMe(cond);
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

    assert (argType.isPrimitiveType());
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
