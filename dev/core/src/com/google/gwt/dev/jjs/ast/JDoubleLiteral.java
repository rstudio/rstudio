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
 * Java double literal expression.
 */
public class JDoubleLiteral extends JValueLiteral {

  private final double value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JDoubleLiteral(JProgram program, SourceInfo sourceInfo, double value) {
    super(program, sourceInfo);
    this.value = value;
  }

  @Override
  public JValueLiteral cloneFrom(JValueLiteral value) {
    Object valueObj = value.getValueObj();
    if (valueObj instanceof Character) {
      Character character = (Character) valueObj;
      return program.getLiteralDouble(character.charValue());
    } else if (valueObj instanceof Number) {
      Number number = (Number) valueObj;
      return program.getLiteralDouble(number.doubleValue());
    }
    return null;
  }

  public JType getType() {
    return program.getTypePrimitiveDouble();
  }

  public double getValue() {
    return value;
  }

  public Object getValueObj() {
    return new Double(value);
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
