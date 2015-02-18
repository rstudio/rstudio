/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.thirdparty.common.css.compiler.ast.CssConstantReferenceNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssDefinitionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.DefaultTreeVisitor;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.common.css.compiler.passes.CollectConstantDefinitions;
import com.google.gwt.thirdparty.common.css.compiler.passes.ConstantDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * A compiler pass that resolves constants to their values.
 */
public class ConstantResolver extends DefaultTreeVisitor implements CssCompilerPass {

  private final MutatingVisitController visitController;
  private CssTree cssTree;
  private ConstantDefinitions constantDefinitions;

  public ConstantResolver(CssTree cssTree, MutatingVisitController visitController) {
    this.cssTree = cssTree;
    this.visitController = visitController;
  }

  @Override
  public boolean enterDefinition(CssDefinitionNode node) {
    List<CssValueNode> newParameters = new ArrayList<>();
    for (CssValueNode parameter : node.getParameters()) {
      if (parameter instanceof CssConstantReferenceNode) {
        CssConstantReferenceNode cssConstantParameter = (CssConstantReferenceNode) parameter;
        String constantName = cssConstantParameter.getValue();
        // Retrieve the actual value of this constant
        CssDefinitionNode constantDefinition = constantDefinitions.getConstantDefinition(constantName);
        newParameters.addAll(constantDefinition.getParameters());
      } else {
        // Here we are just collecting unchanged parameters so they will get readded
        // to the parameter list
        newParameters.add(parameter);
      }
    }
    node.setParameters(newParameters);
    return true;
  }

  @Override
  public void runPass() {
    CollectConstantDefinitions constantDefinitionsCollector =
        new CollectConstantDefinitions(cssTree);
    constantDefinitionsCollector.runPass();
    constantDefinitions = constantDefinitionsCollector.getConstantDefinitions();

    visitController.startVisit(this);
  }
}
