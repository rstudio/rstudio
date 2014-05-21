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

  // If true, list all the JavaScript ranges where a Java method might be inlined.
  boolean includeInlinedRanges = false;

  JsProgram program;

  public void testEmpty() throws Exception {
    program = parseJs("");
    checkMappings();
  }

  public void testAssignmentDraft() throws Exception {
    program = parseJs("x = 1");
    checkMappings(
        "x = 1;\n"
    );
  }

  public void testAssignmentCompact() throws Exception {
    compact = true;
    program = parseJs("x = 1");
    checkMappings(
        "x=1;"
    );
  }

  public void testAssignmentCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
    program = parseJs("x = 1");
    checkMappings(
        "x=1;",
        "x",
        "1"
    );
  }

  public void testTwoStatementsDraft() throws Exception {
    program = parseJs("x = 1; y = 2");
    checkMappings(
        "x = 1;\ny = 2;\n",
        "x = 1;\n",
        "y = 2;\n"
    );
  }

  public void testTwoStatementsCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
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

  public void testIfStatementDraft() throws Exception {
    program = parseJs("if(true) { x=1 } else { y=2 }");
    checkMappings(
        "if (true) {\n  x = 1;\n}\n else {\n  y = 2;\n}\n",
        "  x = 1;\n",
        "  y = 2;\n"
    );
  }

  public void testIfStatementCompactInlined() throws Exception {
    includeInlinedRanges = true;
    compact = true;
    program = parseJs("if(true) { x=1 } else { y=2 }");
    checkMappings(
        "if(true){x=1}else{y=2}",
        "true",
        "x=1",
        "x",
        "1",
        "y=2",
        "y",
        "2"
    );
  }

  public void testFunctionDraft() throws Exception {
    program = parseJs("function f() { return 42; }");
    checkMappings(
        "function f(){\n  return 42;\n}\n\n",
        "function f(){\n  return 42;\n}\n",
        "  return 42;\n"
    );
  }

  public void testFunctionCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
    program = parseJs("function f() { return 42; }");
    checkMappings("function f(){return 42}\n",
        "function f(){return 42}",
        "return 42",
        "42"
    );
  }

  public void testTryStatementDraft() throws Exception {
    program = parseJs("try { 123 } catch (e) { 456 } finally { 789 }");
    checkMappings(
        "try {\n  123;\n}\n catch (e) {\n  456;\n}\n finally {\n  789;\n}\n",
        "  123;\n",
        "  456;\n",
        "  789;\n"
    );
  }

  public void testTryStatementCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
    program = parseJs("try{ 123 } catch (e) { 456 } finally { 789 }");
    checkMappings(
        "try{123}catch(e){456}finally{789}",
        "123",
        "456",
        "789"
    );
  }

  public void testDoWhileDraft() throws Exception {
    program = parseJs("do { something() } while (x>1)");
    checkMappings(
        "do {\n  something();\n}\n while (x > 1);\n",
        "  something();\n",
        "x > 1"
    );
  }

  public void testForStatementDraft() throws Exception {
    program = parseJs("for (var i = 0; i < 10; i++) { something() }");
    checkMappings(
        "for (var i = 0; i < 10; i++) {\n  something();\n}\n",
        "var i = 0;", // a separate range because it's a statement TODO: remove?
        "  something();\n"
    );
  }

  public void testForInStatementDraft() throws Exception {
    program = parseJs("for (var x in someIterable) { something() }");
    checkMappings(
        "for (var x in someIterable) {\n  something();\n}\n",
        "  something();\n"
    );
  }

  public void testSwitchStatementDraft() throws Exception {
    program = parseJs("switch (abc) { case c1: s1; break; case c2: s2; default: s3 }");
    checkMappings(
        "switch (abc) {\n  case c1:\n    s1;\n    break;\n  case c2:\n    s2;\n  default:s3;\n}\n",
        "    s1;",
        "    break;",
        "    s2;",
        "s3;"
    );
  }

  public void testSwitchStatementCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
    program = parseJs("switch (abc) { case c1: s1; break; case c2: s2; default: s3 }");
    checkMappings(
        "switch(abc){case c1:s1;break;case c2:s2;default:s3;}",
        "abc",
        "c1",
        "s1;",
        "break;",
        "c2",
        "s2;",
        "s3;"
    );
  }

  public void testWhileStatementDraft() throws Exception {
    program = parseJs("while (a>2) { a--; }");
    checkMappings(
      "while (a > 2) {\n  a--;\n}\n",
       "  a--;\n"
    );
  }

  public void testWhileStatementCompactInlined() throws Exception {
    compact = true;
    includeInlinedRanges = true;
    program = parseJs("while (a>2) { a--; }");
    checkMappings(
        "while(a>2){a--}",
        "a>2",
        "a",
        "2",
        "a--",
        "a"
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
        JavaToJavaScriptMap.EMPTY, false) {
      @Override
      boolean surroundsInJavaSource(SourceInfo parent, SourceInfo child) {
        // The Rhino-based JavaScript parser doesn't provide character ranges
        // in SourceInfo. Therefore we can't test this method directly
        // and have to mock it out.
        return !includeInlinedRanges;
      }
    };
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
