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
 * An abstract representation of a CSS stylesheet.
 */
public class CssStylesheet extends CssNode implements HasNodes {
  private List<CssNode> rules = new ArrayList<CssNode>();

  public CssStylesheet() {
  }

  /**
   * A copy constructor that will clone the contents of an existing
   * CssStylesheet.
   */
  public CssStylesheet(CssStylesheet other) {
    append(other);
  }

  /**
   * Append the given stylesheet. The contents of the other stylesheet will be
   * cloned.
   */
  public void append(CssStylesheet other) {
    rules.addAll(CssNodeCloner.clone(CssNode.class, other.rules));
  }

  public List<CssNode> getNodes() {
    return rules;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public void traverse(CssVisitor visitor, Context context) {
    if (visitor.visit(this, context)) {
      visitor.acceptWithInsertRemove(rules);
    }
    visitor.endVisit(this, context);
  }
}
