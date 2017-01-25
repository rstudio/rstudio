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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests {@link ControlFlowAnalyzer}.
 */
public class ControlFlowAnalyzerTest extends JJSTestBase {

  /**
   * Answers predicates about an analyzed program.
   */
  private static final class Result {
    private final ControlFlowAnalyzer cfa;
    private final JProgram program;

    public Result(JProgram program, ControlFlowAnalyzer cfa) {
      this.program = program;
      this.cfa = cfa;
    }

    public void assertOnlyFieldsWritten(String... expectedFields) {
      Set<JField> expectedSet = new HashSet<JField>();
      for (String expectedField : expectedFields) {
        JField field = findField(program, expectedField);
        assertNotNull(field);
        expectedSet.add(field);
      }
      assertEquals(expectedSet, cfa.getFieldsWritten());
    }

    public void assertOnlyInstantiatedTypes(String... expectedTypes) {
      Set<JType> expectedSet = Sets.newHashSet();
      for (String expectedType : expectedTypes) {
        JType type = findType(program, expectedType);
        assertNotNull("Type " + expectedType + " not instantiated.", type);
        expectedSet.add(type);
      }
      assertEquals(expectedSet, cfa.getInstantiatedTypes());
    }

    public void assertOnlyLiveFieldsAndMethods(String... expected) {
      Set<JNode> expectedSet = new HashSet<JNode>();
      for (String expectedRef : expected) {
        JField field = findField(program, expectedRef);
        if (field != null) {
          expectedSet.add(field);
        } else {
          JMethod method = findQualifiedMethod(program, expectedRef);
          assertNotNull(method);
          expectedSet.add(method);
        }
      }
      assertEquals(expectedSet, cfa.getLiveFieldsAndMethods());
    }

    public void assertOnlyLiveStrings(String... expectedStrings) {
      Set<String> expectedSet = new HashSet<String>();
      Collections.addAll(expectedSet, expectedStrings);
      cfa.getLiveStrings();
      assertEquals(expectedSet, cfa.getLiveStrings());
    }
  }

  /**
   * Tests properties of an empty program.
   */
  public void testEmpty() throws Exception {
    Result result = analyzeSnippet("");
    result.assertOnlyFieldsWritten(Empty.STRINGS);
    result.assertOnlyInstantiatedTypes(Empty.STRINGS);
    result.assertOnlyLiveStrings(Empty.STRINGS);
  }

