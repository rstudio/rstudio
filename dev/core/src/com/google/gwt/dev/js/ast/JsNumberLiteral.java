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
package com.google.gwt.dev.js.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Represents a JavaScript literal decimal expression.
 */
public final class JsNumberLiteral extends JsValueLiteral {

  private final double value;

  // Should be interned by JsProgram
  JsNumberLiteral(SourceInfo sourceInfo, double value) {
    super(sourceInfo);
    this.value = value;
  }

  public double getValue() {
    return value;
  }

  public boolean isBooleanFalse() {
    return value == 0.0;
  }

  public boolean isBooleanTrue() {
    return value != 0.0;
  }

  public boolean isDefinitelyNotNull() {
    return true;
  }

  public boolean isDefinitelyNull() {
    return false;
  }

  public void traverse(JsVisitor v, JsContext<JsExpression> ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
