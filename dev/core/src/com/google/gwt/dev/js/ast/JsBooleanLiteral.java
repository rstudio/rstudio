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
 * Represents a JavaScript literal boolean expression.
 */
public final class JsBooleanLiteral extends JsValueLiteral {

  public static final JsBooleanLiteral FALSE = new JsBooleanLiteral(SourceOrigin.UNKNOWN, false);

  public static final JsBooleanLiteral TRUE = new JsBooleanLiteral(SourceOrigin.UNKNOWN, true);

  public static JsBooleanLiteral get(boolean value) {
    return value ? TRUE : FALSE;
  }

  private final boolean value;

  private JsBooleanLiteral(SourceInfo sourceInfo, boolean value) {
    super(sourceInfo);
    this.value = value;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.BOOLEAN;
  }

  public boolean getValue() {
    return value;
  }

  @Override
  public boolean isBooleanFalse() {
    return value == false;
  }

  @Override
  public boolean isBooleanTrue() {
    return value == true;
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

  private Object readResolve() {
    return get(value);
  }
}
