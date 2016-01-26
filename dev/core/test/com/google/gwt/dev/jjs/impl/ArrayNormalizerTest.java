/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;

/**
 * Test for {@link ArrayNormalizer}.
 */
public class ArrayNormalizerTest extends OptimizerTestBase {
  // TODO(rluble): add unit test for the rest of the functionality.

  public void testSetCheckElimination_finalTypeArray() throws Exception {
    addSnippetClassDecl("final static class A {String name; public void set() { name = \"A\";} }");

    Result result =
        optimize("void", "A[] a = new A[1]; a[1] = new A();");
    result.intoString(
        "EntryPoint$A[] a = Array.initUnidimensionalArray(EntryPoint$A.class, [], "
            + "/* JRuntimeTypeReference */\"test.EntryPoint$A\", 1, 0, 1);",
        "a[1] = new EntryPoint$A();");
  }

  public void testSetCheckPreservation_nonFinalTypeArray() throws Exception {
    addSnippetClassDecl("static class A {String name; public void set() { name = \"A\";} }");
    addSnippetClassDecl("static class B extends A { }");

    Result result =
        optimize("void", "A[] a = new A[1]; a = new B[1]; a[1] = new A();");
    result.intoString(
        "EntryPoint$A[] a = Array.initUnidimensionalArray(EntryPoint$A.class, [], "
            + "/* JRuntimeTypeReference */\"test.EntryPoint$A\", 1, 0, 1);",
        "a = Array.initUnidimensionalArray(EntryPoint$B.class, [], "
            + "/* JRuntimeTypeReference */\"test.EntryPoint$B\", 1, 0, 1);",
        "Array.setCheck(a, 1, new EntryPoint$A());");
  }

  public void testObjectArray() throws Exception {
    optimize("void", "Object[] o = new Object[10];")
        .intoString("Object[] o = Array.initUnidimensionalArray(Object.class, [], "
            + "/* JRuntimeTypeReference */\"java.lang.Object\", 10, "
            + TypeCategory.TYPE_JAVA_LANG_OBJECT.ordinal() + ", 1);");
    optimize("void", "Object[] o = {null, null, Object.class};")
        .intoString("Object[] o = Array.stampJavaTypeInfo("
            + "Array.getClassLiteralForArray(ClassLiteralHolder.Ljava_lang_Object_2_classLit, 1), "
            + "[], /* JRuntimeTypeReference */\"java.lang.Object\", "
            + TypeCategory.TYPE_JAVA_LANG_OBJECT.ordinal() + ", [null, null, Object.class]);");
    optimize("void", "Object[] o = new Object[] {};")
        .intoString("Object[] o = Array.stampJavaTypeInfo(Array.getClassLiteralForArray("
            + "ClassLiteralHolder.Ljava_lang_Object_2_classLit, 1), [], "
            + "/* JRuntimeTypeReference */\"java.lang.Object\", "
            + TypeCategory.TYPE_JAVA_LANG_OBJECT.ordinal() + ", []);");
  }

  public void testNativeJsTypeArray() throws Exception {
    addSnippetImport("jsinterop.annotations.JsType");
    addSnippetClassDecl("@JsType(isNative = true) static class A {public String name; }");
    optimize("void", "A[] a = new A[10];")
        .intoString("EntryPoint$A[] a = Array.newArray(10);");
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    boolean didChange = true;
    // TODO(jbrosenberg): remove loop when Pruner/CFA interaction is perfect.

    do {
      didChange &= TypeTightener.exec(program).didChange();
      didChange &= MethodCallTightener.exec(program).didChange();
    } while (didChange);
    ArrayNormalizer.exec(program);
    return true;
  }
}
