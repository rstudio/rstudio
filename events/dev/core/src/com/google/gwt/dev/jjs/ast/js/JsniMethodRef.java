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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * A call to a JSNI method.
 */
public class JsniMethodRef extends JMethodCall {

  private final String ident;

  public JsniMethodRef(JProgram program, SourceInfo info, String ident,
      JMethod method) {
    // Just use a null literal as the qualifier on a non-static method
    super(program, info, method.isStatic() ? null : program.getLiteralNull(),
        method);
    this.ident = ident;
  }

  public String getIdent() {
    return ident;
  }

  @Override
  public JType getType() {
    // If JavaScriptObject type is not available, just return the Object type
    JClassType jsoType = program.getJavaScriptObject();
    return (jsoType != null) ? jsoType : program.getTypeJavaLangObject();
  }

  @Override
  public boolean hasSideEffects() {
    return false;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
