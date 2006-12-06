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
package com.google.gwt.dev.js.ast;

/**
 * Implemented by nodes that will visit child nodes.
 */
public interface JsVisitor {

  void endVisit(JsArrayAccess x);

  void endVisit(JsArrayLiteral x);

  void endVisit(JsBinaryOperation x);

  void endVisit(JsBlock x);

  void endVisit(JsBooleanLiteral x);

  void endVisit(JsBreak x);

  void endVisit(JsCase x);

  void endVisit(JsCatch x);

  void endVisit(JsConditional x);

  void endVisit(JsContinue x);

  void endVisit(JsDecimalLiteral x);

  void endVisit(JsDefault x);

  void endVisit(JsDelete x);

  void endVisit(JsDoWhile while1);

  void endVisit(JsEmpty x);

  void endVisit(JsExprStmt x);

  void endVisit(JsFor x);

  void endVisit(JsForIn x);

  void endVisit(JsFunction x);

  void endVisit(JsIf x);

  void endVisit(JsIntegralLiteral x);

  void endVisit(JsInvocation x);

  void endVisit(JsLabel x);

  void endVisit(JsNameRef x);

  void endVisit(JsNew x);

  void endVisit(JsNullLiteral x);

  void endVisit(JsObjectLiteral x);

  void endVisit(JsParameter x);

  void endVisit(JsParameters x);

  void endVisit(JsPostfixOperation x);

  void endVisit(JsPrefixOperation x);

  void endVisit(JsProgram x);

  void endVisit(JsPropertyInitializer x);

  void endVisit(JsRegExp x);

  void endVisit(JsReturn x);

  void endVisit(JsStringLiteral x);

  void endVisit(JsSwitch x);

  void endVisit(JsThisRef x);

  void endVisit(JsThrow x);

  void endVisit(JsTry x);

  void endVisit(JsVars x);

  void endVisit(JsWhile x);

  boolean visit(JsArrayAccess x);

  boolean visit(JsArrayLiteral x);

  boolean visit(JsBinaryOperation x);

  boolean visit(JsBlock x);

  boolean visit(JsBooleanLiteral x);

  boolean visit(JsBreak x);

  boolean visit(JsCase x);

  boolean visit(JsCatch x);

  boolean visit(JsConditional x);

  boolean visit(JsContinue x);

  boolean visit(JsDecimalLiteral x);

  boolean visit(JsDefault x);

  boolean visit(JsDelete x);

  boolean visit(JsDoWhile x);

  boolean visit(JsEmpty x);

  boolean visit(JsExprStmt x);

  boolean visit(JsFor x);

  boolean visit(JsForIn x);

  boolean visit(JsFunction x);

  boolean visit(JsIf x);

  boolean visit(JsIntegralLiteral x);

  boolean visit(JsInvocation x);

  boolean visit(JsLabel x);

  boolean visit(JsNameRef x);

  boolean visit(JsNew x);

  boolean visit(JsNullLiteral x);

  boolean visit(JsObjectLiteral x);

  boolean visit(JsParameter x);

  boolean visit(JsParameters x);

  boolean visit(JsPostfixOperation x);

  boolean visit(JsPrefixOperation x);

  boolean visit(JsProgram x);

  boolean visit(JsPropertyInitializer x);

  boolean visit(JsRegExp x);

  boolean visit(JsReturn x);

  boolean visit(JsStringLiteral x);

  boolean visit(JsSwitch x);

  boolean visit(JsThisRef x);

  boolean visit(JsThrow x);

  boolean visit(JsTry x);

  boolean visit(JsVars x);

  boolean visit(JsWhile x);
}
