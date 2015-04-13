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
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

/**
 * Test for {@link ImplicitUpcastAnalyzer}.
 */
public class ImplicitUpcastAnalyzerTest extends OptimizerTestBase {
  // TODO(rluble): populate with unit tests for all upcast scenarios.
  // Currently only upcasts due to overriding are tested here.

  public void testOverrides() throws Exception {
    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("test.A",
            "package test;",
            "public class A {",
            "   public A m() { return new B(); } ",
            "}");

    final MockJavaResource I =
        JavaResourceBase.createMockJavaResource("test.I",
            "package test;",
            "public interface I {",
            "   public A m(); ",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("test.B",
            "package test;",
            "public class B extends A implements I {",
            "   public A m() { return new B(); } ",
            "}");

    final MockJavaResource C =
        JavaResourceBase.createMockJavaResource("test.C",
            "package test;",
            "public class C implements I {",
            "   public A m() { return new A(); } ",
            "}");

    addAll(A, I, B, C);
    Result result = optimize("void", "new A().m(); new B().m();");
    Multimap<JType, JType> upcastTypesByType = computeImplicitUpcasts(result.getOptimizedProgram());
    JType classA = result.findClass("A");
    JType classB = result.findClass("B");

    assertMapsToAll(upcastTypesByType, classB, classA);
  }

  public void testTightening() throws Exception {
    final MockJavaResource A =
        JavaResourceBase.createMockJavaResource("test.A",
            "package test;",
            "public class A {",
            "   public A m() { return new B(); } ",
            "}");

    final MockJavaResource I =
        JavaResourceBase.createMockJavaResource("test.I",
            "package test;",
            "public interface I {",
            "   public A m(); ",
            "}");

    final MockJavaResource B =
        JavaResourceBase.createMockJavaResource("test.B",
            "package test;",
            "public class B extends A implements I {",
            "   public A m() { return new B(); } ",
            "}");

    final MockJavaResource C =
        JavaResourceBase.createMockJavaResource("test.C",
            "package test;",
            "public class C implements I {",
            "   public A m() { return new A(); } ",
            "}");

    addAll(A, I, B, C);
    // Variable a will be typed by a non null analysis type.
    Result result = optimize("void", "A a = new A(); a = new B(); a.m();");
    Multimap<JType, JType> upcastTypesByType = computeImplicitUpcasts(result.getOptimizedProgram());
    JType classA = result.findClass("A");
    JType classB = result.findClass("B");

    assertMapsToAll(upcastTypesByType, classB, classA);
  }

  private void assertMapsToAll(Multimap<JType, JType> upcastTypesByType,
      JType fromType, JType... toTypes) {
    assertEquals(ImmutableSet.copyOf(toTypes), upcastTypesByType.get(fromType));
  }

  private  Multimap<JType, JType> computeImplicitUpcasts(JProgram program) {
    final Multimap<JType, JType> upcastTypesByType = HashMultimap.create();
    ImplicitUpcastAnalyzer implicitUpcastAnalyzer = new ImplicitUpcastAnalyzer(program) {
      @Override
      protected void processImplicitUpcast(JType fromType, JType destType, SourceInfo info) {
        upcastTypesByType.put(fromType.getUnderlyingType(), destType.getUnderlyingType());
      }
    };
    implicitUpcastAnalyzer.accept(program);
    return upcastTypesByType;
  }

  @Override
  protected boolean doOptimizeMethod(TreeLogger logger, JProgram program, JMethod method) {
    program.addEntryMethod(findMainMethod(program));
    TypeTightener.exec(program);
    return false;
  }
}
