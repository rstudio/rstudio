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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents multiple ordered expressions as a single compound expression.
 */
public class JMultiExpression extends JExpression {

  private List<JExpression> expressions = Lists.newArrayList();

  /**
   * Construct an empty multi expression.
   */
  public JMultiExpression(SourceInfo info) {
    super(info);
  }

  /**
   * Construct a multi expression containing {@code expressions}.
   */
  public JMultiExpression(SourceInfo info, List<JExpression> expressions) {
    super(info);
    this.expressions.addAll(expressions);
  }

  /**
   * Adds {@code expressions} to the multi expression at the end.
   */
  public void addExpressions(JExpression... expressions) {
    this.expressions.addAll(Arrays.asList(expressions));
  }

  /**
   * Adds a list of expressions to the multi expression at the end.
   */
  public void addExpressions(List<JExpression> expressions) {
    this.expressions.addAll(expressions);
  }

  /**
   * Adds a list of expressions to the multi expression at position {@code index}.
   */
  public void addExpressions(int index, List<JExpression> expressions) {
    this.expressions.addAll(index, expressions);
  }

  /**
   * Returns the expression at {@code index}.
   */
  public JExpression getExpression(int index) {
    return expressions.get(index);
  }

  /**
   * Returns the list of expressions.
   */
  public List<JExpression> getExpressions() {
    return Collections.unmodifiableList(expressions);
  }

  /**
   * Returns the number of expressions directly included in the multi expression.
   */
  public int getNumberOfExpressions() {
    return expressions.size();
  }

  /**
   * Returns the multi expression type, i.e. the type of the last expression in the list or
   * {@code void} if empty.
   */
  @Override
  public JType getType() {
    int size = expressions.size();
    if (size == 0) {
      return JPrimitiveType.VOID;
    }
    return expressions.get(size - 1).getType();
  }

  @Override
  public boolean hasSideEffects() {
    for (JExpression expression : expressions) {
      if (expression.hasSideEffects()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if the multi expression is empty.
   */
  public boolean isEmpty() {
    return expressions.isEmpty();
  }

  /**
   * Removes the expression at {@code index} from this multi expression.
   */
  public JExpression removeExpression(int index) {
    return expressions.remove(index);
  }

  /**
   * Replaces the expression at {@code index} by {@code expression}.
   */
  public void setExpression(int index, JExpression expression) {
    this.expressions.set(index, expression);
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.acceptWithInsertRemove(expressions);
    }
    visitor.endVisit(this, ctx);
  }
}
