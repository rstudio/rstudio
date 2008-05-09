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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeOracleMediatorTest extends TestCase {

  private abstract class CheckedMockCompilationUnit extends MockCompilationUnit {
    public CheckedMockCompilationUnit(String packageName,
        String shortMainTypeName, String... shortTypeNames) {
      super(Shared.makeTypeName(packageName, shortMainTypeName));
      register(getTypeName(), this);
      for (String shortTypeName : shortTypeNames) {
        register(Shared.makeTypeName(packageName, shortTypeName), this);
      }
    }

    @Override
    public abstract String getSource();

    public abstract void check(JClassType type) throws NotFoundException;
  }

  private static void assertEqualArraysUnordered(Object[] expected,
      Object[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      boolean matched = false;
      for (int j = 0; j < actual.length; j++) {
        if (expected[i].equals(actual[j])) {
          matched = true;
          break;
        }
      }
      assertTrue(matched);
    }
  }

  private static void assertIsAssignable(JClassType from, JClassType to) {
    assertTrue("'" + from + "' should be assignable to '" + to + "'",
        from.isAssignableTo(to));
    assertTrue("'" + to + "' should be assignable from '" + from + "'",
        to.isAssignableFrom(from));
  }

  private static void assertIsNotAssignable(JClassType from, JClassType to) {
    assertFalse(from.isAssignableTo(to));
    assertFalse(to.isAssignableFrom(from));
  }

  private static void recordAssignability(
      Map<JClassType, Set<JClassType>> assignabilityMap, JClassType from,
      JClassType to) {
    Set<JClassType> set = assignabilityMap.get(from);
    if (set == null) {
      set = new HashSet<JClassType>();
      assignabilityMap.put(from, set);
    }
    set.add(to);
  }

  /**
   * Public so that this will be initialized before the CUs.
   */
  public final Map<String, CheckedMockCompilationUnit> publicTypeNameToTestCupMap = new HashMap<String, CheckedMockCompilationUnit>();

  protected CheckedMockCompilationUnit CU_AfterAssimilate = new CheckedMockCompilationUnit(
      "test.assim", "AfterAssimilate") {
    public void check(JClassType type) {
      assertEquals("test.assim.BeforeAssimilate",
          type.getSuperclass().getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class AfterAssimilate extends BeforeAssimilate { }");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_Assignable = new CheckedMockCompilationUnit(
      "test.sub", "Derived", "BaseInterface", "DerivedInterface",
      "Derived.Nested") {
    public void check(JClassType type) {
      if ("Derived".equals(type.getSimpleSourceName()))
        checkDerived(type);
      else if ("Nested".equals(type.getSimpleSourceName()))
        checkNested(type);
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.sub;\n");
      sb.append("import test.Outer;");
      sb.append("interface BaseInterface { }");
      sb.append("interface DerivedInterface extends BaseInterface { }");
      sb.append("public class Derived extends Outer.Inner {\n");
      sb.append("   public static class Nested extends Outer.Inner implements DerivedInterface { }\n");
      sb.append("}\n");
      return sb.toString();
    }

    private void checkDerived(JClassType type) {
      assertEquals("test.sub.Derived", type.getQualifiedSourceName());
    }

    private void checkNested(JClassType type) {
      assertEquals("test.sub.Derived.Nested", type.getQualifiedSourceName());

    }
  };

  protected CheckedMockCompilationUnit CU_BeforeAssimilate = new CheckedMockCompilationUnit(
      "test.assim", "BeforeAssimilate") {
    public void check(JClassType type) {
      assertEquals("test.assim.BeforeAssimilate", type.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class BeforeAssimilate { }");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_BindToTypeScope = new CheckedMockCompilationUnit(
      "test", "BindToTypeScope", "BindToTypeScope.Object",
      "BindToTypeScope.DerivedObject") {

    public void check(JClassType type) throws NotFoundException {
      if ("BindToTypeScope".equals(type.getSimpleSourceName()))
        checkBindToTypeScope(type);
      else if ("Object".equals(type.getSimpleSourceName()))
        checkObject(type);
      else
        checkDerivedObject(type);
    }

    public void checkBindToTypeScope(JClassType type) throws NotFoundException {
      assertEquals("BindToTypeScope", type.getSimpleSourceName());
      assertEquals("test.BindToTypeScope", type.getQualifiedSourceName());
      JClassType object = type.getNestedType("Object");
      assertNotNull(object);
      JClassType derivedObject = type.getNestedType("DerivedObject");
      assertNotNull(derivedObject);
    }

    public void checkObject(JClassType type) {
      assertEquals("Object", type.getSimpleSourceName());
      assertEquals("test.BindToTypeScope.Object", type.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class BindToTypeScope {\n");
      sb.append("   public static class Object { }\n");
      sb.append("   public static class DerivedObject extends Object { }\n");
      sb.append("}\n");
      return sb.toString();
    }

    private void checkDerivedObject(JClassType type) throws NotFoundException {
      JClassType bindToTypeScope = type.getEnclosingType();
      assertNotNull(bindToTypeScope);

      JClassType object = bindToTypeScope.getNestedType("Object");
      assertNotNull(object);

      JClassType derivedObject = bindToTypeScope.getNestedType("DerivedObject");
      assertNotNull(derivedObject);

      assertEquals(object, derivedObject.getSuperclass());
    }
  };

  protected CheckedMockCompilationUnit CU_DeclaresInnerGenericType = new CheckedMockCompilationUnit(
      "parameterized.type.build.dependency", "Class1", "Class1.Inner") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class1<T> {\n");
      sb.append("  public interface Inner<T> {}\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_DefaultClass = new CheckedMockCompilationUnit(
      "test", "DefaultClass") {
    public void check(JClassType type) {
      assertEquals("DefaultClass", type.getSimpleSourceName());
      assertEquals("test.DefaultClass", type.getQualifiedSourceName());
      JClassType object = type.getOracle().findType("java.lang", "Object");
      assertNotNull(object);
      assertEquals(object, type.getSuperclass());
      assertNull(type.isInterface());
      assertEquals(0, type.getMethods().length);
      assertEquals(0, type.getFields().length);
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class DefaultClass extends Object { }\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_ExtendsGenericList = new CheckedMockCompilationUnit(
      "test.refresh", "ExtendsGenericList") {

    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.getSuperclass().isParameterized());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class ExtendsGenericList extends GenericList<Object> {}");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_ExtendsParameterizedType = new CheckedMockCompilationUnit(
      "parameterized.type.build.dependency", "Class2") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.getSuperclass().isParameterized());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class2 extends Class1<Object> {}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_FieldsAndTypes = new CheckedMockCompilationUnit(
      "test", "Fields", "SomeType") {
    public void check(JClassType type) throws NotFoundException {
      if ("Fields".equals(type.getSimpleSourceName())) {
        assertEquals("test.Fields", type.getQualifiedSourceName());

        TypeOracle tio = type.getOracle();

        JField[] fields = type.getFields();
        assertEquals(12, fields.length);

        JField field;
        JType fieldType;
        JArrayType arrayType;
        JType componentType;
        final JClassType someType = tio.getType("test", "SomeType");
        final JArrayType intArrayType = tio.getArrayType(JPrimitiveType.INT);
        final JArrayType someTypeArrayType = tio.getArrayType(someType);
        final JArrayType intArrayArrayType = tio.getArrayType(intArrayType);

        field = type.getField("privateInt");
        assertTrue(field.isPrivate());
        assertEquals(JPrimitiveType.INT, field.getType());

        field = type.getField("privateSomeType");
        assertTrue(field.isPrivate());
        assertEquals(someType, field.getType());

        field = type.getField("protectedInt");
        assertTrue(field.isProtected());

        field = type.getField("publicInt");
        assertTrue(field.isPublic());

        field = type.getField("packageInt");
        assertTrue(field.isDefaultAccess());

        field = type.getField("staticInt");
        assertTrue(field.isStatic());

        field = type.getField("transientInt");
        assertTrue(field.isTransient());

        field = type.getField("volatileInt");
        assertTrue(field.isVolatile());

        field = type.getField("multiInt");
        assertTrue(field.isPublic());
        assertTrue(field.isStatic());
        assertTrue(field.isFinal());
        assertTrue(field.isTransient());

        field = type.getField("intArray");
        fieldType = field.getType();
        arrayType = fieldType.isArray();
        assertNotNull(arrayType);
        assertSame(intArrayType, arrayType);
        componentType = arrayType.getComponentType();
        assertNotNull(componentType);
        assertSame(JPrimitiveType.INT, componentType);
        assertEquals("int[]", fieldType.getQualifiedSourceName());

        field = type.getField("someTypeArray");
        fieldType = field.getType();
        arrayType = fieldType.isArray();
        assertNotNull(arrayType);
        assertSame(someTypeArrayType, arrayType);
        componentType = arrayType.getComponentType();
        assertNotNull(componentType);
        assertSame(someType, componentType);
        assertEquals("test.SomeType[]", fieldType.getQualifiedSourceName());

        field = type.getField("intArrayArray");
        fieldType = field.getType();
        arrayType = fieldType.isArray();
        assertNotNull(arrayType);
        assertSame(intArrayArrayType, arrayType);
        componentType = arrayType.getComponentType();
        assertNotNull(componentType);
        assertSame(intArrayType, arrayType.getComponentType());
        arrayType = (JArrayType) arrayType.getComponentType();
        assertSame(JPrimitiveType.INT, arrayType.getComponentType());
        assertEquals("int[][]", fieldType.getQualifiedSourceName());

      } else {
        // No need to check SomeType since
        // there's already a DefaultClass
        // test.
      }
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("class SomeType { }");
      sb.append("public class Fields {\n");
      sb.append("   private int privateInt;\n");
      sb.append("   private SomeType privateSomeType;\n");
      sb.append("   protected int protectedInt;\n");
      sb.append("   public int publicInt;\n");
      sb.append("   int packageInt;\n");
      sb.append("   private static int staticInt;\n");
      sb.append("   private transient int transientInt;\n");
      sb.append("   private volatile int volatileInt;\n");
      sb.append("   public static final transient int multiInt = 0;\n");
      sb.append("   private int[] intArray;\n");
      sb.append("   private SomeType[] someTypeArray;\n");
      sb.append("   private int[][] intArrayArray;\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_GenericList = new CheckedMockCompilationUnit(
      "test.refresh", "GenericList") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class GenericList<T> {\n");
      sb.append("  public static final int CONSTANT = 0;\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_HasSyntaxErrors = new CheckedMockCompilationUnit(
      "test", "HasSyntaxErrors", "NoSyntaxErrors") {
    public void check(JClassType classInfo) {
      fail("This class should have been removed");
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("class NoSyntaxErrors { }\n");
      sb.append("public class HasSyntaxErrors { a syntax error }\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_HasUnresolvedSymbols = new CheckedMockCompilationUnit(
      "test", "Invalid", "Valid") {
    public void check(JClassType classInfo) {
      fail("Both classes should have been removed");
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Invalid extends NoSuchClass { }\n");
      sb.append("class Valid extends Object { }\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_LocalClass = new CheckedMockCompilationUnit(
      "test", "Enclosing", "Enclosing.1") {

    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("Enclosing".equals(name))
        checkEnclosing(type);
      else
        checkLocal(type);
    }

    public void checkEnclosing(JClassType type) {
      assertEquals("Enclosing", type.getSimpleSourceName());
      assertEquals("test.Enclosing", type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals("test.Enclosing.1", inner.getQualifiedSourceName());
    }

    public void checkLocal(JClassType type) {
      assertEquals("1", type.getSimpleSourceName());
      assertEquals("test.Enclosing.1", type.getQualifiedSourceName());
      assertEquals("test.Enclosing",
          type.getEnclosingType().getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Enclosing {\n");
      sb.append("   public static Object getLocal() {");
      sb.append("     return new Object() { };\n");
      sb.append("   }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_MetaData = new CheckedMockCompilationUnit(
      "test", "MetaData") {

    public void check(JClassType type) throws NotFoundException {
      {
        String[] tags = type.getMetaDataTags();
        assertEqualArraysUnordered(new String[] {"gwt.tag1"}, tags);
        String[][] md = type.getMetaData("gwt.tag1");
        assertEquals("tagValueA", md[0][0]);
        assertEquals("tagValueB", md[0][1]);
        assertEquals("tagValueC", md[1][0]);
        assertEquals("tagValueD", md[1][1]);
      }

      {
        JMethod method = type.getOverloads("foo")[0]; // will succeed
        String[] tags = method.getMetaDataTags();
        assertEqualArraysUnordered(new String[] {"gwt.tag2", "gwt.tag3"}, tags);

        String[][] mdNotThere = method.getMetaData("not there");
        assertNotNull(mdNotThere);
        assertEquals(0, mdNotThere.length);

        String[][] mdTag2 = method.getMetaData("gwt.tag2");
        assertEquals(2, mdTag2.length);
        assertEquals(3, mdTag2[0].length);
        assertEquals("tagValueV", mdTag2[0][0]);
        assertEquals("tagValueW", mdTag2[0][1]);
        assertEquals("tagValueX", mdTag2[0][2]);
        assertEquals(2, mdTag2[1].length);
        assertEquals("tagValueY", mdTag2[1][0]);
        assertEquals("tagValueZ", mdTag2[1][1]);

        String[][] mdTag3 = method.getMetaData("gwt.tag3");
        assertEquals(1, mdTag3.length);
        assertEquals(0, mdTag3[0].length);
      }

      {
        JField field = type.getField("bar");
        String[] tags = field.getMetaDataTags();
        assertEqualArraysUnordered(new String[] {"gwt.tag4"}, tags);

        String[][] mdTag4 = field.getMetaData("gwt.tag4");
        assertEquals(2, mdTag4.length);
        assertEquals(2, mdTag4[0].length);
        assertEquals("tagValueQ", mdTag4[0][0]);
        assertEquals("tagValueR", mdTag4[0][1]);
        assertEquals(1, mdTag4[1].length);
        assertEquals("tagValueS", mdTag4[1][0]);
      }

      {
        JField field = type.getField("noMd");
        String[] tags = field.getMetaDataTags();
        assertEquals(0, tags.length);
      }
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("/**\n");
      sb.append(" * Class metadata.\n");
      sb.append(" * @gwt.tag1 tagValueA tagValueB\n");
      sb.append(" * @gwt.tag1 tagValueC tagValueD\n");
      sb.append(" */\n");
      sb.append("public class MetaData {\n");
      sb.append("   /**\n");
      sb.append("    * Method metadata.\n");
      sb.append("    * @gwt.tag2 tagValueV tagValueW tagValueX\n");
      sb.append("    * @gwt.tag2 tagValueY tagValueZ\n");
      sb.append("    * @gwt.tag3*/\n");
      sb.append("   private void foo(int x) { };\n");
      sb.append("\n");
      sb.append("   /**@gwt.tag4 tagValueQ tagValueR\n"); // funny start
      sb.append("    * @gwt.tag4 tagValueS*/\n"); // funny end
      sb.append("   private Object bar = null;\n");
      sb.append("   private Object noMd = null;\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_MethodsAndParams = new CheckedMockCompilationUnit(
      "test", "Methods") {

    public void check(JClassType type) throws NotFoundException {
      TypeOracle tio = type.getOracle();
      JMethod[] methods = type.getMethods();
      assertEquals(6, methods.length);
      JMethod method;
      JType[] thrownTypes;
      final JClassType javaLangObject = tio.findType("java.lang", "Object");
      final JClassType javaLangThrowable = tio.findType("java.lang",
          "Throwable");
      final JType[] noParamTypes = new JType[0];

      method = type.getMethod("returnsInt", noParamTypes);
      assertSame(JPrimitiveType.INT, method.getReturnType());
      assertEquals(0, method.getParameters().length);

      method = type.getMethod("returnsSomeType", noParamTypes);
      assertSame(javaLangObject, method.getReturnType());
      assertEquals(0, method.getParameters().length);

      method = type.getMethod("staticMethod", noParamTypes);
      assertSame(JPrimitiveType.VOID, method.getReturnType());
      assertEquals(0, method.getParameters().length);
      assertTrue(method.isStatic());

      method = type.getMethod("finalMethod", noParamTypes);
      assertSame(JPrimitiveType.VOID, method.getReturnType());
      assertEquals(0, method.getParameters().length);
      assertTrue(method.isFinal());

      try {
        method = type.getMethod("overloaded", noParamTypes);
        fail("expected throw");
      } catch (NotFoundException e) {
      }

      methods = type.getOverloads("overloaded");
      assertEquals(2, methods.length);
      for (int i = 0; i < methods.length; i++)
        assertEquals("overloaded", methods[i].getName());

      method = type.getMethod("overloaded", new JType[] {
          JPrimitiveType.INT, javaLangObject});
      assertSame(JPrimitiveType.VOID, method.getReturnType());
      thrownTypes = method.getThrows();
      assertEquals(1, thrownTypes.length);
      assertSame(javaLangThrowable, thrownTypes[0]);

      method = type.getMethod("overloaded", new JType[] {
          JPrimitiveType.INT, JPrimitiveType.CHAR});
      assertSame(javaLangObject, method.getReturnType());
      thrownTypes = method.getThrows();
      assertEquals(0, thrownTypes.length);
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Methods {\n");
      sb.append("   private int returnsInt() { return 0; };\n");
      sb.append("   private Object returnsSomeType() { return null; }\n");
      sb.append("   public static void staticMethod() { return; }\n");
      sb.append("   public final void finalMethod() { return; }\n");
      sb.append("   public void overloaded(int x, Object y) throws Throwable { return; }\n");
      sb.append("   public Object overloaded(int x, char y) { return null; }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_Object = new CheckedMockCompilationUnit(
      "java.lang", "Object") {
    public void check(JClassType type) {
      assertEquals("Object", type.getSimpleSourceName());
      assertEquals("java.lang.Object", type.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Object { }");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_OuterInner = new CheckedMockCompilationUnit(
      "test", "Outer", "Outer.Inner") {

    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("Outer".equals(name))
        checkOuter(type);
      else
        checkInner(type);
    }

    public void checkInner(JClassType type) {
      assertEquals("Inner", type.getSimpleSourceName());
      assertEquals("test.Outer.Inner", type.getQualifiedSourceName());
      assertEquals("test.Outer",
          type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("Outer", type.getSimpleSourceName());
      assertEquals("test.Outer", type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals("test.Outer.Inner", inner.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Outer {\n");
      sb.append("   public static class Inner { }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_ReferencesGenericListConstant = new CheckedMockCompilationUnit(
      "test.refresh", "ReferencesGenericListConstant") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertEquals("test.refresh.ReferencesGenericListConstant",
          type.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class ReferencesGenericListConstant {\n");
      sb.append("  public static final int MY_CONSTANT = GenericList.CONSTANT;\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed = new CheckedMockCompilationUnit(
      "parameterized.type.build.dependency", "Class0") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      JClassType[] intfs = type.getImplementedInterfaces();
      assertEquals(1, intfs.length);
      assertNotNull(intfs[0].isParameterized());
    }

    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class0 implements Class2.Inner<Object> {\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_RefsInfectedCompilationUnit = new CheckedMockCompilationUnit(
      "test", "RefsInfectedCompilationUnit") {
    public void check(JClassType classInfo) {
      fail("This class should should have been removed because it refers to a class in another compilation unit that had problems");
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class RefsInfectedCompilationUnit extends Valid { }\n");
      return sb.toString();
    }
  };

  protected CheckedMockCompilationUnit CU_Throwable = new CheckedMockCompilationUnit(
      "java.lang", "Throwable") {
    public void check(JClassType type) {
      assertEquals("Throwable", type.getSimpleSourceName());
      assertEquals("java.lang.Throwable", type.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Throwable { }");
      return sb.toString();
    }
  };

  private final TypeOracleMediator mediator = new TypeOracleMediator();

  private final TypeOracle typeOracle = mediator.getTypeOracle();

  private final Set<CompilationUnit> units = new HashSet<CompilationUnit>();

  public void checkTypes(JClassType[] types) throws NotFoundException {
    for (int i = 0; i < types.length; i++) {
      JClassType type = types[i];
      check(type);

      JClassType[] nestedTypes = type.getNestedTypes();
      checkTypes(nestedTypes);
    }
  }

  public void testAssignable() throws UnableToCompleteException,
      TypeOracleException {
    units.add(CU_Object);
    units.add(CU_Assignable);
    units.add(CU_OuterInner);
    compileAndRefresh();
    JClassType[] allTypes = typeOracle.getTypes();
    assertEquals(7, allTypes.length);

    Map<JClassType, Set<JClassType>> assignabilityMap = new HashMap<JClassType, Set<JClassType>>();

    JClassType inner = typeOracle.findType("test.Outer.Inner");
    JClassType baseIntf = typeOracle.findType("test.sub.BaseInterface");
    JClassType derivedIntf = typeOracle.findType("test.sub.DerivedInterface");
    recordAssignability(assignabilityMap, derivedIntf, baseIntf);
    JClassType derived = typeOracle.findType("test.sub.Derived");
    recordAssignability(assignabilityMap, derived, inner);
    JClassType nested = typeOracle.findType("test.sub.Derived.Nested");
    recordAssignability(assignabilityMap, nested, inner);
    recordAssignability(assignabilityMap, nested, derivedIntf);
    recordAssignability(assignabilityMap, nested, baseIntf);

    for (JClassType fromType : allTypes) {
      for (JClassType toType : allTypes) {
        if (fromType == toType || toType == typeOracle.getJavaLangObject()) {
          assertIsAssignable(fromType, toType);
        } else {
          Set<JClassType> set = assignabilityMap.get(fromType);
          if (set != null && set.contains(toType)) {
            assertIsAssignable(fromType, toType);
          } else {
            assertIsNotAssignable(fromType, toType);
          }
        }
      }
    }
  }

  public void testAssimilation() throws UnableToCompleteException,
      TypeOracleException {
    units.add(CU_Object);
    units.add(CU_BeforeAssimilate);
    compileAndRefresh();
    assertEquals(2, typeOracle.getTypes().length);
    JClassType before = typeOracle.findType("test.assim.BeforeAssimilate");

    // Build onto an existing type oracle.
    units.add(CU_AfterAssimilate);
    compileAndRefresh();
    assertEquals(3, typeOracle.getTypes().length);

    // Make sure identities remained intact across the assimilation.
    JClassType after = typeOracle.findType("test.assim.AfterAssimilate");
    assertSame(before, after.getSuperclass());
  }

  public void testBindToTypeScope() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_BindToTypeScope);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(4, types.length);
  }

  public void testDefaultClass() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_DefaultClass);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(2, types.length);
  }

  public void testFieldsAndTypes() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_FieldsAndTypes);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  public void testLocal() throws TypeOracleException, UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_LocalClass);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  public void testMetaData() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_MetaData);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(2, types.length);
  }

  public void testMethodsAndParams() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_Throwable);
    units.add(CU_MethodsAndParams);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  public void testOuterInner() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_OuterInner);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  /**
   * Tests that we can build nested parameterized types even if that happens
   * while the type oracle is being built. This test assumes that
   * CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed will
   * cause a parameterized form of CU_DeclaresInnerGenericType to be created
   * before the type oracle has had a chance to resolve
   * CU_DeclaresInnerGenericType.
   */
  public void testParameterizedTypeBuildDependencies()
      throws UnableToCompleteException, TypeOracleException {
    units.add(CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed);
    units.add(CU_ExtendsParameterizedType);
    units.add(CU_DeclaresInnerGenericType);
    units.add(CU_Object);

    compileAndRefresh();
    assertNull(typeOracle.findType("test.parameterizedtype.build.dependencies.Class2"));
  }

  /**
   * Test that modifying a type will cause any types that depend on it to be
   * rebuilt by the TypeOracleBuilder during a refresh.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   * @throws IOException
   */
  public void testRefresh() throws UnableToCompleteException,
      TypeOracleException {
    units.add(CU_Object);
    units.add(CU_ExtendsGenericList);
    units.add(CU_GenericList);
    units.add(CU_ReferencesGenericListConstant);

    compileAndRefresh();

    // Get the types produced by the TypeOracle
    JClassType extendsGenericListType = typeOracle.getType("test.refresh.ExtendsGenericList");
    JClassType genericListType = typeOracle.getType("test.refresh.GenericList");
    JClassType objectType = typeOracle.getJavaLangObject();
    JClassType referencesGenericListConstantType = typeOracle.getType("test.refresh.ReferencesGenericListConstant");

    /*
     * Invalidate CU_GenericList and simulate a refresh. This should cause
     * anything that depends on GenericList to be rebuilt by the type oracle.
     */
    CU_GenericList.setState(State.FRESH);
    compileAndRefresh();

    assertNotSame(genericListType.getQualifiedSourceName() + "; ",
        typeOracle.getType("test.refresh.GenericList"), genericListType);
    assertNotSame(extendsGenericListType.getQualifiedSourceName() + "; ",
        typeOracle.getType("test.refresh.ExtendsGenericList"),
        extendsGenericListType);
    assertSame(objectType.getQualifiedSourceName() + "; ",
        typeOracle.getJavaLangObject(), objectType);

    /*
     * Make sure that referencing a constant field will cause a type to be
     * rebuilt if the constant changes.
     */
    assertNotSame(referencesGenericListConstantType.getQualifiedSourceName(),
        typeOracle.getType("test.refresh.ReferencesGenericListConstant"),
        referencesGenericListConstantType);
  }

  /**
   * Tests that refreshing with a unit that has errors does not cause new units
   * that reference unchanged units to be removed. The strategy is to add some
   * good units that reference each other and build a {@link TypeOracle}. Then
   * we add some new units that have errors as well as some units that reference
   * old units which did not have errors. This ensures that the correct units
   * are pruned from the type oracle in the case where we encounter units with
   * errors.
   * 
   * @throws UnableToCompleteException
   * @throws IOException
   */
  public void testRefreshWithErrors() throws UnableToCompleteException,
      TypeOracleException {
    // Add Object
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;");
    sb.append("public class Object { }");
    addCompilationUnit("java.lang.Object", sb);

    // Add UnmodifiedClass that will never change.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;");
    sb.append("public class UnmodifiedClass { }");
    addCompilationUnit("test.refresh.with.errors.UnmodifiedClass", sb);

    // Add GoodClass that references a class that will go bad.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;\n");
    sb.append("public class GoodClass {\n");
    sb.append("  ClassThatWillGoBad ctwgb;\n");
    sb.append("}\n");
    addCompilationUnit("test.refresh.with.errors.GoodClass", sb);

    // Add ClassThatWillGoBad that goes bad on the next refresh.
    MockCompilationUnit unitThatWillGoBad = new MockCompilationUnit(
        "test.refresh.with.errors.ClassThatWillGoBad") {
      private String source = "package test.refresh.with.errors;\n"
          + "public class ClassThatWillGoBad { }\n";

      @Override
      public String getSource() {
        return source;
      }

      @Override
      void setState(State newState) {
        super.setState(newState);
        source = "This will cause a syntax error.";
      }
    };
    units.add(unitThatWillGoBad);

    compileAndRefresh();

    assertNotNull(typeOracle.findType("test.refresh.with.errors.UnmodifiedClass"));
    assertNotNull(typeOracle.findType("test.refresh.with.errors.GoodClass"));
    assertNotNull(typeOracle.findType("test.refresh.with.errors.ClassThatWillGoBad"));

    // Add AnotherGoodClass that references a
    // class that was not recompiled.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;\n");
    sb.append("public class AnotherGoodClass {\n");
    sb.append("  UnmodifiedClass uc; // This will cause the runaway pruning.\n");
    sb.append("}\n");
    addCompilationUnit("test.refresh.with.errors.AnotherGoodClass", sb);

    // Add BadClass that has errors and originally
    // forced issue 2238.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;\n");
    sb.append("public class BadClass {\n");
    sb.append("  This will trigger a syntax error.\n");
    sb.append("}\n");
    addCompilationUnit("test.refresh.with.errors.BadClass", sb);

    // Now this cup should cause errors.
    unitThatWillGoBad.setState(State.FRESH);

    compileAndRefresh();

    assertNotNull(typeOracle.findType("test.refresh.with.errors.UnmodifiedClass"));
    assertNotNull(typeOracle.findType("test.refresh.with.errors.AnotherGoodClass"));
    assertNull(typeOracle.findType("test.refresh.with.errors.BadClass"));
    assertNull(typeOracle.findType("test.refresh.with.errors.ClassThatWillGoBad"));
    assertNull(typeOracle.findType("test.refresh.with.errors.GoodClass"));
  }

  public void testSyntaxErrors() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_HasSyntaxErrors);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
  }

  public void testUnresolvedSymbls() throws TypeOracleException,
      UnableToCompleteException {
    units.add(CU_Object);
    units.add(CU_HasUnresolvedSymbols);
    units.add(CU_RefsInfectedCompilationUnit);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
  }

  /**
   * Creates a {@link CompilationUnit} and adds it the set of units.
   * 
   * @throws UnableToCompleteException
   */
  private void addCompilationUnit(String qualifiedTypeName, CharSequence source) {
    units.add(new MockCompilationUnit(qualifiedTypeName, source.toString()));
  }

  private void check(JClassType classInfo) throws NotFoundException {
    final String qName = classInfo.getQualifiedSourceName();
    CheckedMockCompilationUnit cup = publicTypeNameToTestCupMap.get(qName);
    if (cup != null) {
      cup.check(classInfo);
    }
  }

  private void compileAndRefresh() throws UnableToCompleteException,
      TypeOracleException {
    TreeLogger logger = createTreeLogger();
    CompilationUnitInvalidator.invalidateUnitsWithInvalidRefs(logger, units);
    JdtCompiler.compile(units);
    CompilationUnitInvalidator.invalidateUnitsWithErrors(logger, units);
    mediator.refresh(logger, units);
    checkTypes(typeOracle.getTypes());
  }

  /**
   * Tweak this if you want to see the log output.
   */
  private TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    } else {
      return TreeLogger.NULL;
    }
  }

  private void register(String qualifiedTypeName, CheckedMockCompilationUnit cup) {
    publicTypeNameToTestCupMap.put(qualifiedTypeName, cup);
  }
}
