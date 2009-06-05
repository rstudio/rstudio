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

import java.io.StringReader;
import java.util.List;

public class JsStaticEvalTest extends TestCase {

  public void testLiteralEqNull() throws Exception {
    assertTrue(optimize("alert('test' == null)").equals("alert(false);"));
  }

  public void testLiteralNeNull() throws Exception {
    assertTrue(optimize("alert('test' != null)").equals("alert(true);"));
  }

  public void testNullEqNull() throws Exception {
    assertTrue(optimize("alert(null == null)").equals("alert(true);"));
  }

  public void testNullNeNull() throws Exception {
    assertTrue(optimize("alert(null != null)").equals("alert(false);"));
  }

  private String optimize(String js) throws Exception {
    JsProgram program = new JsProgram();
    List<JsStatement> expected = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(expected);
    
    // Run the static evaluation over this new program
    JsStaticEval.exec(program);

    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);
    
    generator.accept(program);
    return text.toString();
  }
}
