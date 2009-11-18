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
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Normalize compound assignments as needed after optimization. Integer division
 * and operations on longs need to be broken up.
 */
public class PostOptimizationCompoundAssignmentNormalizer extends
    CompoundAssignmentNormalizer {

  public static void exec(JProgram program) {
    new PostOptimizationCompoundAssignmentNormalizer(program).breakUpAssignments();
  }

  protected PostOptimizationCompoundAssignmentNormalizer(JProgram program) {
    super(program, true);
  }

  @Override
  protected String getTempPrefix() {
    return "$t";
  }

  @Override
  protected JExpression modifyResultOperation(JBinaryOperation op) {
    JType lhsType = op.getLhs().getType();
    JType rhsType = op.getRhs().getType();
    if (lhsType != rhsType) {
      // first widen binary op to encompass both sides, then add narrow cast
      return new JCastOperation(op.getSourceInfo(), lhsType,
          new JBinaryOperation(op.getSourceInfo(), 
              widenType(lhsType, rhsType),
              op.getOp(),
              op.getLhs(), op.getRhs()));
    }
    return op;
  }
    
  @Override
  protected boolean shouldBreakUp(JBinaryOperation x) {
    if (x.getType() == program.getTypePrimitiveLong()) {
      return true;
    }
    if (x.getOp() == JBinaryOperator.ASG_DIV
        && x.getType() != program.getTypePrimitiveFloat()
        && x.getType() != program.getTypePrimitiveDouble()) {
      return true;
    }
    JType lhsType = x.getLhs().getType();
    JType rhsType = x.getRhs().getType();
    // break up so that result may be coerced to LHS type 
    if (isIntegral(lhsType) && !isIntegral(rhsType)) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldBreakUp(JPostfixOperation x) {
    if (x.getType() == program.getTypePrimitiveLong()) {
      return true;
    }
    return false;
  }

  @Override
  protected boolean shouldBreakUp(JPrefixOperation x) {
    if (x.getType() == program.getTypePrimitiveLong()) {
      return true;
    }
    return false;
  }
 
  private boolean isIntegral(JType type) {
    // if it can't be widened into a long, it must be float
    return widenType(type, JPrimitiveType.LONG) == JPrimitiveType.LONG;
  }
  
  private JType widenType(JType lhsType, JType rhsType) {
    if (lhsType == JPrimitiveType.DOUBLE ||
        rhsType == JPrimitiveType.DOUBLE) {
      return JPrimitiveType.DOUBLE;
    } else if (lhsType == JPrimitiveType.FLOAT ||
              rhsType == JPrimitiveType.FLOAT) {
      return JPrimitiveType.FLOAT;
    } else if (lhsType == JPrimitiveType.LONG || 
               rhsType == JPrimitiveType.LONG) {
      return JPrimitiveType.LONG;
    } else {
      return JPrimitiveType.INT;
    }
  }
}
