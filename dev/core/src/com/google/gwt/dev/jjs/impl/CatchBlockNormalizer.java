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

  private class CollapseCatchBlocks extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Collapse all multi-catch blocks into a single catch block.");

    // @Override
    public void endVisit(JMethod x) {
      clearLocals();
      currentMethod = null;
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

    public ChangeList getChangeList() {
      return changeList;
    }

    // @Override
    public boolean visit(JMethod x) {
      currentMethod = x;
      clearLocals();
      return true;
    }

    // @Override
    public boolean visit(JTryStatement x) {
      if (!x.catchBlocks.isEmpty()) {
        pushTempLocal();
      }
      return true;
    }
  }

  public static void exec(JProgram program) {
    new CatchBlockNormalizer(program).execImpl();
  }

  private JMethod currentMethod;

  private final List/* <JLocal> */tempLocals = new ArrayList/* <JLocal> */();

  private int localIndex;

  private final JProgram program;

  private CatchBlockNormalizer(JProgram program) {
    this.program = program;
  }

  private void clearLocals() {
    tempLocals.clear();
    localIndex = 0;
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

  private JLocal popTempLocal() {
    return (JLocal) tempLocals.get(--localIndex);
  }

  private void pushTempLocal() {
    if (localIndex == tempLocals.size()) {
      JLocal newTemp = program.createLocal(("$e" + localIndex).toCharArray(),
          program.getTypeJavaLangObject(), false, currentMethod);
      tempLocals.add(newTemp);
    }
    ++localIndex;
  }

}
