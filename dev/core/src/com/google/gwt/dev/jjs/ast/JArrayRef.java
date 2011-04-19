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
 * Java array reference expression.
 */
public class JArrayRef extends JExpression {

  private JExpression indexExpr;
  private JExpression instance;

  public JArrayRef(SourceInfo info, JExpression instance, JExpression indexExpr) {
    super(info);
    this.instance = instance;
    this.indexExpr = indexExpr;
  }

  public JArrayType getArrayType() {
    JType type = instance.getType();
    if (type instanceof JNullType) {
      return null;
    }
    return (JArrayType) ((JReferenceType) type).getUnderlyingType();
  }

  public JExpression getIndexExpr() {
    return indexExpr;
  }

  public JExpression getInstance() {
    return instance;
  }

  public JType getType() {
    JArrayType arrayType = getArrayType();
    return (arrayType == null) ? JNullType.INSTANCE : arrayType.getElementType();
  }

  @Override
  public boolean hasSideEffects() {
    // TODO: make the last test better when we have null tracking.
    return instance.hasSideEffects() || indexExpr.hasSideEffects()
        || instance.getType() == JNullType.INSTANCE;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      instance = visitor.accept(instance);
      indexExpr = visitor.accept(indexExpr);
    }
    visitor.endVisit(this, ctx);
  }

}
