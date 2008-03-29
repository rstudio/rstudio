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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Replaces any "GWT.create()" calls with a new expression for the actual result
 * of the deferred binding decision.
 */
public class ReplaceRebinds {

  private class RebindVisitor extends JModVisitor {

    private final JMethod rebindCreateMethod;

    public RebindVisitor(JMethod rebindCreateMethod) {
      this.rebindCreateMethod = rebindCreateMethod;
    }

    // @Override
    public void endVisit(JMethodCall x, Context ctx) {
      JMethod method = x.getTarget();
      if (method == rebindCreateMethod) {
        assert (x.getArgs().size() == 1);
        JExpression arg = x.getArgs().get(0);
        assert (arg instanceof JClassLiteral);
        JClassLiteral classLiteral = (JClassLiteral) arg;
        JClassType classType = program.rebind(classLiteral.getRefType());

        /*
         * Find the appropriate (noArg) constructor. In our AST, constructors
         * are instance methods that should be qualified with a new expression.
         */
        JMethod noArgCtor = null;
        for (int i = 0; i < classType.methods.size(); ++i) {
          JMethod ctor = classType.methods.get(i);
          if (ctor.getName().equals(classType.getShortName())) {
            if (ctor.params.size() == 0) {
              noArgCtor = ctor;
            }
          }
        }
        assert (noArgCtor != null);
        // Call it, using a new expression as a qualifier
        JNewInstance newInstance = new JNewInstance(program, x.getSourceInfo(),
            classType);
        JMethodCall call = new JMethodCall(program, x.getSourceInfo(),
            newInstance, noArgCtor);
        ctx.replaceMe(call);
      }
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
    RebindVisitor rebinder = new RebindVisitor(
        program.getIndexedMethod("GWT.create"));
    rebinder.accept(program);
    return rebinder.didChange();
  }

}
