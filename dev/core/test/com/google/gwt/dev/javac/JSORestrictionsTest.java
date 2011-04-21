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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.UnitTestTreeLogger;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Tests the JSORestrictionsChecker.
 */
public class JSORestrictionsTest extends TestCase {

  public void testBaseClassFullyImplements() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  static interface IntfA {\n");
    goodCode.append("    void a();\n");
    goodCode.append("    void b();\n");
    goodCode.append("  }\n");
    goodCode.append("  static interface IntfB {\n");
    goodCode.append("    void c();\n");
    goodCode.append("  }\n");
    goodCode.append("  static abstract class BaseA extends JavaScriptObject {\n");
    goodCode.append("    public final void a() { }\n");
    goodCode.append("    protected BaseA() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class BaseB extends BaseA implements IntfA {\n");
    goodCode.append("    public final void b() { }\n");
    goodCode.append("    protected BaseB() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class LeafA extends BaseB {\n");
    goodCode.append("    protected LeafA() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class LeafB extends BaseB implements IntfB {\n");
    goodCode.append("    public final void c() { }\n");
    goodCode.append("    protected LeafB() { }\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  /**
    * Java's version of the 'diamond' type definition pattern. Both a subclass
    * and superclass implement the same interface via two different chains of
    * resolution (extended class and inherited interface) Not good style, but
    * should be allowed.
    */
   public void testDiamondInheritance() {
     StringBuffer goodCode = new StringBuffer();
     goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
     goodCode.append("public class Buggy {\n");
     goodCode.append("  public interface Interface {\n");
     goodCode.append("    void method();\n");
     goodCode.append("  }\n");
     goodCode.append("  public static abstract class CommonBase extends JavaScriptObject \n");
     goodCode.append("      implements Interface {\n");
     goodCode.append("    protected CommonBase() {}\n");
     goodCode.append("  }\n");
     goodCode.append("  public static class Impl extends CommonBase implements Interface {\n");
     goodCode.append("    protected Impl() {}\n");
     goodCode.append("    public final void method() {}\n");
     goodCode.append("  }\n");
     goodCode.append("}\n");

     shouldGenerateNoError(goodCode);
   }

  public void testFinalClass() {
    StringBuffer code = new StringBuffer();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("final public class Buggy extends JavaScriptObject {\n");
    code.append("  int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testImplementsInterfaces() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  static interface Squeaks {\n");
    goodCode.append("    public void squeak();\n");
    goodCode.append("  }\n");
    goodCode.append("  static interface Squeaks2 extends Squeaks {\n");
    goodCode.append("    public void squeak();\n");
    goodCode.append("    public void squeak2();\n");
    goodCode.append("  }\n");
    goodCode.append("  static class Squeaker extends JavaScriptObject implements Squeaks {\n");
    goodCode.append("    public final void squeak() { }\n");
    goodCode.append("    protected Squeaker() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class Squeaker2 extends Squeaker implements Squeaks, Squeaks2 {\n");
    goodCode.append("    public final void squeak2() { }\n");
    goodCode.append("    protected Squeaker2() { }\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testInstanceField() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  int myStsate = 3;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_INSTANCE_FIELD);
  }

  public void testMultiArgConstructor() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public final class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy(int howBuggy) { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_CONSTRUCTOR_WITH_PARAMETERS);
  }

  public void testMultipleImplementations() {
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
    buggyCode.append("  static class Squeaker2 extends JavaScriptObject implements Squeaks {\n");
    buggyCode.append("    public final void squeak() { }\n");
    buggyCode.append("    protected Squeaker2() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 10: "
        + JSORestrictionsChecker.errAlreadyImplemented("Buggy$Squeaks",
            "Buggy$Squeaker", "Buggy$Squeaker2"));
  }

  /**
   * Normally, only a single JSO can implement an interface, but if all the
   * implementations are in a common base class, that should be allowed.
   */
  public void testMultipleImplementationsOk() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  public interface CommonInterface {\n");
    goodCode.append("    void method();\n");
    goodCode.append("  }\n");
    goodCode.append("  public interface CommonInterfaceExtended extends CommonInterface {}\n");
    goodCode.append("  public static class CommonBase extends JavaScriptObject\n");
    goodCode.append("      implements CommonInterface {\n");
    goodCode.append("    protected CommonBase() {}\n");
    goodCode.append("    public final void method() {}\n");
    goodCode.append("  }\n");
    goodCode.append("  public static class Impl1 extends CommonBase\n");
    goodCode.append("      implements CommonInterfaceExtended {\n");
    goodCode.append("    protected Impl1() {}\n");
    goodCode.append("  }\n");
    goodCode.append("  public static class Impl2 extends CommonBase\n");
    goodCode.append("      implements CommonInterfaceExtended {\n");
    goodCode.append("    protected Impl2() {}\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testNew() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  public static class MyJSO extends JavaScriptObject { \n");
    buggyCode.append("    protected MyJSO() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("  MyJSO makeOne() { return new MyJSO(); }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 6: "
        + JSORestrictionsChecker.ERR_NEW_JSO);
  }

  public void testNoAnnotationOnInterfaceSubtype() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  static interface Squeaks {\n");
    goodCode.append("    public void squeak();\n");
    goodCode.append("  }\n");
    goodCode.append("  static interface Sub extends Squeaks {\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testNoConstructor() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("}\n");

    // The public constructor is implicit.
    shouldGenerateError(buggyCode, "Line 2: "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNonEmptyConstructor() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { while(true) { } }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_NONEMPTY_CONSTRUCTOR);
  }

  public void testNonFinalMethod() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  int nonfinal() { return 10; }\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_INSTANCE_METHOD_NONFINAL);
  }

