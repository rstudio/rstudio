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
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Tests {@link JsDuplicateFunctionRemover}.
 */
public class JsDuplicateFunctionRemoverTest extends OptimizerTestBase {

  private static class MockNameGenerator implements FreshNameGenerator {
    private int counter = 0;

    @Override
    public String getFreshName() {
      return "__DUP" + counter++;
    }
  }

  // JsDuplicateFunctionRemover does not have a one parameter exec function.
  private static class JsDuplicateFunctionRemoverProxy {
    static public void exec(JsProgram program) {
      JsDuplicateFunctionRemover.exec(program, new MockNameGenerator());
    }
  }

  public void testDontRemoveCtors() throws Exception {
    // As fieldref qualifier
    assertEquals("function a(){}\n;function b(){}\nb.prototype={};a();b();",
        optimize("function a(){};function b(){} b.prototype={}; a(); b();"));
    // As parameter
    assertEquals(
        "function defineClass(a,b){}\n;function a(){}\n;function b(){}\ndefineClass(a,b);a();b();",
        optimize("function defineClass(a,b){};function a(){};function b(){}"
            + " defineClass(a,b); a(); b();"));
  }

  public void testRemoveDuplicates() throws Exception {
    assertEquals("function a(){}\n;a();a();",
        optimize("function a(){};function b(){} a(); b();"));
  }

  public void testVirtualRemoveDuplicates() throws Exception {
    JsProgram program = new JsProgram();
    String js = "_.method1=function(){};_.method2=function(){};_.method1();_.method2();";
    List<JsStatement> input = JsParser.parse(SourceOrigin.UNKNOWN,
      program.getScope(), new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(input);

    // Mark all functions as if they were translated from Java sources.
    setAllFromJava(program);

    String firstName = new MockNameGenerator().getFreshName();
    assertEquals("_.method1=" + firstName + ";_.method2=" + firstName +
        ";_.method1();_.method2();function " + firstName + "(){}\n",
        optimize(program, JsSymbolResolver.class, JsDuplicateFunctionRemoverProxy.class));
  }


  /**
   * Test for one of the bugs causing issue 8284. JsObfuscateNamer was reassigning the same names
   * across different fragments.
   */
  public void testDuplicateNamesWithCodeSplitterError() throws Exception {
    JsProgram program = new JsProgram();
    // Reference to a in .b is to the top level scope a function where the one in _.c is to the
    // local a definition.
    //
    // After deduping _.b and _.c the identifier a in the deduped function points to the top level
    // scope a function and runing a namer afterwards makes the deduping invalid.
    String fragment0js = "_.a=function (){return _.a;}; _.b=function (){return _.a}; _.a();_.b();";
    String fragment1js = "_.c=function (){return _.c;}; _.d=function (){return _.c}; _.c();_.d();";

    List<JsStatement> fragment0 = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(fragment0js));
    List<JsStatement> fragment1 = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(fragment1js));
    program.setFragmentCount(2);
    program.getFragmentBlock(0).getStatements().addAll(fragment0);
    program.getFragmentBlock(1).getStatements().addAll(fragment1);

    // Mark all functions as if they were translated from Java sources.
    setAllFromJava(program);

    optimize(program, JsSymbolResolver.class, JsDuplicateFunctionRemoverProxy.class);

    // There should be two distinct dedupped functions here.
    MockNameGenerator tempFreshNameGenerator = new MockNameGenerator();
    String firstName = tempFreshNameGenerator.getFreshName();
    String secondName = tempFreshNameGenerator.getFreshName();

    assertNotNull(program.getScope().findExistingName(secondName));
  }

  private static class AssignmentGatherer extends JsModVisitor {

    final Map<String, JsName> assignments = new HashMap<String, JsName>();

    @Override
    public void endVisit(JsBinaryOperation expr, JsContext ctx) {
      if (expr.getOperator() != JsBinaryOperator.ASG || !(expr.getArg1() instanceof JsNameRef) ||
          !(expr.getArg2() instanceof JsNameRef)) {
        return;
      }
      assignments.put(expr.getArg1().toString(), ((JsNameRef) expr.getArg2()).getName());
    }

    public static Map<String, JsName> exec(JsProgram jsProgram) {
      AssignmentGatherer assignmentGatherer = new AssignmentGatherer();
      assignmentGatherer.accept(jsProgram);
      return assignmentGatherer.assignments;
    }
  }

  /**
   * Test for one of the bugs causing issue 8284. JsObfuscateNamer was used to assign
   * obfuscated names to deduped functions and as a result it might have modified the other names
   * that had been assigned invalidating the irrevocable decision made by the deduper.
   */
  public void testRerunNamerError() throws Exception {
    JsProgram program = new JsProgram();
    // Reference to a in _.b is to the top level scope a function where the one in _.c is to the
    // local a definition.
    //
    // After deduping _.b and _.c the identifier a in the deduped function points to the top level
    // scope a function and runing a namer afterwards makes the deduping invalid.

    // CAVEAT: The two functions that have {return a;} as their bodies are not actually duplicates
    // but we use them to model functions that refer to names at different scopes. Because this
    // optimization only runs on JsFunctions that come from Java source this situation does not
    // happen.
    String js = "var c; function a(){return f1;}; function f1() {_.b = function() {return a;} }; "
        + "function f2() { var a = null; _.c = function() {return a;} };f1();f2();_.b();_.c();";
    List<JsStatement> input = JsParser.parse(SourceOrigin.UNKNOWN,
        program.getScope(), new StringReader(js));
    program.getGlobalBlock().getStatements().addAll(input);

    // Mark all functions as if they were translated from Java sources.
    setAllFromJava(program);

    // Get the JsNames for the top level a and the f2() scoped a.
    JsName topScope_a = program.getScope().findExistingName("a");
    JsName f2_a = null;
    for (JsScope scope : program.getScope().getChildren()) {
      if (scope.toString().startsWith("function f2->")) {
        f2_a = scope.findExistingName("a");
      }
    }

    assertTrue(topScope_a != f2_a);

    optimize(program, JsSymbolResolver.class, JsDuplicateFunctionRemoverProxy.class);

    // collect values assigned to some identifiers.
    final Map<String, JsName> assignments = AssignmentGatherer.exec(program);

    // If the function have been dedupped then there is a constraint that the different JsNames
    // they referred to are obfuscated to the same id.
    // Hence if _.c and _.b are collapsed the top scope name "a" and the one in f2() need to remain
    // the same.
    assertTrue(assignments.get("_.b") != assignments.get("_.c") ||
        topScope_a.getShortIdent().equals(f2_a.getShortIdent()));
  }

  private static void setAllFromJava(JsProgram program) {
    new JsModVisitor() {
      @Override
      public void endVisit(JsFunction func, JsContext ctx) {
        func.setFromJava(true);
      }
    }.accept(program);
  }

  private String optimize(String js) throws Exception {
    return optimize(js, JsSymbolResolver.class,
        JsDuplicateFunctionRemoverProxy.class);
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

    TextOutput text = new DefaultTextOutput(true);
    JsVisitor generator = new JsSourceGenerationVisitor(text);

    generator.accept(program);
    return text.toString();
  }
}
