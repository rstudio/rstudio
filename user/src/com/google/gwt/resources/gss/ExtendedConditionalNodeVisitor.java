/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.resources.gss;

import com.google.gwt.thirdparty.common.css.compiler.ast.CssBooleanExpressionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssConditionalRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.DefaultTreeVisitor;

import java.util.Stack;

/**
 * GSS doesn't visit the children of a CssConditionalRuleNode. The role of this class
 * is to implement this behavior.
 */
public class ExtendedConditionalNodeVisitor  extends DefaultTreeVisitor {

  private final Stack<CssBooleanExpressionNode> childrenStack;

  public ExtendedConditionalNodeVisitor() {
    childrenStack = new Stack<CssBooleanExpressionNode>();
  }

  @Override
  public boolean enterConditionalRule(CssConditionalRuleNode node) {
    // unfortunately the VisitController doesn't visit the children of a CssConditionalRuleNode
    for (CssValueNode children : node.getChildren()) {
      if (children instanceof CssBooleanExpressionNode) {
        childrenStack.push((CssBooleanExpressionNode) children);
      }
    }

    while (!childrenStack.isEmpty()) {
      CssBooleanExpressionNode visitingNode = childrenStack.pop();
      enterBooleanExpression(visitingNode);

      if (visitingNode.getLeft() != null) {
        childrenStack.push(visitingNode.getLeft());
      }
      if (visitingNode.getRight() != null) {
        childrenStack.push(visitingNode.getRight());
      }
    }

    return true;
  }

  public void enterBooleanExpression(CssBooleanExpressionNode node) {
  }
}
