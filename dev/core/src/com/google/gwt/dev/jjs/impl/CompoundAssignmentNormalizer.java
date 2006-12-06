// Copyright 2006 Google Inc. All Rights Reserved.
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

  private JMethod fCurrentMethod;
  private final List/*<JLocal>*/ fTempLocals = new ArrayList/*<JLocal>*/();
  private int fLocalIndex;

  private void clearLocals() {
    fTempLocals.clear();
    fLocalIndex = 0;
  }

  private JLocal getTempLocal() {
    if (fLocalIndex < fTempLocals.size()) {
      return (JLocal) fTempLocals.get(fLocalIndex++);
    }
    JLocal newTemp = program.createLocal(("$t" + fLocalIndex++).toCharArray(),
      program.getTypeVoid(), false, fCurrentMethod);
    fTempLocals.add(newTemp);
    return newTemp;
  }

  private class ReplaceSideEffectsInLvalue extends JVisitor {
    private final ChangeList changeList = new ChangeList(
      "Replace side effects in lvalue.");

    public ChangeList getChangeList() {
      return changeList;
    }

    private final JMultiExpression multi;

    ReplaceSideEffectsInLvalue(JMultiExpression multi) {
      this.multi = multi;
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

  private class BreakupAssignOpsVisitor extends JVisitor {
    private final ChangeList changeList = new ChangeList(
      "Break apart certain complex assignments.");

    public ChangeList getChangeList() {
      return changeList;
    }

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
        final int pushUsedLocals = fLocalIndex;
        JMultiExpression multi = new JMultiExpression(program);
        ReplaceSideEffectsInLvalue replacer = new ReplaceSideEffectsInLvalue(
          multi);
        x.lhs.traverse(replacer);
        fLocalIndex = pushUsedLocals;

        JNullLiteral litNull = program.getLiteralNull();
        JBinaryOperation operation = new JBinaryOperation(program, x.getLhs()
          .getType(), JBinaryOperator.DIV, litNull, litNull);
        JBinaryOperation asg = new JBinaryOperation(program, x.getLhs()
          .getType(), JBinaryOperator.ASG, litNull, operation);

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
      fCurrentMethod = null;
    }

    // @Override
    public boolean visit(JMethod x) {
      fCurrentMethod = x;
      clearLocals();
      return true;
    }
  }

  private void execImpl() {
    BreakupAssignOpsVisitor breaker = new BreakupAssignOpsVisitor();
    program.traverse(breaker);
    ChangeList changes = breaker.getChangeList();
    if (!changes.empty()) {
      changes.apply();
    }
  }

  private final JProgram program;

  private CompoundAssignmentNormalizer(JProgram program) {
    this.program = program;
  }

  public static void exec(JProgram program) {
    new CompoundAssignmentNormalizer(program).execImpl();
  }

}
