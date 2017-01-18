/*
 * Copyright 2017 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;

/** Test for {@ArgHandlerFilterJsInteropExports}. */
public class ArgHandlerFilterJsInteropExportsTest extends ArgProcessorTestBase {
  private final Compiler.ArgProcessor argProcessor;
  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  public ArgHandlerFilterJsInteropExportsTest() {
    argProcessor = new Compiler.ArgProcessor(options);
  }

  public void testSingleFilter() {
    assertProcessSuccess(
        argProcessor, new String[] {"-includeJsInteropExports", "a.b..*", "my.Module"});
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c"));
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c.d"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "k.l.m"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "x.y.z"));
  }

  public void testMultipleFilters() {
    assertProcessSuccess(
        argProcessor,
        new String[] {
          "-includeJsInteropExports", "a.b..*", "-includeJsInteropExports", "k.l.m", "my.Module"
        });
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c"));
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c.d"));
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "k.l.m"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "x.y.z"));
  }

  public void testMultipleFilters2() {
    assertProcessSuccess(
        argProcessor,
        new String[] {
          "-includeJsInteropExports", "a.b..*", "-excludeJsInteropExports", "a.b.c.d.*", "my.Module"
        });
    assertTrue(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "a.b.c.d"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "k.l.m"));
    assertFalse(options.getJsInteropExportFilter().isIncluded(TreeLogger.NULL, "x.y.z"));
  }

  public void testSingleExcludeFilter() {
    assertProcessFailure(
        argProcessor,
        "-excludeJsInteropExports must be preceeded by -includeJsInteropExports",
        new String[] {"-excludeJsInteropExports", "a.b.*", "my.Module"});
  }

  public void testMalformedFilter() {
    assertProcessFailure(
        argProcessor,
        "-includeJsInteropExports regex cannot start with '+' or '-'",
        new String[] {"-includeJsInteropExports", "-a.b.*", "my.Module"});
  }

  public void testMalformedFilter2() {
    assertProcessFailure(
        argProcessor,
        "-includeJsInteropExports regex is invalid: Unclosed character class near index 5",
        new String[] {"-includeJsInteropExports", "a.b.*[", "my.Module"});
  }

}
