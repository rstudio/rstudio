/*
 * Copyright 2011 Google Inc.
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
 * An AST node whose evaluation results in a runtime type reference of its node.
 */
public class JRuntimeTypeReference extends JExpression {

  private JReferenceType typeReference;
  private JType expressionType;

  public JRuntimeTypeReference(SourceInfo info, JType expressionType, JReferenceType typeReference) {
    super(info);
    this.typeReference = typeReference;
    this.expressionType = expressionType;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      // Intentionally not visiting referenced node
    }
    visitor.endVisit(this, ctx);
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  @Override
  public JType getType() {
    // TODO(rluble): Here we should return Unknown type. JRuntimeTypeReference is an expression that
    // will be replaces by the appropriate JLiteral (a handle to a type at runtime) during
    // translation. At this point it is not known what is the actual type of literal as depending
    // on compiler options it could be a integer or a string.
    return expressionType;
  }

  /**
   * Returns the type this node is proxy for.
   */
  public JReferenceType getReferredType() {
    return typeReference;
  }
}
