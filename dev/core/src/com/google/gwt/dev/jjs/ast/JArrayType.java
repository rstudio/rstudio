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

/**
 * Instances are shared.
 */
public class JArrayType extends JClassType {

  private static String calcName(JType leafType, int dims) {
    String name = leafType.getName();
    for (int i = 0; i < dims; ++i) {
      name = name + "[]";
    }
    return name;
  }

  private int dims;
  private JType leafType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JArrayType(JProgram program, JType leafType, int dims) {
    super(program, leafType.getSourceInfo().makeChild(JArrayType.class,
        "Array type"), calcName(leafType, dims), false, false);
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
    if (dims == 1) {
      return leafType;
    }
    return program.getTypeArray(leafType, dims - 1);
  }

  public String getJavahSignatureName() {
    String s = leafType.getJavahSignatureName();
    for (int i = 0; i < dims; ++i) {
      s = "_3" + s;
    }
    return s;
  }

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

  public boolean isFinal() {
    return leafType.isFinal();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.acceptWithInsertRemove(fields);
    }
    visitor.endVisit(this, ctx);
  }

}
