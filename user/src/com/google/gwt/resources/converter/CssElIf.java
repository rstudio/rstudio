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
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssVisitor;

/**
 * Represents a else if node in the css ast.
 */
public class CssElIf extends CssIf {

  public CssElIf(CssIf originalNode) {
    doClone(originalNode);
  }

  private void doClone(CssIf originalNode) {
    if (originalNode == null) {
      throw new IllegalArgumentException("originalNode cannot be null");
    }

    getNodes().addAll(originalNode.getNodes());
    setExpression(originalNode.getExpression());
    setNegated(originalNode.isNegated());
    setProperty(originalNode.getPropertyName());
    setPropertyValues(originalNode.getPropertyValues());
  }

  @Override
  public void traverse(CssVisitor visitor, Context context) {
    boolean visitChildren;
    ExtendedCssVisitor extendedCssVisitor = visitor instanceof ExtendedCssVisitor ?
        (ExtendedCssVisitor) visitor : null;

    if (extendedCssVisitor != null) {
      visitChildren = extendedCssVisitor.visit(this, context);
    } else {
      visitChildren = visitor.visit(this, context);
    }

    if (visitChildren) {
      visitor.acceptWithInsertRemove(getNodes());
    }

    if (extendedCssVisitor != null) {
      extendedCssVisitor.endVisit(this, context);
    } else {
      visitor.endVisit(this, context);
    }
  }
}
