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
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 * A JavaScript object literal.
 */
public final class JsObjectLiteral extends JsLiteral {

  public static Builder builder(SourceInfo info) {
    return new Builder(info);
  }

  /**
   * Builder class for JsObjectLiterals.
   */
  public static class Builder {

    private Builder(SourceInfo info) {
      this.sourceInfo = info;
    }

    private List<JsPropertyInitializer> propertyInitializers = Lists.newArrayList();
    private final SourceInfo sourceInfo;
    private boolean internable = false;

    public Builder add(String property, JsExpression value) {
      add(new JsStringLiteral(sourceInfo, property), value);
      return this;
    }

    public Builder add(JsExpression property, JsExpression value) {
      add(sourceInfo, property, value);
      return this;
    }

    public Builder add(SourceInfo sourceInfo, JsExpression property, JsExpression value) {
      propertyInitializers.add(new JsPropertyInitializer(sourceInfo, property, value));
      return this;
    }

    public Builder setInternable() {
      internable = true;
      return this;
    }

    public JsObjectLiteral build() {
      JsObjectLiteral objectLiteral = new JsObjectLiteral(sourceInfo);
      objectLiteral.properties.addAll(propertyInitializers);
      if (internable) {
        objectLiteral.setInternable();
      }
      return objectLiteral;
    }
  }

  public static final JsObjectLiteral EMPTY = new JsObjectLiteral(SourceOrigin.UNKNOWN);

  private final List<JsPropertyInitializer> properties = Lists.newArrayList();

  private boolean internable = false;

  private JsObjectLiteral(SourceInfo sourceInfo) {
    super(sourceInfo);
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
    return Collections.unmodifiableList(properties);
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
