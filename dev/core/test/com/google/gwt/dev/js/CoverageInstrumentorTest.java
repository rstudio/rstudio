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
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.thirdparty.guava.common.base.Splitter;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import junit.framework.TestCase;

import java.io.StringReader;
import java.util.Map;

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
    program.setIndexedFields(fields("CoverageUtil.coverage"));
    program.setIndexedFunctions(functions("CoverageUtil.cover", "CoverageUtil.onBeforeUnload"));
    JsBlock globalBlock = program.getGlobalBlock();
    JsFunction function = new JsFunction(info, program.getScope());
    functionBody = new JsBlock(info);
    function.setBody(functionBody);
    globalBlock.getStatements().add(new JsExprStmt(info, function));
  }

  private Map<String, JsName> fields(String... names) {
    Map<String, JsName> fields = Maps.newHashMap();
    for (String name : names) {
      JsName n = program.getScope().declareName(name, name);
      fields.put(name, n);
    }
    return fields;
  }

  private Map<String, JsFunction> functions(String... names) {
    Map<String, JsFunction> funcs = Maps.newHashMap();
    for (String name : names) {
      JsFunction f = new JsFunction(program.getSourceInfo(), program.getScope());
      f.setName(program.getScope().declareName(name));
      funcs.put(name, f);
    }
    return funcs;
  }

  private String instrument(String code) throws Exception {
    functionBody.getStatements().clear();
    CoverageInstrumentor.exec(program, parse(code));
    return functionBody.toSource().trim().replaceAll("\\s+", " ");
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
    Multimap<String, Integer> instrumentableLines = LinkedHashMultimap.create();
    for (int i = 1; i < 6; i++) {
      instrumentableLines.put("A.java", i);
      instrumentableLines.put("B.java", i);
    }

    JsObjectLiteral baseline = CoverageInstrumentor.baselineCoverage(program.getSourceInfo(),
        instrumentableLines);
    assertEquals("{'A.java':{1:0, 2:0, 3:0, 4:0, 5:0}, 'B.java':{1:0, 2:0, 3:0, 4:0, 5:0}}",
        baseline.toSource().trim().replaceAll("\\s+", " "));
  }

  public void testSimpleInstrumentation() throws Exception {
    assertEquals("{ CoverageUtil.cover('Test.java', 1) , f(); }", instrument("f()"));
    assertEquals(
        "{ CoverageUtil.cover('Test.java', 1) , f(); CoverageUtil.cover('Test.java', 2) , g(); }",
        instrument("f() \n g()"));
  }

  public void testPreserveLiterals() throws Exception {
    assertEquals("{ 'x'; }", instrument("'x'"));
  }
}