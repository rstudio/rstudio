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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Replace cast and instanceof operations with calls to the Cast class.
 */
public class JavaScriptObjectCaster {

  /**
   * Synthesize casts from JavaScriptObjects to trigger wrapping.
   */
  private class AssignmentVisitor extends JModVisitor {

    private JMethod currentMethod;

    // @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment()) {
        JType lhsType = x.getLhs().getType();
        JExpression newRhs = checkAndReplaceJso(x.getRhs(), lhsType);
        if (newRhs != x.getRhs()) {
          JBinaryOperation asg = new JBinaryOperation(program,
              x.getSourceInfo(), lhsType, x.getOp(), x.getLhs(), newRhs);
          ctx.replaceMe(asg);
        }
      }
    }

    // @Override
    public void endVisit(JConditional x, Context ctx) {
      JExpression newThen = checkAndReplaceJso(x.getThenExpr(), x.getType());
      JExpression newElse = checkAndReplaceJso(x.getElseExpr(), x.getType());
      if (newThen != x.getThenExpr() || newElse != x.getElseExpr()) {
        JConditional newCond = new JConditional(program, x.getSourceInfo(),
            x.getType(), x.getIfTest(), newThen, newElse);
        ctx.replaceMe(newCond);
      }
    }

    // @Override
    public void endVisit(JLocalDeclarationStatement x, Context ctx) {
      JExpression newInst = x.getInitializer();
      if (newInst != null) {
        newInst = checkAndReplaceJso(newInst, x.getLocalRef().getType());
        if (newInst != x.getInitializer()) {
          JLocalDeclarationStatement newStmt = new JLocalDeclarationStatement(
              program, x.getSourceInfo(), x.getLocalRef(), newInst);
          ctx.replaceMe(newStmt);
        }
      }
    }

    // @Override
    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      for (int i = 0; i < x.getTarget().params.size(); ++i) {
        JParameter param = (JParameter) x.getTarget().params.get(i);
        JExpression newArg = checkAndReplaceJso(
            (JExpression) x.getArgs().get(i), param.getType());
        x.getArgs().set(i, newArg);
      }
      if (!x.getTarget().isStatic()) {
        // for polymorphic calls, force wrapping
        JExpression newInst = checkAndReplaceJso(x.getInstance(),
            program.getTypeJavaLangObject());
        if (newInst != x.getInstance()) {
          JMethodCall newCall = new JMethodCall(program, x.getSourceInfo(),
              newInst, x.getTarget(), x.isStaticDispatchOnly());
          newCall.getArgs().addAll(x.getArgs());
          ctx.replaceMe(newCall);
        }
      }
    }

    // @Override
    public void endVisit(JReturnStatement x, Context ctx) {
      if (x.getExpr() != null) {
        JExpression newExpr = checkAndReplaceJso(x.getExpr(),
            currentMethod.getType());
        if (newExpr != x.getExpr()) {
          JReturnStatement newStmt = new JReturnStatement(program,
              x.getSourceInfo(), newExpr);
          ctx.replaceMe(newStmt);
        }
      }
    }

    // @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }

    private JExpression checkAndReplaceJso(JExpression arg, JType targetType) {
      JType argType = arg.getType();
      if (argType == targetType) {
        return arg;
      }

      if (!(targetType instanceof JReferenceType)) {
        return arg;
      }

      if (!program.isJavaScriptObject(argType)) {
        return arg;
      }
      // Synthesize a cast to the target type
      JCastOperation cast = new JCastOperation(program, arg.getSourceInfo(),
          targetType, arg);
      return cast;
    }
  }

  public static void exec(JProgram program) {
    new JavaScriptObjectCaster(program).execImpl();
  }

  private final JProgram program;

  private JavaScriptObjectCaster(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    AssignmentVisitor visitor = new AssignmentVisitor();
    visitor.accept(program);
  }

}
