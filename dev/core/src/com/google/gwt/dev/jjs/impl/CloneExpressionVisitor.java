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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBooleanLiteral;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JCharLiteral;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JDoubleLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JFloatLiteral;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JLongLiteral;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.ArrayList;

/**
 * A general purpose expression cloner.
 */
public class CloneExpressionVisitor extends JVisitor {
  private JExpression expression;
  private JProgram program;

  public CloneExpressionVisitor(JProgram program) {
    this.program = program;
  }

  public JExpression cloneExpression(JExpression expr) {
    if (expr == null) {
      return null;
    }

    this.accept(expr);

    if (expression == null) {
      throw new InternalCompilerException(expr, "Unable to clone expression",
          null);
    }

    return expression;
  }

  @Override
  public boolean visit(JAbsentArrayDimension x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JArrayRef x, Context ctx) {
    expression = new JArrayRef(program, x.getSourceInfo(),
        cloneExpression(x.getInstance()), cloneExpression(x.getIndexExpr()));
    return false;
  }

  @Override
  public boolean visit(JBinaryOperation x, Context ctx) {
    expression = new JBinaryOperation(program, x.getSourceInfo(), x.getType(),
        x.getOp(), cloneExpression(x.getLhs()), cloneExpression(x.getRhs()));
    return false;
  }

  @Override
  public boolean visit(JBooleanLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JCastOperation x, Context ctx) {
    expression = new JCastOperation(program, x.getSourceInfo(),
        x.getCastType(), cloneExpression(x.getExpr()));
    return false;
  }

  @Override
  public boolean visit(JCharLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JClassLiteral x, Context ctx) {
    expression = program.getLiteralClass(x.getRefType());
    return false;
  }

  @Override
  public boolean visit(JClassSeed x, Context ctx) {
    expression = new JClassSeed(program, x.getRefType());
    return false;
  }

  @Override
  public boolean visit(JConditional x, Context ctx) {
    expression = new JConditional(program, x.getSourceInfo(), x.getType(),
        cloneExpression(x.getIfTest()), cloneExpression(x.getThenExpr()),
        cloneExpression(x.getElseExpr()));
    return false;
  }

  @Override
  public boolean visit(JDoubleLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JFieldRef x, Context ctx) {
    expression = new JFieldRef(program, x.getSourceInfo(),
        cloneExpression(x.getInstance()), x.getField(), x.getEnclosingType());
    return false;
  }

  @Override
  public boolean visit(JFloatLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JInstanceOf x, Context ctx) {
    expression = new JInstanceOf(program, x.getSourceInfo(), x.getTestType(),
        cloneExpression(x.getExpr()));
    return false;
  }

  @Override
  public boolean visit(JIntLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JLocalRef x, Context ctx) {
    expression = new JLocalRef(program, x.getSourceInfo(), x.getLocal());
    return false;
  }

  @Override
  public boolean visit(JLongLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JMethodCall x, Context ctx) {
    JMethodCall newMethodCall = new JMethodCall(program, x.getSourceInfo(),
        cloneExpression(x.getInstance()), x.getTarget());
    if (!x.canBePolymorphic()) {
      newMethodCall.setCannotBePolymorphic();
    }

    for (JExpression arg : x.getArgs()) {
      newMethodCall.getArgs().add(cloneExpression(arg));
    }

    expression = newMethodCall;
    return false;
  }

  @Override
  public boolean visit(JMultiExpression x, Context ctx) {
    JMultiExpression multi = new JMultiExpression(program, x.getSourceInfo());
    for (JExpression expr : x.exprs) {
      multi.exprs.add(cloneExpression(expr));
    }

    expression = multi;
    return false;
  }

  @Override
  public boolean visit(JNewArray x, Context ctx) {
    JNewArray newArray = new JNewArray(program, x.getSourceInfo(),
        x.getArrayType());

    if (x.dims != null) {
      newArray.dims = new ArrayList<JExpression>();
      for (JExpression dim : x.dims) {
        newArray.dims.add(cloneExpression(dim));
      }
    }
    if (x.initializers != null) {
      newArray.initializers = new ArrayList<JExpression>();
      for (JExpression initializer : x.initializers) {
        newArray.initializers.add(cloneExpression(initializer));
      }
    }

    expression = newArray;
    return false;
  }

  @Override
  public boolean visit(JNewInstance x, Context ctx) {
    expression = new JNewInstance(program, x.getSourceInfo(), x.getClassType());
    return false;
  }

  @Override
  public boolean visit(JNullLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JParameterRef x, Context ctx) {
    expression = new JParameterRef(program, x.getSourceInfo(), x.getParameter());
    return false;
  }

  @Override
  public boolean visit(JPostfixOperation x, Context ctx) {
    expression = new JPostfixOperation(program, x.getSourceInfo(), x.getOp(),
        cloneExpression(x.getArg()));
    return false;
  }

  @Override
  public boolean visit(JPrefixOperation x, Context ctx) {
    expression = new JPrefixOperation(program, x.getSourceInfo(), x.getOp(),
        cloneExpression(x.getArg()));
    return false;
  }

  @Override
  public boolean visit(JStringLiteral x, Context ctx) {
    expression = x;
    return false;
  }

  @Override
  public boolean visit(JThisRef x, Context ctx) {
    expression = program.getExprThisRef(x.getSourceInfo(), x.getClassType());
    return false;
  }
}