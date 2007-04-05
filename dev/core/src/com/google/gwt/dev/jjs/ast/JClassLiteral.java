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

/**
 * Java class literal expression.
 */
public class JClassLiteral extends JLiteral {

  private final JType refType;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JClassLiteral(JProgram program, JType type) {
    super(program);
    refType = type;
  }

  public JType getRefType() {
    return refType;
  }

  public JType getType() {
    return program.getTypeJavaLangClass();
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
