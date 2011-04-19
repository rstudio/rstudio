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

import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JPostfixOperation;
import com.google.gwt.dev.jjs.ast.JPrefixOperation;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JUnaryOperator;

/**
 * Replaces long operations with calls to the emulation library. Depends on
 * {@link LongCastNormalizer} and {@link CompoundAssignmentNormalizer} having
 * been run.
 */
public class LongEmulationNormalizer {

  /**
   * Replace all long math with calls into the long emulation library.
   */
  private class LongOpVisitor extends JModVisitor {

    private final JPrimitiveType longType;

    public LongOpVisitor(JPrimitiveType longType) {
      this.longType = longType;
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      // Concats are handled by CastNormalizer.ConcatVisitor.
      if (program.isJavaLangString(x.getType())) {
        return;
      }
      JType lhsType = x.getLhs().getType();
      JType rhsType = x.getRhs().getType();
      if (lhsType != longType) {
        return;
      }

      String methodName = getEmulationMethod(x.getOp());
      if (methodName == null) {
        return;
      }

      // Check operand types.
      switch (x.getOp()) {
        case SHL:
        case SHR:
        case SHRU:
          if (rhsType == longType) {
            throw new InternalCompilerException("Expected right operand not to be of type long");
          }
          break;
        default:
          if (rhsType != longType) {
            throw new InternalCompilerException("Expected right operand to be of type long");
          }
      }

      JMethod method = program.getIndexedMethod("LongLib." + methodName);
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method, x.getType());
      call.addArgs(x.getLhs(), x.getRhs());
      ctx.replaceMe(call);
    }

    @Override
    public void endVisit(JPostfixOperation x, Context ctx) {
      JType argType = x.getArg().getType();
      if (argType == longType) {
        throw new InternalCompilerException("Postfix operations on longs should not reach here");
      }
    }

    @Override
    public void endVisit(JPrefixOperation x, Context ctx) {
      JType argType = x.getArg().getType();
      if (argType != longType) {
        return;
      }

      String methodName = getEmulationMethod(x.getOp());
      JMethod method = program.getIndexedMethod("LongLib." + methodName);
      JMethodCall call = new JMethodCall(x.getSourceInfo(), null, method, x.getType());
      call.addArg(x.getArg());
      ctx.replaceMe(call);
    }

    private String getEmulationMethod(JBinaryOperator op) {
      switch (op) {
        case MUL:
          return "mul";
        case DIV:
          return "div";
        case MOD:
          return "mod";
        case ADD:
          return "add";
        case SUB:
          return "sub";
        case SHL:
          return "shl";
        case SHR:
          return "shr";
        case SHRU:
          return "shru";
        case LT:
          return "lt";
        case LTE:
          return "lte";
        case GT:
          return "gt";
        case GTE:
          return "gte";
        case EQ:
          return "eq";
        case NEQ:
          return "neq";
        case BIT_AND:
          return "and";
        case BIT_XOR:
          return "xor";
        case BIT_OR:
          return "or";

        case AND:
        case OR:
          throw new InternalCompilerException("AND and OR should not have long operands");

        case ASG:
          // Nothing to do.
          return null;

        case ASG_ADD:
        case ASG_SUB:
        case ASG_MUL:
        case ASG_DIV:
        case ASG_MOD:
        case ASG_SHL:
        case ASG_SHR:
        case ASG_SHRU:
        case ASG_BIT_AND:
        case ASG_BIT_OR:
        case ASG_BIT_XOR:
          throw new InternalCompilerException("Modifying long ops should not reach here");
        default:
          throw new InternalCompilerException("Should not reach here");
      }
    }

    private String getEmulationMethod(JUnaryOperator op) {
      switch (op) {
        case INC:
        case DEC:
          throw new InternalCompilerException("Modifying long ops should not reach here");
        case NEG:
          return "neg";
        case NOT:
          throw new InternalCompilerException("NOT should not have a long operand");
        case BIT_NOT:
          return "not";
        default:
          throw new InternalCompilerException("Should not reach here");
      }
    }
  }

  public static void exec(JProgram program) {
    new LongEmulationNormalizer(program).execImpl();
  }

  private final JProgram program;

  private LongEmulationNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    LongOpVisitor visitor = new LongOpVisitor(program.getTypePrimitiveLong());
    visitor.accept(program);
  }

}
