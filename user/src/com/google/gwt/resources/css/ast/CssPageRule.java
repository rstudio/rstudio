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
 * A page rule in CSS.
 */
public class CssPageRule extends CssNode implements HasProperties {
  private final List<CssProperty> nodes = new ArrayList<CssProperty>();
  private String pseudoPage;

  public List<CssProperty> getProperties() {
    return nodes;
  }

  public String getPseudoPage() {
    return pseudoPage;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public void setPseudoPage(String pseudoPage) {
    this.pseudoPage = pseudoPage;
  }

  public void traverse(CssVisitor visitor, Context context) {
    if (visitor.visit(this, context)) {
      visitor.acceptWithInsertRemove(nodes);
    }
    visitor.endVisit(this, context);
  }
}