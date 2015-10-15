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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JProgram.DispatchType;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;

import java.util.Map;

/**
 * <p>
 * Rewrite Java <code>==</code> so that it will execute correctly in JavaScript.
 * After this pass, Java's <code>==</code> is considered equivalent to
 * JavaScript's <code>===</code>.
 * </p>
 * <p>
 * Whenever possible, a Java <code>==</code> is replaced by a JavaScript
 * <code>==</code>. This is shorter than <code>===</code>, and it avoids any
 * complication due to GWT treating both <code>null</code> and
 * <code>undefined</code> as a valid translation of a Java <code>null</code>.
 * </p>
 * <p>
 * However, whenever something that may be an unboxed type is compared to something
 * that may not be a unboxed type, use <code>===</code>. A Java object
 * compared to a string should always yield false, but that's not true when
 * comparing in JavaScript using <code>==</code>. The cases where
 * <code>===</code> must be used are:
 * </p>
 * <ul>
 * <li>One or both sides have unknown <i>unboxed type</i> status.</li>
 * <li>One side is definitely an <i>unboxed type</i> and one side is definitely !
 * <i>unboxed type</i>. <br/>
 * TODO: This case could be optimized as
 * <code>(a == null) &amp; (b == null)</code>.</li>
 * </ul>
 * <p>
 * Since <code>null !== undefined</code>, it is also necessary to normalize
 * <code>null</code> vs. <code>undefined</code> if it's possible for one side to
 * be <code>null</code> and the other to be <code>undefined</code>.
 * </p>
 * <p> An "unboxed type" is a String, Boolean, or Double that is represented as a naked raw
 * JS type.
 */
public class EqualityNormalizer {

  /**
   * Breaks apart certain complex assignments.
   */
  private class BreakupAssignOpsVisitor extends JModVisitor {

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      JBinaryOperator op = x.getOp();
      if (op != JBinaryOperator.EQ && op != JBinaryOperator.NEQ) {
        return;
      }
      JExpression lhs = x.getLhs();
      JExpression rhs = x.getRhs();
      JType lhsType = lhs.getType();
      JType rhsType = rhs.getType();
      if (!(lhsType instanceof JReferenceType)) {
        assert !(rhsType instanceof JReferenceType);
        return;
      }

      UnboxedTypeStatus lhsStatus = getUnboxedTypeStatus((JReferenceType) lhsType);
      UnboxedTypeStatus rhsStatus = getUnboxedTypeStatus((JReferenceType) rhsType);
      int comparisonStrategy = COMPARISON_TABLE[lhsStatus.getIndex()][rhsStatus.getIndex()];

      switch (comparisonStrategy) {
        case T: {
          if (canBeNull(lhs) && canBeNull(rhs)) {
            /*
             * If it's possible for one side to be null and the other side
             * undefined, then mask both sides.
             */
            lhs = maskUndefined(lhs);
            rhs = maskUndefined(rhs);
          }

          JBinaryOperation binOp =
              new JBinaryOperation(x.getSourceInfo(), x.getType(), x.getOp(), lhs, rhs);
          ctx.replaceMe(binOp);
          break;
        }

        case D: {
          boolean lhsNullLit = lhs == program.getLiteralNull();
          boolean rhsNullLit = rhs == program.getLiteralNull();
          if ((lhsNullLit && rhsStatus == UnboxedTypeStatus.NOT_UNBOXEDTYPE)
              || (rhsNullLit && lhsStatus == UnboxedTypeStatus.NOT_UNBOXEDTYPE)) {
            /*
             * If either side is a null literal and the other is non-String,
             * replace with a null-check.
             */
            String methodName;
            if (op == JBinaryOperator.EQ) {
              methodName = "Cast.isNull";
            } else {
              methodName = "Cast.isNotNull";
            }
            JMethod isNullMethod = program.getIndexedMethod(methodName);
            JMethodCall call = new JMethodCall(x.getSourceInfo(), null, isNullMethod);
            call.addArg(lhsNullLit ? rhs : lhs);
            ctx.replaceMe(call);
          } else {
            // Replace with a call to Cast.jsEquals, which does a == internally.
            String methodName;
            if (op == JBinaryOperator.EQ) {
              methodName = "Cast.jsEquals";
            } else {
              methodName = "Cast.jsNotEquals";
            }
            JMethod eqMethod = program.getIndexedMethod(methodName);
            JMethodCall call = new JMethodCall(x.getSourceInfo(), null, eqMethod);
            call.addArgs(lhs, rhs);
            ctx.replaceMe(call);
          }
          break;
        }
      }
    }

