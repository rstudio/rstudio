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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JDeclarationStatement;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JIfStatement;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JLocalRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStatement;
import com.google.gwt.dev.jjs.ast.JThrowStatement;
import com.google.gwt.dev.jjs.ast.JTryStatement;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge multi-catch blocks into a single catch block that uses instanceof tests
 * to determine which user block to run.
 */
public class CatchBlockNormalizer {

  /**
   * Collapses all multi-catch blocks into a single catch block.
   */
  private class CollapseCatchBlocks extends JModVisitor {

    // @Override
    @Override
    public void endVisit(JMethodBody x, Context ctx) {
      clearLocals();
      currentMethodBody = null;
    }

    // @Override
    @Override
    public void endVisit(JTryStatement x, Context ctx) {
      if (x.getCatchBlocks().isEmpty()) {
        return;
      }

      SourceInfo catchInfo = x.getCatchBlocks().get(0).getSourceInfo();
      JLocal exVar = popTempLocal();
      JBlock newCatchBlock = new JBlock(catchInfo);

      {
        // $e = Exceptions.caught($e)
        JMethod caughtMethod = program.getIndexedMethod("Exceptions.caught");
        JMethodCall call = new JMethodCall(catchInfo, null, caughtMethod);
        call.addArg(new JLocalRef(catchInfo, exVar));
        newCatchBlock.addStmt(JProgram.createAssignmentStmt(catchInfo, new JLocalRef(catchInfo,
            exVar), call));
      }

      /*
       * Build up a series of if, else if statements to test the type of the
       * exception object against the type of the user's catch block.
       * 
       * Go backwards so we can nest the else statements in the correct order!
       */
      // rethrow the current exception if no one caught it
      JStatement cur = new JThrowStatement(catchInfo, new JLocalRef(catchInfo, exVar));
      for (int i = x.getCatchBlocks().size() - 1; i >= 0; --i) {
        JBlock block = x.getCatchBlocks().get(i);
        JLocalRef arg = x.getCatchArgs().get(i);
        catchInfo = block.getSourceInfo();
        JReferenceType argType = (JReferenceType) arg.getType();
        // if ($e instanceof ArgType) { var userVar = $e; <user code> }
        JExpression ifTest = new JInstanceOf(catchInfo, argType, new JLocalRef(catchInfo, exVar));
        JDeclarationStatement declaration =
            new JDeclarationStatement(catchInfo, arg, new JLocalRef(catchInfo, exVar));
        block.addStmt(0, declaration);
        // nest the previous as an else for me
        cur = new JIfStatement(catchInfo, ifTest, block, cur);
      }

      newCatchBlock.addStmt(cur);
      x.getCatchArgs().clear();
      x.getCatchArgs().add(new JLocalRef(newCatchBlock.getSourceInfo(), exVar));
      x.getCatchBlocks().clear();
      x.getCatchBlocks().add(newCatchBlock);
    }

    // @Override
    @Override
    public boolean visit(JMethodBody x, Context ctx) {
      currentMethodBody = x;
      clearLocals();
      return true;
    }

    // @Override
    @Override
    public boolean visit(JTryStatement x, Context ctx) {
      if (!x.getCatchBlocks().isEmpty()) {
        pushTempLocal(x.getSourceInfo());
      }
      return true;
    }
  }

  public static void exec(JProgram program) {
    new CatchBlockNormalizer(program).execImpl();
  }

  private JMethodBody currentMethodBody;
  private int localIndex;
  private final JProgram program;
  private final List<JLocal> tempLocals = new ArrayList<JLocal>();

  private CatchBlockNormalizer(JProgram program) {
    this.program = program;
  }

  private void clearLocals() {
    tempLocals.clear();
    localIndex = 0;
  }

  private void execImpl() {
    CollapseCatchBlocks collapser = new CollapseCatchBlocks();
    collapser.accept(program);
  }

  private JLocal popTempLocal() {
    return tempLocals.get(--localIndex);
  }

  private void pushTempLocal(SourceInfo sourceInfo) {
    if (localIndex == tempLocals.size()) {
      JLocal newTemp =
          JProgram.createLocal(sourceInfo, "$e" + localIndex, program.getTypeJavaLangObject(),
              false, currentMethodBody);
      tempLocals.add(newTemp);
    }
    ++localIndex;
  }

}
