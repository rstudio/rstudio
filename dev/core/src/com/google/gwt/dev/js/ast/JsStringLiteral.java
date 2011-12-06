/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.StringInterner;

/**
 * A JavaScript string literal expression.
 */
public final class JsStringLiteral extends JsValueLiteral {

  private final String value;

  public JsStringLiteral(SourceInfo sourceInfo, String value) {
    super(sourceInfo);
    this.value = StringInterner.get().intern(value);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.STRING;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean isBooleanFalse() {
    return value.length() == 0;
  }

  @Override
  public boolean isBooleanTrue() {
    return value.length() != 0;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
