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
    super(elementType.getSourceInfo().makeChild(SourceOrigin.UNKNOWN),
        elementType.getName() + "[]");
    // Array types are never constructed with analysis decorated types.
    assert elementType == elementType.getUnderlyingType();
    this.elementType = elementType;
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

  @Override
  public JEnumType isEnumOrSubclass() {
    return null;
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

  @Override
  public boolean isArrayType() {
    return true;
  }

  @Override
  public boolean isPrimitiveType() {
    return false;
  }

  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public boolean isExternal() {
    return elementType.isExternal();
  }

  @Override
  public boolean isFinal() {
    return elementType.isFinal();
  }

  // Returns true only for c.g.g.c.JavaScriptObject
  private boolean isJavaScriptObject(JType elementType) {
    if (!(elementType instanceof JClassType)) {
      return false;
    }

    JClassType classType = (JClassType) elementType;
    return classType.isJsoType() && !classType.getSuperClass().isJsoType();
  }

  @Override
  public boolean canBeImplementedExternally() {
    return getLeafType().canBeImplementedExternally()
        // JSO[] is considered implemented by native arrays.
        || isJavaScriptObject(getElementType());
  }

  @Override
  public boolean isJsType() {
    return false;
  }

  @Override
  public boolean isJsFunction() {
    return false;
  }

  @Override
  public boolean isJsNative() {
    return getLeafType().isJsNative();
  }

  @Override
  public boolean isJsoType() {
    return false;
  }

  @Override
  public boolean canBeReferencedExternally() {
    return getLeafType().canBeReferencedExternally();
  }

  @Override
  public boolean isJavaLangObject() {
    return false;
  }

  @Override
  public boolean replaces(JType originalType) {
    return (originalType instanceof JArrayType)
        && elementType.replaces(((JArrayType) originalType).getElementType());
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
