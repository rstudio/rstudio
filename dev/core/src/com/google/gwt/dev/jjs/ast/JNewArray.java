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
import com.google.gwt.dev.util.collect.Lists;

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

    List<JClassLiteral> classLiterals = Lists.create();
    JType cur = arrayType;
    for (int i = 0; i < realDims; ++i) {
      // Walk down each type from most dims to least.
      JClassLiteral classLit = new JClassLiteral(info.makeChild(), cur);
      classLiterals = Lists.add(classLiterals, classLit);
      cur = ((JArrayType) cur).getElementType();
    }
    return new JNewArray(info, arrayType, dims, null, classLiterals);
  }

  public static JNewArray createInitializers(SourceInfo info, JArrayType arrayType,
      List<JExpression> initializers) {
    List<JClassLiteral> classLiterals =
        Lists.create(new JClassLiteral(info.makeChild(), arrayType));
    return new JNewArray(info, arrayType, null, initializers, classLiterals);
  }

  public final List<JExpression> dims;

  public final List<JExpression> initializers;

  /**
   * The list of class literals that will be needed to support this expression.
   */
  private final List<JClassLiteral> classLiterals;

  private JArrayType type;

  public JNewArray(SourceInfo info, JArrayType type, List<JExpression> dims,
      List<JExpression> initializers, List<JClassLiteral> classLits) {
    super(info);
    this.type = type;
    this.dims = dims;
    this.initializers = initializers;
    this.classLiterals = classLits;
  }

  public JArrayType getArrayType() {
    return type;
  }

  /**
   * Return a class literal for the array type itself.
   */
  public JClassLiteral getClassLiteral() {
    // the class literal for the array type itself is always first
    return getClassLiterals().get(0);
  }

  /**
   * Get the list of class literals that will be needed to support this
   * expression. If this literal has dimension expressions in <code>dims</code>,
   * then the literals will be the array type, followed by the array's component
   * type, followed by array's component type's component type, etc.
   */
  public List<JClassLiteral> getClassLiterals() {
    return classLiterals;
  }

  public JNonNullType getType() {
    return type.getNonNull();
  }

  @Override
  public boolean hasSideEffects() {
    if (initializers != null) {
      for (int i = 0, c = initializers.size(); i < c; ++i) {
        if (initializers.get(i).hasSideEffects()) {
          return true;
        }
      }
    }
    if (dims != null) {
      for (int i = 0, c = dims.size(); i < c; ++i) {
        if (dims.get(i).hasSideEffects()) {
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

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      assert ((dims != null) ^ (initializers != null));

      if (dims != null) {
        visitor.accept(dims);
      }

      if (initializers != null) {
        visitor.accept(initializers);
      }

      // Visit all the class literals that will eventually get generated.
      visitor.accept(getClassLiterals());
    }
    visitor.endVisit(this, ctx);
  }
}
