/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

/**
 * Verifies that the generator returns sourcemaps whose keys are the correct ranges of
 * JavaScript. (Doesn't check that they point to the right place.)
 */
public class JsReportGenerationVisitorTest extends TestCase {
  boolean compact = false;
  JsProgram program;

  // TODO(skybrian) don't generate ranges for all of these nodes. Just do executable code.
  // (Documenting it as-is for now.)

  public void testEmpty() throws Exception {
    program = parseJs("");
    checkMappings();
  }

  public void testOneStatement() throws Exception {
    program = parseJs("x = 1");
    checkMappings(
        "x = 1;\n",
        "x",
        "1"
    );
  }

  public void testTwoStatementsCompact() throws Exception {
    compact = true;
    program = parseJs("x = 1; y = 2");
    checkMappings(
        "x=1;y=2;",
        "x=1;",
        "x",
        "1",
        "y=2;",
        "y",
        "2"
    );
  }

  public void testTwoStatementsPretty() throws Exception {
    program = parseJs("x = 1; y = 2");
    checkMappings(
        "x = 1;\ny = 2;\n",
        "x = 1;\n",
        "x",
        "1",
        "y = 2;\n",
        "y",
        "2"
    );
  }

  public void testIfStatementCompact() throws Exception {
    compact = true;
    program = parseJs("if(true) { x=1 } else { y=2 }");
    checkMappings(
        "if(true){x=1}else{y=2}",
        "true",
        "{x=1}",
        "x=1",
        "x",
        "1",
        "{y=2}",
        "y=2",
        "y",
        "2"
    );
  }

  public void testIfStatementPretty() throws Exception {
    program = parseJs("if(true) { x=1 } else { y=2 }");
    checkMappings(
        "if (true) {\n  x = 1;\n}\n else {\n  y = 2;\n}\n",
        "true",
        "{\n  x = 1;\n}\n",
        "  x = 1;\n",
        "  x",
        "1",
        "{\n  y = 2;\n}\n",
        "  y = 2;\n",
        "  y",
        "2"
    );
  }

  public void testFunctionCompact() throws Exception {
    compact = true;
    program = parseJs("function f() { return 42; }");
    checkMappings("function f(){return 42}\n",
        "function f(){return 42}",
        "{return 42}",
        "return 42",
        "42"
    );
  }

  public void testFunctionPretty() throws Exception {
    program = parseJs("function f() { return 42; }");
    checkMappings(
      "function f(){\n  return 42;\n}\n\n",
      "function f(){\n  return 42;\n}\n",
      "{\n  return 42;\n}\n",
      "  return 42;\n",
      "42"
    );
  }

  private JsProgram parseJs(String js) throws IOException, JsParserException {
    JsProgram program = new JsProgram();
    SourceInfo info = SourceOrigin.create(0, js.length(), 123, "test.js");
    List<JsStatement> statements = JsParser.parse(info, program.getScope(),
        new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(statements);
    return program;
  }

  private void checkMappings(String ...expectedLines)
      throws IOException, JsParserException {
    DefaultTextOutput text = new DefaultTextOutput(compact);
    JsReportGenerationVisitor generator = new JsReportGenerationVisitor(text,
        JavaToJavaScriptMap.EMPTY);
    generator.accept(program);
    String actual = dumpMappings(text.toString(), generator.getSourceInfoMap());

    StringBuilder expected = new StringBuilder();
    expected.append("Mappings:\n");
    for (String line : expectedLines) {
      expected.append(escape(line));
      expected.append("\n");
    }

    assertEquals(expected.toString(), actual);
  }

  private String dumpMappings(String javascript, JsSourceMap mappings) {
    List<Range> ranges = Lists.newArrayList(mappings.keySet());
    Collections.sort(ranges, Range.DEPENDENCY_ORDER_COMPARATOR);

    StringBuilder out = new StringBuilder();
    out.append("Mappings:\n");
    for (Range r : ranges) {
      String js = javascript.substring(r.getStart(), r.getEnd());
      out.append(escape(js));
      out.append("\n");
    }
    return out.toString();
  }

  /**
   * Escape newlines in a readable way so that each range is on one line for comparison.
   */
  private String escape(String js) {
    return js.replace("\\n", "\\\\n").replace("\n", "\\n");
  }
}
