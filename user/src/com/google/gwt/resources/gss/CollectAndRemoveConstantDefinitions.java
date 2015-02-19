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
import com.google.gwt.thirdparty.common.css.compiler.ast.CssDefinitionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.common.css.compiler.passes.CollectConstantDefinitions;

/**
 * A pass that collects the constant definitions inside the tree. This pass removes the constant
 * definitions during the collect.
 */
public class CollectAndRemoveConstantDefinitions extends CollectConstantDefinitions
    implements CssCompilerPass {

  private final MutatingVisitController visitController;

  public CollectAndRemoveConstantDefinitions(CssTree tree) {
    this(tree.getMutatingVisitController());
  }

  public CollectAndRemoveConstantDefinitions(MutatingVisitController visitController) {
    super(visitController);

    this.visitController = visitController;
  }

  @Override
  public boolean enterDefinition(CssDefinitionNode node) {
    super.enterDefinition(node);

    visitController.removeCurrentNode();

    return false;
  }
}