  /**
   * Tests that the JavaScriptObject type gets rescued in the three specific
   * circumstances where values can pass from JS into Java.
   */
  public void testRescueJavaScriptObjectFromJsni() throws Exception {
    addJsoResources();
    addOrReplaceResource("test.VirtualUpRef",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "final public class VirtualUpRef extends NonImplementor implements UpRefIntf {",
        "  protected VirtualUpRef() {}",
        "  public static native VirtualUpRef create() /*-{ return  {}; }-*/;",
        "}");
    addOrReplaceResource("test.Foo",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "public class Foo {",
        "  public static native JavaScriptObject returnsJso() /*-{ return {}; }-*/;",
        "  public static native void assignsJsoField() /*-{ @test.Foo::jsoField = {}; }-*/;",
        "  public static native void readsJsoField() /*-{ var x = @test.Foo::jsoField; }-*/;",
        "  public static native void passesJsoParam() /*-{ @test.Foo::calledFromJsni(Lcom/google/gwt/core/client/JavaScriptObject;)({}); }-*/;",
        "  private static JavaScriptObject jsoField = null;",
        "  private static void calledFromJsni(JavaScriptObject arg) { }",
        "  public static native JsoIntf returnsJsoIntf() /*-{ return {}; }-*/;",
        "  public static native SingleJso returnsSingleJso() /*-{ return {}; }-*/;",
        "}");
    addSnippetImport("test.Foo");

    analyzeSnippet("").assertOnlyInstantiatedTypes(Empty.STRINGS);

    // Returning a JSO from a JSNI method rescues.
    analyzeSnippet("Foo.returnsJso();").assertOnlyInstantiatedTypes(
        "JavaScriptObject", "Object");

    // Assigning into a JSO field from a JSNI method rescues.
    analyzeSnippet("Foo.assignsJsoField();").assertOnlyInstantiatedTypes(
        "JavaScriptObject", "Object");

    // Passing from Java to JS via a JSNI field read should NOT rescue.
    analyzeSnippet("Foo.readsJsoField();").assertOnlyInstantiatedTypes(
        Empty.STRINGS);

    // Passing a parameter from JS to Java rescues.
    analyzeSnippet("Foo.passesJsoParam();").assertOnlyInstantiatedTypes(
        "JavaScriptObject", "Object");

    // Returning a JSO subType instantiates it
    analyzeSnippet("Foo.returnsSingleJso();").assertOnlyInstantiatedTypes(
        "SingleJso", "JavaScriptObject", "Object", "JsoIntf");

    // Returning a JSO SingleJsoImpl instantiates it and the implementor
    analyzeSnippet("Foo.returnsJsoIntf();").assertOnlyInstantiatedTypes(
        "SingleJso", "JavaScriptObject", "Object", "JsoIntf");

    // A virtual upref should still be rescued
    analyzeSnippet("VirtualUpRef.create().getFoo();").assertOnlyInstantiatedTypes(
        "VirtualUpRef", "NonImplementor", "JavaScriptObject", "Object", "UpRefIntf");

    // and its methods
    analyzeSnippet("VirtualUpRef.create().getFoo();").assertOnlyLiveFieldsAndMethods(
        "VirtualUpRef.$clinit", "VirtualUpRef.create", "VirtualUpRef.getFoo",
        "NonImplementor.$clinit","NonImplementor.getFoo",
        "UpRefIntf.$clinit",
        "JavaScriptObject.$clinit",
        "EntryPoint.$clinit",
        "EntryPoint.onModuleLoad",
        "Object.$clinit");
  }

  /**
   * Tests that certain Java arrays are rescued if returned from JSNI code. Arrays that are rescued
   * if returned from JSNI: code whose leaf types are either primitive or types that might be
   * instantiated in JSNI.
   */
  public void testRescueArraysFromJSNI() throws Exception {
    addOrReplaceResource("test.Foo",
        "package test;", "public class Foo {",
        "  public static native int[] create_array() /*-{ return {}; }-*/;",
        "  public static native int[][] create_2d_array() /*-{ return {}; }-*/;",
        "}");
    addSnippetImport("test.Foo");

    analyzeSnippet("").assertOnlyInstantiatedTypes(Empty.STRINGS);

    // Returning a JSO from a JSNI method rescues.
    analyzeSnippet("Foo.create_array();").assertOnlyInstantiatedTypes(
        "int[]", "Object");

    // Returning a JSO from a JSNI method rescues.
    analyzeSnippet("Foo.create_2d_array();").assertOnlyInstantiatedTypes(
        "int[][]", "Object[]", "Object");
  }

