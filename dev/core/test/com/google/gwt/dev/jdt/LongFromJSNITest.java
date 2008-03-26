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
import com.google.gwt.core.ext.TreeLogger.Type;
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

    shouldGenerateWarning(
        code,
        3,
        "Referencing field 'Buggy.x': 'long' is an opaque, non-numeric value in JS code");
  }

  public void testLongArray() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long[] m() { return new long[] { -1 }; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(
        code,
        3,
        "Referencing method \'Buggy.m\': return type \'long[]\' is an opaque, non-numeric value in JS code");
  }

  public void testLongParameter() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native void jsniMeth(long x) /*-{ return; }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 2,
        "Parameter 'x': 'long' is an opaque, non-numeric value in JS code");
  }

  public void testLongReturn() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  native long jsniMeth() /*-{ return 0; }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(code, 2,
        "Return value of type 'long' is an opaque, non-numeric value in JS code");
  }

  public void testMethodArgument() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  void print(long x) { }\n");
    code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(
        code,
        3,
        "Referencing method \'Buggy.print\': parameter \'x\': \'long\' is an opaque, non-numeric value in JS code");
  }

  public void testMethodReturn() throws UnableToCompleteException {
    StringBuffer code = new StringBuffer();
    code.append("class Buggy {\n");
    code.append("  long m() { return -1; }\n");
    code.append("  native void jsniMeth() /*-{\n");
    code.append("    $wnd.alert(this.@Buggy::m()()); }-*/;\n");
    code.append("}\n");

    shouldGenerateWarning(
        code,
        3,
        "Referencing method 'Buggy.m': return type 'long' is an opaque, non-numeric value in JS code");
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

    shouldGenerateNoWarning(code);
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

    shouldGenerateWarning(
        code,
        4,
        "Referencing method 'Buggy.m': return type 'long' is an opaque, non-numeric value in JS code");
  }

  public void testSuppressWarnings() throws UnableToCompleteException {
    {
      StringBuffer code = new StringBuffer();
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  @SuppressWarnings(\"restriction\")\n");
      code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
      code.append("}\n");

      shouldGenerateNoWarning(code);
    }

    {
      StringBuffer code = new StringBuffer();
      code.append("@SuppressWarnings(\"restriction\")\n");
      code.append("class Buggy {\n");
      code.append("  void print(long x) { }\n");
      code.append("  native void jsniMeth() /*-{ this.@Buggy::print(J)(0); }-*/;\n");
      code.append("}\n");

      shouldGenerateNoWarning(code);
    }
  }

  private void shouldGenerateNoWarning(StringBuffer code)
      throws UnableToCompleteException {
    shouldGenerateWarning(code, -1, null);
  }

  private void shouldGenerateWarning(CharSequence code, int line, String message)
      throws UnableToCompleteException {
    Type logType = TreeLogger.WARN;
    UnitTestTreeLogger.Builder b = new UnitTestTreeLogger.Builder();
    b.setLowestLogLevel(logType);
    if (message != null) {
      final String fullMessage = "transient source for Buggy(" + line + "): "
          + message;
      b.expect(logType, fullMessage, null);
    }
    UnitTestTreeLogger logger = b.createLogger();
    TypeOracleTestingUtils.buildTypeOracleForCode("Buggy", code, logger);
    logger.assertCorrectLogEntries();
  }
}
