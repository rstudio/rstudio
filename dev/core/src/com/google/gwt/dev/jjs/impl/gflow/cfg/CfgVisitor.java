/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.jjs.impl.gflow.cfg;

/**
 * Visitor for all CFG nodes.
 */
public abstract class CfgVisitor {
  public void visitBinaryConditionalOperationNode(
      CfgBinaryConditionalOperationNode node) {
    visitConditionalNode(node);
  }

  public void visitBlockNode(CfgBlockNode node) {
    visitSimpleNode(node);
  }

  public void visitBreakNode(CfgBreakNode node) {
    visitGotoNode(node);
  }

  public void visitCaseNode(CfgCaseNode node) {
    visitConditionalNode(node);
  }

  public void visitConditionalExpressionNode(
      CfgConditionalExpressionNode node) {
    visitConditionalNode(node);
  }

  public void visitConditionalNode(CfgConditionalNode<?> node) {
    visitNode(node);
  }

  public void visitContinueNode(CfgContinueNode node) {
    visitGotoNode(node);
  }

  public void visitDoNode(CfgDoNode node) {
    visitConditionalNode(node);
  }

  public void visitEndNode(CfgEndNode node) {
    visitNopNode(node);
  }

  public void visitForNode(CfgForNode node) {
    visitConditionalNode(node);
  }

  public void visitGotoNode(CfgGotoNode<?> node) {
    visitNode(node);
  }

  public void visitIfNode(CfgIfNode node) {
    visitConditionalNode(node);
  }

  public void visitMethodCallNode(CfgMethodCallNode node) {
    visitSimpleNode(node);
  }

  public void visitNode(@SuppressWarnings("unused") CfgNode<?> node) {
    //
  }

  public void visitNopNode(CfgNopNode node) {
    visitNode(node);
  }

  public void visitOptionalThrowNode(CfgOptionalThrowNode node) {
    visitNode(node);
  }

  public void visitReadNode(CfgReadNode node) {
    visitSimpleNode(node);
  }

  public void visitReadWriteNode(CfgReadWriteNode node) {
    visitSimpleNode(node);
  }

  public void visitReturnNode(CfgReturnNode node) {
    visitGotoNode(node);
  }

  public void visitSimpleNode(CfgSimpleNode<?> node) {
    visitNode(node);
  }

  public void visitStatementNode(CfgStatementNode<?> node) {
    visitSimpleNode(node);
  }

  public void visitSwitchGotoNode(CfgSwitchGotoNode node) {
    visitGotoNode(node);
  }

  public void visitThrowNode(CfgThrowNode node) {
    visitNode(node);
  }

  public void visitTryNode(CfgTryNode node) {
    visitSimpleNode(node);
  }

  public void visitWhileNode(CfgWhileNode node) {
    visitConditionalNode(node);
  }

  public void visitWriteNode(CfgWriteNode node) {
    visitSimpleNode(node);
  }
}
