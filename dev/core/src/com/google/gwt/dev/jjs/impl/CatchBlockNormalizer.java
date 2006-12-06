// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge multi-catch blocks into a single catch block that uses instanceof tests
 * to determine which user block to run.
 */
public class CatchBlockNormalizer {

  private JMethod fCurrentMethod;
  private final List/*<JLocal>*/ fTempLocals = new ArrayList/*<JLocal>*/();
  private int fLocalIndex;

  private void clearLocals() {
    fTempLocals.clear();
    fLocalIndex = 0;
  }

  private void pushTempLocal() {
    if (fLocalIndex == fTempLocals.size()) {
      JLocal newTemp = program.createLocal(("$e" + fLocalIndex).toCharArray(),
        program.getTypeJavaLangObject(), false, fCurrentMethod);
      fTempLocals.add(newTemp);
    }
    ++fLocalIndex;
  }

  private JLocal popTempLocal() {
    return (JLocal) fTempLocals.get(--fLocalIndex);
  }

  private class CollapseCatchBlocks extends JVisitor {

    private final ChangeList changeList = new ChangeList(
      "Collapse all multi-catch blocks into a single catch block.");

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public void endVisit(JTryStatement x) {
      if (x.catchBlocks.isEmpty()) {
        return;
      }

      ChangeList myChangeList = new ChangeList("Merge " + x.catchBlocks.size()
        + " catch blocks.");
      JLocal exObj = popTempLocal();
      JLocalRef exRef = new JLocalRef(program, exObj);
      JBlock newCatchBlock = new JBlock(program);
      // $e = Exceptions.caught($e)
      JMethod caughtMethod = program.getSpecialMethod("Exceptions.caught");
      JMethodCall call = new JMethodCall(program, null, caughtMethod);
      call.args.add(exRef);
      JExpressionStatement asg = program.createAssignmentStmt(exRef, call);
      newCatchBlock.statements.add(asg);

      /*
       * Build up a series of if, else if statements to test the type of the
       * exception object against the type of the user's catch block.
       * 
       * Go backwards so we can nest the else statements in the correct order!
       */
      // rethrow the current exception if no one caught it
      JStatement cur = new JThrowStatement(program, exRef);
      for (int i = x.catchBlocks.size() - 1; i >= 0; --i) {
        JBlock block = (JBlock) x.catchBlocks.get(i);
        JLocalRef arg = (JLocalRef) x.catchArgs.get(i);
        JReferenceType argType = (JReferenceType) arg.getType();
        // if ($e instanceof Argtype) { userVar = $e; <user code> }
        JExpression ifTest = new JInstanceOf(program, argType, exRef);
        asg = program.createAssignmentStmt(arg, exRef);
        myChangeList.addStatement(asg, 0, block);
        // nest the previous as an else for me
        cur = new JIfStatement(program, ifTest, block, cur);
      }

      newCatchBlock.statements.add(cur);
      myChangeList.clear(x.catchArgs);
      myChangeList.clear(x.catchBlocks);
      myChangeList.addNode(exRef, 0, x.catchArgs);
      myChangeList.addNode(newCatchBlock, 0, x.catchBlocks);
      changeList.add(myChangeList);
    }

    // @Override
    public boolean visit(JTryStatement x) {
      if (!x.catchBlocks.isEmpty()) {
        pushTempLocal();
      }
      return true;
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
    {
      CollapseCatchBlocks collapser = new CollapseCatchBlocks();
      program.traverse(collapser);
      ChangeList changes = collapser.getChangeList();
      if (!changes.empty()) {
        changes.apply();
      }
    }
  }

  private final JProgram program;

  private CatchBlockNormalizer(JProgram program) {
    this.program = program;
  }

  public static void exec(JProgram program) {
    new CatchBlockNormalizer(program).execImpl();
  }

}
