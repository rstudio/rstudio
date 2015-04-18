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
import com.google.gwt.dev.util.arg.SourceLevel;

import junit.framework.TestCase;

import java.util.Collections;

/**
 * Tests the JSORestrictionsChecker.
 */
public class JSORestrictionsTest extends TestCase {

  public void testBaseClassFullyImplements() {
    StringBuilder goodCode = new StringBuilder();
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
     StringBuilder goodCode = new StringBuilder();
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
    StringBuilder code = new StringBuilder();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("final public class Buggy extends JavaScriptObject {\n");
    code.append("  int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testImplementsInterfaces() {
    StringBuilder goodCode = new StringBuilder();
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
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  int myStsate = 3;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_INSTANCE_FIELD);
  }

  public void testMultiArgConstructor() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public final class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy(int howBuggy) { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_CONSTRUCTOR_WITH_PARAMETERS);
  }

  public void testMultipleImplementations() {
    StringBuilder buggyCode = new StringBuilder();
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
    StringBuilder goodCode = new StringBuilder();
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
    StringBuilder buggyCode = new StringBuilder();
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
    StringBuilder goodCode = new StringBuilder();
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
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("}\n");

    // The public constructor is implicit.
    shouldGenerateError(buggyCode, "Line 2: "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNonEmptyConstructor() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { while(true) { } }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_NONEMPTY_CONSTRUCTOR);
  }

  public void testNonFinalMethod() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  int nonfinal() { return 10; }\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_INSTANCE_METHOD_NONFINAL);
  }

  public void testNonJsoInterfaceExtension() {
    StringBuilder goodCode = new StringBuilder();
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
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  Buggy() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_NONPROTECTED_CONSTRUCTOR);
  }

  public void testNonStaticInner() {
    StringBuilder buggyCode = new StringBuilder();
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
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    buggyCode.append("public class Buggy extends JavaScriptObject {\n");
    buggyCode.append("  protected Buggy() { }\n");
    buggyCode.append("  public final int hashCode() { return 0; }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_OVERRIDDEN_METHOD);
  }

  public void testPrivateMethod() {
    StringBuilder code = new StringBuilder();
    code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
    code.append("public class Buggy extends JavaScriptObject {\n");
    code.append("  private int nonfinal() { return 10; }\n");
    code.append("  protected Buggy() { }\n");
    code.append("}\n");

    shouldGenerateNoError(code);
  }

  public void testTagInterfaces() {
    StringBuilder goodCode = new StringBuilder();
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

  public void testJsExport() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("public class Buggy {\n");
    goodCode.append("  @JsExport public static final String field = null;\n");
    goodCode.append("  @JsExport public static void method() {}\n");
    goodCode.append("  public interface Foo {\n");
    goodCode.append("    @JsExport String field1 = null;\n");
    goodCode.append("    interface ImplicitlyPublicInner {\n");
    goodCode.append("      @JsExport String field2 = null;\n");
    goodCode.append("    }\n");
    // TODO: enable after java 8 becomes default
    // goodCode.append("@JsExport static void method1() {}\n");
    goodCode.append("  }\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnClass() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport public class Buggy {}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnInterface() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport public interface Buggy {}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnEnum() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport enum Buggy { TEST1, TEST2;}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportNotOnEnumeration() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public enum Buggy {\n");
    buggyCode.append(" @JsExport TEST1, TEST2;\n;");
    buggyCode.append("}");

    shouldGenerateError(buggyCode, "Line 3: " + JSORestrictionsChecker.ERR_JSEXPORT_ON_ENUMERATION);
  }

  public void testJsExportOnConstructors() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("public class Buggy {\n");
    // A constructor JsExported without explicit symbol is fine here.
    // Leave it to NameConflictionChecker.
    goodCode.append("  @JsExport public Buggy() { }\n");
    goodCode.append("  @JsExport(\"buggy1\") public Buggy(int a) { }\n");
    goodCode.append("  public Buggy(int a, int b) { }\n");
    goodCode.append("}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnClassWithDefaultConstructor() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport public class Buggy {}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnClassWithExplicitConstructor() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport public class Buggy {\n");
    goodCode.append("  public Buggy() { }");
    goodCode.append("}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnClassWithOnePublicConstructor() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("@JsExport public class Buggy {\n");
    goodCode.append("  public Buggy() { }\n");
    goodCode.append("  private Buggy(int a) { }\n");
    goodCode.append("  protected Buggy(int a, int b) { }\n");
    goodCode.append("  Buggy(int a, int b, int c) { }\n");
    goodCode.append("}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportOnClassWithMultipleConstructors() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    goodCode.append("import com.google.gwt.core.client.js.JsNoExport;\n");
    goodCode.append("@JsExport public class Buggy {\n");
    goodCode.append("  @JsExport(\"Buggy1\") public Buggy() { }\n");
    goodCode.append("  @JsExport(\"Buggy2\") public Buggy(int a) { }\n");
    goodCode.append("  @JsExport public Buggy(int a, int b) { }\n");
    goodCode.append("  @JsNoExport public Buggy(int a, int b, int c) { }\n");
    goodCode.append("}");

    shouldGenerateNoError(goodCode);
  }

  public void testJsExportNotOnNonPublicClass() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("  private static class PrivateNested {\n");
    buggyCode.append("    public static class PublicNested {\n");
    buggyCode.append("      @JsExport public static Object foo() {return null;}\n");
    buggyCode.append("    }\n");
    buggyCode.append("  }\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 5: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportNotOnNonPublicField() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport final static String foo = null;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportNotOnNonPublicMethod() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport static Object foo() {return null;}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportNotOnObjectMethod() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport public void foo() {}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportNotOnObjectField() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport public final String foo = null;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportNotOnNonFinalField() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport public static String foo = null;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
        + JSORestrictionsChecker.ERR_JSEXPORT_ONLY_CTORS_STATIC_METHODS_AND_STATIC_FINAL_FIELDS);
  }

  public void testJsExportAndJsNotExportNotOnField() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("import com.google.gwt.core.client.js.JsNoExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport @JsNoExport public final static String foo = null;\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_EITHER_JSEXPORT_JSNOEXPORT);
  }

  public void testJsExportAndJsNotExportNotOnMethod() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsExport;\n");
    buggyCode.append("import com.google.gwt.core.client.js.JsNoExport;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsExport @JsNoExport public static void method() {}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 4: "
        + JSORestrictionsChecker.ERR_EITHER_JSEXPORT_JSNOEXPORT);
  }

  public void testJsPrototypeNotOnClass() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("@JsType(prototype = \"foo\")\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("void foo() {}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 3: "
      + JSORestrictionsChecker.ERR_JS_TYPE_WITH_PROTOTYPE_SET_NOT_ALLOWED_ON_CLASS_TYPES);
  }

  public void testJsTypePrototypeExtensionNotAllowed() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("import com.google.gwt.core.client.js.impl.PrototypeOfJsType;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsType interface Foo { " +
        "@PrototypeOfJsType static class Foo_Prototype implements Foo {} }\n");
    buggyCode.append("static class BuggyFoo extends Foo.Foo_Prototype {\n");
    buggyCode.append("}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 5: "
        + JSORestrictionsChecker.ERR_CLASS_EXTENDS_MAGIC_PROTOTYPE_BUT_NO_PROTOTYPE_ATTRIBUTE);
  }

  public void testJsTypePrototypeExtensionNoError() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("import com.google.gwt.core.client.js.impl.PrototypeOfJsType;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsType (prototype =\"foo\") interface Foo { " +
        "@PrototypeOfJsType static class Foo_Prototype implements Foo {} }\n");
    buggyCode.append("static class BuggyFoo extends Foo.Foo_Prototype {\n");
    buggyCode.append("}\n");
    buggyCode.append("}\n");

    shouldGenerateNoError(buggyCode);
  }

  public void testJsTypePrototypeExtensionNoError2() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("import com.google.gwt.core.client.js.impl.PrototypeOfJsType;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsType (prototype =\"foo\") interface Foo { }\n ");
    buggyCode.append("@PrototypeOfJsType static class Foo_Prototype implements Foo {}\n");
    buggyCode.append("static class BuggyFoo extends Foo_Prototype {\n");
    buggyCode.append("}\n");
    buggyCode.append("}\n");

    shouldGenerateNoError(buggyCode);
  }

  public void testJsTypePrototypeExtensionNotAllowed2() {
    // TODO (cromwellian): add a command-line flag for this later
    JSORestrictionsChecker.LINT_MODE = true;
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("public class Buggy {\n");
    buggyCode.append("@JsType (prototype =\"foo\") interface Foo { }\n");
    buggyCode.append("static class BuggyBar {}\n");
    buggyCode.append("static class BuggyFoo extends BuggyBar implements Foo {\n");
    buggyCode.append("}\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode, "Line 5: "
        + JSORestrictionsChecker.ERR_MUST_EXTEND_MAGIC_PROTOTYPE_CLASS);
  }

  public void testJsPropertyNoErrors() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsType;\n");
    buggyCode.append("import com.google.gwt.core.client.js.JsProperty;\n");
    buggyCode.append("@JsType public interface Buggy {\n");

    buggyCode.append("@JsProperty int foo();\n");
    buggyCode.append("@JsProperty void foo(int x);\n");

    buggyCode.append("@JsProperty int getFoo();\n");
    buggyCode.append("@JsProperty void setFoo(int x);\n");

    buggyCode.append("@JsProperty boolean hasFoo();\n");
    buggyCode.append("@JsProperty boolean isFoo();\n");
    buggyCode.append("@JsProperty Buggy setFoo(String x);\n");

    buggyCode.append("}\n");

    shouldGenerateNoError(buggyCode);
  }

  public void testJsFunctionOnFunctionalInterface() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    goodCode.append("@JsFunction public interface Buggy {\n");
    goodCode.append("int foo(int x);\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  // it is OK on JSORestrictionChecker but will be disallowed by JsInteropRestrictionChecker.
  public void testJsFunctionAndJsTypeOnInterface() {
    StringBuilder goodCode = new StringBuilder();
    goodCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    goodCode.append("import com.google.gwt.core.client.js.JsType;\n");
    goodCode.append("@JsFunction @JsType public interface Buggy {\n");
    goodCode.append("int foo(int x);\n");
    goodCode.append("}\n");

    shouldGenerateNoError(goodCode);
  }

  public void testJsFunctionNotOnClass() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    buggyCode.append("@JsFunction public class Buggy {\n");
    buggyCode.append("int foo(int x) {return 0;} \n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode,
        "Line 2: " + JSORestrictionsChecker.ERR_JS_FUNCTION_ONLY_ALLOWED_ON_FUNCTIONAL_INTERFACE);
  }

  public void testJsFunctionNotOnNonFunctionalInterface1() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    buggyCode.append("@JsFunction public interface Buggy {\n");
    buggyCode.append("int foo(int x);\n");
    buggyCode.append("int bar(int x);\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode,
        "Line 2: " + JSORestrictionsChecker.ERR_JS_FUNCTION_ONLY_ALLOWED_ON_FUNCTIONAL_INTERFACE);
  }

  public void testJsFunctionNotOnNonFunctionalInterface2() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    buggyCode.append("@JsFunction public interface Buggy {\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode,
        "Line 2: " + JSORestrictionsChecker.ERR_JS_FUNCTION_ONLY_ALLOWED_ON_FUNCTIONAL_INTERFACE);
  }

  public void testJsFunctionNotOnInterfaceWithSuperInterfaces() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    buggyCode.append("import java.io.Serializable;\n");
    buggyCode.append("@JsFunction public interface Buggy extends Serializable {\n");
    buggyCode.append("int foo(int x);\n");
    buggyCode.append("}\n");

    shouldGenerateError(buggyCode,
        "Line 3: " + JSORestrictionsChecker.ERR_JS_FUNCTION_INTERFACE_CANNOT_EXTEND_ANY_INTERFACE);
  }

  public void testJsFunctionNotOnInterfaceWithDefaultMethod() {
    StringBuilder buggyCode = new StringBuilder();
    buggyCode.append("import com.google.gwt.core.client.js.JsFunction;\n");
    buggyCode.append("@JsFunction public interface Buggy {\n");
    buggyCode.append("int foo(int x);\n");
    buggyCode.append("default void bar() { }\n");
    buggyCode.append("}\n");

    shouldGenerateError(SourceLevel.JAVA8, buggyCode,
        "Line 2: " + JSORestrictionsChecker.ERR_JS_FUNCTION_CANNOT_HAVE_DEFAULT_METHODS);
  }

  /**
   * Test that when compiling buggyCode, the TypeOracleUpdater emits
   * expectedError somewhere in its output. The code should define a class named
   * Buggy.
   */
  private void shouldGenerateError(SourceLevel sourceLevel, CharSequence buggyCode,
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
        CompilationStateTestBase.getGeneratedUnits(buggyResource),
        sourceLevel);
    logger.assertCorrectLogEntries();
  }

  private void shouldGenerateError(CharSequence buggyCode, String... expectedErrors) {
    shouldGenerateError(SourceLevel.DEFAULT_SOURCE_LEVEL, buggyCode, expectedErrors);
  }

  private void shouldGenerateNoError(StringBuilder buggyCode) {
    shouldGenerateError(buggyCode, (String[]) null);
  }
}