  public void testNonJsoInterfaceExtension() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  static interface Squeaks {\n");
    goodCode.append("    public void squeak();\n");
    goodCode.append("  }\n");
    goodCode.append("  static interface Squeaks2 extends Squeaks {\n");
    goodCode.append("    public void squeak2();\n");
    goodCode.append("  }\n");
    goodCode.append("  static class JsoSqueaker extends JavaScriptObject implements Squeaks {\n");
    goodCode.append("    protected JsoSqueaker() {}\n");
    goodCode.append("    public final void squeak() {}\n");
    goodCode.append("  }\n");
    goodCode.append("  static class JavaSqueaker2 implements Squeaks2 {\n");
    goodCode.append("    protected JavaSqueaker2() {}\n");
    goodCode.append("    public void squeak() {}\n");
    goodCode.append("    public void squeak2() {}\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testNonProtectedConstructor() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNonStaticInner() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  public class MyJSO extends JavaScriptObject {\n");
    buggyCode.append("    protected MyJSO() { }\n");
    buggyCode.append("  }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_IS_NONSTATIC_NESTED);
  }

  public void testNoOverride() {
    StringBuffer buggyCode = new StringBuffer();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  public final Object clone() { return this; }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_OVERRIDDEN_METHOD);
  }

  public void testPrivateMethod() {
    StringBuffer code = new StringBuffer();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("public class Buggy extends JavaScriptObject {\n");
    code.append("  private int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testTagInterfaces() {
    StringBuffer goodCode = new StringBuffer();
    goodCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  static interface Tag {}\n");
    goodCode.append("  static interface Tag2 extends Tag {}\n");
    goodCode.append("  static interface IntrExtendsTag extends Tag2 {\n");
    goodCode.append("    public void intrExtendsTag();\n");
    goodCode.append("  }\n");
    goodCode.append("  static class Squeaker3 extends JavaScriptObject implements Tag {\n");
    goodCode.append("    public final void squeak() { }\n");
    goodCode.append("    protected Squeaker3() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class Squeaker4 extends JavaScriptObject implements Tag2 {\n");
    goodCode.append("    public final void squeak() { }\n");
    goodCode.append("    protected Squeaker4() { }\n");
    goodCode.append("  }\n");
    goodCode.append("  static class Squeaker5 extends JavaScriptObject implements IntrExtendsTag {\n");
    goodCode.append("    public final void intrExtendsTag() { }\n");
    goodCode.append("    protected Squeaker5() { }\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  /**
   * Test that when compiling buggyCode, the TypeOracleBuilder emits
   * expectedError somewhere in its output. The code should define a class named
   * Buggy.
   */
  private void shouldGenerateError(CharSequence buggyCode,
      String... expectedErrors) {
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.setLowestLogLevel(TreeLogger.ERROR);
    if (expectedErrors != null) {
      builder.expectError("Errors in \'/mock/Buggy.java\'", null);
      for (String e : expectedErrors) {
        builder.expectError(e, null);
      }
    }
    UnitTestTreeLogger logger = builder.createLogger();
    StaticJavaResource buggyResource = new StaticJavaResource("Buggy",
        buggyCode);
    TypeOracleTestingUtils.buildStandardTypeOracleWith(logger,
        Collections.<Resource> emptySet(),
        CompilationStateTestBase.getGeneratedUnits(buggyResource));
    logger.assertCorrectLogEntries();
  }

  private void shouldGenerateNoError(StringBuffer buggyCode) {
    shouldGenerateError(buggyCode, (String[]) null);
  }
}
