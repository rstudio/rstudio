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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.util.TextOutput;

public class JsSourceGenerationVisitor extends JsToStringGenerationVisitor {

  public JsSourceGenerationVisitor(TextOutput out, NamingStrategy namer) {
    super(out, namer);
  }

  // function foo(a, b) {
  // stmts...
  // }
  //
  public boolean visit(JsFunction x) {
    super.visit(x);
    x.getBody().traverse(this);
    needSemi = true;
    return false;
  }

  public boolean visit(JsProgram x) {
    // Descend naturally.
    return true;
  }

}
