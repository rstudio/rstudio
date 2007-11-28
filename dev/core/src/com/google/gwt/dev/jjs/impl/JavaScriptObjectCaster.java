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
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JLocalDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JReturnStatement;
import com.google.gwt.dev.jjs.ast.JType;

import java.util.ArrayList;
import java.util.List;

/**
 * Synthesize casts for JavaScriptObject types in cases where dynamic type
 * information may be necessary.
 */
public class JavaScriptObjectCaster {

  /**
   * Synthesize casts from JavaScriptObjects to trigger wrapping.
   */
  private class AssignmentVisitor extends JModVisitor {

    private JMethod currentMethod;

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.isAssignment()) {
        JType lhsType = x.getLhs().getType();
        JExpression newRhs = checkAndReplaceJso(x.getRhs(), lhsType);
        if (newRhs == x.getRhs()) {
          // There's another case to check: if we have an array store that may
          // trigger a type check, we need to wrap the rhs.
          if (x.getLhs() instanceof JArrayRef) {
            newRhs = checkAndReplaceJsoArrayStore(newRhs, lhsType);
          }
        }
        if (newRhs != x.getRhs()) {
          JBinaryOperation asg = new JBinaryOperation(program,
              x.getSourceInfo(), lhsType, x.getOp(), x.getLhs(), newRhs);
          ctx.replaceMe(asg);
        }
      }
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      JExpression newThen = checkAndReplaceJso(x.getThenExpr(), x.getType());
      JExpression newElse = checkAndReplaceJso(x.getElseExpr(), x.getType());
      if (newThen != x.getThenExpr() || newElse != x.getElseExpr()) {
        JConditional newCond = new JConditional(program, x.getSourceInfo(),
            x.getType(), x.getIfTest(), newThen, newElse);
        ctx.replaceMe(newCond);
      }
    }

    @Override
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

    @Override
    public void endVisit(JMethod x, Context ctx) {
      currentMethod = null;
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {
      // Check implicit assignments from argument and instance passing.

      ArrayList<JExpression> args = x.getArgs();
      JMethod target = x.getTarget();
      /*
       * Check arguments for calls to non-native methods. Do not check native
       * invocations because we're losing all static type information anyway.
       */
      if (!target.isNative()) {
        for (int i = 0; i < target.params.size(); ++i) {
          JParameter param = target.params.get(i);
          JExpression arg = args.get(i);
          JExpression newArg = checkAndReplaceJso(arg, param.getType());
          this.didChange |= (newArg != arg);
          args.set(i, newArg);
        }
      }

      /*
       * Virtual calls *require* wrapping to dispatch correctly. This should be
       * a rare case since most call should get statically resolved already.
       */
      if (!target.isStatic()) {
        JExpression newInst = checkAndReplaceJso(x.getInstance(),
            program.getTypeJavaLangObject());
        if (newInst != x.getInstance()) {
          JMethodCall newCall = new JMethodCall(program, x.getSourceInfo(),
              newInst, target, x.isStaticDispatchOnly());
          newCall.getArgs().addAll(args);
          ctx.replaceMe(newCall);
        }
      }
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      List<JExpression> initializers = x.initializers;
      if (initializers != null) {
        for (int i = 0; i < initializers.size(); ++i) {
          JExpression intializer = initializers.get(i);
          JExpression newInitializer = checkAndReplaceJsoArrayStore(intializer,
              x.getArrayType().getLeafType());
          if (intializer != newInitializer) {
            initializers.set(i, newInitializer);
            this.didChange = true;
          }
        }
      }
    }

    @Override
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

    @Override
    public boolean visit(JMethod x, Context ctx) {
      currentMethod = x;
      return true;
    }

    /**
     * Wraps a JSO-typed argument if the target type is a different type.
     */
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
      // Synthesize a cast to the arg type to force a wrap
      JCastOperation cast = new JCastOperation(program, arg.getSourceInfo(),
          argType, arg);
      return cast;
    }

    /**
     * Wraps a JSO-typed argument.
     * 
     * TODO: We could eliminate casts cases where the array instance was never
     * cast to a weaker type.
     */
    private JExpression checkAndReplaceJsoArrayStore(JExpression arg,
        JType targetType) {
      if (!(targetType instanceof JReferenceType)) {
        return arg;
      }
      if (!program.isJavaScriptObject(arg.getType())) {
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
