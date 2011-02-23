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

import com.google.gwt.resources.css.ast.CssProperty.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * A constant definition.
 */
public class CssDef extends CssNode {
  private final String key;
  private final List<Value> values = new ArrayList<Value>();

  public CssDef(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public List<Value> getValues() {
    return values;
  }
  
  @Override
  public boolean isStatic() {
    return true;
  }

  public void traverse(CssVisitor visitor, Context context) {
    visitor.visit(this, context);
    visitor.endVisit(this, context);
  }
}
