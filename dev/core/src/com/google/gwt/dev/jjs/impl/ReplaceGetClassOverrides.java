/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Prune all overrides of Object.getClass() except when the enclosing class is JavaScriptObject
 * (to permit getClass() devirtualization in JsoDevirtualizer to continue to work).
 * Also Inline all method calls to Object.getClass() as Object.clazz.
 */
public class ReplaceGetClassOverrides {
  public static void exec(JProgram program) {
    new GetClassInlinerRemover(program).accept(program);
  }

  private static class GetClassInlinerRemover extends JModVisitor {

    private JProgram program;
    private JMethod getClassMethod;
    private JField clazzField;

    public GetClassInlinerRemover(JProgram program) {
      this.program = program;
      getClassMethod = program.getIndexedMethod("Object.getClass");
      clazzField = program.getIndexedField("Object.___clazz");
    }

    public void endVisit(JMethod x, Context ctx) {
      // don't prune JSO.getClass()
      if (x.getEnclosingType() == program.getJavaScriptObject()) {
        return;
      }
      if (x.getOverrides().contains(getClassMethod)) {
        ctx.removeMe();
      }
    }

    public void endVisit(JMethodCall x, Context ctx) {
      // don't inline JSO.getClass()
      if (x.getTarget().getEnclosingType() == program.getJavaScriptObject()) {
        return;
      }
      // replace overridden getClass() with reference to Object.clazz field
      if (x.getTarget() == getClassMethod ||
          x.getTarget().getOverrides().contains(getClassMethod)) {
        ctx.replaceMe(new JFieldRef(x.getSourceInfo(), x.getInstance(),
            clazzField, clazzField.getEnclosingType()));
      }
    }
  }
}
