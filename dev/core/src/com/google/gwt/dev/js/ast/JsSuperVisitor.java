/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.js.ast.JsVars.JsVar;

/**
 * A visitor that walks up the type hierarchy. By default, if a subclass has no specific override
 * for a concrete node type, this visitor will call a visit/endVisit for its super class, and so on
 * up the type heirarchy.
 */
public class JsSuperVisitor extends JsVisitor {

  @Override
  public void endVisit(JsArrayAccess x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsArrayLiteral x, JsContext ctx) {
    endVisit((JsLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsBinaryOperation x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsBlock x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsBooleanLiteral x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsBreak x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsCase x, JsContext ctx) {
    endVisit((JsSwitchMember) x, ctx);
  }

  @Override
  public void endVisit(JsCatch x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsConditional x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsContinue x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsDebugger x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsDefault x, JsContext ctx) {
    endVisit((JsSwitchMember) x, ctx);
  }

  @Override
  public void endVisit(JsDoWhile x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsEmpty x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  public void endVisit(JsExpression x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsExprStmt x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsFor x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsForIn x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsFunction x, JsContext ctx) {
    endVisit((JsLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsIf x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsInvocation x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsLabel x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  public void endVisit(JsLiteral x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsNameOf x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsNameRef x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @Override
  public void endVisit(JsNew x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  @SuppressWarnings("unused")
  public void endVisit(JsNode x, JsContext ctx) {
  }

  @Override
  public void endVisit(JsNullLiteral x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsNumberLiteral x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsObjectLiteral x, JsContext ctx) {
    endVisit((JsLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsParameter x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsPostfixOperation x, JsContext ctx) {
    endVisit((JsUnaryOperation) x, ctx);
  }

  @Override
  public void endVisit(JsPrefixOperation x, JsContext ctx) {
    endVisit((JsUnaryOperation) x, ctx);
  }

  @Override
  public void endVisit(JsProgram x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsProgramFragment x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsPropertyInitializer x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsRegExp x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsReturn x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  public void endVisit(JsStatement x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsStringLiteral x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsSwitch x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  public void endVisit(JsSwitchMember x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsThisRef x, JsContext ctx) {
    endVisit((JsValueLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsThrow x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsTry x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  public void endVisit(JsUnaryOperation x, JsContext ctx) {
    endVisit((JsExpression) x, ctx);
  }

  public void endVisit(JsValueLiteral x, JsContext ctx) {
    endVisit((JsLiteral) x, ctx);
  }

  @Override
  public void endVisit(JsVar x, JsContext ctx) {
    endVisit((JsNode) x, ctx);
  }

  @Override
  public void endVisit(JsVars x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public void endVisit(JsWhile x, JsContext ctx) {
    endVisit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsArrayAccess x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return visit((JsLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsBlock x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsBooleanLiteral x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsBreak x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsCase x, JsContext ctx) {
    return visit((JsSwitchMember) x, ctx);
  }

  @Override
  public boolean visit(JsCatch x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsConditional x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsContinue x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsDebugger x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsDefault x, JsContext ctx) {
    return visit((JsSwitchMember) x, ctx);
  }

  @Override
  public boolean visit(JsDoWhile x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsEmpty x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  public boolean visit(JsExpression x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsExprStmt x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsFor x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsForIn x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsFunction x, JsContext ctx) {
    return visit((JsLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsIf x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsInvocation x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsLabel x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  public boolean visit(JsLiteral x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsNameOf x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsNameRef x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @Override
  public boolean visit(JsNew x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  @SuppressWarnings("unused")
  public boolean visit(JsNode x, JsContext ctx) {
    return true;
  }

  @Override
  public boolean visit(JsNullLiteral x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsNumberLiteral x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return visit((JsLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsParameter x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsPostfixOperation x, JsContext ctx) {
    return visit((JsUnaryOperation) x, ctx);
  }

  @Override
  public boolean visit(JsPrefixOperation x, JsContext ctx) {
    return visit((JsUnaryOperation) x, ctx);
  }

  @Override
  public boolean visit(JsProgram x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsProgramFragment x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsPropertyInitializer x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsRegExp x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsReturn x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  public boolean visit(JsStatement x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsStringLiteral x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsSwitch x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  public boolean visit(JsSwitchMember x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsThisRef x, JsContext ctx) {
    return visit((JsValueLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsThrow x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsTry x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  public boolean visit(JsUnaryOperation x, JsContext ctx) {
    return visit((JsExpression) x, ctx);
  }

  public boolean visit(JsValueLiteral x, JsContext ctx) {
    return visit((JsLiteral) x, ctx);
  }

  @Override
  public boolean visit(JsVar x, JsContext ctx) {
    return visit((JsNode) x, ctx);
  }

  @Override
  public boolean visit(JsVars x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

  @Override
  public boolean visit(JsWhile x, JsContext ctx) {
    return visit((JsStatement) x, ctx);
  }

}