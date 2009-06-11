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

  public void testIfWithEmptyThen() throws Exception {
    assertEquals("a();", optimize("if (a()) { }"));
  }

  public void testIfWithEmptyThenAndElseExpression() throws Exception {
    assertEquals("a()||b();", optimize("if (a()) { } else { b(); }"));
  }

  public void testIfWithEmptyThenAndElse() throws Exception {
    assertEquals("if(!a()){throw 1}",
        optimize("if (a()) { } else { throw 1; }"));
  }

  public void testIfWithEmptyThenAndEmptyElse() throws Exception {
    assertEquals("a();", optimize("if (a()) { } else { }"));
  }

  public void testIfWithThenAndEmptyElse() throws Exception {
    assertEquals("if(a()){throw 1}", optimize("if (a()) { throw 1; } else { }"));
  }

  public void testIfWithThenExpressionAndEmptyElse() throws Exception {
    assertEquals("a()&&b();", optimize("if (a()) { b() } else { }"));
  }

  public void testIfWithThenExpressionAndElseExpression() throws Exception {
    assertEquals("a()?b():c();", optimize("if (a()) { b() } else { c(); }"));
  }

  public void testIfWithThenExpressionAndElseStatement() throws Exception {
    // This can't be optimized further
    assertEquals("if(a()){b()}else{throw 1}",
        optimize("if (a()) { b() } else { throw 1; }"));
  }

  public void testLiteralEqNull() throws Exception {
    assertEquals("alert(false);", optimize("alert('test' == null)"));
  }

  public void testLiteralNeNull() throws Exception {
    assertEquals("alert(true);", optimize("alert('test' != null)"));
  }

  public void testNullEqNull() throws Exception {
    assertEquals("alert(true);", optimize("alert(null == null)"));
  }

  public void testNullNeNull() throws Exception {
    assertEquals("alert(false);", optimize("alert(null != null)"));
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
