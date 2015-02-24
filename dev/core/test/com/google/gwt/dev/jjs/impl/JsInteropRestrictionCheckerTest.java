/*
 * Copyright 2015 Google Inc.
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Tests for the JsInteropRestrictionChecker.
 */
public class JsInteropRestrictionCheckerTest extends OptimizerTestBase {

  public void testCollidingFieldExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static final int show = 0;",
        "  @JsExport(\"show\")",
        "  public static final int display = 0;",
        "}");

    assertCompileFails();
  }

  public void testCollidingJsTypeJsPropertiesSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetImport("com.google.gwt.core.client.js.JsProperty");
    addSnippetClassDecl(
        "@JsType",
        "public static interface IBuggy {",
        "  @JsProperty",
        "  int x();",
        "  @JsProperty",
        "  void x(int x);",
        "}",
        "public static class Buggy implements IBuggy {",
        "  public int x() {return 0;}",
        "  public void x(int x) {}",
        "}");

    assertCompileSucceeds();
  }

  public void testCollidingMethodExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "  @JsExport(\"show\")",
        "  public static void display() {}",
        "}");

    assertCompileFails();
  }

  public void testCollidingMethodToFieldExportsFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "  @JsExport(\"show\")",
        "  public static final int display = 0;",
        "}");

    assertCompileFails();
  }

  public void testCollidingMethodToFieldJsTypeFails() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "  public final int show = 0;",
        "}");

    assertCompileFails();
  }

  public void testSingleExportSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsExport");
    addSnippetClassDecl(
        "public static class Buggy {",
        "  @JsExport(\"show\")",
        "  public static void show() {}",
        "}");

    assertCompileSucceeds();
  }

  public void testSingleJsTypeSucceeds() throws Exception {
    addSnippetImport("com.google.gwt.core.client.js.JsType");
    addSnippetClassDecl(
        "@JsType",
        "public static class Buggy {",
        "  public void show() {}",
        "}");

    assertCompileSucceeds();
  }

  @Override
  protected boolean optimizeMethod(JProgram program, JMethod method) {
    try {
      JsInteropRestrictionChecker.exec(TreeLogger.NULL, program, new MinimalRebuildCache());
    } catch (UnableToCompleteException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private void assertCompileFails() {
    try {
      optimize("void", "new Buggy();");
      fail("JsInteropRestrictionCheckerTest should have prevented the name collision.");
    } catch (Exception e) {
      assertTrue(e.getCause() instanceof UnableToCompleteException);
    }
  }

  private void assertCompileSucceeds() throws UnableToCompleteException {
    optimize("void", "new Buggy();");
  }
}
