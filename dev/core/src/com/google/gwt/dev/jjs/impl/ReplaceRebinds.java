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
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.Mutator;
import com.google.gwt.dev.jjs.ast.change.ChangeList;

/**
 * Replaces any "GWT.create()" calls with a new expression for the actual result
 * of the deferred binding decision.
 */
public class ReplaceRebinds {

  private class RebindVisitor extends JVisitor {

    private final ChangeList changeList = new ChangeList(
        "Replace GWT.create() with new expressions.");

    // @Override
    public void endVisit(JMethodCall x, Mutator mutator) {
      JMethod method = x.getTarget();
      if (method == program.getRebindCreateMethod()) {
        assert (x.args.size() == 1);
        JExpression arg = x.args.getExpr(0);
        assert (arg instanceof JClassLiteral);
        JClassLiteral classLiteral = (JClassLiteral) arg;
        JClassType classType = program.rebind(classLiteral.refType);

        /*
         * Find the appropriate (noArg) constructor. In our AST, constructors
         * are instance methods that should be qualified with a new expression.
         */

        JMethod noArgCtor = null;
        for (int i = 0; i < classType.methods.size(); ++i) {
          JMethod ctor = (JMethod) classType.methods.get(i);
          if (ctor.getName().equals(classType.getShortName())) {
            if (ctor.params.size() == 0) {
              noArgCtor = ctor;
            }
          }
        }
        assert (noArgCtor != null);
        // Call it, using a new expression as a qualifier
        JNewInstance newInstance = new JNewInstance(program, classType);
        JMethodCall call = new JMethodCall(program, newInstance, noArgCtor);
        changeList.replaceExpression(mutator, new Holder(call));
      }
    }

    public ChangeList getChangeList() {
      return changeList;
    }
  }

  public static boolean exec(JProgram program) {
    return new ReplaceRebinds(program).execImpl();
  }

  private final JProgram program;

  private ReplaceRebinds(JProgram program) {
    this.program = program;
  }

  private boolean execImpl() {
    RebindVisitor rebinder = new RebindVisitor();
    program.traverse(rebinder);
    ChangeList changes = rebinder.getChangeList();
    if (changes.empty()) {
      return false;
    }
    changes.apply();
    return true;
  }

}
