/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * A new instance expression.
 */
public class JNewInstance extends JMethodCall {

  /**
   * The enclosing type of this new operation, used to compute clinit.
   */
  private final JDeclaredType enclosingType;

  /**
   * Initialize a new instance operation equivalent to another one. The new
   * object has no arguments on initialization. This forces the caller to
   * potentially deal with cloning objects if needed.
   */
  public JNewInstance(JNewInstance other) {
    super(other, null);
    this.enclosingType = other.enclosingType;
  }

  public JNewInstance(SourceInfo info, JConstructor ctor, JDeclaredType enclosingType) {
    super(info, null, ctor);
    this.enclosingType = enclosingType;
    setStaticDispatchOnly();
  }

  public JClassType getClassType() {
    return getTarget().getEnclosingType();
  }

  public JDeclaredType getEnclosingType() {
    return enclosingType;
  }

  @Override
  public JConstructor getTarget() {
    return (JConstructor) super.getTarget();
  }

  @Override
  public JNonNullType getType() {
    return getTarget().getNewType();
  }

  public boolean hasClinit() {
    return getEnclosingType().checkClinitTo(getTarget().getEnclosingType());
  }

  @Override
  public boolean hasSideEffects() {
    if (hasClinit()) {
      return true;
    }
    for (JExpression arg : getArgs()) {
      if (arg.hasSideEffects()) {
        return true;
      }
    }
    return !getTarget().isEmpty();
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitChildren(visitor);
    }
    visitor.endVisit(this, ctx);
  }

}
