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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Java method parameter definition.
 */
public class JParameter extends JVariable implements HasEnclosingMethod {

  public static JParameter create(SourceInfo info, String name, JType type, boolean isFinal,
      boolean isThis, JMethod enclosingMethod) {
    assert (name != null);
    assert (type != null);
    assert (enclosingMethod != null);

    JParameter x = new JParameter(info, name, type, isFinal, isThis, enclosingMethod);

    enclosingMethod.addParam(x);
    return x;
  }

  private final JMethod enclosingMethod;
  private final boolean isThis;

  public JParameter(SourceInfo info, String name, JType type, boolean isFinal, boolean isThis,
      JMethod enclosingMethod) {
    super(info, name, type, isFinal);
    this.enclosingMethod = enclosingMethod;
    this.isThis = isThis;
  }

  @Override
  public JMethod getEnclosingMethod() {
    return enclosingMethod;
  }

  /**
   * Returns <code>true</code> if this parameter is the this parameter of a
   * static impl method.
   */
  public boolean isThis() {
    return isThis;
  }

  @Override
  public void setInitializer(JDeclarationStatement declStmt) {
    throw new UnsupportedOperationException("A JParameter cannot have an initializer");
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
