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
 * Java character literal expression.
 */
public class JCharLiteral extends JValueLiteral {

  private final char value;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JCharLiteral(JProgram program, SourceInfo sourceInfo, char value) {
    super(program, sourceInfo);
    this.value = value;
  }

  @Override
  public JValueLiteral cloneFrom(JValueLiteral value) {
    Object valueObj = value.getValueObj();
    if (valueObj instanceof Character) {
      return value;
    } else if (valueObj instanceof Number) {
      Number number = (Number) valueObj;
      return program.getLiteralChar((char) number.intValue());
    }
    return null;
  }

  public JType getType() {
    return program.getTypePrimitiveChar();
  }

  public char getValue() {
    return value;
  }

  public Object getValueObj() {
    return new Character(value);
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

}
