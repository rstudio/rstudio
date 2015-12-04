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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge multi-catch blocks into a single catch block that uses instanceof tests
 * to determine which user block to run.
 */
public class CatchBlockNormalizer {

  /**
   * Collapses all multi-catch blocks into a single catch block.
   */
  private class CollapseCatchBlocks extends JModVisitor {
    JMethod wrapMethod = program.getIndexedMethod(RuntimeConstants.EXCEPTIONS_WRAP);

    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      clearLocals();
      currentMethodBody = null;
    }

    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      if (x.getCatchClauses().isEmpty() &&
          (x.getFinallyBlock() == null || x.getFinallyBlock().isEmpty())) {
        // Remove {@code try { ...  }} if there are not catch and/or finally blocks. Translating it
        // to JavaScript as {@code try { ... }} without catch and/or finally is illegal.
        ctx.replaceMe(x.getTryBlock());
      }

      if (x.getCatchClauses().isEmpty()) {
        return;
      }

      SourceInfo catchInfo = x.getCatchClauses().get(0).getBlock().getSourceInfo();
      JLocal exceptionVariable = newExceptionVariable(x.getSourceInfo());
      JBlock newCatchBlock = new JBlock(catchInfo);

      {
        // $e = Exceptions.wrap($e)
        JMethodCall call =
            new JMethodCall(catchInfo, null, wrapMethod, exceptionVariable.makeRef(catchInfo));
        newCatchBlock.addStmt(
            JProgram.createAssignmentStmt(catchInfo, exceptionVariable.makeRef(catchInfo), call));
      }

      /*
       * Build up a series of if, else if statements to test the type of the
       * exception object against the types of the user's catch block. Each catch block might have
       * multiple types in Java 7.
       *
       * Go backwards so we can nest the else statements in the correct order!
       */
      // rethrow the current exception if no one caught it.
      JStatement cur = new JThrowStatement(catchInfo, exceptionVariable.makeRef(catchInfo));
      for (int i = x.getCatchClauses().size() - 1; i >= 0; i--) {
        JTryStatement.CatchClause clause = x.getCatchClauses().get(i);
        JBlock block = clause.getBlock();
        JLocalRef arg = clause.getArg();
        List<JType> exceptionsTypes = clause.getTypes();
        catchInfo = block.getSourceInfo();

        // if ($e instanceof ArgType1 or $e instanceof ArgType2 ...) {
        //   var userVar = $e; <user code>
        // }

        // Handle the first Exception type.
        JExpression ifTest = new JInstanceOf(catchInfo, (JReferenceType) exceptionsTypes.get(0),
            exceptionVariable.makeRef(catchInfo));
        // Handle the rest of the Exception types if any.
        for (int j = 1; j < exceptionsTypes.size(); j++) {
          JExpression orExp = new JInstanceOf(catchInfo, (JReferenceType) exceptionsTypes.get(j),
              exceptionVariable.makeRef(catchInfo));
          ifTest = new JBinaryOperation(catchInfo, JPrimitiveType.BOOLEAN, JBinaryOperator.OR,
              ifTest, orExp);
        }
        JDeclarationStatement declaration =
            new JDeclarationStatement(catchInfo, arg, exceptionVariable.makeRef(catchInfo));
        block.addStmt(0, declaration);
        // nest the previous as an else for me
        cur = new JIfStatement(catchInfo, ifTest, block, cur);
      }

      newCatchBlock.addStmt(cur);

      // Replace with a single catch block.
      x.getCatchClauses().clear();
      List<JType> newCatchTypes = new ArrayList<JType>(1);
      newCatchTypes.add(exceptionVariable.getType());
      x.getCatchClauses().add(new JTryStatement.CatchClause(newCatchTypes,
          exceptionVariable.makeRef(catchInfo), newCatchBlock));
    }

    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody = x;
      clearLocals();
      return true;
    }
  }

  private class UnwrapJavaScriptExceptionVisitor extends JModVisitor {
    JMethod unwrapMethod = program.getIndexedMethod(RuntimeConstants.EXCEPTIONS_UNWRAP);

    @Override
    public void endVisit(JThrowStatement x, Context ctx) {
      // throw x; -> throw Exceptions.unwrap(x);
      ctx.replaceMe(createUnwrappedThrow(x));
    }

    private JThrowStatement createUnwrappedThrow(JThrowStatement x) {
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, unwrapMethod);
      call.addArg(x.getExpr());
      return new JThrowStatement(x.getSourceInfo(), call);
    }
  }

  public static void exec(JProgram program) {
    new CatchBlockNormalizer(program).execImpl();
  }

  private JMethodBody currentMethodBody;
  private int catchVariableIndex;
  private final JProgram program;

  private CatchBlockNormalizer(JProgram program) {
    this.program = program;
  }

  private void clearLocals() {
    catchVariableIndex = 0;
  }

  private void execImpl() {
    CollapseCatchBlocks collapser = new CollapseCatchBlocks();
    collapser.accept(program);
    UnwrapJavaScriptExceptionVisitor unwrapper = new UnwrapJavaScriptExceptionVisitor();
    unwrapper.accept(program);
  }

  private JLocal newExceptionVariable(SourceInfo sourceInfo) {
    return JProgram.createLocal(sourceInfo, "$e" + catchVariableIndex++,
        program.getTypeJavaLangObject(), false, currentMethodBody);
  }
}
