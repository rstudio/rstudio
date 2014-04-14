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
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;

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
 * However, whenever something that may be a String is compared to something
 * that may not be a <code>String</code>, use <code>===</code>. A Java object
 * compared to a string should always yield false, but that's not true when
 * comparing in JavaScript using <code>==</code>. The cases where
 * <code>===</code> must be used are:
 * </p>
 * <ul>
 * <li>One or both sides have unknown <code>String</code> status.</li>
 * <li>One side is definitely <code>String</code> and one side is definitely !
 * <code>String</code>. <br/>
 * TODO: This case could be optimized as
 * <code>(a == null) &amp; (b == null)</code>.</li>
 * </ul>
 * <p>
 * Since <code>null !== undefined</code>, it is also necessary to normalize
 * <code>null</code> vs. <code>undefined</code> if it's possible for one side to
 * be <code>null</code> and the other to be <code>undefined</code>.
 * </p>
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

      StringStatus lhsStatus = getStringStatus((JReferenceType) lhsType);
      StringStatus rhsStatus = getStringStatus((JReferenceType) rhsType);
      int strat = COMPARISON_STRAT[lhsStatus.getIndex()][rhsStatus.getIndex()];

      switch (strat) {
        case STRAT_TRIPLE: {
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

        case STRAT_DOUBLE: {
          boolean lhsNullLit = lhs == program.getLiteralNull();
          boolean rhsNullLit = rhs == program.getLiteralNull();
          if ((lhsNullLit && rhsStatus == StringStatus.NOTSTRING)
              || (rhsNullLit && lhsStatus == StringStatus.NOTSTRING)) {
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

    private StringStatus getStringStatus(JReferenceType type) {
      JClassType stringType = program.getTypeJavaLangString();
      if (type == program.getTypeNull()) {
        return StringStatus.NULL;
      } else if (program.typeOracle.canTriviallyCast(type, stringType)) {
        return StringStatus.STRING;
      } else if (program.typeOracle.canTheoreticallyCast(type, stringType)) {
        return StringStatus.UNKNOWN;
      } else {
        return StringStatus.NOTSTRING;
      }
    }

    private JExpression maskUndefined(JExpression lhs) {
      assert ((JReferenceType) lhs.getType()).canBeNull();

      JMethod maskMethod = program.getIndexedMethod("Cast.maskUndefined");
      JMethodCall lhsCall = new JMethodCall(lhs.getSourceInfo(), null, maskMethod, lhs.getType());
      lhsCall.addArg(lhs);
      return lhsCall;
    }
  }

  /**
   * Represents what we know about an operand type in terms of its type and
   * <code>null</code> status.
   */
  private enum StringStatus {
    NOTSTRING(2), NULL(3), STRING(1), UNKNOWN(0);

    private int index;

    StringStatus(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }
  }

  /**
   * A map of the combinations where each comparison strategy should be used.
   */
  private static int[][] COMPARISON_STRAT = {
      // ..U..S.!S..N
      {1, 1, 1, 0,}, // UNKNOWN
      {1, 0, 1, 0,}, // STRING
      {1, 1, 0, 0,}, // NOTSTRING
      {0, 0, 0, 0,}, // NULL
  };

  /**
   * The comparison strategy of using ==.
   */
  private static final int STRAT_DOUBLE = 0;

  /**
   * The comparison strategy of using ===.
   */
  private static final int STRAT_TRIPLE = 1;

  public static void exec(JProgram program) {
    new EqualityNormalizer(program).execImpl();
  }

  private static boolean canBeNull(JExpression x) {
    return ((JReferenceType) x.getType()).canBeNull();
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
