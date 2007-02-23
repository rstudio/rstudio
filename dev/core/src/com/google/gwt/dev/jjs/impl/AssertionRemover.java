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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JAssertStatement;
import com.google.gwt.dev.jjs.ast.JBlock;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JStatement;

/**
 * Removes all assertion statements from the AST.
 */
public class AssertionRemover {

  /**
   * Removes all asserts.
   */
  public class AssertVisitor extends JModVisitor {

    public void endVisit(JAssertStatement x, Context ctx) {
      removeMe(x, ctx);
    }

    private void removeMe(JStatement stmt, Context ctx) {
      if (ctx.canRemove()) {
        ctx.removeMe();
      } else {
        // empty block statement
        ctx.replaceMe(new JBlock(program, stmt.getSourceInfo()));
      }
    }
  }

  public static void exec(JProgram program) {
    new AssertionRemover(program).execImpl();
  }

  private final JProgram program;

  public AssertionRemover(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    AssertVisitor assertVisitor = new AssertVisitor();
    assertVisitor.accept(program);
  }
}
