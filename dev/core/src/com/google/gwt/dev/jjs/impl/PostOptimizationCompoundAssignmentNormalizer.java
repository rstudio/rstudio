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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Normalize compound assignments as needed after optimization. Integer division
 * and operations on longs need to be broken up.
 */
public class PostOptimizationCompoundAssignmentNormalizer extends
    CompoundAssignmentNormalizer {
  public static void exec(JProgram program) {
    new PostOptimizationCompoundAssignmentNormalizer().accept(program);
  }

  protected PostOptimizationCompoundAssignmentNormalizer() {
    super(true);
  }

  @Override
  protected String getTempPrefix() {
    return "$t";
  }

  @Override
  protected boolean shouldBreakUp(JBinaryOperation x) {
    if (x.getType() == JPrimitiveType.LONG) {
      return true;
    }
    if (x.getOp() == JBinaryOperator.ASG_DIV
        && x.getType() != JPrimitiveType.FLOAT
        && x.getType() != JPrimitiveType.DOUBLE) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldBreakUp(JPostfixOperation x) {
    if (x.getType() == JPrimitiveType.LONG) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldBreakUp(JPrefixOperation x) {
    if (x.getType() == JPrimitiveType.LONG) {
      return true;
    }
    return false;
  }
}
