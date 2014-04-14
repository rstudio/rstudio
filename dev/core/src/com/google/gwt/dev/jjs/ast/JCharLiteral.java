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
import com.google.gwt.dev.jjs.SourceOrigin;

/**
 * Java character literal expression.
 */
public class JCharLiteral extends JValueLiteral {

  public static final JCharLiteral NULL = new JCharLiteral(SourceOrigin.UNKNOWN, (char) 0);

  public static JCharLiteral get(char value) {
    return (value == 0) ? NULL : new JCharLiteral(SourceOrigin.UNKNOWN, value);
  }

  private final char value;

  public JCharLiteral(SourceInfo sourceInfo, char value) {
    super(sourceInfo);
    this.value = value;
  }

  @Override
  public JValueLiteral cloneFrom(JValueLiteral value) {
    Object valueObj = value.getValueObj();
    if (valueObj instanceof Character) {
      return value;
    } else if (valueObj instanceof Number) {
      Number number = (Number) valueObj;
      return new JCharLiteral(value.getSourceInfo(), (char) number.intValue());
    }
    return null;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.CHAR;
  }

  public char getValue() {
    return value;
  }

  @Override
  public Object getValueObj() {
    return Character.valueOf(value);
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

  private Object readResolve() {
    return (value == 0) ? NULL : this;
  }
}
