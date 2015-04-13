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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;

/**
 * Tests {@link ComputePotentiallyObservableUninitializedValues}.
 */
public class ComputePotentiallyObservableUninitializedValuesTest extends OptimizerTestBase {

  private boolean runMethodInliner;
  private boolean runSpecializer;

  @Override
  public void setUp() throws Exception {
    runMethodInliner = false;
    runSpecializer = false;
  }

  public void testSimpleClass() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program, ImmutableList.<String>of(),
        ImmutableList
            .of("EntryPoint$A.i1", "EntryPoint$A.i2", "EntryPoint$A.fi1", "EntryPoint$A.fi2"));
  }

  public void testOnlyFinalLiteralUnobservable() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  A() { m(); }",
        "  void m() { }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
  }

  public void testSafeCustomInitializer() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  { m(); }",
        "  static void m() { }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program, ImmutableList.<String>of(),
        ImmutableList
            .of("EntryPoint$B.fi1", "EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"));
    MakeCallsStatic.exec(program, false);
    assertAnalysisCorrect(program, ImmutableList.<String>of(),
        ImmutableList
            .of("EntryPoint$B.fi1", "EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"));
  }

  public void testSafeStaticCall() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  A() { m(new A(), 2); }",
        "  static void m(A a, int i) { }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program, ImmutableList.<String>of(),
        ImmutableList
            .of("EntryPoint$B.fi1", "EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"));
  }

  public void testSafePolyCall() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  A() { String s = new Integer(0).toString(); }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program, ImmutableList.<String>of(),
        ImmutableList
            .of("EntryPoint$B.fi1", "EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"));
  }

  public void testUnSafeCustomInitializer_polymorphicDispatch() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  void m() { }",
        "  { m(); }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
  }

  public void testUnSafeCustomInitializer_escapingThis() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  static void m(A a) { }",
        "  { m(this); }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
  }

  public void testUnSafeCustomInitializer_escapingThisThroughArrayInitializer() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  { A[] a = new A[] {this}; }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
  }

  public void testUnSafeCustomInitializer_escapingDeepReference() throws Exception {
    addSnippetClassDecl(
        "static class A  { ",
        "  A() {String s = (new Integer(0).toString() + this.toString()).substring(0, 1); }",
        "}");
    addSnippetClassDecl(
        "static class B extends A { ",
        "  int i1 = 1;",
        "  Integer i2 = new Integer(1);",
        "  final int fi1 = 1;",
        "  final Integer fi2 = new Integer(1);",
        "}");
    JProgram program = compileSnippet("void", "return;");
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
    MakeCallsStatic.exec(program, false); // required so that method is static
    assertAnalysisCorrect(program,
        ImmutableList.of("EntryPoint$B.i1", "EntryPoint$B.i2", "EntryPoint$B.fi2"),
        ImmutableList.of("EntryPoint$B.fi1"));
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    if (runMethodInliner) {
      MethodInliner.exec(program);
    }
    if (runSpecializer) {
      Finalizer.exec(program); // required so that method is marked final
      MakeCallsStatic.exec(program, false); // required so that method is static
      TypeTightener.exec(program); // required so that the parameter types are tightened
      MethodCallSpecializer.exec(program);
    }

    OptimizerStats result = DeadCodeElimination.exec(program, method);
    if (result.didChange()) {
      // Make sure we converge in one pass.
      //
      // TODO(rluble): It does not appear to be true in general unless we iterate until a
      // fixpoint in exec().
      //
      // Example:
      //
      //     Constructor( ) { deadcode }
      //     m( new Constructor(); }
      //
      // If m is processed first, it will see the constructor as having side effects.
      // Then the constructor will become empty enabling m() become empty in the next round.
      //
      assertFalse(DeadCodeElimination.exec(program, method).didChange());
    }
    return result.didChange();
  }

  private void assertAnalysisCorrect(JProgram program,
      Iterable<String> fieldsThatCanBeObservedUninitialized,
      Iterable<String> fieldsThatCannotBeObservedUninitialized) {
    Predicate<JField> uninitializedValuesCanBeObserved =
        ComputePotentiallyObservableUninitializedValues.analyze(program);

    for (String fieldName : fieldsThatCanBeObservedUninitialized) {
      JField field = findField(program, fieldName);
      assertNotNull(field);
      assertTrue("Field " + fieldName + " was erroneously determined to uninitialized unobservable",
          uninitializedValuesCanBeObserved.apply(field));
    }
    for (String fieldName : fieldsThatCannotBeObservedUninitialized) {
      JField field = findField(program, fieldName);
      assertNotNull(field);
      assertFalse("Field " + fieldName + " was erroneously determined to uninitialized observable",
          uninitializedValuesCanBeObserved.apply(field));
    }
  }
}
