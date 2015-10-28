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

import java.util.List;

/**
 * New array expression.
 */
public class JNewArray extends JExpression {

  public static JNewArray createArrayWithDimensionExpressions(
      SourceInfo info, JArrayType arrayType, List<JExpression> dimensionExpressions) {
    // Produce all class literals that will eventually get generated.
    assert dimensionExpressions != null;
    return new JNewArray(info, arrayType, dimensionExpressions, null,
        new JClassLiteral(info.makeChild(), arrayType.getLeafType()));
  }

  public static JNewArray createArrayWithInitializers(SourceInfo info, JArrayType arrayType,
      List<JExpression> initializers) {
    assert initializers != null;
    return new JNewArray(info, arrayType, null, initializers,
        new JClassLiteral(info.makeChild(), arrayType.getLeafType()));
  }

  private final List<JExpression> dimensionExpressions;

  private final List<JExpression> initializers;

  /**
   * The list of class literals that will be needed to support this expression.
   */
  private JClassLiteral leafTypeClassLiteral;

  private JArrayType type;

  public JNewArray(SourceInfo info, JArrayType type, List<JExpression> dimensionExpressions,
      List<JExpression> initializers, JClassLiteral leafTypeClassLiteral) {
    super(info);
    this.type = type;
    this.dimensionExpressions = dimensionExpressions;
    this.initializers = initializers;
    this.leafTypeClassLiteral = leafTypeClassLiteral;
    assert !(leafTypeClassLiteral.getRefType().isArrayType());
  }

  public JArrayType getArrayType() {
    return type;
  }

  public List<JExpression> getDimensionExpressions() {
    return dimensionExpressions;
  }

  public List<JExpression> getInitializers() {
    return initializers;
  }

  /**
   * Return a class literal for the leaf type of the array.
   */
  public JClassLiteral getLeafTypeClassLiteral() {
    return leafTypeClassLiteral;
  }

  @Override
  public JReferenceType getType() {
    return type.strengthenToNonNull().strengthenToExact();
  }

  @Override
  public boolean hasSideEffects() {
    assert ((dimensionExpressions != null) ^ (initializers != null));

    for (JExpression expression : initializers != null ? initializers : dimensionExpressions) {
      if (expression.hasSideEffects()) {
        return true;
      }
    }

    return false;
  }

  public void setType(JArrayType type) {
    this.type = type;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      assert ((dimensionExpressions != null) ^ (initializers != null));

      visitor.accept(initializers != null ? initializers : dimensionExpressions);

      leafTypeClassLiteral = (JClassLiteral) visitor.accept(leafTypeClassLiteral);
    }
    visitor.endVisit(this, ctx);
  }
}
