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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDecimalLiteral;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDelete;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsIntegralLiteral;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsParameters;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.js.ast.JsWhile;

/**
 * Implements stubs for the <code>visit()</code> interface methods.
 */
public abstract class JsAbstractVisitorWithVisits implements JsVisitor {

  public boolean visit(JsArrayAccess x) {
    return true;
  }

  public boolean visit(JsArrayLiteral x) {
    return true;
  }

  public boolean visit(JsBinaryOperation x) {
    return true;
  }

  public boolean visit(JsBlock x) {
    return true;
  }

  public boolean visit(JsBooleanLiteral x) {
    return true;
  }

  public boolean visit(JsBreak x) {
    return true;
  }

  public boolean visit(JsCase x) {
    return true;
  }

  public boolean visit(JsCatch x) {
    return true;
  }

  public boolean visit(JsConditional x) {
    return true;
  }

  public boolean visit(JsContinue x) {
    return true;
  }

  public boolean visit(JsDecimalLiteral x) {
    return true;
  }

  public boolean visit(JsDefault x) {
    return true;
  }

  public boolean visit(JsDelete x) {
    return true;
  }

  public boolean visit(JsDoWhile x) {
    return true;
  }

  public boolean visit(JsEmpty x) {
    return true;
  }

  public boolean visit(JsExprStmt x) {
    return true;
  }

  public boolean visit(JsFor x) {
    return true;
  }

  public boolean visit(JsForIn x) {
    return true;
  }

  public boolean visit(JsFunction x) {
    return true;
  }

  public boolean visit(JsIf x) {
    return true;
  }

  public boolean visit(JsIntegralLiteral x) {
    return true;
  }

  public boolean visit(JsInvocation x) {
    return true;
  }

  public boolean visit(JsLabel x) {
    return true;
  }

  public boolean visit(JsNameRef x) {
    return true;
  }

  public boolean visit(JsNew x) {
    return true;
  }

  public boolean visit(JsNullLiteral x) {
    return true;
  }

  public boolean visit(JsObjectLiteral x) {
    return true;
  }

  public boolean visit(JsParameter x) {
    return true;
  }

  public boolean visit(JsParameters x) {
    return true;
  }

  public boolean visit(JsPostfixOperation x) {
    return true;
  }

  public boolean visit(JsPrefixOperation x) {
    return true;
  }

  public boolean visit(JsProgram x) {
    return true;
  }

  public boolean visit(JsPropertyInitializer x) {
    return true;
  }

  public boolean visit(JsRegExp x) {
    return true;
  }

  public boolean visit(JsReturn x) {
    return true;
  }

  public boolean visit(JsStringLiteral x) {
    return true;
  }

  public boolean visit(JsSwitch x) {
    return true;
  }

  public boolean visit(JsThisRef x) {
    return true;
  }

  public boolean visit(JsThrow x) {
    return true;
  }

  public boolean visit(JsTry x) {
    return true;
  }

  public boolean visit(JsVars x) {
    return true;
  }

  public boolean visit(JsWhile x) {
    return true;
  }

}
