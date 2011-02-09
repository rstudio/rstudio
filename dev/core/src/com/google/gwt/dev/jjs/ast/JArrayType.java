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

  private static String calcName(JType leafType, int dims) {
    String name = leafType.getName();
    for (int i = 0; i < dims; ++i) {
      name = name + "[]";
    }
    return name;
  }

  private int dims;
  private JType elementType;
  private JType leafType;

  public JArrayType(JType elementType, JType leafType, int dims) {
    super(leafType.getSourceInfo().makeChild(SourceOrigin.UNKNOWN), calcName(
        leafType, dims));
    this.elementType = elementType;
    this.leafType = leafType;
    this.dims = dims;
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForArray";
  }

  public int getDims() {
    return dims;
  }

  public JType getElementType() {
    return elementType;
  }

  @Override
  public String getJavahSignatureName() {
    String s = leafType.getJavahSignatureName();
    for (int i = 0; i < dims; ++i) {
      s = "_3" + s;
    }
    return s;
  }

  @Override
  public String getJsniSignatureName() {
    String s = leafType.getJsniSignatureName();
    for (int i = 0; i < dims; ++i) {
      s = "[" + s;
    }
    return s;
  }

  public JType getLeafType() {
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
    return leafType.isFinal();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
