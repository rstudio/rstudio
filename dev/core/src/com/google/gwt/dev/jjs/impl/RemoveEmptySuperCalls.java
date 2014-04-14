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
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JExpressionStatement;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewInstance;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.js.JMultiExpression;

/**
 * Removes calls to no-op super constructors.
 */
public class RemoveEmptySuperCalls {

  /**
   * Removes calls to no-op super constructors.
   */
  public static class EmptySuperCallVisitor extends JModVisitor {
    @Override
    public void endVisit(JExpressionStatement x, Context ctx) {
      if (x.getExpr() instanceof JMethodCall && !(x.getExpr() instanceof JNewInstance)) {
        JMethodCall call = (JMethodCall) x.getExpr();
        if (call.getTarget() instanceof JConstructor) {
          JConstructor ctor = (JConstructor) call.getTarget();
          if (JProgram.isJsInterfacePrototype(ctor.getEnclosingType())) {
            // don't remove calls to JsInterface super-constructors;
            return;
          }
          if (ctor.isEmpty()) {
            // TODO: move this 3-way into Simplifier.
            if (call.getArgs().isEmpty()) {
              ctx.removeMe();
            } else if (call.getArgs().size() == 1) {
              ctx.replaceMe(call.getArgs().get(0).makeStatement());
            } else {
              JMultiExpression multi = new JMultiExpression(call.getSourceInfo());
              multi.addExpressions(call.getArgs());
              ctx.replaceMe(multi.makeStatement());
            }
          }
        }
      }
    }
  }

  public static boolean exec(JProgram program) {
    EmptySuperCallVisitor v = new EmptySuperCallVisitor();
    v.accept(program);
    return v.didChange();
  }
}
