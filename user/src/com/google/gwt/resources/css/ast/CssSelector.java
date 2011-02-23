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

import java.util.regex.Pattern;

/**
 * An opaque view of a selector.
 */
public class CssSelector extends CssNode {
  public static final Pattern CLASS_SELECTOR_PATTERN = Pattern.compile("\\.([^ \\[:>+#.]+)");

  /*
   * TODO: Evaluate whether or not we need to have a type hierarchy of
   * selectors.
   */
  private String selector;

  public CssSelector(String selector) {
    this.selector = selector;
  }

  public String getSelector() {
    return selector;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  public void setSelector(String selector) {
    this.selector = selector;
  }

  public void traverse(CssVisitor visitor, Context context) {
    visitor.visit(this, context);
    visitor.endVisit(this, context);
  }
}
