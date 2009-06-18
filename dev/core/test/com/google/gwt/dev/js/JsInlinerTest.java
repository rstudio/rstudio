/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsProgram;

/**
 * Safety checks for JsInliner.
 */
public class JsInlinerTest extends OptimizerTestBase {

  public void testSelfRecursion() throws Exception {
    String input = "function a1() { return blah && b1() }"
        + "function b1() { return bar && a1()}" + "function c() { a1() } c()";
    input = optimize(input, JsSymbolResolver.class, FixStaticRefsVisitor.class,
        JsInliner.class, JsUnusedFunctionRemover.class);

    String expected = "function a1() { return blah && bar && a1() }"
        + "function c() { a1() } c()";
    expected = optimize(expected);
    assertEquals(expected, input);
  }

  private static class FixStaticRefsVisitor extends JsModVisitor {
    @Override
    public void endVisit(JsFunction x, JsContext<JsExpression> ctx) {
      JsName name = x.getName();
      name.setStaticRef(x);
    }

    public static void exec(JsProgram program) {
      (new FixStaticRefsVisitor()).accept(program);
    }
  }
}
