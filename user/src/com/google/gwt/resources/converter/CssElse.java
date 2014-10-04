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
package com.google.gwt.resources.converter;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.resources.css.ast.HasNodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a else node in the css ast.
 */
public class CssElse extends CssNode implements HasNodes {
  private final List<CssNode> nodes = new ArrayList<CssNode>();

  @Override
  public List<CssNode> getNodes() {
    return nodes;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public void traverse(CssVisitor visitor, Context context) {
    boolean visitChildren = true;

    ExtendedCssVisitor extendedCssVisitor = visitor instanceof ExtendedCssVisitor ?
        (ExtendedCssVisitor) visitor : null;

    if (extendedCssVisitor != null) {
      visitChildren &= extendedCssVisitor.visit(this, context);
    }

    if (visitChildren) {
      visitor.acceptWithInsertRemove(nodes);
    }

    if (extendedCssVisitor != null) {
      extendedCssVisitor.endVisit(this, context);
    }
  }
}
