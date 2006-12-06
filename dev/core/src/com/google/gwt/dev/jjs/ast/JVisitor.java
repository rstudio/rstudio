// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.ast.js.JClassSeed;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;
import com.google.gwt.dev.jjs.ast.js.JsniFieldRef;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
import com.google.gwt.dev.jjs.ast.js.JsniMethodRef;
import com.google.gwt.dev.jjs.ast.js.JsonArray;
import com.google.gwt.dev.jjs.ast.js.JsonObject;
import com.google.gwt.dev.jjs.ast.js.JsonObject.JsonPropInit;

/**
 * A visitor for iterating through the parse tree.
 */
public class JVisitor {

  public void endVisit(JAbsentArrayDimension x, Mutator m) {
  }

  public void endVisit(JArrayRef x, Mutator m) {
  }

  public void endVisit(JArrayType x) {
  }

  public void endVisit(JAssertStatement x) {
  }

  public void endVisit(JBinaryOperation x, Mutator m) {
  }

  public void endVisit(JBlock x) {
  }

  public void endVisit(JBooleanLiteral x, Mutator m) {
  }

  public void endVisit(JBreakStatement x) {
  }

  public void endVisit(JCaseStatement x) {
  }

  public void endVisit(JCastOperation x, Mutator m) {
  }

  public void endVisit(JCharLiteral x, Mutator m) {
  }

  public void endVisit(JClassLiteral x, Mutator m) {
  }

  public void endVisit(JClassSeed x, Mutator m) {
  }

  public void endVisit(JClassType x) {
  }

  public void endVisit(JConditional x, Mutator m) {
  }

  public void endVisit(JContinueStatement x) {
  }

  public void endVisit(JDoStatement x) {
  }

  public void endVisit(JDoubleLiteral x, Mutator m) {
  }

  public void endVisit(JExpressionStatement x) {
  }

  public void endVisit(JField x) {
  }

  public void endVisit(JFieldRef x, Mutator m) {
  }

  public void endVisit(JFloatLiteral x, Mutator m) {
  }

  public void endVisit(JForStatement x) {
  }

  public void endVisit(JIfStatement x) {
  }

  public void endVisit(JInstanceOf x, Mutator m) {
  }

  public void endVisit(JInterfaceType x) {
  }

  public void endVisit(JIntLiteral x, Mutator m) {
  }

  public void endVisit(JLabel x) {
  }

  public void endVisit(JLabeledStatement x) {
  }

  public void endVisit(JLocal x) {
  }

  public void endVisit(JLocalDeclarationStatement x) {
  }

  public void endVisit(JLocalRef x, Mutator m) {
  }

  public void endVisit(JLongLiteral x, Mutator m) {
  }

  public void endVisit(JMethod x) {
  }

  public void endVisit(JMethodCall x, Mutator m) {
  }

  public void endVisit(JMultiExpression x, Mutator m) {
  }

  public void endVisit(JNewArray x, Mutator m) {
  }

  public void endVisit(JNewInstance x, Mutator m) {
  }

  public void endVisit(JNullLiteral x, Mutator m) {
  }

  public void endVisit(JNullType x) {
  }

  public void endVisit(JParameter x) {
  }

  public void endVisit(JParameterRef x, Mutator m) {
  }

  public void endVisit(JPostfixOperation x, Mutator m) {
  }

  public void endVisit(JPrefixOperation x, Mutator m) {
  }

  public void endVisit(JPrimitiveType x) {
  }

  public void endVisit(JProgram x) {
  }

  public void endVisit(JReturnStatement x) {
  }

  public void endVisit(JsniFieldRef x) {
  }

  public void endVisit(JsniMethod x) {
  }

  public void endVisit(JsniMethodRef x) {
  }

  public void endVisit(JsonArray x, Mutator m) {
  }

  public void endVisit(JsonObject x, Mutator m) {
  }

  public void endVisit(JsonPropInit x) {
  }

  public void endVisit(JStringLiteral x, Mutator m) {
  }

  public void endVisit(JSwitchStatement x) {
  }

