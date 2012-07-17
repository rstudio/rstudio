/*
 * Copyright 2012 Google Inc.
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

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Tests for CoverageInstrumentor.
 */
public class CoverageInstrumentorTest extends TestCase {
  private JsProgram program;
  private JsBlock functionBody;

  @Override
  public void setUp() {
    program = new JsProgram();
    SourceInfo info = program.createSourceInfo(1, "Test.java");
    JsBlock globalBlock = program.getGlobalBlock();
    JsFunction function = new JsFunction(info, program.getScope());
    functionBody = new JsBlock(info);
    function.setBody(functionBody);
    globalBlock.getStatements().add(new JsExprStmt(info, function));
  }

  private String instrument(String code) throws Exception {
    functionBody.getStatements().clear();
    CoverageInstrumentor.exec(program, parse(code));
    return functionBody.toSource().trim().replaceAll("\\s+", " ");
  }

  private String instrumentedProgram(Multimap<String, Integer> baseline) {
    CoverageInstrumentor.exec(program, baseline);
    return program.toSource();
  }

  private String instrumentedProgram() {
    return instrumentedProgram(HashMultimap.<String, Integer>create());
  }

  private Multimap<String, Integer> parse(String code) throws Exception {
    Iterable<String> lines = Splitter.on('\n').split(code);
    Multimap<String, Integer> instrumentableLines = HashMultimap.create();
    int i = 0;
    for (String line : lines) {
      instrumentableLines.put("Test.java", ++i);
    }
    JsParser.parseInto(functionBody.getSourceInfo(), program.getScope(), 
        functionBody, new StringReader(code));
    i = 0;
    for (JsStatement statement : functionBody.getStatements()) {
      final SourceInfo info = program.createSourceInfo(++i, "Test.java");
      statement.setSourceInfo(info);
      new CoverageVisitor(instrumentableLines.keySet()) {
        @Override public void endVisit(JsExpression x, JsContext ctx) {
          x.setSourceInfo(info);
        }
      }.accept(statement);
    }
    return instrumentableLines;
  }

  public void testBaselineCoverage() throws Exception {
    Multimap<String, Integer> baselineCoverage = LinkedHashMultimap.create();
    for (int i = 1; i < 6; i++) {
      baselineCoverage.put("A.java", i);
      baselineCoverage.put("B.java", i);
    }
    assertTrue(instrumentedProgram(baselineCoverage).contains(new StringBuilder()
        .append("var $coverage = {'A.java':{1:0, 2:0, 3:0, 4:0, 5:0}, ")
        .append("'B.java':{1:0, 2:0, 3:0, 4:0, 5:0}}")));
  }

  public void testBeforeUnloadListenerExists() throws Exception {
    String program = instrumentedProgram();
    assertTrue(program.contains("var merge_coverage"));
    assertTrue(program.contains("var merge"));
    assertTrue(program.contains("window.onbeforeunload = function()"));
  }

  public void testSimpleInstrumentation() throws Exception {
    assertEquals("{ $coverage['Test.java'][1] = 1 , f(); }", instrument("f()"));
    assertEquals("{ $coverage['Test.java'][1] = 1 , f(); $coverage['Test.java'][2] = 1 , g(); }",
        instrument("f() \n g()"));
  }

  public void testPreserveLiterals() throws Exception {
    assertEquals("{ 'x'; }", instrument("'x'"));
  }
}