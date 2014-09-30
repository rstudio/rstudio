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
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.List;

/**
 * A utility base type for writing tests for JS optimizers.
 */
public abstract class OptimizerTestBase extends TestCase {

  protected static JsProgram parseJs(String js) throws IOException, JsParserException {
    JsProgram program = new JsProgram();
    List<JsStatement> statements = JsParser.parse(SourceOrigin.UNKNOWN, program.getScope(),
        new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(statements);
    return program;
  }

  /**
   * Optimize a JS program.
   *
   * @param js the source program
   * @param toExec a list of classes that implement
   *          <code>static void exec(JsProgram)</code>
   * @return optimized JS
   */
  protected String optimize(String js, Class<?>... toExec) throws Exception {
    JsProgram program = new JsProgram();
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));

    program.getGlobalBlock().getStatements().addAll(expected);

    return optimize(program, toExec);
  }

  /**
   * Optimize a JS program.
   *
   * @param program the source program
   * @param toExec a list of classes that implement
   *          <code>static void exec(JsProgram)</code>
   * @return optimized JS
   */
  protected String optimize(JsProgram program, Class<?>... toExec) throws Exception {

    for (Class<?> clazz : toExec) {
      Method m = clazz.getMethod("exec", JsProgram.class);
      m.invoke(null, program);
    }

    return getOutputJs(program);
  }

  /**
   * Returns the source representation of the JsProgram. Removes the first (empty) line.
   */
  protected static String getOutputJs(JsProgram program) {
    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);

    generator.accept(program);
    String outputJs = text.toString();
    assert outputJs.charAt(0) == '\n';
    return outputJs.substring(1);
  }
}