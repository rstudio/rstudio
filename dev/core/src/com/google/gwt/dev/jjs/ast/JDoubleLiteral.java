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
 * Java double literal expression.
 */
public class JDoubleLiteral extends JValueLiteral {

  public static final JDoubleLiteral ZERO = new JDoubleLiteral(SourceOrigin.UNKNOWN, Double
      .longBitsToDouble(0L));

  public static JDoubleLiteral get(double value) {
    return isZero(value) ? ZERO : new JDoubleLiteral(SourceOrigin.UNKNOWN, value);
  }

  /**
   * Does this value match the exact 0 bit pattern? (This precludes
   * canonicalizing -0.0 as 0.0).
   */
  private static boolean isZero(double value) {
    return Double.doubleToRawLongBits(value) == 0L;
  }

  private final double value;

  public JDoubleLiteral(SourceInfo sourceInfo, double value) {
    super(sourceInfo);
    this.value = value;
  }

  @Override
  public JValueLiteral cloneFrom(JValueLiteral value) {
    Object valueObj = value.getValueObj();
    if (valueObj instanceof Character) {
      Character character = (Character) valueObj;
      return new JDoubleLiteral(value.getSourceInfo(), character.charValue());
    } else if (valueObj instanceof Number) {
      Number number = (Number) valueObj;
      return new JDoubleLiteral(value.getSourceInfo(), number.doubleValue());
    }
    return null;
  }

  @Override
  public JType getType() {
    return JPrimitiveType.DOUBLE;
  }

  public double getValue() {
    return value;
  }

  @Override
  public Object getValueObj() {
    return new Double(value);
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }

  private Object readResolve() {
    return isZero(value) ? ZERO : this;
  }
}
