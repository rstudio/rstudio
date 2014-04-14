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
import com.google.gwt.dev.jjs.SourceOrigin;

/**
 * A JavaScript null literal.
 */
public final class JsNullLiteral extends JsValueLiteral {

  public static final JsNullLiteral INSTANCE = new JsNullLiteral(SourceOrigin.UNKNOWN);

  private JsNullLiteral(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.NULL;
  }

  @Override
  public boolean isBooleanFalse() {
    return true;
  }

  @Override
  public boolean isBooleanTrue() {
    return false;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return false;
  }

  @Override
  public boolean isDefinitelyNull() {
    return true;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }

  /**
   * Note, if this ever becomes not-a-singleton, we'll need to check the SourceInfo ==
   * SourceOrigin.UNKNOWN.
   */
  private Object readResolve() {
    return INSTANCE;
  }
}
