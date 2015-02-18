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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.thirdparty.guava.common.base.Joiner;

import java.util.List;

/**
 * Test class for GwtIncompatible annotations in {@link JdtCompiler}.
 */
public class UnusedImportRemoverJdtCompilerTest extends JdtCompilerTestBase {

  // TODO(rluble): write tests for all cases.

  public void testParseError() throws Exception {
    List<CompilationUnit> units = compile(SOME_CLASS,
        PARSE_ERROR);
    assertOnlyLastUnitHasErrors(units,
        "Invalid escape sequence (valid ones are  \\b  \\t  \\n  \\f  \\r  \\\"  \\'  \\\\ )");
  }

  public static final MockJavaResource SOME_CLASS = new MockJavaResource(
      "other.SomeClass") {
    @Override
    public CharSequence getContent() {
      return Joiner.on("\n").join(
          "package other;",
          "public class SomeClass {",
          "  public static void method() { }",
          "}");
    }
  };

  public static final MockJavaResource PARSE_ERROR =
      new MockJavaResource("some.ParseError") {
        @Override
        public CharSequence getContent() {
          return Joiner.on("\n").join(
              "package some;",
              "import other.SomeClass;",
              "public class ParseError {",
              "  String s = \"\\w\"",
              "  public static class Inner {  ",
              "    Inner() { SomeClass.method(); }",
              "  }",
              "}");
        }
      };

}

