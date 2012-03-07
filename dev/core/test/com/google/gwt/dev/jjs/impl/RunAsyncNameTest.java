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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.UnitTestTreeLogger;

/**
 * This class tests naming of runAsync calls. Mostly it tests names that are
 * invalid.
 *
 */
public class RunAsyncNameTest extends JJSTestBase {
  @Override
  public void setUp() {
    sourceOracle.addOrReplace(new MockJavaResource("test.CallRunAsync") {
      @Override
      public CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("import com.google.gwt.core.client.GWT;\n");
        code.append("public class CallRunAsync {\n");
        code.append("  public static int notAmethod;");
        code.append("  public static void call0() { }\n");
        code.append("  public static void call1() {\n");
        code.append("    GWT.runAsync(null);\n");
        code.append("  }\n");
        code.append("  public static void call2() {\n");
        code.append("    GWT.runAsync(null);\n");
        code.append("    GWT.runAsync(null);\n");
        code.append("  }\n");
        code.append("}\n");
        return code;
      }
    });
  }

  /**
   * Tests that it's an error to call the 2-argument version of GWT.runAsync
   * with anything but a class literal.
   */
  public void testNonLiteralInCall() {
    UnitTestTreeLogger logger;
    {
      UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
      builder.setLowestLogLevel(TreeLogger.ERROR);
      builder.expectError("Errors in 'test/EntryPoint.java'", null);
      builder.expectError(
          "Line 5: Only class literals may be used to name a call to GWT.runAsync()", null);
      logger = builder.createLogger();
      this.logger = logger;
    }

    addSnippetImport("com.google.gwt.core.client.GWT");
    try {
      JProgram program = compileSnippet("void", "GWT.runAsync((new Object()).getClass(), null);");
      ReplaceRunAsyncs.exec(logger, program);
      fail("Expected compilation to fail");
    } catch (UnableToCompleteException e) {
      // expected
    }

    logger.assertCorrectLogEntries();
  }
}
