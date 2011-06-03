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

import com.google.gwt.dev.jjs.SourceOrigin;

/**
 * Instances are shared.
 */
public class JArrayType extends JReferenceType {

  private transient int dims = 0;
  private final JType elementType;
  private transient JType leafType = null;

  public JArrayType(JType elementType) {
    super(elementType.getSourceInfo().makeChild(SourceOrigin.UNKNOWN), elementType.getName() + "[]");
    assert !(elementType instanceof JNonNullType);
    this.elementType = elementType;
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForArray";
  }

  public int getDims() {
    if (dims == 0) {
      dims = 1;
      if (elementType instanceof JArrayType) {
        dims += ((JArrayType) elementType).getDims();
      }
    }
    return dims;
  }

  public JType getElementType() {
    return elementType;
  }

  @Override
  public String getJavahSignatureName() {
    return "_3" + elementType.getJavahSignatureName();
  }

  @Override
  public String getJsniSignatureName() {
    return "[" + elementType.getJsniSignatureName();
  }

  public JType getLeafType() {
    if (leafType == null) {
      if (elementType instanceof JArrayType) {
        leafType = ((JArrayType) elementType).getLeafType();
      } else {
        leafType = elementType;
      }
    }
    return leafType;
  }

  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isExternal() {
    return elementType.isExternal();
  }

  public boolean isFinal() {
    return elementType.isFinal();
  }

  @Override
  public boolean replaces(JType originalType) {
    return (originalType instanceof JArrayType)
        && elementType.replaces(((JArrayType) originalType).getElementType());
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
