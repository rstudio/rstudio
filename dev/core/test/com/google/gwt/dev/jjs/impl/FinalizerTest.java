/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dev.jjs.ast.CanBeFinal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for {@link Finalizer}.
 */
public class FinalizerTest extends OptimizerTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    runDeadCodeElimination = true;
  }

  public void testFinalizeClass() throws Exception {
    addSnippetClassDecl("static class Foo { }");
    Result result = optimize("void", "");
    assertFinal(result.findClass("EntryPoint$Foo"));
  }

  public void testFinalizeLocal() throws Exception {
    optimize("void", "int i = 5;").into("final int i = 5;");
  }

  public void testFinalizeLocal_Binop() throws Exception {
    optimize("void", "int i = 5; if (i == 0) { }").into(
        "final int i = 5;");
  }

  public void testFinalizeLocal_Prefix() throws Exception {
    optimize("void", "int i = 5; if (-i == 2) { }").into(
        "final int i = 5;");
  }

  public void testFinalizeField() throws Exception {
    addSnippetClassDecl("static int foo = 0;");
    Result result = optimize("void", "");
    assertFinal(result.findField("foo"));
  }

  public void testFinalizeField_Jsni() throws Exception {
    addSnippetClassDecl("static int foo = 0;");
    addSnippetClassDecl(
        "static native void nativeMethod()",
        "/*-{",
        "  i = @test.EntryPoint::foo;",
        "}-*/;");
    Result result = optimize("void", "");
    assertFinal(result.findField("foo"));
  }


  public void testFinalizeField_Volatile() throws Exception {
    addSnippetClassDecl("static volatile int foo = 0;");
    Result result = optimize("void", "");
    assertNotFinal(result.findField("foo"));
  }

  public void testRescue_Assign() throws Exception {
    optimize("void", "int i = 5; i = 6;").into(
        "int i = 5;",
        "i = 6;");
  }

  public void testRescue_Prefix() throws Exception {
    optimize("void", "int i = 5; ++i;").into(
        "int i = 5;",
        "++i;");
  }

  public void testRescue_Postfix() throws Exception {
    optimize("void", "int i = 5; i++;").into(
        "int i = 5;",
        "++i;");
  }

  public void testRescue_Jsni() throws Exception {
    addSnippetClassDecl("static int foo = 0;");
    addSnippetClassDecl(
        "static native void nativeMethod()",
        "/*-{",
        "  @test.EntryPoint::foo++;",
        "}-*/;");
    Result result = optimize("void", "");
    assertNotFinal(result.findField("foo"));
  }

  private void assertFinal(CanBeFinal canBeFinal) {
    assertTrue(canBeFinal.isFinal());
  }

  private void assertNotFinal(CanBeFinal canBeFinal) {
    assertFalse(canBeFinal.isFinal());
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    return Finalizer.exec(program).didChange();
  }
}
