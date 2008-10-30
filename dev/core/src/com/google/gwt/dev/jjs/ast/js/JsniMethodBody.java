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
import com.google.gwt.dev.jjs.ast.JAbstractMethodBody;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a the body of a method. Can be Java or JSNI.
 */
public class JsniMethodBody extends JAbstractMethodBody {

  public final List<JsniFieldRef> jsniFieldRefs = new ArrayList<JsniFieldRef>();
  public final List<JsniMethodRef> jsniMethodRefs = new ArrayList<JsniMethodRef>();
  private final Set<String> stringLiterals = new HashSet<String>();

  private JsFunction jsFunction = null;

  public JsniMethodBody(JProgram program, SourceInfo info) {
    super(program, info);
  }

  public JsFunction getFunc() {
    assert (this.jsFunction != null);
    return jsFunction;
  }

  public Set<String> getUsedStrings() {
    return stringLiterals;
  }

  @Override
  public boolean isNative() {
    return true;
  }

  public void setFunc(JsFunction jsFunction) {
    assert (this.jsFunction == null);
    this.jsFunction = jsFunction;
    class RecordStrings extends JsVisitor {
      @Override
      public void endVisit(JsStringLiteral lit, JsContext<JsExpression> ctx) {
        stringLiterals.add(lit.getValue());
      }
    }
    (new RecordStrings()).accept(jsFunction);
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(jsniFieldRefs);
      visitor.accept(jsniMethodRefs);
    }
    visitor.endVisit(this, ctx);
  }
}
