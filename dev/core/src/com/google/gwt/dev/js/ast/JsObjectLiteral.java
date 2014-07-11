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

import java.util.ArrayList;
import java.util.List;

/**
 * A JavaScript object literal.
 */
public final class JsObjectLiteral extends JsLiteral {

  private final List<JsPropertyInitializer> properties = new ArrayList<JsPropertyInitializer>();

  private boolean internable = false;

  public JsObjectLiteral(SourceInfo sourceInfo) {
    super(sourceInfo);
  }

  /**
   * Adds a property and its initial value to the object literal.
   * <p>
   * NOTE: Does not check for duplicate names.
   */
  public void addProperty(SourceInfo sourceInfo, JsExpression label, JsExpression value) {
    properties.add(new JsPropertyInitializer(sourceInfo, label, value));
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || this.getClass() != that.getClass()) {
      return false;
    }
    JsObjectLiteral thatLiteral = (JsObjectLiteral) that;
    return internable == thatLiteral.internable && properties.equals(thatLiteral.properties);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.OBJECT;
  }

  public List<JsPropertyInitializer> getPropertyInitializers() {
    return properties;
  }

  @Override
  public int hashCode() {
    return  properties.hashCode() + 17 * (internable ? 0 : 1);
  }

  @Override
  public boolean hasSideEffects() {
    for (JsPropertyInitializer prop : properties) {
      if (prop.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBooleanFalse() {
    return false;
  }

  @Override
  public boolean isBooleanTrue() {
    return true;
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
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(properties);
    }
    v.endVisit(this, ctx);
  }

  /**
   * Some object literals are not mutated and hence internable.
   */
  @Override
  public boolean isInternable() {
    return internable;
  }

  public void setInternable() {
    internable = true;
  }
}