  public void endVisit(JThisRef x, Mutator m) {
  }

  public void endVisit(JThrowStatement x) {
  }

  public void endVisit(JTryStatement x) {
  }

  public void endVisit(JWhileStatement x) {
  }

  public boolean visit(JAbsentArrayDimension x, Mutator m) {
    return true;
  }

  public boolean visit(JArrayRef x, Mutator m) {
    return true;
  }

  public boolean visit(JArrayType x) {
    return true;
  }

  public boolean visit(JAssertStatement x) {
    return true;
  }

  public boolean visit(JBinaryOperation x, Mutator m) {
    return true;
  }

  public boolean visit(JBlock x) {
    return true;
  }

  public boolean visit(JBooleanLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JBreakStatement x) {
    return true;
  }

  public boolean visit(JCaseStatement x) {
    return true;
  }

  public boolean visit(JCastOperation x, Mutator m) {
    return true;
  }

  public boolean visit(JCharLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JClassLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JClassSeed x, Mutator m) {
    return true;
  }

  public boolean visit(JClassType x) {
    return true;
  }

  public boolean visit(JConditional x, Mutator m) {
    return true;
  }

  public boolean visit(JContinueStatement x) {
    return true;
  }

  public boolean visit(JDoStatement x) {
    return true;
  }

  public boolean visit(JDoubleLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JExpressionStatement x) {
    return true;
  }

  public boolean visit(JField x) {
    return true;
  }

  public boolean visit(JFieldRef x, Mutator m) {
    return true;
  }

  public boolean visit(JFloatLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JForStatement x) {
    return true;
  }

  public boolean visit(JIfStatement x) {
    return true;
  }

  public boolean visit(JInstanceOf x, Mutator m) {
    return true;
  }

  public boolean visit(JInterfaceType x) {
    return true;
  }

  public boolean visit(JIntLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JLabel x) {
    return true;
  }

  public boolean visit(JLabeledStatement x) {
    return true;
  }

  public boolean visit(JLocal x) {
    return true;
  }

  public boolean visit(JLocalDeclarationStatement x) {
    return true;
  }

  public boolean visit(JLocalRef x, Mutator m) {
    return true;
  }

  public boolean visit(JLongLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JMethod x) {
    return true;
  }

  public boolean visit(JMethodCall x, Mutator m) {
    return true;
  }

  public boolean visit(JMultiExpression x, Mutator m) {
    return true;
  }

  public boolean visit(JNewArray x, Mutator m) {
    return true;
  }

  public boolean visit(JNewInstance x, Mutator m) {
    return true;
  }

  public boolean visit(JNullLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JNullType x) {
    return true;
  }

  public boolean visit(JParameter x) {
    return true;
  }

  public boolean visit(JParameterRef x, Mutator m) {
    return true;
  }

  public boolean visit(JPostfixOperation x, Mutator m) {
    return true;
  }

  public boolean visit(JPrefixOperation x, Mutator m) {
    return true;
  }

  public boolean visit(JPrimitiveType x) {
    return true;
  }

  public boolean visit(JProgram x) {
    return true;
  }

  public boolean visit(JReturnStatement x) {
    return true;
  }

  public boolean visit(JsniFieldRef x) {
    return true;
  }

  public boolean visit(JsniMethod x) {
    return true;
  }

  public boolean visit(JsniMethodRef x) {
    return true;
  }

  public boolean visit(JsonArray x, Mutator m) {
    return true;
  }

  public boolean visit(JsonObject x, Mutator m) {
    return true;
  }

  public boolean visit(JsonPropInit x) {
    return true;
  }

  public boolean visit(JStringLiteral x, Mutator m) {
    return true;
  }

  public boolean visit(JSwitchStatement x) {
    return true;
  }

  public boolean visit(JThisRef x, Mutator m) {
    return true;
  }

  public boolean visit(JThrowStatement x) {
    return true;
  }

  public boolean visit(JTryStatement x) {
    return true;
  }

  public boolean visit(JWhileStatement x) {
    return true;
  }

}
