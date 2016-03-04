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
        assertNotNull(type);
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
      sourceOracle.addOrReplace(new MockJavaResource("test.JsoIntf") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "public interface JsoIntf {",
            "  public int getAny();",
            "}");
      }
    });

      sourceOracle.addOrReplace(new MockJavaResource("test.UpRefIntf") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "public interface UpRefIntf {",
            "  public int getFoo();",
            "}");
      }
    });

     sourceOracle.addOrReplace(new MockJavaResource("test.NonImplementor") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "public class NonImplementor extends JavaScriptObject {",
            "  protected NonImplementor() {}",
            "  final public native int getFoo() /*-{ return 0; }-*/;",
            "}");
      }
    });

     sourceOracle.addOrReplace(new MockJavaResource("test.VirtualUpRef") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "final public class VirtualUpRef extends NonImplementor implements UpRefIntf {",
            "  protected VirtualUpRef() {}",
            "  public static native VirtualUpRef create() /*-{ return  {}; }-*/;",
            "}");
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.SingleJso") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "final public class SingleJso extends JavaScriptObject implements JsoIntf {",
            "  protected SingleJso() {}",
            "  public native int getAny() /*-{ return 1; }-*/;",
            "  public static native JsoIntf returnsJsoIntf() /*-{ return {}; }-*/;",
            "  public static native SingleJso returnsJso() /*-{ return {}; }-*/;",
            "}");
      }
    });

    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "import com.google.gwt.core.client.JavaScriptObject;",
            "public class Foo {",
            "  public static native JavaScriptObject returnsJso() /*-{ return {}; }-*/;",
            "  public static native void assignsJsoField() /*-{ @test.Foo::jsoField = {}; }-*/;",
            "  public static native void readsJsoField() /*-{ var x = @test.Foo::jsoField; }-*/;",
            "  public static native void passesJsoParam() /*-{ @test.Foo::calledFromJsni(Lcom/google/gwt/core/client/JavaScriptObject;)({}); }-*/;",
            "  private static JavaScriptObject jsoField = null;",
            "  private static void calledFromJsni(JavaScriptObject arg) { }",
            "}");
      }
    });
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
    analyzeSnippet("SingleJso.returnsJso();").assertOnlyInstantiatedTypes(
        "SingleJso", "JavaScriptObject", "Object", "JsoIntf");

    // Returning a JSO SingleJsoImpl instantiates it and the implementor
    analyzeSnippet("SingleJso.returnsJsoIntf();").assertOnlyInstantiatedTypes(
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
    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      public CharSequence getContent() {
        return Joiner.on("\n").join(
            "package test;",
            "public class Foo {",
            "  public static native int[] create_array() /*-{ return {}; }-*/;",
            "  public static native int[][] create_2d_array() /*-{ return {}; }-*/;",
            "}");
      }
    });
    addSnippetImport("test.Foo");

    analyzeSnippet("").assertOnlyInstantiatedTypes(Empty.STRINGS);

    // Returning a JSO from a JSNI method rescues.
    analyzeSnippet("Foo.create_array();").assertOnlyInstantiatedTypes(
        "int[]", "Object",
        // Classes rescued due to rescueing classliterals for instantiated arrays.
        "Class", "String", "Serializable", "Comparable", "CharSequence");

    // Returning a JSO from a JSNI method rescues.
    analyzeSnippet("Foo.create_2d_array();").assertOnlyInstantiatedTypes(
        "int[][]", "Object[]", "Object",
        // Classes rescued due to rescueing classliterals for instantiated arrays.
        "Class", "String", "Serializable", "Comparable", "CharSequence");
  }

  private Result analyzeSnippet(String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", codeSnippet, true);
    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(program);
    cfa.traverseFrom(findMainMethod(program));
    return new Result(program, cfa);
  }
}
