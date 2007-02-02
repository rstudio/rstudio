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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.ast.JSourceInfo;
import com.google.gwt.dev.js.ast.JsFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * A Java native method that is implemented in JSNI code.
 */
public class JsniMethod extends JMethod {

  public final List/* <JsniFieldRef> */jsniFieldRefs = new ArrayList/* <JsniFieldRef> */();
  public final List/* <JsniMethodRef> */jsniMethodRefs = new ArrayList/* <JsniMethodRef> */();
  private JsFunction jsFunction = null;

  public JsniMethod(JProgram program, JSourceInfo info,
      String name, JReferenceType enclosingType, JType returnType,
      boolean isStatic, boolean isFinal, boolean isPrivate) {
    super(program, info, name, enclosingType, returnType, false, isStatic,
        isFinal, isPrivate);
  }

  public JsFunction getFunc() {
    assert (this.jsFunction != null);
    return jsFunction;
  }

  public boolean isNative() {
    return true;
  }

  public void setFunc(JsFunction jsFunction) {
    assert (this.jsFunction == null);
    this.jsFunction = jsFunction;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(params);
      visitor.accept(jsniFieldRefs);
      visitor.accept(jsniMethodRefs);
    }
    visitor.endVisit(this, ctx);
  }

}
