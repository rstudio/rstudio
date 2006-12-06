// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

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
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;

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
    expression.traverse(visitor);
    if (visitor.answer < 0) {
      throw new InternalCompilerException("Precedence must be >= 0!");
    }
    return visitor.answer;
  }

  private int answer = -1;

  private JavaPrecedenceVisitor() {
  }

  // @Override
  public boolean visit(JAbsentArrayDimension x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JArrayRef x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JBinaryOperation operation, Mutator mutator) {
    answer = operation.op.getPrecedence();
    return false;
  }

  // @Override
  public boolean visit(JBooleanLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JCastOperation operation, Mutator mutator) {
    answer = 2;
    return false;
  }

  // @Override
  public boolean visit(JCharLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JClassLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JConditional conditional, Mutator mutator) {
    answer = 13;
    return false;
  }

  // @Override
  public boolean visit(JDoubleLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JFieldRef x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JFloatLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JInstanceOf of, Mutator mutator) {
    answer = 6;
    return false;
  }

  // @Override
  public boolean visit(JIntLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JLocalRef x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JLongLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JMethodCall x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JNewArray array, Mutator mutator) {
    answer = 2;
    return false;
  }

  // @Override
  public boolean visit(JNewInstance instance, Mutator mutator) {
    answer = 2;
    return false;
  }

  // @Override
  public boolean visit(JNullLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JParameterRef x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JPostfixOperation operation, Mutator mutator) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JPrefixOperation operation, Mutator mutator) {
    answer = 1;
    return false;
  }

  // @Override
  public boolean visit(JStringLiteral x, Mutator m) {
    answer = 0;
    return false;
  }

  // @Override
  public boolean visit(JThisRef x, Mutator m) {
    answer = 0;
    return false;
  }

}