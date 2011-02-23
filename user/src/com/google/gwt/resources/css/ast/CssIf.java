/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.resources.css.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A GWTCSS if statement. The elif and else constructs are modeled as nested if
 * statement is the elseNodes.
 */
public class CssIf extends CssNode implements CssSubstitution, HasNodes {
  private final List<CssNode> elseNodes = new ArrayList<CssNode>();
  private final List<CssNode> nodes = new ArrayList<CssNode>();
  private String expression;
  private boolean isNegated;
  private String property;
  private String[] propertyValues;

  public List<CssNode> getElseNodes() {
    return elseNodes;
  }

  public String getExpression() {
    return expression;
  }

  public List<CssNode> getNodes() {
    return nodes;
  }

  public String getPropertyName() {
    return property;
  }

  public String[] getPropertyValues() {
    return propertyValues;
  }

  public boolean isNegated() {
    return isNegated;
  }

  /**
   * A CssIf is static if it uses only a deferred-binding property.
   */
  @Override
  public boolean isStatic() {
    return expression == null;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public void setNegated(boolean isNegated) {
    this.isNegated = isNegated;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void setPropertyValues(String[] propertyValues) {
    this.propertyValues = propertyValues;
  }

  public void traverse(CssVisitor visitor, Context context) {
    if (visitor.visit(this, context)) {
      visitor.acceptWithInsertRemove(nodes);
      visitor.acceptWithInsertRemove(elseNodes);
    }
    visitor.endVisit(this, context);
  }
}
