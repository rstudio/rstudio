/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for {@link ImplementCastsAndTypeChecksTest}.
 */
public class ImplementCastsAndTypeChecksTest extends OptimizerTestBase {
  // TODO(rluble): add unit test for the rest of the functionality.

  public void testCastCheckIntoNullCheck() throws Exception {
    addSnippetClassDecl("static class A { String name; public void set() { name = \"A\";} }");

    Result result =
        optimize("void", "A a = new A(); a = null; if (a instanceof A) {}");
    result.intoString(
        "EntryPoint$A a = new EntryPoint$A();",
        "a = null;",
        "if (a != null) {",
        "}");
  }

  public void testRemoveCastCheck_exactType() throws Exception {
    addSnippetClassDecl("static class A { String name; public void set() { name = \"A\";} }");

    Result result =
        optimize("void", "A a = new A(); if (a instanceof A) {}");
    result.intoString(
        "EntryPoint$A a = new EntryPoint$A();",
        "if (a != null) {",
        "}");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = true;

    do {
      didChange &= TypeTightener.exec(program).didChange();
      didChange &= MethodCallTightener.exec(program).didChange();
    } while (didChange);
    ImplementCastsAndTypeChecks.exec(program);
    return true;
  }
}
