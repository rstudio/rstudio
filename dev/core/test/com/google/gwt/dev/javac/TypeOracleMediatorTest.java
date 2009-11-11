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
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.javac.impl.MockJavaResource;
import com.google.gwt.dev.javac.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeOracleMediatorTest extends TestCase {

  private abstract class MutableJavaResource extends MockJavaResource {
    private String extraSource = "";
    private long lastModified = System.currentTimeMillis();

    public MutableJavaResource(String qualifiedTypeName) {
      super(qualifiedTypeName);
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    protected CharSequence getContent() {
      return getSource() + extraSource;
    }

    public abstract String getSource();

    public void touch() {
      extraSource += '\n';
      lastModified = System.currentTimeMillis();
    }
  }

  private abstract class CheckedJavaResource extends MutableJavaResource {
    public CheckedJavaResource(String packageName, String shortMainTypeName,
        String... shortTypeNames) {
      super(Shared.makeTypeName(packageName, shortMainTypeName));
      register(getTypeName(), this);
      for (String shortTypeName : shortTypeNames) {
        register(Shared.makeTypeName(packageName, shortTypeName), this);
      }
    }

    public abstract void check(JClassType type) throws NotFoundException;
  }

  private static void assertIsAssignable(JClassType from, JClassType to) {
    assertTrue("'" + from + "' should be assignable to '" + to + "'",
        from.isAssignableTo(to));
    assertTrue("'" + to + "' should be assignable from '" + from + "'",
        to.isAssignableFrom(from));
  }

  private static void assertIsNotAssignable(JClassType from, JClassType to) {
    assertFalse(from + " should not be assignable to " + to,
        from.isAssignableTo(to));
    assertFalse(to + " should not be assignable to " + from,
        to.isAssignableFrom(from));
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
  public final Map<String, CheckedJavaResource> publicTypeNameToTestCupMap = new HashMap<String, CheckedJavaResource>();

  protected CheckedJavaResource CU_AfterAssimilate = new CheckedJavaResource(
      "test.assim", "AfterAssimilate") {
    @Override
    public void check(JClassType type) {
      assertEquals("test.assim.BeforeAssimilate",
          type.getSuperclass().getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class AfterAssimilate extends BeforeAssimilate { }");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_Assignable = new CheckedJavaResource(
      "test.sub", "Derived", "BaseInterface", "DerivedInterface",
      "Derived.Nested") {
    @Override
    public void check(JClassType type) {
      if ("Derived".equals(type.getSimpleSourceName())) {
        checkDerived(type);
      } else if ("Nested".equals(type.getSimpleSourceName())) {
        checkNested(type);
      }
    }

    @Override
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

  protected CheckedJavaResource CU_BeforeAssimilate = new CheckedJavaResource(
      "test.assim", "BeforeAssimilate") {
    @Override
    public void check(JClassType type) {
      assertEquals("test.assim.BeforeAssimilate", type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class BeforeAssimilate { }");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_BindToTypeScope = new CheckedJavaResource(
      "test", "BindToTypeScope", "BindToTypeScope.Object",
      "BindToTypeScope.DerivedObject") {

    @Override
    public void check(JClassType type) throws NotFoundException {
      if ("BindToTypeScope".equals(type.getSimpleSourceName())) {
        checkBindToTypeScope(type);
      } else if ("Object".equals(type.getSimpleSourceName())) {
        checkObject(type);
      } else {
        checkDerivedObject(type);
      }
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

    @Override
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

  protected CheckedJavaResource CU_ConstrainedList = new CheckedJavaResource(
      "test", "ConstrainedList") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test;\n");
      sb.append("public interface ConstrainedList<E extends Throwable> {\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ConstrainedListAsField = new CheckedJavaResource(
      "test", "ConstrainedListAsField") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNull(type.isGenericType());
      assertNull(type.getEnclosingType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test;\n");
      sb.append("public class ConstrainedListAsField {\n");
      sb.append("  private ConstrainedList<?> field;");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_DeclaresInnerGenericType = new CheckedJavaResource(
      "parameterized.type.build.dependency", "Class1", "Class1.Inner") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class1<T> {\n");
      sb.append("  public interface Inner<T> {}\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_DefaultClass = new CheckedJavaResource(
      "test", "DefaultClass") {
    @Override
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

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class DefaultClass extends Object { }\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ExtendsGenericList = new CheckedJavaResource(
      "test.refresh", "ExtendsGenericList") {

    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.getSuperclass().isParameterized());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class ExtendsGenericList extends GenericList<Object> {}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ExtendsGenericOuterInner = new CheckedJavaResource(
      "test", "ExtendsOuter", "ExtendsOuter.ExtendsInner") {

    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("ExtendsOuter".equals(name)) {
        checkOuter(type);
      } else {
        checkInner(type);
      }
    }

    public void checkInner(JClassType type) {
      assertEquals("ExtendsInner", type.getSimpleSourceName());
      assertEquals("test.ExtendsOuter.ExtendsInner",
          type.getQualifiedSourceName());
      assertEquals("test.ExtendsOuter",
          type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("ExtendsOuter", type.getSimpleSourceName());
      assertEquals("test.ExtendsOuter", type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals("test.ExtendsOuter.ExtendsInner",
          inner.getQualifiedSourceName());
    }

    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class ExtendsOuter extends Outer<Object> {\n");
      sb.append("   public class ExtendsInner extends Inner {\n");
      sb.append("   }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ExtendsParameterizedType = new CheckedJavaResource(
      "parameterized.type.build.dependency", "Class2") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.getSuperclass().isParameterized());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class2 extends Class1<Object> {}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_FieldsAndTypes = new CheckedJavaResource(
      "test", "Fields", "SomeType") {
    @Override
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

    @Override
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

  protected CheckedJavaResource CU_GenericList = new CheckedJavaResource(
      "test.refresh", "GenericList") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class GenericList<T> {\n");
      sb.append("  public static final int CONSTANT = 0;\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_GenericOuterInner = new CheckedJavaResource(
      "test", "Outer", "Outer.Inner") {

    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("GenericOuter".equals(name)) {
        checkOuter(type);
      } else {
        checkInner(type);
      }
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
      sb.append("import java.util.List;\n");
      sb.append("public class Outer<V> {\n");
      sb.append("   public class Inner {\n");
      sb.append("     private V field;\n");
      sb.append("     private List<V> list;\n");
      sb.append("   }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_HasSyntaxErrors = new CheckedJavaResource(
      "test", "HasSyntaxErrors", "NoSyntaxErrors") {
    @Override
    public void check(JClassType classInfo) {
      fail("This class should have been removed");
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("class NoSyntaxErrors { }\n");
      sb.append("public class HasSyntaxErrors { a syntax error }\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_HasUnresolvedSymbols = new CheckedJavaResource(
      "test", "Invalid", "Valid") {
    @Override
    public void check(JClassType classInfo) {
      fail("Both classes should have been removed");
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Invalid extends NoSuchClass { }\n");
      sb.append("class Valid extends Object { }\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_List = new CheckedJavaResource("java.util",
      "List") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package java.util;\n");
      sb.append("public interface List<E> {\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ListAsField = new CheckedJavaResource(
      "test.refresh", "ListAsField") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNull(type.isGenericType());
      assertNull(type.getEnclosingType());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("import java.util.List;\n");
      sb.append("public class ListAsField {\n");
      sb.append("  private List<Object> field;");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_LocalClass = new CheckedJavaResource("test",
      "Enclosing", "Enclosing.1") {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      assertEquals("Enclosing", name);
      checkEnclosing(type);
    }

    public void checkEnclosing(JClassType type) {
      assertEquals("Enclosing", type.getSimpleSourceName());
      assertEquals("test.Enclosing", type.getQualifiedSourceName());
      // verify the anonymous class doesn't show up
      JClassType[] nested = type.getNestedTypes();
      assertEquals(0, nested.length);
    }

    @Override
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

  protected CheckedJavaResource CU_MethodsAndParams = new CheckedJavaResource(
      "test", "Methods") {

    @Override
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
      for (JMethod element : methods) {
        assertEquals("overloaded", element.getName());
      }

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

    @Override
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

  protected CheckedJavaResource CU_NestedGenericInterfaces = new CheckedJavaResource(
      "test", "OuterInt", "OuterInt.InnerInt") {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("OuterInt".equals(name)) {
        checkOuter(type);
      } else {
        checkInner(type);
      }
    }

    public void checkInner(JClassType type) {
      assertEquals("InnerInt", type.getSimpleSourceName());
      assertEquals("test.OuterInt.InnerInt", type.getQualifiedSourceName());
      assertEquals("test.OuterInt",
          type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("OuterInt", type.getSimpleSourceName());
      assertEquals("test.OuterInt", type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals("test.OuterInt.InnerInt", inner.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public interface OuterInt<K,V> {\n");
      sb.append("   public interface InnerInt<V> { }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_Object = new CheckedJavaResource(
      "java.lang", "Object") {
    @Override
    public void check(JClassType type) {
      assertEquals("Object", type.getSimpleSourceName());
      assertEquals("java.lang.Object", type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Object { }");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_OuterInner = new CheckedJavaResource("test",
      "Outer", "Outer.Inner") {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("Outer".equals(name)) {
        checkOuter(type);
      } else {
        checkInner(type);
      }
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

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Outer {\n");
      sb.append("   public static class Inner { }\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ReferencesGenericListConstant = new CheckedJavaResource(
      "test.refresh", "ReferencesGenericListConstant") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertEquals("test.refresh.ReferencesGenericListConstant",
          type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test.refresh;\n");
      sb.append("class ReferencesGenericListConstant {\n");
      sb.append("  public static final int MY_CONSTANT = GenericList.CONSTANT;\n");
      sb.append("}");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed = new CheckedJavaResource(
      "parameterized.type.build.dependency", "Class0") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      JClassType[] intfs = type.getImplementedInterfaces();
      assertEquals(1, intfs.length);
      assertNotNull(intfs[0].isParameterized());
    }

    @Override
    public String getSource() {
      StringBuilder sb = new StringBuilder();
      sb.append("package parameterized.type.build.dependency;\n");
      sb.append("public class Class0 implements Class2.Inner<Object> {\n");
      sb.append("}\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_RefsInfectedCompilationUnit = new CheckedJavaResource(
      "test", "RefsInfectedCompilationUnit") {
    @Override
    public void check(JClassType classInfo) {
      fail("This class should should have been removed because it refers to a class in another compilation unit that had problems");
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class RefsInfectedCompilationUnit extends Valid { }\n");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_Throwable = new CheckedJavaResource(
      "java.lang", "Throwable") {
    @Override
    public void check(JClassType type) {
      assertEquals("Throwable", type.getSimpleSourceName());
      assertEquals("java.lang.Throwable", type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Throwable { }");
      return sb.toString();
    }
  };

  protected CheckedJavaResource CU_UnnestedImplementations = new CheckedJavaResource(
      "test", "Implementations") {
    @Override
    public void check(JClassType type) {
      assertEquals("Implementations", type.getSimpleSourceName());
      assertEquals("test.Implementations", type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;");
      sb.append("public class Implementations {");
      sb.append("  public static class OuterImpl<K,V> implements OuterInt<K,V> {}");
      sb.append("  public static class InnerImpl<V> implements OuterInt.InnerInt<V> {}");
      sb.append("}");
      return sb.toString();
    }
  };

  private TypeOracle typeOracle;

  private final Set<Resource> resources = new HashSet<Resource>();

  public void checkTypes(JClassType[] types) throws NotFoundException {
    for (JClassType type : types) {
      check(type);

      JClassType[] nestedTypes = type.getNestedTypes();
      checkTypes(nestedTypes);
    }
  }

  /**
   * Tests which variant of AbstractRefrenceMap we want to store the map for
   * parameterizedTypes, arrayTypes, and wildCardTypes in TypeOracle. Note: this
   * test is manual because gc can be unreliable.
   */
  @SuppressWarnings("unchecked")
  public void manualTestAbstractRefrenceMap() {

    /*
     * with a HARD -> WEAK map, verify that the entry remains if there is no
     * reference to key, but is deleted when the reference to value is gone
     */
    Map<Integer, Integer> simpleMap = new ReferenceMap(
        AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK, true);
    Integer bar = new Integer(42);
    simpleMap.put(new Integer(32), bar);
    Runtime.getRuntime().gc();
    assertEquals(1, simpleMap.size());
    bar = null;
    Runtime.getRuntime().gc();
    assertEquals(0, simpleMap.size());

    /*
     * with a WEAK -> WEAK map, verify that the entry is gone if there are no
     * references to either the key or the value.
     */
    simpleMap = new ReferenceMap(AbstractReferenceMap.WEAK,
        AbstractReferenceMap.WEAK, true);
    Map<Integer, Integer> reverseMap = new ReferenceMap(
        AbstractReferenceMap.WEAK, AbstractReferenceMap.WEAK, true);
    Integer foo = new Integer(32);
    bar = new Integer(42);
    simpleMap.put(foo, bar);
    reverseMap.put(bar, foo);
    Runtime.getRuntime().gc();
    assertEquals(1, simpleMap.size());
    assertEquals(1, reverseMap.size());
    bar = null;
    Runtime.getRuntime().gc();
    assertEquals(0, simpleMap.size());
    assertEquals(0, reverseMap.size());
  }

  public void testAssignable() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_Assignable);
    resources.add(CU_OuterInner);
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

  public void testAssimilation() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_BeforeAssimilate);
    compileAndRefresh();
    assertEquals(2, typeOracle.getTypes().length);

    // Build onto an existing type oracle.
    resources.add(CU_AfterAssimilate);
    compileAndRefresh();
    assertEquals(3, typeOracle.getTypes().length);
  }

  public void testBindToTypeScope() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_BindToTypeScope);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(4, types.length);
  }

  public void testConstructors() throws TypeOracleException {
    resources.add(CU_Object);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(1, types.length);
    JClassType objectType = types[0];
    assertEquals("Object", objectType.getSimpleSourceName());
    JConstructor[] ctors = objectType.getConstructors();
    assertEquals(1, ctors.length);
    JConstructor defaultCtor = ctors[0];
    assertEquals("Object", defaultCtor.getName());
    assertEquals(0, defaultCtor.getParameters().length);
  }

  public void testConstrainedList() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_Throwable);
    resources.add(CU_ConstrainedList);

    compileAndRefresh();

    JClassType type = typeOracle.getType("test.ConstrainedList");
    JClassType throwable = typeOracle.getType("java.lang.Throwable");

    assertNull(type.isParameterized());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);
    JTypeParameter[] typeParams = genericType.getTypeParameters();
    assertEquals(1, typeParams.length);
    assertEquals(throwable, typeParams[0].getBaseType());
    assertEquals(type, typeParams[0].getDeclaringClass());
    JClassType[] bounds = typeParams[0].getBounds();
    assertEquals(1, bounds.length);
    assertEquals(throwable, bounds[0]);
  }

  public void testConstrainedField() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_Throwable);
    resources.add(CU_ConstrainedList);
    resources.add(CU_ConstrainedListAsField);

    compileAndRefresh();

    // Get the types produced by the TypeOracle
    JClassType type = typeOracle.getType("test.ConstrainedListAsField");

    assertNull(type.isParameterized());
    JField field = type.getField("field");
    assertNotNull(field);
    JType fieldType = field.getType();
    JParameterizedType fieldParamType = fieldType.isParameterized();
    assertNotNull(fieldParamType);
    assertNull(fieldParamType.getEnclosingType());
    JGenericType baseType = fieldParamType.getBaseType();
    assertNotNull(baseType);
    assertEquals("test.ConstrainedList", baseType.getQualifiedSourceName());
    JClassType[] typeArgs = fieldParamType.getTypeArgs();
    assertEquals(1, typeArgs.length);
    JWildcardType wildcard = typeArgs[0].isWildcard();
    assertNotNull(wildcard);
    JClassType upperBound = wildcard.getUpperBound();
    assertEquals("Throwable", upperBound.getSimpleSourceName());
  }

  public void testDefaultClass() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_DefaultClass);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(2, types.length);
  }

  public void testEnclosingGenericType() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_List);
    resources.add(CU_GenericOuterInner);
    resources.add(CU_ExtendsGenericOuterInner);

    compileAndRefresh();

    // Get the types produced by the TypeOracle
    JClassType outer = typeOracle.getType("test.Outer");
    JClassType inner = typeOracle.getType("test.Outer.Inner");

    assertNull(outer.getEnclosingType());
    assertEquals(outer, inner.getEnclosingType());
    assertNull(inner.isParameterized());
    assertNotNull(outer.isGenericType());
    assertNotNull(inner.isGenericType());
    JField field = inner.getField("field");
    assertNotNull(field);
    JType fieldType = field.getType();
    JTypeParameter typeParam = fieldType.isTypeParameter();
    assertNotNull(typeParam);
    assertEquals("V", typeParam.getName());
    JClassType[] bounds = typeParam.getBounds();
    assertEquals(1, bounds.length);
    assertEquals(typeOracle.getJavaLangObject(), bounds[0]);

    JClassType extendsOuter = typeOracle.getType("test.ExtendsOuter");
    JClassType extendsInner = typeOracle.getType("test.ExtendsOuter.ExtendsInner");
    assertNull(extendsOuter.getEnclosingType());
    assertEquals(extendsOuter, extendsInner.getEnclosingType());
    JClassType outerSuper = extendsOuter.getSuperclass();
    JParameterizedType outerSuperParam = outerSuper.isParameterized();
    assertNotNull(outerSuperParam);
    assertEquals(outer, outerSuperParam.getBaseType());
    JClassType innerSuper = extendsInner.getSuperclass();
    JParameterizedType innerSuperParam = innerSuper.isParameterized();
    assertNotNull(innerSuperParam);
    assertEquals(inner, innerSuperParam.getBaseType());
  }

  public void testEnclosingType() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_List);
    resources.add(CU_ListAsField);

    compileAndRefresh();

    // Get the types produced by the TypeOracle
    JClassType listAsField = typeOracle.getType("test.refresh.ListAsField");

    assertNull(listAsField.isParameterized());
    JField field = listAsField.getField("field");
    assertNotNull(field);
    JType fieldType = field.getType();
    JParameterizedType fieldParamType = fieldType.isParameterized();
    assertNotNull(fieldParamType);
    assertNull(fieldParamType.getEnclosingType());
    JGenericType baseType = fieldParamType.getBaseType();
    assertNotNull(baseType);
    assertEquals("java.util.List", baseType.getQualifiedSourceName());
  }

  public void testFieldsAndTypes() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_FieldsAndTypes);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  // Check that anonymous classes are not reflected in TypeOracle
  public void testLocal() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_LocalClass);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(2, types.length);
  }

  public void testMethodsAndParams() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_Throwable);
    resources.add(CU_MethodsAndParams);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
  }

  public void testOuterInner() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_OuterInner);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(3, types.length);
    JClassType outer = null;
    for (JClassType type : types) {
      if ("Outer".equals(type.getSimpleSourceName())) {
        outer = type;
        break;
      }
    }
    assertNotNull(outer);
    JClassType superclass = outer.getSuperclass();
    assertEquals(typeOracle.getJavaLangObject(), superclass);
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
      throws TypeOracleException {
    resources.add(CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed);
    resources.add(CU_ExtendsParameterizedType);
    resources.add(CU_DeclaresInnerGenericType);
    resources.add(CU_Object);

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
  public void testRefresh() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_ExtendsGenericList);
    resources.add(CU_GenericList);
    resources.add(CU_ReferencesGenericListConstant);

    compileAndRefresh();

    // Get the types produced by the TypeOracle
    JClassType extendsGenericListType = typeOracle.getType("test.refresh.ExtendsGenericList");
    JClassType genericListType = typeOracle.getType("test.refresh.GenericList");
    JClassType referencesGenericListConstantType = typeOracle.getType("test.refresh.ReferencesGenericListConstant");

    /*
     * Invalidate CU_GenericList and simulate a refresh. This should cause
     * anything that depends on GenericList to be rebuilt by the type oracle.
     */
    CU_GenericList.touch();
    compileAndRefresh();

    assertNotSame(genericListType.getQualifiedSourceName() + "; ",
        typeOracle.getType("test.refresh.GenericList"), genericListType);
    assertNotSame(extendsGenericListType.getQualifiedSourceName() + "; ",
        typeOracle.getType("test.refresh.ExtendsGenericList"),
        extendsGenericListType);

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
  public void testRefreshWithErrors() throws TypeOracleException {
    // Add Object
    StringBuffer sb = new StringBuffer();
    sb.append("package java.lang;");
    sb.append("public class Object { }");
    addResource("java.lang.Object", sb);

    // Add UnmodifiedClass that will never change.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;");
    sb.append("public class UnmodifiedClass { }");
    addResource("test.refresh.with.errors.UnmodifiedClass", sb);

    // Add GoodClass that references a class that will go bad.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;\n");
    sb.append("public class GoodClass {\n");
    sb.append("  ClassThatWillGoBad ctwgb;\n");
    sb.append("}\n");
    addResource("test.refresh.with.errors.GoodClass", sb);

    // Add ClassThatWillGoBad that goes bad on the next refresh.
    MutableJavaResource unitThatWillGoBad = new MutableJavaResource(
        "test.refresh.with.errors.ClassThatWillGoBad") {
      private String source = "package test.refresh.with.errors;\n"
          + "public class ClassThatWillGoBad { }\n";

      @Override
      public String getSource() {
        return source;
      }

      @Override
      public void touch() {
        super.touch();
        source = "This will cause a syntax error.";
      }
    };
    resources.add(unitThatWillGoBad);

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
    addResource("test.refresh.with.errors.AnotherGoodClass", sb);

    // Add BadClass that has errors and originally
    // forced issue 2238.
    sb = new StringBuffer();
    sb.append("package test.refresh.with.errors;\n");
    sb.append("public class BadClass {\n");
    sb.append("  This will trigger a syntax error.\n");
    sb.append("}\n");
    addResource("test.refresh.with.errors.BadClass", sb);

    // Now this cup should cause errors.
    unitThatWillGoBad.touch();

    compileAndRefresh();

    assertNotNull(typeOracle.findType("test.refresh.with.errors.UnmodifiedClass"));
    assertNotNull(typeOracle.findType("test.refresh.with.errors.AnotherGoodClass"));
    assertNull(typeOracle.findType("test.refresh.with.errors.BadClass"));
    assertNull(typeOracle.findType("test.refresh.with.errors.ClassThatWillGoBad"));
    assertNull(typeOracle.findType("test.refresh.with.errors.GoodClass"));
  }

  public void testSyntaxErrors() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_HasSyntaxErrors);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
  }

  public void testTypeParams() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_NestedGenericInterfaces);
    resources.add(CU_UnnestedImplementations);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(6, types.length);
    JClassType type = typeOracle.findType("test.Implementations.InnerImpl");
    assertNotNull(type);
    JClassType[] interfaces = type.getImplementedInterfaces();
    assertEquals(1, interfaces.length);
    JClassType intf = interfaces[0];
    JParameterizedType intfParam = intf.isParameterized();
    assertNotNull(intfParam);
    JClassType intfEnclosing = intf.getEnclosingType();
    assertNotNull(intfEnclosing.isRawType());
  }

  public void testUnresolvedSymbls() throws TypeOracleException {
    resources.add(CU_Object);
    resources.add(CU_HasUnresolvedSymbols);
    resources.add(CU_RefsInfectedCompilationUnit);
    compileAndRefresh();
    JClassType[] types = typeOracle.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
  }

  /**
   * Creates a {@link Resource} and adds it the set of resources.
   * 
   * @throws UnableToCompleteException
   */
  private void addResource(String qualifiedTypeName, CharSequence source) {
    resources.add(new StaticJavaResource(qualifiedTypeName, source));
  }

  private void check(JClassType classInfo) throws NotFoundException {
    final String qName = classInfo.getQualifiedSourceName();
    CheckedJavaResource cup = publicTypeNameToTestCupMap.get(qName);
    if (cup != null) {
      cup.check(classInfo);
    }
  }

  private void compileAndRefresh() throws TypeOracleException {
    typeOracle = TypeOracleTestingUtils.buildTypeOracle(createTreeLogger(),
        resources);
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

  private void register(String qualifiedTypeName, CheckedJavaResource cup) {
    publicTypeNameToTestCupMap.put(qualifiedTypeName, cup);
  }
}