  public void testRescueJavaScriptObjectReturnedFromExternallyImplementedMethod() throws Exception {
    addJsoResources();
    addSnippetImport("test.Test");

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsMethod;",
        "public class Test {",
        "  @JsMethod",
        "  public static native JsoIntf returnsJsoInterface();",
        "}"
    );
    analyzeSnippet("Test.returnsJsoInterface();").assertOnlyInstantiatedTypes(
        "JsoIntf", "SingleJso", "JavaScriptObject", "Object");

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsMethod;",
        "public class Test {",
        "  @JsMethod",
        "  public static native SingleJso returnsJso();",
        "}"
    );
    analyzeSnippet("Test.returnsJso();").assertOnlyInstantiatedTypes(
        "JsoIntf", "SingleJso", "JavaScriptObject", "Object");
  }

  public void testRescueJavaScriptObjectParameterToJsInteropEntryPoint() throws Exception {
    addJsoResources();
    addSnippetImport("test.Test");

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsMethod;",
        "public class Test {",
        "  public static void receivesJsoInterface(JsoIntf i) {};",
        "}"
    );
    analyzeSnippet("Test.receivesJsoInterface(null);").assertOnlyInstantiatedTypes(
        Empty.STRINGS);

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsMethod;",
        "public class Test {",
        "  @JsMethod",
        "  public static void receivesJsoInterface(JsoIntf i) {};",
        "}"
    );
    analyzeSnippet("Test.receivesJsoInterface(null);").assertOnlyInstantiatedTypes(
        "JsoIntf", "SingleJso", "JavaScriptObject", "Object");
  }

    /**
     * Tests that the JavaScriptObject type gets rescued in the three specific
     * circumstances where values can pass from JS into Java.
     */
  public void testRescueJavaScriptObjectReturnedFromFieldReference() throws Exception {
    addJsoResources();
    addSnippetImport("test.Test");

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsType;",
        "@JsType(isNative = true)",
        "public class Test {",
        "  public static JsoIntf field;",
        "}"
    );
    analyzeSnippet("Test.field.hashCode();").assertOnlyInstantiatedTypes(
        "JsoIntf", "SingleJso", "JavaScriptObject", "Object",
        // These are all live because of the method Test.toString and equals can be referenced
        // externally
        "String", "Comparable", "CharSequence", "Serializable", "Boolean", "Number", "Double");

    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsType;",
        "@JsType(isNative = true)",
        "public class Test {",
        "  public static SingleJso field;",
        "}"
    );
    analyzeSnippet("Test.field.hashCode();").assertOnlyInstantiatedTypes(
        "JsoIntf", "SingleJso", "JavaScriptObject", "Object",
        // These are all live because of the method Test.toString and equals can be referenced
        // externally
        "String", "Comparable", "CharSequence", "Serializable", "Boolean", "Number", "Double");
  }

  /**
   * Tests that the JavaScriptObject type gets rescued in the three specific
   * circumstances where values can pass from JS into Java.
   */
  public void testRescueRepresentedAsNative() throws Exception {
    addOrReplaceResource("test.Test",
        "package test;",
        "import jsinterop.annotations.JsMethod;",
        "public class Test {",
        "  @JsMethod",
        "  public static native Object getObject();",
        "  @JsMethod",
        "  public static native Number getNumber();",
        "  @JsMethod",
        "  public static native Comparable getComparable();",
        "  @JsMethod",
        "  public static native Double getDouble();",
        "}"
    );

    addSnippetImport("test.Test");

    analyzeSnippet("Test.getObject();").assertOnlyInstantiatedTypes(
        "Object", "Boolean", "Number", "Double",
        "String", "Comparable", "CharSequence", "Serializable");
    analyzeSnippet("Test.getNumber();").assertOnlyInstantiatedTypes(
        "Object", "Number", "Double", "Serializable");
    analyzeSnippet("Test.getComparable();").assertOnlyInstantiatedTypes(
        "Object", "String", "Comparable", "CharSequence", "Serializable");
    analyzeSnippet("Test.getDouble();").assertOnlyInstantiatedTypes(
        "Object", "Number", "Double", "Serializable");
  }

  private void addOrReplaceResource(String qualifiedTypeName, final String... source) {
    sourceOracle.addOrReplace(
        new MockJavaResource(qualifiedTypeName) {
          @Override
          public CharSequence getContent() {
            return Joiner.on("\n").join(source);
          }
        });
    }

  private void addJsoResources() {
    addOrReplaceResource("test.JsoIntf",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "public interface JsoIntf {",
        "  public int getAny();",
        "}");
    addOrReplaceResource("test.UpRefIntf",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "public interface UpRefIntf {",
        "  public int getFoo();",
        "}");
    addOrReplaceResource("test.NonImplementor",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "public class NonImplementor extends JavaScriptObject {",
        "  protected NonImplementor() {}",
        "  final public native int getFoo() /*-{ return 0; }-*/;",
        "}");
    addOrReplaceResource("test.SingleJso",
        "package test;",
        "import com.google.gwt.core.client.JavaScriptObject;",
        "final public class SingleJso extends JavaScriptObject implements JsoIntf {",
        "  protected SingleJso() {}",
        "  public native int getAny() /*-{ return 1; }-*/;",
        "}");
  }

  private Result analyzeSnippet(String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", codeSnippet, true);
    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(program);
    cfa.traverseFrom(findMainMethod(program));
    return new Result(program, cfa);
  }

}
