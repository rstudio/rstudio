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

  private TypeOracle buildOracleWithCode(CharSequence code,
      UnitTestTreeLogger logger) throws UnableToCompleteException {
    TypeOracleBuilder builder = new TypeOracleBuilder();
    TypeOracleTestingUtils.addStandardCups(builder);
    addLongCheckingCups(builder);
    TypeOracleTestingUtils.addCup(builder, "Buggy", code);
    return builder.build(logger);
  }

  private void shouldGenerateError(CharSequence code, int line, String message)
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
    TypeOracle oracle = buildOracleWithCode(code, logger);
    logger.assertCorrectLogEntries();
    if (message != null) {
      assertEquals("Buggy compilation unit not removed from type oracle", null,
          oracle.findType("Buggy"));
    }
  }

  private void shouldGenerateNoError(StringBuffer code)
      throws UnableToCompleteException {
    shouldGenerateError(code, -1, null);
  }
}
