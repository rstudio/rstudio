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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsVisitor;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link JsniMethodCollector}.
 */
public class JsniMethodCollectorTest extends CompilationStateTestBase {

  /**
   * TODO: currently JSNI does not parse character position. Turn this on (and
   * delete it, actually) when it does.
   */
  public static final boolean JSNI_PARSES_SOURCE_POSITION = false;

  public void testErrorPosition() {
    StringBuffer code = new StringBuffer();
    code.append("class Foo {\n");
    code.append("  native void m(Object o) /*-{\n");
    code.append("    o.@Foo::m(Ljava/lang/String);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    String source = code.toString();

    CategorizedProblem[] problems = getProblems("Foo", source);
    assertEquals(1, problems.length);
    CategorizedProblem problem = problems[0];
    if (JSNI_PARSES_SOURCE_POSITION) {
      assertEquals(source.indexOf('@'), problem.getSourceStart());
    }
    assertEquals(3, problem.getSourceLineNumber());
    assertTrue(problem.isError());
    assertEquals(
        "Referencing method 'Foo.m(Ljava/lang/String)': unable to resolve method in class 'Foo'",
        problem.getMessage());
  }

  public void testMalformedJsniRefPosition() {
    StringBuffer code = new StringBuffer();
    code.append("class Foo {\n");
    code.append("  native void m() /*-{\n");
    code.append("    @Bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    String source = code.toString();
    CategorizedProblem[] problems = getProblems("Foo", source);
    assertEquals(1, problems.length);
    CategorizedProblem problem = problems[0];
    assertEquals(source.indexOf('@') + "Bar".length(), problem.getSourceStart());
    assertEquals(3, problem.getSourceLineNumber());
    assertTrue(problem.isError());
    assertEquals("Expected \":\" in JSNI reference\n>     @Bar;\n> --------^",
        problem.getMessage());
  }

  public void testMalformedJsniRefPositionWithExtraLines() {
    StringBuffer code = new StringBuffer();
    code.append("class Foo {\n");
    code.append("  native\nvoid\nm()\n\n\n/*-{\n\n");
    code.append("    @Bar;\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    String source = code.toString();
    CategorizedProblem[] problems = getProblems("Foo", source);
    assertEquals(1, problems.length);
    CategorizedProblem problem = problems[0];
    assertEquals(source.indexOf('@') + "Bar".length(), problem.getSourceStart());
    assertEquals(9, problem.getSourceLineNumber());
    assertTrue(problem.isError());
    assertEquals("Expected \":\" in JSNI reference\n>     @Bar;\n> --------^",
        problem.getMessage());
  }

  public void testSourcePosition() {
    StringBuffer code = new StringBuffer();
    code.append("class Foo {\n");
    code.append("  native void m(Object o) /*-{\n");
    code.append("    o.@Foo::m(Ljava/lang/Object);\n");
    code.append("  }-*/;\n");
    code.append("}\n");
    String source = code.toString();

    List<JsNameRef> foundRefs = findJsniRefs("Foo", source);
    assertEquals(1, foundRefs.size());
    JsNameRef ref = foundRefs.get(0);
    SourceInfo info = ref.getSourceInfo();
    if (JSNI_PARSES_SOURCE_POSITION) {
      assertEquals(source.indexOf('@'), info.getStartPos());
    }
    assertEquals(3, info.getStartLine());
  }

  private List<JsNameRef> findJsniRefs(String typeName, final String source) {
    addGeneratedUnits(new StaticJavaResource(typeName, source));
    CompilationUnit unit = state.getCompilationUnitMap().get(typeName);
    assertNotNull(unit);
    List<JsniMethod> jsniMethods = unit.getJsniMethods();
    assertEquals(1, jsniMethods.size());
    JsniMethod jsniMethod = jsniMethods.get(0);
    final List<JsNameRef> foundRefs = new ArrayList<JsNameRef>();
    new JsVisitor() {
      @Override
      public void endVisit(JsNameRef x, JsContext ctx) {
        if (x.getIdent().startsWith("@")) {
          foundRefs.add(x);
        }
      }
    }.accept(jsniMethod.function());
    return foundRefs;
  }

  private CategorizedProblem[] getProblems(String typeName, String source) {
    addGeneratedUnits(new StaticJavaResource(typeName, source));
    CompilationUnit unit = state.getCompilationUnitMap().get(typeName);
    assertNotNull(unit);
    CategorizedProblem[] problems = unit.getProblems();
    return problems;
  }
}
