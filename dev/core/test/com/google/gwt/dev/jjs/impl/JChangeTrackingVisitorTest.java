/*
 * Copyright 2014 Google Inc.
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

import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JVariable;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.FluentIterable;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;

/**
 * Test for {@link JChangeTrackingVisitor}.
 */
public class JChangeTrackingVisitorTest extends JJSTestBase {

  private static final class AddParamWhenEnterMethodVisitor extends JChangeTrackingVisitor {

    public AddParamWhenEnterMethodVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_enter", JPrimitiveType.INT);
      return true;
    }
  }

  private static final class AddParamWhenExitMethodVisitor extends JChangeTrackingVisitor {
    public AddParamWhenExitMethodVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_exit", JPrimitiveType.INT);
    }
  }

  private static final class AddParamsWhenEnterAndExitMethodVisitor extends JChangeTrackingVisitor {
    public AddParamsWhenEnterAndExitMethodVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_enter", JPrimitiveType.INT);
      return true;
    }

    @Override
    public void exit(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_exit", JPrimitiveType.INT);
    }
  }

  private static final class AddParamWhenEnterNonConstructorMethodVisitor extends
      JChangeTrackingVisitor {
    public AddParamWhenEnterNonConstructorMethodVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_enter", JPrimitiveType.INT);
      return true;
    }

    @Override
    public boolean enter(JConstructor x, Context ctx) {
      return true;
    }
  }

  private static final class AddParamWhenExitNonConstructorMethodVisitor extends
      JChangeTrackingVisitor {
    public AddParamWhenExitNonConstructorMethodVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JMethod x, Context ctx) {
      x.createParameter(SourceOrigin.UNKNOWN, "_newParam_exit", JPrimitiveType.INT);
    }

    @Override
    public void exit(JConstructor x, Context ctx) {
      return;
    }
  }

  private static final class SetVariableOfIntToLongByEnterVisitor extends JChangeTrackingVisitor {
    public SetVariableOfIntToLongByEnterVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JVariable x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
      return true;
    }
  }

  private static final class SetVariableOfIntToLongByExitVisitor extends JChangeTrackingVisitor {
    public SetVariableOfIntToLongByExitVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JVariable x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
    }
  }

  private static final class SetNonFieldVariableOfIntToLongByEnterVisitor extends
      JChangeTrackingVisitor {
    public SetNonFieldVariableOfIntToLongByEnterVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JVariable x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
      return true;
    }

    @Override
    public boolean enter(JField x, Context ctx) {
      return true;
    }
  }

  private static final class SetNonFieldVariableOfIntToLongByExitVisitor extends
      JChangeTrackingVisitor {
    public SetNonFieldVariableOfIntToLongByExitVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JVariable x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
    }

    @Override
    public void exit(JField x, Context ctx) {
      return;
    }
  }

  private static final class SetFieldOfIntToLongByEnterVisitor extends JChangeTrackingVisitor {
    public SetFieldOfIntToLongByEnterVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean enter(JField x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
      return true;
    }
  }

  private static final class SetFieldOfIntToLongByExitVisitor extends JChangeTrackingVisitor {
    public SetFieldOfIntToLongByExitVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JField x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
    }
  }

  private static final class ReplaceConditionalExprWithItsThenExprVisitor extends
      JChangeTrackingVisitor {

    public ReplaceConditionalExprWithItsThenExprVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void endVisit(JConditional x, Context ctx) {
      ctx.replaceMe(x.getThenExpr());
    }
  }

  private static final class ReplaceAddOperationWithItsFirstOperandVisitor extends
      JChangeTrackingVisitor {

    public ReplaceAddOperationWithItsFirstOperandVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void endVisit(JBinaryOperation x, Context ctx) {
      if (x.getOp() == JBinaryOperator.ADD) {
        ctx.replaceMe(x.getLhs());
      }
    }
  }

  private static final class SetFieldOfIntToLongVisitor extends JChangeTrackingVisitor {

    public SetFieldOfIntToLongVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public void exit(JField x, Context ctx) {
      if (x.getType().equals(JPrimitiveType.INT)) {
        x.setType(JPrimitiveType.LONG);
        madeChanges();
      }
    }
  }

  private static final class RemoveMethodsWithThreeParamsVisitor extends JChangeTrackingVisitor {

    public RemoveMethodsWithThreeParamsVisitor(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean visit(JDeclaredType x, Context ctx) {
      for (int i = 1; i < x.getMethods().size(); ++i) {
        JMethod method = x.getMethods().get(i);
        if (method.getParams().size() == 3) {
          wasRemoved(method);
          x.removeMethod(i);
          madeChanges();
          --i;
        }
      }
      return false;
    }
  }

  private static final class RemoveFieldsOfLongType extends JChangeTrackingVisitor {

    public RemoveFieldsOfLongType(OptimizerContext optimizerCtx) {
      super(optimizerCtx);
    }

    @Override
    public boolean visit(JDeclaredType x, Context ctx) {
      for (int i = 0; i < x.getFields().size(); ++i) {
        JField field = x.getFields().get(i);
        if (field.getType().equals(JPrimitiveType.LONG)) {
          wasRemoved(field);
          x.removeField(i);
          madeChanges();
          --i;
        }
      }
      return false;
    }
  }

  public void testAddParamWhenEnterMethodVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    AddParamWhenEnterMethodVisitor addParamWhenEnterMethodVisitor =
        new AddParamWhenEnterMethodVisitor(new FullOptimizerContext(program));
    addParamWhenEnterMethodVisitor.accept(program);
    assertParameterTypes(program, "test.EntryPoint$A.fun()V", "int");
    assertParameterTypes(program, "test.EntryPoint$A.EntryPoint$A(I) <init>", "int", "int");
  }

  public void testAddParamWhenExitMethodVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    AddParamWhenExitMethodVisitor addParamWhenExitMethodVisitor =
        new AddParamWhenExitMethodVisitor(new FullOptimizerContext(program));
    addParamWhenExitMethodVisitor.accept(program);
    assertParameterTypes(program, "test.EntryPoint$A.fun()V", "int");
    assertParameterTypes(program, "test.EntryPoint$A.EntryPoint$A(I) <init>", "int", "int");
  }

  public void testAddParamsWhenEnterAndExitMethodVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    AddParamsWhenEnterAndExitMethodVisitor addParamsWhenEnterAndExitMethodVisitor =
        new AddParamsWhenEnterAndExitMethodVisitor(new FullOptimizerContext(program));
    addParamsWhenEnterAndExitMethodVisitor.accept(program);
    assertParameterTypes(program, "test.EntryPoint$A.fun()V", "int", "int");
    assertParameterTypes(program, "test.EntryPoint$A.EntryPoint$A(I) <init>", "int", "int", "int");
  }

  public void testAddParamWhenEnterNonConstructorMethodVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    AddParamWhenEnterNonConstructorMethodVisitor addParamsWhenEnterNonConstructorMethodVisitor =
        new AddParamWhenEnterNonConstructorMethodVisitor(new FullOptimizerContext(program));
    addParamsWhenEnterNonConstructorMethodVisitor.accept(program);
    assertParameterTypes(program, "test.EntryPoint$A.fun()V", "int");
    assertParameterTypes(program, "test.EntryPoint$A.EntryPoint$A(I) <init>", "int");
  }

  public void testAddParamWhenExitNonConstructorMethodVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    AddParamWhenExitNonConstructorMethodVisitor addParamsWhenExitNonConstructorMethodVisitor =
        new AddParamWhenExitNonConstructorMethodVisitor(new FullOptimizerContext(program));
    addParamsWhenExitNonConstructorMethodVisitor.accept(program);
    assertParameterTypes(program, "test.EntryPoint$A.fun()V", "int");
    assertParameterTypes(program, "test.EntryPoint$A.EntryPoint$A(I) <init>", "int");
  }

  public void testSetVariableOfIntToLongByEnterVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetVariableOfIntToLongByEnterVisitor setVariableOfIntToLongByEnterVisitor =
        new SetVariableOfIntToLongByEnterVisitor(new FullOptimizerContext(program));
    setVariableOfIntToLongByEnterVisitor.accept(program);
    assertEquals("long field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "long");
  }

  public void testSetVariableOfIntToLongByExitVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetVariableOfIntToLongByExitVisitor setVariableOfIntToLongByExitVisitor =
        new SetVariableOfIntToLongByExitVisitor(new FullOptimizerContext(program));
    setVariableOfIntToLongByExitVisitor.accept(program);
    assertEquals("long field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "long");
  }

  public void testSetNonFieldVariableOfIntToLongByEnterVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetNonFieldVariableOfIntToLongByEnterVisitor setNonFieldVariableOfIntToLongByEnterVisitor =
        new SetNonFieldVariableOfIntToLongByEnterVisitor(new FullOptimizerContext(program));
    setNonFieldVariableOfIntToLongByEnterVisitor.accept(program);
    assertEquals("int field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "long");
  }

  public void testSetNonFieldVariableOfIntToLongByExitVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetNonFieldVariableOfIntToLongByExitVisitor setNonFieldVariableOfIntToLongByExitVisitor =
        new SetNonFieldVariableOfIntToLongByExitVisitor(new FullOptimizerContext(program));
    setNonFieldVariableOfIntToLongByExitVisitor.accept(program);
    assertEquals("int field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "long");
  }

  public void testSetFieldOfIntToLongByEnterVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetFieldOfIntToLongByEnterVisitor setFieldOfIntToLongByEnterVisitor =
        new SetFieldOfIntToLongByEnterVisitor(new FullOptimizerContext(program));
    setFieldOfIntToLongByEnterVisitor.accept(program);
    assertEquals("long field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "int");
  }

  public void testSetFieldOfIntToLongByExitVisitor() throws Exception {
    addSnippetClassDecl("static class A {", "public int field1;", "public double field2;",
        "public void fun(int a) { a++; }", "}");
    JProgram program = compileSnippet("void", "");
    SetFieldOfIntToLongByExitVisitor setFieldOfIntToLongByExitVisitor =
        new SetFieldOfIntToLongByExitVisitor(new FullOptimizerContext(program));
    setFieldOfIntToLongByExitVisitor.accept(program);
    assertEquals("long field1",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field1").toString());
    assertEquals("double field2",
        findField(program.getFromTypeMap("test.EntryPoint$A"), "field2").toString());
    assertParameterTypes(program, "test.EntryPoint$A.fun(I)V", "int");
  }

  public void testInitialModifications() throws Exception {
    addSnippetClassDecl("static class A {", "  public int field;",
        "  public A(int f) { field = f; }",
        "  public void fun () { for(int i = 3; i < 4; i++) i = 8; }", "}");
    JProgram program = compileSnippet("void", "");
    OptimizerContext optimizerCtx = new FullOptimizerContext(program);
    int countMethod = 0;
    int countField = 0;
    for (JDeclaredType type : program.getModuleDeclaredTypes()) {
      for (JMethod method : type.getMethods()) {
        assertTrue(optimizerCtx.getModifiedMethodsSince(0).contains(method));
      }
      for (JField field : type.getFields()) {
        assertTrue(optimizerCtx.getModifiedFieldsSince(0).contains(field));
      }
      countMethod += type.getMethods().size();
      countField += type.getFields().size();
    }
    assertEquals(countMethod, optimizerCtx.getModifiedMethodsSince(0).size());
    assertEquals(countField, optimizerCtx.getModifiedFieldsSince(0).size());
  }

  public void testModificationTracking() throws Exception {
    addSnippetClassDecl("static class A {",
        "  public double field;",
        "  public static int staticField = 1 < 2 ? 1 : 0;",
        "  public int instanceField = 1 + 1;",
        "  public A(double f) { field = f; }",
        "  public void fun1 () { for(int i = 3; i < 4; i++) i = 8; }",
        "  public int fun2 (int a) { return a > 1 ? 1 : 0; }",
        "  public void fun3 () { int a; for(int i = 3; i < 4; i++) a = i < 4 ? 1 : 0; }",
        "  public int fun4 (int a, int b) { return a + b; }",
        "  public int fun5 (int a, int b, int c) { return c > 0 ? a + b : a - b; }",
        "}");
    addSnippetClassDecl("static class B{",
        "  public int field1;",
        "  public boolean field2;",
        "  public char field3;",
        "  public short field5;",
        "}");
    JProgram program = compileSnippet("void", "");
    OptimizerContext optimizerCtx = new FullOptimizerContext(program);

    int first = optimizerCtx.getOptimizationStep();
    ReplaceConditionalExprWithItsThenExprVisitor repalceConditionalExprVisitor =
        new ReplaceConditionalExprWithItsThenExprVisitor(optimizerCtx);
    repalceConditionalExprVisitor.accept(program.getFromTypeMap("test.EntryPoint$A"));
    optimizerCtx.incOptimizationStep();

    Set<JMethod> modifiedMethodsInIteration1 =
        ImmutableSet.copyOf(getMethods(program, "test.EntryPoint$A",
            "fun2", "fun3", "fun5", GwtAstBuilder.CLINIT_METHOD_NAME));
    Set<JField> modifiedFieldsInIteration1 =
        ImmutableSet.copyOf(getFields(program, "test.EntryPoint$A",
            "staticField"));

    assertEquals(modifiedFieldsInIteration1, optimizerCtx.getModifiedFieldsSince(first));
    assertEquals(modifiedMethodsInIteration1, optimizerCtx.getModifiedMethodsSince(first));

    int second = optimizerCtx.getOptimizationStep();
    ReplaceAddOperationWithItsFirstOperandVisitor replaceAddOperationVisitor =
        new ReplaceAddOperationWithItsFirstOperandVisitor(optimizerCtx);
    replaceAddOperationVisitor.accept(program.getFromTypeMap("test.EntryPoint$A"));
    optimizerCtx.incOptimizationStep();

    Set<JMethod> modifiedMethodsInIteration2 =
        ImmutableSet.copyOf(getMethods(program, "test.EntryPoint$A",
                "fun4", "fun5", GwtAstBuilder.INIT_NAME_METHOD_NAME));
    Set<JField> modifiedFieldsInIteration2 =
        ImmutableSet.copyOf(getFields(program, "test.EntryPoint$A",
            "instanceField", "field"));

    assertEquals(modifiedFieldsInIteration2, optimizerCtx.getModifiedFieldsSince(second));
    assertEquals(modifiedMethodsInIteration2, optimizerCtx.getModifiedMethodsSince(second));

    assertEquals(Sets.union(modifiedFieldsInIteration1, modifiedFieldsInIteration2),
        optimizerCtx.getModifiedFieldsSince(first));
    assertEquals(Sets.union(modifiedMethodsInIteration1, modifiedMethodsInIteration2),
        optimizerCtx.getModifiedMethodsSince(first));

    int third = optimizerCtx.getOptimizationStep();
    SetFieldOfIntToLongVisitor setFieldOfIntToLongVisitor =
        new SetFieldOfIntToLongVisitor(optimizerCtx);
    setFieldOfIntToLongVisitor.accept(program.getFromTypeMap("test.EntryPoint$B"));
    optimizerCtx.incOptimizationStep();

    Set<JMethod> modifiedMethodsInIteration3 = ImmutableSet.of();
    Set<JField> modifiedFieldsInIteration3 =
        ImmutableSet.copyOf(getFields(program, "test.EntryPoint$B",
            "field1"));

    assertEquals(modifiedFieldsInIteration3, optimizerCtx.getModifiedFieldsSince(third));
    assertEquals(modifiedMethodsInIteration3, optimizerCtx.getModifiedMethodsSince(third));

    assertEquals(Sets.union(modifiedFieldsInIteration2, modifiedFieldsInIteration3),
        optimizerCtx.getModifiedFieldsSince(second));
    assertEquals(Sets.union(modifiedMethodsInIteration2, modifiedMethodsInIteration3),
        optimizerCtx.getModifiedMethodsSince(second));

    assertEquals(Sets.union(Sets.union(
            modifiedFieldsInIteration1, modifiedFieldsInIteration2), modifiedFieldsInIteration3),
        optimizerCtx.getModifiedFieldsSince(first));
    assertEquals(Sets.union(Sets.union(
            modifiedMethodsInIteration1, modifiedMethodsInIteration2), modifiedMethodsInIteration3),
        optimizerCtx.getModifiedMethodsSince(first));

    Set<JMethod> methodsDeletedInIteration4 = getMethods(program, "test.EntryPoint$A", "fun5");

    RemoveMethodsWithThreeParamsVisitor removeMethodsWithThreeParamsVisitor =
        new RemoveMethodsWithThreeParamsVisitor(optimizerCtx);
    removeMethodsWithThreeParamsVisitor.accept(program.getFromTypeMap("test.EntryPoint$A"));

    assertEquals(Sets.difference(Sets.union(Sets.union(
                modifiedMethodsInIteration1, modifiedMethodsInIteration2),
                modifiedMethodsInIteration3),methodsDeletedInIteration4),
        optimizerCtx.getModifiedMethodsSince(first));

    Set<JField> fieldsDeletedInIteration4 = getFields(program, "test.EntryPoint$B", "field1");
    RemoveFieldsOfLongType removeFieldsOfLongType = new RemoveFieldsOfLongType(optimizerCtx);
    removeFieldsOfLongType.accept(program.getFromTypeMap("test.EntryPoint$B"));

    assertEquals(Sets.difference(Sets.union(Sets.union(
                modifiedFieldsInIteration1, modifiedFieldsInIteration2),
            modifiedFieldsInIteration3),fieldsDeletedInIteration4),
        optimizerCtx.getModifiedFieldsSince(first));
  }

  private Set<JMethod> getMethods(
      final JProgram program, final String clazz, String... methodNames) {
    return FluentIterable.from(Arrays.asList(methodNames)).transform(
        new Function<String, JMethod>() {
          @Override
          public JMethod apply(String methodName) {
            return JJSTestBase.findMethod(program.getFromTypeMap(clazz), methodName);
          }
        }).toSet();
  }

  private Set<JField> getFields(final JProgram program, final String clazz, String... fieldNames) {
    return FluentIterable.from(Arrays.asList(fieldNames)).transform(
        new Function<String, JField>() {
          @Override
          public JField apply(String fieldName) {
            return JJSTestBase.findField(program.getFromTypeMap(clazz), fieldName);
          }
        }).toSet();
  }
}
