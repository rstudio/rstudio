/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import junit.framework.TestCase;

public class JSORestrictionsTest extends TestCase {

  public void testFinalClass() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("final public class Buggy extends JavaScriptObject {\n");
    code.append("  int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testInstanceField() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  int myStsate = 3;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4:  "
        + JSORestrictionsChecker.ERR_INSTANCE_FIELD);
  }

  public void testMultiArgConstructor() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public final class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy(int howBuggy) { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3:  "
        + JSORestrictionsChecker.ERR_CONSTRUCTOR_WITH_PARAMETERS);
  }

  public void testNew() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  public static class MyJSO extends JavaScriptObject { \n");
    buggyCode.append("    protected MyJSO() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("  MyJSO makeOne() { return new MyJSO(); }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 6:  "
        + JSORestrictionsChecker.ERR_NEW_JSO);
  }

  public void testNoConstructor() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("}\n");

    // The public constructor is implicit.
    shouldGenerateError(buggyCode, "Line 2:  "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNoInterfaces() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  static interface Squeaks {\n");
    buggyCode.append("    public void squeak();\n");
    buggyCode.append("  }\n");
    buggyCode.append("  static class Squeaker extends JavaScriptObject implements Squeaks {\n");
    buggyCode.append("    public final void squeak() { }\n");
    buggyCode.append("    protected Squeaker() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 6:  "
        + JSORestrictionsChecker.errInterfaceWithMethods("Buggy.Squeaks"));
  }

  public void testNonEmptyConstructor() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { while(true) { } }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3:  "
        + JSORestrictionsChecker.ERR_NONEMPTY_CONSTRUCTOR);
  }

  public void testNonFinalMethod() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  int nonfinal() { return 10; }\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3:  "
        + JSORestrictionsChecker.ERR_INSTANCE_METHOD_NONFINAL);
  }

  public void testNonProtectedConstructor() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3:  "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNonStaticInner() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  public class MyJSO extends JavaScriptObject {\n");
    buggyCode.append("    protected MyJSO() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3:  "
        + JSORestrictionsChecker.ERR_IS_NONSTATIC_NESTED);
  }

  public void testNoOverride() throws UnableToCompleteException {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  public final Object clone() { return this; }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4:  "
        + JSORestrictionsChecker.ERR_OVERRIDDEN_METHOD);
  }

  public void testPrivateMethod() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("public class Buggy extends JavaScriptObject {\n");
    code.append("  private int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  /**
   * Test that when compiling buggyCode, the TypeOracleBuilder emits
   * expectedError somewhere in its output. The code should define a class named
   * Buggy.
   */
  private void shouldGenerateError(CharSequence buggyCode,
      final String expectedError) throws UnableToCompleteException {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    if (expectedError != null) {
      builder.expectError("Errors in \'transient source for Buggy\'", null);
      builder.expectError(expectedError, null);
      builder.expectError(
          "Compilation problem due to \'transient source for Buggy\'", null);
    }
    UnitTestTreeLogger logger = builder.createLogger();
    TypeOracleTestingUtils.buildTypeOracleForCode("Buggy", buggyCode, logger);
    logger.assertCorrectLogEntries();
  }

  private void shouldGenerateNoError(StringBuffer buggyCode)
      throws UnableToCompleteException {
    shouldGenerateError(buggyCode, null);
  }
}
