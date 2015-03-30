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

  public static JNewArray createDims(SourceInfo info, JArrayType arrayType, List<JExpression> dims) {
    // Produce all class literals that will eventually get generated.
    int realDims = 0;
    for (JExpression dim : dims) {
      if (dim instanceof JAbsentArrayDimension) {
        break;
      }
      ++realDims;
    }

    return new JNewArray(info, arrayType, dims, null,
        new JClassLiteral(info.makeChild(), arrayType.getLeafType()));
  }

  public static JNewArray createInitializers(SourceInfo info, JArrayType arrayType,
      List<JExpression> initializers) {
    return new JNewArray(info, arrayType, null, initializers,
        new JClassLiteral(info.makeChild(), arrayType.getLeafType()));
  }

  public final List<JExpression> dims;

  public final List<JExpression> initializers;

  /**
   * The list of class literals that will be needed to support this expression.
   */
  private JClassLiteral leafTypeClassLiteral;

  private JArrayType type;

  public JNewArray(SourceInfo info, JArrayType type, List<JExpression> dims,
      List<JExpression> initializers, JClassLiteral leafTypeClassLiteral) {
    super(info);
    this.type = type;
    this.dims = dims;
    this.initializers = initializers;
    this.leafTypeClassLiteral = leafTypeClassLiteral;
    assert !(leafTypeClassLiteral.getRefType() instanceof JArrayType);
  }

  public JArrayType getArrayType() {
    return type;
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
    if (initializers != null) {
      for (JExpression initializer : initializers) {
        if (initializer.hasSideEffects()) {
          return true;
        }
      }
    }
    if (dims != null) {
      for (JExpression dim : dims) {
        if (dim.hasSideEffects()) {
          return true;
        }
      }
    }
    // The new operation on an array does not actually cause side effects.
    return false;
  }

  public void setType(JArrayType type) {
    this.type = type;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      assert ((dims != null) ^ (initializers != null));

      if (dims != null) {
        visitor.accept(dims);
      }

      if (initializers != null) {
        visitor.accept(initializers);
      }

      // Visit the base class that will eventually get generated.
      leafTypeClassLiteral = (JClassLiteral) visitor.accept(leafTypeClassLiteral);
    }
    visitor.endVisit(this, ctx);
  }
}
