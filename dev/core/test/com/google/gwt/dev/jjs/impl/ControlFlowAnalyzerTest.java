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
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.util.Empty;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests {@link ControlFlowAnalyzer}.
 */
public class ControlFlowAnalyzerTest extends OptimizerTestBase {

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
      Set<JDeclaredType> expectedSet = new HashSet<JDeclaredType>();
      for (String expectedType : expectedTypes) {
        JDeclaredType type = findType(program, expectedType);
        assertNotNull(type);
        expectedSet.add(type);
      }
      assertEquals(expectedSet, cfa.getInstantiatedTypes());
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
    sourceOracle.addOrReplace(new MockJavaResource("test.Foo") {
      @Override
      protected CharSequence getContent() {
        StringBuffer code = new StringBuffer();
        code.append("package test;\n");
        code.append("import com.google.gwt.core.client.JavaScriptObject;\n");
        code.append("public class Foo {\n");
        code.append("  public static native JavaScriptObject returnsJso() /*-{ return {}; }-*/;\n");
        code.append("  public static native void assignsJsoField() /*-{ @test.Foo::jsoField = {}; }-*/;\n");
        code.append("  public static native void readsJsoField() /*-{ var x = @test.Foo::jsoField; }-*/;\n");
        code.append("  public static native void passesJsoParam() /*-{ @test.Foo::calledFromJsni(Lcom/google/gwt/core/client/JavaScriptObject;)({}); }-*/;\n");
        code.append("  private static JavaScriptObject jsoField = null;\n");
        code.append("  private static void calledFromJsni(JavaScriptObject arg) { }\n");
        code.append("}\n");
        return code;
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
  }

  private Result analyzeSnippet(String codeSnippet)
      throws UnableToCompleteException {
    JProgram program = compileSnippet("void", codeSnippet);
    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(program);
    cfa.traverseFrom(findMainMethod(program));
    return new Result(program, cfa);
  }
}
