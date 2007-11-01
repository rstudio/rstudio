/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.typeinfo.test;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.jdt.TypeOracleBuilder;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TypeOracleBuilderTest extends TestCase {

  private static abstract class TestCup implements CompilationUnitProvider {
    private final String packageName;

    private final String[] typeNames;

    public TestCup(String packageName, String onlyTypeName) {
      this(packageName, new String[] {onlyTypeName});
    }

    public TestCup(String packageName, String[] typeNames) {
      this.packageName = packageName;
      this.typeNames = typeNames;
      for (int i = 0; i < typeNames.length; i++) {
        String typeName = typeNames[i];
        register(typeName, this);
      }
    }

    public abstract void check(JClassType type) throws NotFoundException;

    public long getLastModified() throws UnableToCompleteException {
      return 0;
    }

    public String getLocation() {
      return "transient source for " + this.packageName + "."
          + this.typeNames[0];
    }

    public String getMainTypeName() {
      return null;
    }

    public String getPackageName() {
      return packageName;
    }

    public String[] getTypeNames() {
      return typeNames;
    }
    public boolean isTransient() {
      return true;
    }
  }

  private static Map<String, TestCup> publicTypeNameToTestCupMap = new HashMap<String, TestCup>();

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

  private static void check(JClassType classInfo) throws NotFoundException {
    final String qName = classInfo.getQualifiedSourceName();
    TestCup cup = publicTypeNameToTestCupMap.get(qName);
    assertNotNull(cup); // should've been declared during TestCup ctor
    cup.check(classInfo);
  }

  private static void register(String simpleTypeName, TestCup cup) {
    String qName = cup.getPackageName() + "." + simpleTypeName;
    publicTypeNameToTestCupMap.put(qName, cup);
  }

  protected TestCup CU_AfterAssimilate = new TestCup("test.assim",
      new String[] {"AfterAssimilate"}) {
    public void check(JClassType type) {
      // Don't need to check the type itself.
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class AfterAssimilate extends BeforeAssimilate { }");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_Assignable = new TestCup("test.sub", new String[] {
      "BaseInterface", "DerivedInterface", "Derived", "Derived.Nested"}) {
    public void check(JClassType type) {
      if ("Derived".equals(type.getSimpleSourceName()))
        checkDerived(type);
      else if ("Nested".equals(type.getSimpleSourceName()))
        checkNested(type);
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.sub;\n");
      sb.append("import test.Outer;");
      sb.append("interface BaseInterface { }");
      sb.append("interface DerivedInterface extends BaseInterface { }");
      sb.append("public class Derived extends Outer.Inner {\n");
      sb.append("   public static class Nested extends Outer.Inner implements DerivedInterface { }\n");
      sb.append("}\n");
      return sb.toString().toCharArray();
    }

    private void checkDerived(JClassType type) {
      assertEquals("test.sub.Derived", type.getQualifiedSourceName());
    }

    private void checkNested(JClassType type) {
      assertEquals("test.sub.Derived.Nested", type.getQualifiedSourceName());

    }
  };

  protected TestCup CU_BeforeAssimilate = new TestCup("test.assim",
      new String[] {"BeforeAssimilate"}) {
    public void check(JClassType type) {
      // Don't need to check the type itself.
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.assim;\n");
      sb.append("class BeforeAssimilate { }");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_BindToTypeScope = new TestCup("test", new String[] {
      "BindToTypeScope", "BindToTypeScope.Object",
      "BindToTypeScope.DerivedObject"}) {

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

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class BindToTypeScope {\n");
      sb.append("   public static class Object { }\n");
      sb.append("   public static class DerivedObject extends Object { }\n");
      sb.append("}\n");
      return sb.toString().toCharArray();
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

  protected TestCup CU_DefaultClass = new TestCup("test", "DefaultClass") {
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

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class DefaultClass extends Object { }\n");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_FieldsAndTypes = new TestCup("test", new String[] {
      "Fields", "SomeType"}) {
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
        // No need to check SomeType since there's already a DefaultClass
        // test.
      }
    }

    public char[] getSource() {
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
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_HasSyntaxErrors = new TestCup("test", new String[] {
      "HasSyntaxErrors", "NoSyntaxErrors"}) {
    public void check(JClassType classInfo) {
      fail("This class should have been removed");
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class NoSyntaxErrors { }\n");
      sb.append("public class HasSyntaxErrors { a syntax error }\n");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_HasUnresolvedSymbols = new TestCup("test", new String[] {
      "Invalid", "Valid"}) {
    public void check(JClassType classInfo) {
      fail("Both classes should have been removed");
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Invalid extends NoSuchClass { }\n");
      sb.append("public class Valid extends Object { }\n");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_MetaData = new TestCup("test", "MetaData") {

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

    public char[] getSource() {
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
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_MethodsAndParams = new TestCup("test", "Methods") {

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

    public char[] getSource() {
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
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_Object = new TestCup("java.lang", "Object") {
    public void check(JClassType type) {
      assertEquals("Object", type.getSimpleSourceName());
      assertEquals("java.lang.Object", type.getQualifiedSourceName());
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Object { }");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_OuterInner = new TestCup("test", new String[] {
      "Outer", "Outer.Inner"}) {

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

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class Outer {\n");
      sb.append("   public static class Inner { }\n");
      sb.append("}\n");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_RefsInfectedCompilationUnit = new TestCup("test",
      new String[] {"RefsInfectedCompilationUnit"}) {
    public void check(JClassType classInfo) {
      fail("This class should should have been removed because it refers to a class in another compilation unit that had problems");
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class RefsInfectedCompilationUnit extends Valid { }\n");
      return sb.toString().toCharArray();
    }
  };

  protected TestCup CU_Throwable = new TestCup("java.lang", "Throwable") {
    public void check(JClassType type) {
      assertEquals("Throwable", type.getSimpleSourceName());
      assertEquals("java.lang.Throwable", type.getQualifiedSourceName());
    }

    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class Throwable { }");
      return sb.toString().toCharArray();
    }
  };

  public void checkTypes(JClassType[] types) throws NotFoundException {
    for (int i = 0; i < types.length; i++) {
      JClassType type = types[i];
      check(type);

      JClassType[] nestedTypes = type.getNestedTypes();
      checkTypes(nestedTypes);
    }
  }

  public void testAssimilation() throws UnableToCompleteException {
    TypeOracle typeOracle0 = new TypeOracle();
    TreeLogger logger = createTreeLogger();

    // Build onto an empty type oracle.
    //
    TypeOracleBuilder builder1 = new TypeOracleBuilder(typeOracle0);
    builder1.addCompilationUnit(CU_Object);
    builder1.addCompilationUnit(CU_BeforeAssimilate);
    TypeOracle typeOracle1 = builder1.build(logger);
    assertSame(typeOracle0, typeOracle1);
    assertEquals(2, typeOracle1.getTypes().length);
    JClassType before = typeOracle1.findType("test.assim.BeforeAssimilate");

    // Build onto an existing type oracle.
    //
    TypeOracleBuilder builder2 = new TypeOracleBuilder(typeOracle1);
    builder2.addCompilationUnit(CU_AfterAssimilate);
    TypeOracle typeOracle2 = builder2.build(logger);
    assertSame(typeOracle1, typeOracle2);
    assertEquals(3, typeOracle2.getTypes().length);

    // Make sure identities remained intact across the assimilation.
    //
    JClassType after = typeOracle2.findType("test.assim.AfterAssimilate");

    assertSame(before, after.getSuperclass());
  }

  public void testBindToTypeScope() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_BindToTypeScope);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(4, types.length);
    checkTypes(types);
  }

  public void testDefaultClass() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_DefaultClass);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(2, types.length);
    checkTypes(types);
  }

  public void testFieldsAndTypes() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_FieldsAndTypes);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(3, types.length);
    checkTypes(types);
  }

  public void testMetaData() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_MetaData);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(2, types.length);
    checkTypes(types);
  }

  public void testMethodsAndParams() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_Throwable);
    tiob.addCompilationUnit(CU_MethodsAndParams);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(3, types.length);
    checkTypes(types);
  }

  public void testOuterInner() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_OuterInner);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    assertEquals(3, types.length);
    checkTypes(types);
  }

  public void testSyntaxErrors() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_HasSyntaxErrors);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
    checkTypes(types);
  }

  public void testUnresolvedSymbls() throws TypeOracleException,
      UnableToCompleteException {
    TypeOracleBuilder tiob = createTypeInfoOracleBuilder();
    tiob.addCompilationUnit(CU_Object);
    tiob.addCompilationUnit(CU_HasUnresolvedSymbols);
    tiob.addCompilationUnit(CU_RefsInfectedCompilationUnit);
    TypeOracle tio = tiob.build(createTreeLogger());
    JClassType[] types = tio.getTypes();
    // Only java.lang.Object should remain.
    //
    assertEquals(1, types.length);
    assertEquals("java.lang.Object", types[0].getQualifiedSourceName());
    checkTypes(types);
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

  private TypeOracleBuilder createTypeInfoOracleBuilder() {
    return new TypeOracleBuilder();
  }
}
