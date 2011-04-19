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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAbsentArrayDimension;
import com.google.gwt.dev.jjs.ast.JArrayLength;
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
import com.google.gwt.dev.jjs.ast.JGwtCreate;
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
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

/**
 * See the Java Programming Language, 4th Edition, p. 750, Table 2. I just
 * numbered the table top to bottom as 0 through 14. Lower number means higher
 * precedence. I also gave primaries a precedence of 0; maybe I should have
 * started operators at 1, but in practice it won't matter since primaries can't
 * have children.
 */
class JavaPrecedenceVisitor extends JVisitor {

  public static int exec(JExpression expression) {
    JavaPrecedenceVisitor visitor = new JavaPrecedenceVisitor();
    visitor.accept(expression);
    if (visitor.answer < 0) {
      throw new InternalCompilerException("Precedence must be >= 0 (" + expression + ") "
          + expression.getClass());
    }
    return visitor.answer;
  }

  private int answer = -1;

  private JavaPrecedenceVisitor() {
  }

  @Override
  public boolean visit(JAbsentArrayDimension x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JArrayLength x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JArrayRef x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JBinaryOperation operation, Context ctx) {
    answer = operation.getOp().getPrecedence();
    return false;
  }

  @Override
  public boolean visit(JBooleanLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JCastOperation operation, Context ctx) {
    answer = 2;
    return false;
  }

  @Override
  public boolean visit(JCharLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JClassLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JConditional conditional, Context ctx) {
    answer = 13;
    return false;
  }

  @Override
  public boolean visit(JDoubleLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JFieldRef x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JFloatLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JGwtCreate x, Context ctx) {
    // It's a method call.
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JInstanceOf of, Context ctx) {
    answer = 6;
    return false;
  }

  @Override
  public boolean visit(JIntLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JLocalRef x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JLongLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JMethodCall x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JMultiExpression x, Context ctx) {
    answer = 14;
    return false;
  }

  @Override
  public boolean visit(JNewArray array, Context ctx) {
    answer = 2;
    return false;
  }

  @Override
  public boolean visit(JNewInstance instance, Context ctx) {
    answer = 2;
    return false;
  }

  @Override
  public boolean visit(JNullLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JParameterRef x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JPostfixOperation operation, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JPrefixOperation operation, Context ctx) {
    answer = 1;
    return false;
  }

  @Override
  public boolean visit(JStringLiteral x, Context ctx) {
    answer = 0;
    return false;
  }

  @Override
  public boolean visit(JThisRef x, Context ctx) {
    answer = 0;
    return false;
  }

}