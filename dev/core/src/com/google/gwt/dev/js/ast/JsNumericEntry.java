/*
 * Copyright 2011 Google Inc.
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
 * Represent an index that can be replacable by the compiler at compile time.
 */
public final class JsNumericEntry extends JsExpression {
  private final String key;
  private int value;

  public JsNumericEntry(SourceInfo info, String key, int value) {
    super(info);
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.NUMBER;
  }

  public int getValue() {
    return value;
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public boolean isDefinitelyNotNull() {
    return true;
  }

  @Override
  public boolean isDefinitelyNull() {
    return false;
  }

  public void setValue(int value) {
    this.value = value;
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    v.visit(this, ctx);
    v.endVisit(this, ctx);
  }
}
