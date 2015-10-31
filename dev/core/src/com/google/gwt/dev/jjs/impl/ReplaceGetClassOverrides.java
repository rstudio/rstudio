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
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.RuntimeConstants;

/**
 * Prune all overrides of Object.getClass() and inline all method calls to Object.getClass()
 * as Object.___clazz.
 * <p>
 * The Devirtualizer pass needs to run before this pass.
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
      getClassMethod = program.getIndexedMethod(RuntimeConstants.OBJECT_GET_CLASS);
      clazzField = program.getIndexedField(RuntimeConstants.OBJECT_CLAZZ);
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      if (!isGetClassMethod(x)) {
        return;
      }
      
      if (x.getEnclosingType() == program.getTypeJavaLangObject()) {
        // Remove all overrides but keep the method in java.lang.Object as references from JSNI
        // are not removed.
        x.getOverridingMethods().clear();
        return;
      }
      ctx.removeMe();
    }

    @Override
    public void endVisit(JMethodCall x, Context ctx) {

      // All calls to getClass with reference to Object.clazz field
      if (isGetClassMethod(x.getTarget())) {
        assert !isGetClassDevirtualized(x.getTarget().getEnclosingType());
        ctx.replaceMe(new JFieldRef(x.getSourceInfo(), x.getInstance(),
            clazzField, clazzField.getEnclosingType()));
      }
    }

    private boolean isGetClassMethod(JMethod method) {
      return method == getClassMethod || method.getOverriddenMethods().contains(getClassMethod);
    }

    private boolean isGetClassDevirtualized(JType type) {
      return type == program.getJavaScriptObject()
          || program.getRepresentedAsNativeTypes().contains(type)
          || type.isJsNative();
    }
  }
}
