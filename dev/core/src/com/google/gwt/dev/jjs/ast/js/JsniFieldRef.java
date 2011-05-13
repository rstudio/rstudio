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
package com.google.gwt.dev.jjs.ast.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JFieldRef;
import com.google.gwt.dev.jjs.ast.JNullLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;

/**
 * JSNI reference to a Java field.
 */
public class JsniFieldRef extends JFieldRef {

  private final String ident;
  private final boolean isLvalue;

  public JsniFieldRef(SourceInfo info, String ident, JField field, JDeclaredType enclosingType,
      boolean isLvalue) {
    super(info, field.isStatic() ? null : JNullLiteral.INSTANCE, field, enclosingType);
    assert ident != null;
    this.ident = ident;
    this.isLvalue = isLvalue;
  }

  public String getIdent() {
    return ident;
  }

  public boolean isLvalue() {
    return isLvalue;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
    }
    visitor.endVisit(this, ctx);
  }
}
