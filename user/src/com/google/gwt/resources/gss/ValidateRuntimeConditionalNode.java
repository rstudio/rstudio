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

import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompilerPass;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssConditionalRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssDefinitionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssUnknownAtRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.DefaultTreeVisitor;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.VisitController;

/**
 * Visitor that validates runtime conditional node.
 * <p/>
 * Runtime conditional node shouldn't contain any constant definitions nor external at-rule.
 */
public class ValidateRuntimeConditionalNode extends DefaultTreeVisitor implements
    CssCompilerPass {

  private final VisitController visitController;
  private final ErrorManager errorManager;
  private final boolean lenient;

  private int cssConditionalRuleNodes;

  public ValidateRuntimeConditionalNode(VisitController visitController,
      ErrorManager errorManager, boolean lenient) {
    this.visitController = visitController;
    this.errorManager = errorManager;
    this.lenient = lenient;
  }

  @Override
  public boolean enterDefinition(CssDefinitionNode node) {
    if (inConditionalRule()) {
      if (lenient) {
        errorManager.reportWarning(new GssError("You should not define a constant inside a " +
            "ConditionalNode that will be evaluated at runtime. This will be disallowed in " +
            "the next version of GWT.", node.getSourceCodeLocation()));
      } else {
        errorManager.report(new GssError("You cannot define a constant inside a ConditionalNode " +
            "that will be evaluated at runtime.", node.getSourceCodeLocation()));
      }
    }
    return false;
  }

  @Override
  public boolean enterUnknownAtRule(CssUnknownAtRuleNode node) {
    if (inConditionalRule() && "external".equals(node.getName().getValue())) {
      if (lenient) {
        errorManager.reportWarning(new GssError("You should not define a external at-rule inside" +
            " a  ConditionalNode that will be evaluated at runtime. This will be disallowed in " +
            "the next version of GWT.", node.getSourceCodeLocation()));
      } else {
        errorManager.report(new GssError("You cannot define a external at-rule inside a " +
            "ConditionalNode that will be evaluated at runtime.", node.getSourceCodeLocation()));
      }
    }
    return super.enterUnknownAtRule(node);
  }

  @Override
  public boolean enterConditionalRule(CssConditionalRuleNode node) {
    cssConditionalRuleNodes++;
    return true;
  }

  @Override
  public void leaveConditionalRule(CssConditionalRuleNode node) {
    cssConditionalRuleNodes--;
  }

  @Override
  public void runPass() {
    cssConditionalRuleNodes = 0;

    visitController.startVisit(this);
  }

  private boolean inConditionalRule() {
    return cssConditionalRuleNodes > 0;
  }
}
