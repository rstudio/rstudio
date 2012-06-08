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

import com.google.gwt.dev.jjs.SourceOrigin;

import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.DefaultTextOutput;

import java.lang.reflect.Method;
import java.io.StringReader;
import java.util.List;


/**
 * Tests the JsStaticEval optimizer.
 */
public class JsDuplicateFunctionRemoverTest extends OptimizerTestBase {

  public void testDontRemoveCtors() throws Exception {
    // As fieldref qualifier
    assertEquals("function a(){}\n;function b(){}\nb.prototype={};a();b();",
        optimize("function a(){};function b(){} b.prototype={}; a(); b();"));
    // As parameter
    assertEquals(
        "function defineSeed(a,b){}\n;function a(){}\n;function b(){}\ndefineSeed(a,b);a();b();",
        optimize("function defineSeed(a,b){};function a(){};function b(){} defineSeed(a,b); a(); b();"));
  }

  public void testRemoveDuplicates() throws Exception {
    assertEquals("function a(){}\n;a();a();",
        optimize("function a(){};function b(){} a(); b();"));
  }

  public void testVirtualRemoveDuplicates() throws Exception {
    JsProgram program = new JsProgram();
    String js = "_.method1=function(){};_.method2=function(){};_.method1();_.method2();";
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
      program.getScope(), new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(expected);
    new JsModVisitor() {
	public void endVisit(JsFunction func, JsContext ctx) {
          func.setFromJava(true);
        }
    }.accept(program);

    assertEquals("_.method1=_DUP0;_.method2=_DUP0;_.method1();_.method2();function _DUP0(){}\n",
		 optimize(program, JsSymbolResolver.class, JsDuplicateFunctionRemover.class,
                         JsUnusedFunctionRemover.class));		 
  }

  private String optimize(String js) throws Exception {
    return optimize(js, JsSymbolResolver.class,
        JsDuplicateFunctionRemover.class, JsUnusedFunctionRemover.class);
  }


  /**
   * Optimize a JS program.
   * 
   * @param js the source program
   * @param toExec a list of classes that implement
   *          <code>static void exec(JsProgram)</code>
   * @return optimized JS
   */
  protected String optimize(JsProgram program, Class<?>... toExec) throws Exception {
 
    for (Class<?> clazz : toExec) {
      Method m = clazz.getMethod("exec", JsProgram.class);
      m.invoke(null, program);
    }

    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);

    generator.accept(program);
    return text.toString();
  }
}
