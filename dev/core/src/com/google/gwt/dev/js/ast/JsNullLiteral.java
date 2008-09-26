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
 * A JavaScript null literal.
 */
public final class JsNullLiteral extends JsValueLiteral {

  // Should only be instantiated in JsProgram
  JsNullLiteral(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  public boolean isBooleanFalse() {
    return true;
  }

  public boolean isBooleanTrue() {
    return false;
  }

  public boolean isDefinitelyNotNull() {
    return false;
  }

  public boolean isDefinitelyNull() {
    return true;
  }

  public void traverse(JsVisitor v, JsContext<JsExpression> ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
