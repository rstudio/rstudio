/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.dev.jjs.ast.Holder;
import com.google.gwt.dev.jjs.ast.JArrayRef;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JParameterRef;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JThisRef;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Replace any complex assignments that will cause problems down the road with
 * broken expressions; replace side-effect expressions in the lhs with temps to
 * prevent multiple evaluation.
 */
public class CompoundAssignmentNormalizer {

  private class BreakupAssignOpsVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Break apart certain complex assignments.");

    // @Override
    public void endVisit(JBinaryOperation x, Mutator m) {
      /*
       * Convert to a normal divide operation so we can cast the result. Since
       * the left hand size must be computed twice, we have to replace any
       * left-hand side expressions that could have side effects with
       * temporaries, so that they are only run once.
       */
      if (x.op == JBinaryOperator.ASG_DIV
          && x.getType() != program.getTypePrimitiveFloat()
          && x.getType() != program.getTypePrimitiveDouble()) {

        /*
         * Convert to a normal divide operation so we can cast the result. Since
         * the left hand size must be computed twice, we have to replace any
         * left-hand side expressions that could have side effects with
         * temporaries, so that they are only run once.
         */
        final int pushUsedLocals = localIndex;
        JMultiExpression multi = new JMultiExpression(program);
        ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(
            multi);
        x.lhs.traverse(replacer);
        localIndex = pushUsedLocals;

        JNullLiteral litNull = program.getLiteralNull();
        JBinaryOperation operation = new JBinaryOperation(program,
            x.getLhs().getType(), JBinaryOperator.DIV, litNull, litNull);
        JBinaryOperation asg = new JBinaryOperation(program,
            x.getLhs().getType(), JBinaryOperator.ASG, litNull, operation);

        ChangeList myChangeList = new ChangeList("Break '" + x
            + "' into two operations.");

        myChangeList.replaceExpression(operation.lhs, x.lhs);
        myChangeList.replaceExpression(operation.rhs, x.rhs);
        myChangeList.replaceExpression(asg.lhs, x.lhs);

        if (replacer.getChangeList().empty()) {
          myChangeList.replaceExpression(m, asg);
        } else {
          myChangeList.add(replacer.getChangeList());
          myChangeList.addExpression(asg, multi.exprs);
          myChangeList.replaceExpression(m, multi);
        }

        changeList.add(myChangeList);
      }
    }

    // @Override
    public void endVisit(JMethod x) {
      clearLocals();
      currentMethod = null;
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JMethod x) {
      currentMethod = x;
      clearLocals();
      return true;
    }
  }
  private class ReplaceSideEffectsInLvalue extends JVisitor {
    private final ChangeList changeList = new ChangeList(
        "Replace side effects in lvalue.");

    private final JMultiExpression multi;

    ReplaceSideEffectsInLvalue(JMultiExpression multi) {
      this.multi = multi;
    }

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JArrayRef x, Mutator m) {
      possiblyReplace(x.instance);
      possiblyReplace(x.indexExpr);
      return false;
    }

    // @Override
    public boolean visit(JFieldRef x, Mutator m) {
      if (x.getInstance() != null) {
        possiblyReplace(x.instance);
      }
      return false;
    }

    // @Override
    public boolean visit(JLocalRef x, Mutator m) {
      return false;
    }

    // @Override
    public boolean visit(JParameterRef x, Mutator m) {
      return false;
    }

    // @Override
    public boolean visit(JThisRef x, Mutator m) {
      return false;
    }

    private void possiblyReplace(Holder x) {
      if (!x.get().hasSideEffects()) {
        return;
      }

      // Create a temp local
      JLocal tempLocal = getTempLocal();

      // Create an assignment for this temp and add it to multi.
      JLocalRef tempRef = new JLocalRef(program, tempLocal);
      JBinaryOperation asg = new JBinaryOperation(program, x.get().getType(),
          JBinaryOperator.ASG, tempRef, program.getLiteralNull());
      changeList.replaceExpression(asg.rhs, x);
      changeList.addExpression(asg, multi.exprs);
      // Update me with the temp
      changeList.replaceExpression(x, tempRef);
    }
  }

  public static void exec(JProgram program) {
    new CompoundAssignmentNormalizer(program).execImpl();
  }

  private JMethod currentMethod;

  private final List/* <JLocal> */tempLocals = new ArrayList/* <JLocal> */();

  private int localIndex;

  private final JProgram program;

  private CompoundAssignmentNormalizer(JProgram program) {
    this.program = program;
  }

  private void clearLocals() {
    tempLocals.clear();
    localIndex = 0;
  }

  private void execImpl() {
    BreakupAssignOpsVisitor breaker = new BreakupAssignOpsVisitor();
    program.traverse(breaker);
    ChangeList changes = breaker.getChangeList();
    if (!changes.empty()) {
      changes.apply();
    }
  }

  private JLocal getTempLocal() {
    if (localIndex < tempLocals.size()) {
      return (JLocal) tempLocals.get(localIndex++);
    }
    JLocal newTemp = program.createLocal(("$t" + localIndex++).toCharArray(),
        program.getTypeVoid(), false, currentMethod);
    tempLocals.add(newTemp);
    return newTemp;
  }

}
