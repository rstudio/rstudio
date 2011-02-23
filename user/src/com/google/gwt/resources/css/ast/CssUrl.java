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

import com.google.gwt.resources.css.ast.CssProperty.DotPathValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;

import java.util.Collections;
import java.util.List;

/**
 * A reference to a DataResource that results in a url expression being inserted
 * into the generated CSS.
 */
public class CssUrl extends CssDef {
  private final List<Value> values;

  public CssUrl(String key, String functionPath) {
    this(key, new DotPathValue(functionPath));
  }

  public CssUrl(String key, DotPathValue functionValue) {
    super(key);
    values = Collections.<Value> singletonList(functionValue);
  }

  @Override
  public List<Value> getValues() {
    return values;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  @Override
  public void traverse(CssVisitor visitor, Context context) {
    visitor.visit(this, context);
    visitor.endVisit(this, context);
  }
}
