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
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import junit.framework.TestCase;

/**
 * Test access to longs from JSNI.
 */
public class LongFromJSNITest extends TestCase {
  public void testBogusRef() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {                                  \n");
    code.append("volatile long x = -1;                          \n");
    code.append("native void jsniMeth() /*-{                    \n");
    code.append("  // @\\bogus refs should just be skipped      \n");
    code.append("  $wnd.alert(\"x is: \"+this.@Buggy::x); }-*/; \n");
    code.append("}                                              \n");

    shouldGenerateError(code, 3,
        "Referencing field 'Buggy.x': type 'long' is not safe to access in JSNI code");
  }

  public void testCyclicReferences() throws UnableToCompleteException {
    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 3, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  Extra anExtra = new Extra();\n");
      buggy.append("  static int anint = 3;\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra::along);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  Buggy mattress = new Buggy();\n");
      extra.append("  static long along = 3;\n");
      extra.append("  native void jsniMeth() /*-{\n");
      extra.append("    $wnd.alert(@Buggy::anint);\n");
      extra.append("  }-*/;\n");
      extra.append("}\n");

      shouldGenerateError(buggy, extra, 4, "Referencing field 'Extra.along': "
          + "type 'long' is not safe to access in JSNI code");
    }
  }

  public void testFieldAccess() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("volatile long x = -1;\n");
    code.append("native void jsniMeth() /*-{\n");
    code.append("  $wnd.alert(\"x is: \"+this.@Buggy::x); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 3,
        "Referencing field 'Buggy.x': type 'long' is not safe to access in JSNI code");
  }

  public void testInnerClass() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("public class Buggy {\n");
    code.append("  static class Inner {\n");
    code.append("    long x = 3;\n");
    code.append("  }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(@Buggy.Inner::x);\n");
    code.append("  }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 5, "Referencing field 'Buggy.Inner.x': "
        + "type 'long' is not safe to access in JSNI code");
  }

  /**
   * The proper behavior here is a close call. In hosted mode, Java arrays are
   * completely unusable in JavaScript, so the current reasoning is to allow
   * them.
   */
  public void testLongArray() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long[] m() { return new long[] { -1 }; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testLongParameter() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(long x) /*-{ return; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Parameter 'x': type 'long' is not safe to access in JSNI code");
  }

  public void testLongReturn() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native long jsniMeth() /*-{ return 0; }-*/;\n");
    code.append("}\n");

    shouldGenerateError(code, 2,
        "Type 'long' may not be returned from a JSNI method");
  }

  public void testMethodArgument() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void print(long x) { }\n");
    code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        3,
        "Parameter 1 of method \'Buggy.print\': type 'long' may not be passed out of JSNI code");
  }

  public void testMethodReturn() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m() { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        3,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testOverloadedMethodWithNoWarning()
      throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(Ljava/lang/String;)(\"hello\")); }-*/;\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testOverloadedMethodWithWarning()
      throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m(int x) { return -1; }\n");
    code.append("  int m(String x) { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m(I)(10)); }-*/;\n");
    code.append("}\n");

    shouldGenerateError(
        code,
        4,
        "Referencing method 'Buggy.m': return type 'long' is not safe to access in JSNI code");
  }

  public void testRefInString() throws UnableToCompleteException {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  native void jsniMeth() /*-{ 'this.@Buggy::print(J)(0)'; }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testUnsafeAnnotation() throws UnableToCompleteException {
    {
      StringBuffer code = new StringBuffer();
      code.append("import com.google.gwt.core.client.UnsafeNativeLong;");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  @UnsafeNativeLong\n");
      code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
      code.append("}\n");

      shouldGenerateNoError(code);
    }
  }

  public void testViolator() throws UnableToCompleteException {
    {
      StringBuffer okay = new StringBuffer();
      okay.append("class Buggy {\n");
      okay.append("  native void jsniMeth() /*-{\n");
      okay.append("    $wnd.alert(@Extra.Inner::x);\n");
      okay.append("  }-*/;\n");
      okay.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static int x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateNoError(okay, extra);
    }

    {
      StringBuffer buggy = new StringBuffer();
      buggy.append("class Buggy {\n");
      buggy.append("  native void jsniMeth() /*-{\n");
      buggy.append("    $wnd.alert(@Extra.Inner::x);\n");
      buggy.append("  }-*/;\n");
      buggy.append("}\n");

      StringBuffer extra = new StringBuffer();
      extra.append("class Extra {\n");
      extra.append("  private static class Inner { \n");
      extra.append("    private static long x = 3;\n");
      extra.append("  }\n");
      extra.append("}\n");

      shouldGenerateError(
          buggy,
          extra,
          2,
          "Referencing field 'Extra.Inner.x': type 'long' is not safe to access in JSNI code");
    }
  }

  private void addLongCheckingCups(TypeOracleBuilder builder)
      throws UnableToCompleteException {
    {
      StringBuilder code = new StringBuilder();
      code.append("package com.google.gwt.core.client;\n");
      code.append("public @interface UnsafeNativeLong {\n");
      code.append("}\n");

      TypeOracleTestingUtils.addCup(builder,
          "com.google.gwt.core.client.UnsafeNativeLong", code);
    }
  }

  private TypeOracle buildOracle(CharSequence buggyCode,
      CharSequence extraCode, UnitTestTreeLogger logger)
      throws UnableToCompleteException {
    TypeOracleBuilder builder = new TypeOracleBuilder();
    TypeOracleTestingUtils.addStandardCups(builder);
    addLongCheckingCups(builder);
    TypeOracleTestingUtils.addCup(builder, "Buggy", buggyCode);
    if (extraCode != null) {
      TypeOracleTestingUtils.addCup(builder, "Extra", extraCode);
    }
    return builder.build(logger);
  }

  private void shouldGenerateError(CharSequence buggyCode,
      CharSequence extraCode, int line, String message)
      throws UnableToCompleteException {
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.setLowestLogLevel(TreeLogger.ERROR);
    if (message != null) {
      b.expect(TreeLogger.ERROR, "Errors in 'transient source for Buggy'", null);
      final String fullMessage = "Line " + line + ":  " + message;
      b.expect(TreeLogger.ERROR, fullMessage, null);
      b.expect(TreeLogger.ERROR,
          "Compilation problem due to 'transient source for Buggy'", null);
    }
    UnitTestTreeLogger logger = b.createLogger();
    TypeOracle oracle = buildOracle(buggyCode, extraCode, logger);
    logger.assertCorrectLogEntries();
    if (message != null) {
      assertEquals("Buggy compilation unit not removed from type oracle", null,
          oracle.findType("Buggy"));
    }
  }

  private void shouldGenerateError(CharSequence buggyCode, int line,
      String message) throws UnableToCompleteException {
    shouldGenerateError(buggyCode, null, line, message);
  }

  private void shouldGenerateNoError(CharSequence code)
      throws UnableToCompleteException {
    shouldGenerateNoError(code, null);
  }

  private void shouldGenerateNoError(CharSequence code, CharSequence extraCode)
      throws UnableToCompleteException {
    shouldGenerateError(code, extraCode, -1, null);
  }
}