    private UnboxedTypeStatus getUnboxedTypeStatus(JReferenceType type) {
      if (type.isNullType()) {
        return UnboxedTypeStatus.NULL;
      } else {
        for (Map.Entry<JClassType, DispatchType> nativeDispatchType :
            program.getRepresentedAsNativeTypesDispatchMap().entrySet()) {
          if (type.getUnderlyingType() == nativeDispatchType.getKey()) {
            switch (nativeDispatchType.getValue()) {
              case DOUBLE:
                return UnboxedTypeStatus.DOUBLE;
              case BOOLEAN:
                return UnboxedTypeStatus.BOOLEAN;
              case STRING:
                return UnboxedTypeStatus.STRING;
              default:
                throw new AssertionError("Shouldn't happen");
            }
          } else if (!program.typeOracle.castFailsTrivially(type, nativeDispatchType.getKey())) {
            return UnboxedTypeStatus.UNKNOWN;
          }
        }
        return UnboxedTypeStatus.NOT_UNBOXEDTYPE;
      }
    }

    private JExpression maskUndefined(JExpression lhs) {
      assert lhs.getType().canBeNull();

      JMethod maskMethod = program.getIndexedMethod("Cast.maskUndefined");
      JMethodCall lhsCall = new JMethodCall(lhs.getSourceInfo(), null, maskMethod, lhs);
      lhsCall.overrideReturnType(lhs.getType());
      return lhsCall;
    }
  }

  /**
   * Represents what we know about an operand type in terms of its type and
   * <code>null</code> status.
   */
  private enum UnboxedTypeStatus {
    NOT_UNBOXEDTYPE(4), NULL(5), DOUBLE(3), BOOLEAN(2) ,STRING(1), UNKNOWN(0);

    private int index;

    UnboxedTypeStatus(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }

  /**
   * The comparison strategy of using ==.
   * Mnemonic: D = double eq
   */
  private static final int D = 0;

  /**
   * The comparison strategy of using ===.
   * Mnemonic: T = triple eq
   */
  private static final int T = 1;

  /**
   * A map of the combinations where each comparison strategy should be used.
   */
  private static int[][] COMPARISON_TABLE = {
      // any type compared to unknown uses triple eq
      {T, T, T, T, T, D,}, // UNKNOWN
      // string type uses D only against String and null
      {T, D, T, T, T, D,}, // STRING
      // double type uses D only against Double and null
      {T, T, D, T, T, D,}, // DOUBLE
      // boolean type uses D only against Boolean and null
      {T, T, T, D, T, D,}, // BOOLEAN
      // non-unboxed type uses D only against other non-unboxed types and null
      {T, T, T, T, D, D,}, // NOT_UNBOXEDTYPE
      // null vs null is safe everywhere
      {D, D, D, D, D, D,}, // NULL
  };

  public static void exec(JProgram program) {
    new EqualityNormalizer(program).execImpl();
  }

  private static boolean canBeNull(JExpression x) {
    return x.getType().canBeNull();
  }

  private final JProgram program;

  private EqualityNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    BreakupAssignOpsVisitor breaker = new BreakupAssignOpsVisitor();
    breaker.accept(program);
  }

}
