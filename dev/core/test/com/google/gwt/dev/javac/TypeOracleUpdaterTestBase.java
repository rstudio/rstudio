/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.javac.CompilationUnitTypeOracleUpdater.TypeData;
import com.google.gwt.dev.javac.mediatortest.AfterAssimilate;
import com.google.gwt.dev.javac.mediatortest.BaseInterface;
import com.google.gwt.dev.javac.mediatortest.BeforeAssimilate;
import com.google.gwt.dev.javac.mediatortest.BindToTypeScope;
import com.google.gwt.dev.javac.mediatortest.ConstrainedList;
import com.google.gwt.dev.javac.mediatortest.ConstrainedListAsField;
import com.google.gwt.dev.javac.mediatortest.DeclaresGenericInnerInterface;
import com.google.gwt.dev.javac.mediatortest.DeclaresGenericInnerType;
import com.google.gwt.dev.javac.mediatortest.DefaultClass;
import com.google.gwt.dev.javac.mediatortest.Derived;
import com.google.gwt.dev.javac.mediatortest.DerivedInterface;
import com.google.gwt.dev.javac.mediatortest.EnclosingLocal;
import com.google.gwt.dev.javac.mediatortest.EnclosingLocalWithMember;
import com.google.gwt.dev.javac.mediatortest.ExtendsGenericList;
import com.google.gwt.dev.javac.mediatortest.ExtendsGenericOuter;
import com.google.gwt.dev.javac.mediatortest.ExtendsParameterizedInterface;
import com.google.gwt.dev.javac.mediatortest.Fields;
import com.google.gwt.dev.javac.mediatortest.GenericList;
import com.google.gwt.dev.javac.mediatortest.GenericOuter;
import com.google.gwt.dev.javac.mediatortest.Implementations;
import com.google.gwt.dev.javac.mediatortest.ListAsField;
import com.google.gwt.dev.javac.mediatortest.Methods;
import com.google.gwt.dev.javac.mediatortest.Outer;
import com.google.gwt.dev.javac.mediatortest.OuterInt;
import com.google.gwt.dev.javac.mediatortest.ReferencesGenericListConstant;
import com.google.gwt.dev.javac.mediatortest.ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.guava.common.collect.MapMaker;
import com.google.gwt.thirdparty.guava.common.io.BaseEncoding;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test TypeOracleUpdater.
 *
 * NOTE: These tests require the test source code to be on the classpath. In
 * Eclipse, make sure your launch configuration includes the 'core/test'
 * directory on the classpath tab.
 */
public abstract class TypeOracleUpdaterTestBase extends TestCase {

  /**
   * A Java resource that provides a check() method.
   */
  protected static abstract class CheckedJavaResource extends MutableJavaResource {
    private final String[] shortTypeNames;

    public CheckedJavaResource(Class<?> clazz, String... shortTypeNames) {
      super(clazz);
      this.shortTypeNames = shortTypeNames;
    }

    public CheckedJavaResource(String packageName, String shortMainTypeName,
        String... shortTypeNames) {
      super(packageName, Shared.makeTypeName(packageName, shortMainTypeName));
      this.shortTypeNames = shortTypeNames;
    }

    public abstract void check(JClassType type) throws NotFoundException;

    public List<String> getTypeNames() {
      List<String> typeNames = new ArrayList<String>();
      typeNames.add(getTypeName());
      for (String shortTypeName : shortTypeNames) {
        String typeName = Shared.makeTypeName(getPackageName(), shortTypeName);
        typeNames.add(typeName);
      }
      return typeNames;
    }
  }

  /**
   * A mutable Java resource.
   */
  protected static abstract class MutableJavaResource extends MockJavaResource {
    private static byte[] getByteCode(Class<?> aClass) {
      String resourcePath = aClass.getName().replace(".", "/") + ".class";
      ClassLoader loader = aClass.getClassLoader();
      if (loader == null && aClass.getName().startsWith("java.")) {
        loader = Thread.currentThread().getContextClassLoader();
      }
      InputStream istream = loader.getResourceAsStream(resourcePath);
      assertNotNull(istream);
      return Util.readStreamAsBytes(istream);
    }

    // For building the type oracle from bytecode
    private final Class<?> clazz;
    private String extraSource = "";
    private final String packageName;

    public MutableJavaResource(Class<?> clazz) {
      super(clazz.getName());
      this.clazz = clazz;
      this.packageName = clazz.getPackage().getName();
    }

    public MutableJavaResource(String packageName, String qualifiedTypeName) {
      super(qualifiedTypeName);
      this.clazz = null;
      this.packageName = packageName;
    }

    public String getPackageName() {
      return packageName;
    }

    /**
     * This method is used to pull sample source from inside the test case. By
     * default, return <code>null</code> to indicate that source should be on
     * the classpath.
     */
    public String getSource() {
      return null;
    }

    /**
     * Pulls Java source from files in the mediatortest package. If source files
     * are on the classpath, prefer this data.
     */
    public String getSourceFromClasspath() {
      if (clazz == null) {
        return null;
      }
      assertFalse(clazz.getName().startsWith("java."));
      ClassLoader loader = clazz.getClassLoader();
      String resourcePath = clazz.getName().replace(".", "/") + ".java";
      InputStream istream = loader.getResourceAsStream(resourcePath);
      if (istream == null) {
        fail("Could not read " + resourcePath + " from classloader.");
      }
      return Util.readStreamAsString(istream);
    }

    public TypeData[] getTypeData() throws IOException {
      return getTypeData(clazz);
    }

    @Override
    public void touch() {
      super.touch();
      extraSource += '\n';
    }

    /**
     * Looks for the source data on the classpath.
     */
    @Override
    public CharSequence getContent() {
      String source = getSource();
      if (source == null) {
        source = getSourceFromClasspath();
        assertNotNull("Make sure your runtime environment includes the source "
            + "for the testcases on the classpath if this assertion fails", source);
      }
      return source + extraSource;
    }

    private TypeData[] getTypeData(Class<?> aClass) throws IOException {
      List<TypeData> results = new ArrayList<TypeData>();
      String packageName = Shared.getPackageName(aClass.getName());
      TypeData newData =
          new TypeData(packageName, aClass.getSimpleName(), aClass.getName().replace(".", "/"),
              getByteCode(aClass), System.currentTimeMillis());
      results.add(newData);
      Class<?>[] subclasses = aClass.getDeclaredClasses();
      for (Class<?> subclass : subclasses) {
        for (TypeData result : getTypeData(subclass)) {
          results.add(result);
        }
      }
      return results.toArray(new TypeData[results.size()]);
    }
  }

  protected static final CheckedJavaResource CU_AfterAssimilate = new CheckedJavaResource(
      AfterAssimilate.class) {
    @Override
    public void check(JClassType type) {
      assertNotNull(type);
      assertEquals("AfterAssimilate", type.getSimpleSourceName());
      assertNotNull(type.getSuperclass());
      assertEquals(getPackageName() + ".BeforeAssimilate", type.getSuperclass()
          .getQualifiedSourceName());
    }
  };

  protected static final CheckedJavaResource CU_BaseInterface = new CheckedJavaResource(
      BaseInterface.class) {

    @Override
    public void check(JClassType type) {
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      assertNotNull(type.isInterface());
    }
  };

  protected static final CheckedJavaResource CU_BeforeAssimilate = new CheckedJavaResource(
      BeforeAssimilate.class) {
    @Override
    public void check(JClassType type) {
      assertEquals(getTypeName(), type.getQualifiedSourceName());
    }
  };

  protected static final CheckedJavaResource CU_BindToTypeScope = new CheckedJavaResource(
      BindToTypeScope.class, "BindToTypeScope.Object", "BindToTypeScope.DerivedObject") {

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
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      JClassType object = type.getNestedType("Object");
      assertNotNull(object);
      JClassType derivedObject = type.getNestedType("DerivedObject");
      assertNotNull(derivedObject);
    }

    public void checkObject(JClassType type) {
      assertEquals("Object", type.getSimpleSourceName());
      assertEquals(getTypeName() + ".Object", type.getQualifiedSourceName());
    }

    private void checkDerivedObject(JClassType type) throws NotFoundException {
      JClassType bindToTypeScope = type.getEnclosingType();
      assertNotNull(bindToTypeScope);

      JClassType object = bindToTypeScope.getNestedType("Object");
      assertNotNull(object);
      assertEquals(object.getSimpleSourceName(), "Object");

      JClassType derivedObject = bindToTypeScope.getNestedType("DerivedObject");
      assertNotNull(derivedObject);
      assertEquals(derivedObject.getSimpleSourceName(), "DerivedObject");

      assertEquals(object, derivedObject.getSuperclass());
    }
  };

  protected static final CheckedJavaResource CU_ConstrainedList = new CheckedJavaResource(
      ConstrainedList.class) {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }
  };

  protected static final CheckedJavaResource CU_ConstrainedListAsField = new CheckedJavaResource(
      ConstrainedListAsField.class) {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNull(type.isGenericType());
      assertNull(type.getEnclosingType());
    }
  };

  protected static final CheckedJavaResource CU_DeclaresInnerGenericInterface =
      new CheckedJavaResource(DeclaresGenericInnerInterface.class,
          "DeclaresGenericInnerInterface.Inner") {
        @Override
        public void check(JClassType type) throws NotFoundException {
          assertNotNull(type.isGenericType());
        }
      };

  protected static final CheckedJavaResource CU_DeclaresInnerGenericType = new CheckedJavaResource(
      DeclaresGenericInnerType.class, "DeclaresGenericInnerType.Inner") {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }
  };

  protected static final CheckedJavaResource CU_DefaultClass = new CheckedJavaResource(
      DefaultClass.class) {
    @Override
    public void check(JClassType type) {
      assertEquals("DefaultClass", type.getSimpleSourceName());
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      JClassType object = type.getOracle().findType("java.lang", "Object");
      assertNotNull(object);

      assertEquals(object, type.getSuperclass());
      assertNull(type.isInterface());
      assertEquals(0, type.getMethods().length);
      assertEquals(0, type.getFields().length);
    }
  };

  protected static final CheckedJavaResource CU_Derived = new CheckedJavaResource(Derived.class,
      "Derived.Nested") {
    @Override
    public void check(JClassType type) {
      if ("Derived".equals(type.getSimpleSourceName())) {
        checkDerived(type);
      } else if ("Nested".equals(type.getSimpleSourceName())) {
        checkNested(type);
      } else {
        assert (false);
      }
    }

    private void checkDerived(JClassType type) {
      assertEquals(getTypeName(), type.getQualifiedSourceName());
    }

    private void checkNested(JClassType type) {
      assertEquals(getTypeName() + ".Nested", type.getQualifiedSourceName());
    }
  };

  protected static final CheckedJavaResource CU_DerivedInterface = new CheckedJavaResource(
      DerivedInterface.class) {

    @Override
    public void check(JClassType type) {
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      assertNotNull(type.isInterface());
    }
  };

  protected static final CheckedJavaResource CU_EnclosingLocalClass = new CheckedJavaResource(
      EnclosingLocal.class) {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      assertEquals("EnclosingLocal", name);
      checkEnclosing(type);
    }

    public void checkEnclosing(JClassType type) {
      assertEquals("EnclosingLocal", type.getSimpleSourceName());
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      // verify the local class doesn't show up
      JClassType[] nested = type.getNestedTypes();
      assertEquals(0, nested.length);
    }
  };

  protected static final CheckedJavaResource CU_EnclosingLocalWithMember = new CheckedJavaResource(
      EnclosingLocalWithMember.class) {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      assertEquals("EnclosingLocalWithMember", name);
      checkEnclosing(type);
    }

    public void checkEnclosing(JClassType type) {
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      // verify the local class doesn't show up
      JClassType[] nested = type.getNestedTypes();
      assertEquals(0, nested.length);
    }
  };

  protected static final CheckedJavaResource CU_ExtendsGenericList = new CheckedJavaResource(
      ExtendsGenericList.class) {

    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.getSuperclass().isParameterized());
    }
  };

  protected static final CheckedJavaResource CU_ExtendsGenericOuterInner = new CheckedJavaResource(
      ExtendsGenericOuter.class, "ExtendsGenericOuter.ExtendsInner") {

    @Override
    public void check(JClassType type) {
      final String name = type.getSimpleSourceName();
      if ("ExtendsGenericOuter".equals(name)) {
        checkOuter(type);
      } else {
        checkInner(type);
      }
    }

    public void checkInner(JClassType type) {
      assertEquals("ExtendsInner", type.getSimpleSourceName());
      assertEquals(getTypeName() + ".ExtendsInner", type.getQualifiedSourceName());
      assertEquals(getTypeName(), type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("ExtendsGenericOuter", type.getSimpleSourceName());
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals(getTypeName() + ".ExtendsInner", inner.getQualifiedSourceName());
    }
  };

  protected static final CheckedJavaResource CU_ExtendsParameterizedInterface =
      new CheckedJavaResource(ExtendsParameterizedInterface.class) {
        @Override
        public void check(JClassType type) throws NotFoundException {
          assertNotNull(type.getSuperclass().isParameterized());
        }
      };

  protected static final CheckedJavaResource CU_FieldsAndTypes = new CheckedJavaResource(
      Fields.class) {
    @Override
    public void check(JClassType type) throws NotFoundException {
      if ("Fields".equals(type.getSimpleSourceName())) {
        assertEquals(getTypeName(), type.getQualifiedSourceName());

        TypeOracle tio = type.getOracle();

        JField[] fields = type.getFields();
        assertEquals(12, fields.length);

        JField field;
        JType fieldType;
        JArrayType arrayType;
        JType componentType;
        final JClassType someType = tio.getType(getPackageName(), "DefaultClass");
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
        assertEquals(getPackageName() + ".DefaultClass[]", fieldType.getQualifiedSourceName());

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
  };

  protected static final CheckedJavaResource CU_GenericList = new CheckedJavaResource(
      GenericList.class) {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
    }
  };

  protected static final CheckedJavaResource CU_GenericOuterInner = new CheckedJavaResource(
      GenericOuter.class, "GenericOuter.Inner") {

    @Override
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
      assertEquals(getTypeName() + ".Inner", type.getQualifiedSourceName());
      assertEquals(getTypeName(), type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("GenericOuter", type.getSimpleSourceName());
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals(getTypeName() + ".Inner", inner.getQualifiedSourceName());
    }
  };

  protected static final CheckedJavaResource CU_List = new CheckedJavaResource(List.class) {

    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNotNull(type.isGenericType());
      assertNotNull(type.isInterface());
      // TODO(zundel): This is a bug when building from source: interfaces
      // should not be default instantiable
      // assertNull(type.isDefaultInstantiable());
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

  protected static final CheckedJavaResource CU_Object = new CheckedJavaResource(Object.class) {
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

  protected static final CheckedJavaResource CU_OuterInner = new CheckedJavaResource(Outer.class,
      "Outer.Inner") {

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
      assertEquals(CU_OuterInner.getTypeName() + ".Inner", type.getQualifiedSourceName());
      assertEquals(CU_OuterInner.getTypeName(), type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("Outer", type.getSimpleSourceName());
      assertEquals(CU_OuterInner.getTypeName(), type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals(CU_OuterInner.getTypeName() + ".Inner", inner.getQualifiedSourceName());
    }
  };

  /**
   * Not sourced off disk like the others since some build systems refuse files with $ in the name.
   */
  protected static final MutableJavaResource CU_PseudoInner = new MutableJavaResource(
      "com.google.gwt.dev.javac.mediatortest",
      "com.google.gwt.dev.javac.mediatortest.Pseudo$Inner") {

        @Override
        public CharSequence getContent() {
          StringBuffer code = new StringBuffer();
          code.append("package com.google.gwt.dev.javac.mediatortest;\n");
          code.append("public class Pseudo$Inner {}\n");
          return code;
        }

        @Override
        public TypeData[] getTypeData() {
          byte[] classBytes = BaseEncoding.base16().decode(
              "CAFEBABE000000330010070002010032636F6D2F676F6F676C652F6777742F6465762F6A617661632"
              + "F6D65646961746F72746573742F50736575646F24496E6E65720700040100106A6176612F6C616E672"
              + "F4F626A6563740100063C696E69743E010003282956010004436F64650A000300090C0005000601000"
              + "F4C696E654E756D6265725461626C650100124C6F63616C5661726961626C655461626C65010004746"
              + "869730100344C636F6D2F676F6F676C652F6777742F6465762F6A617661632F6D65646961746F72746"
              + "573742F50736575646F24496E6E65723B01000A536F7572636546696C6501001150736575646F24496"
              + "E6E65722E6A617661002100010003000000000001000100050006000100070000002F0001000100000"
              + "0052AB70008B100000002000A00000006000100000003000B0000000C000100000005000C000D00000"
              + "001000E00000002000F");
          TypeData classData = new TypeData("com.google.gwt.dev.javac.mediatortest", "Pseudo$Inner",
              "com/google/gwt/dev/javac/mediatortest/Pseudo$Inner", classBytes,
              System.currentTimeMillis());
          return new TypeData[] {classData};
        }
      };

  protected static final CheckedJavaResource CU_ReferencesGenericListConstant =
      new CheckedJavaResource(ReferencesGenericListConstant.class) {
        @Override
        public void check(JClassType type) throws NotFoundException {
          assertEquals(getTypeName(), type.getQualifiedSourceName());
        }
      };

  protected static final CheckedJavaResource CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed =
      new CheckedJavaResource(ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed.class) {

        @Override
        public void check(JClassType type) throws NotFoundException {

          JClassType[] intfs = type.getImplementedInterfaces();
          assertEquals(1, intfs.length);
          assertNotNull(intfs[0].isParameterized());
        }
      };

  protected static final CheckedJavaResource CU_String = new CheckedJavaResource(String.class) {
    @Override
    public void check(JClassType type) {
      assertEquals("String", type.getSimpleSourceName());
      assertEquals("java.lang.String", type.getQualifiedSourceName());
    }

    @Override
    public String getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package java.lang;");
      sb.append("public class String { }");
      return sb.toString();
    }
  };

  protected static final CheckedJavaResource CU_Throwable =
      new CheckedJavaResource(Throwable.class) {

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

  protected static final CheckedJavaResource CU_UnnestedImplementations = new CheckedJavaResource(
      Implementations.class, "Implementations.OuterImpl", "Implementations.InnerImpl") {
    @Override
    public void check(JClassType type) {
      if (type.getSimpleSourceName().equals("Implementations")) {
        assertEquals(getTypeName(), type.getQualifiedSourceName());
      }
    }
  };

  protected static void assertIsAssignable(JClassType from, JClassType to) {
    assertTrue("'" + from + "' should be assignable to '" + to + "'", from.isAssignableTo(to));
    assertTrue("'" + to + "' should be assignable from '" + from + "'", to.isAssignableFrom(from));
  }

  protected static void assertIsNotAssignable(JClassType from, JClassType to) {
    assertFalse(from + " should not be assignable to " + to, from.isAssignableTo(to));
    assertFalse(to + " should not be assignable to " + from, to.isAssignableFrom(from));
  }

  protected static void recordAssignability(Map<JClassType, Set<JClassType>> assignabilityMap,
      JClassType from, JClassType to) {
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
  public final Map<String, CheckedJavaResource> publicTypeNameToTestCupMap =
      new HashMap<String, CheckedJavaResource>();

  protected CheckedJavaResource CU_ListAsField = new CheckedJavaResource(ListAsField.class) {
    @Override
    public void check(JClassType type) throws NotFoundException {
      assertNull(type.isGenericType());
      assertNull(type.getEnclosingType());
    }
  };

  protected CheckedJavaResource CU_MethodsAndParams = new CheckedJavaResource(Methods.class) {

    @Override
    public void check(JClassType type) throws NotFoundException {
      TypeOracle tio = type.getOracle();
      JMethod[] methods = type.getMethods();
      assertEquals(6, methods.length);
      JMethod method;
      JType[] thrownTypes;
      final JClassType javaLangObject = tio.findType("java.lang", "Object");
      final JClassType javaLangThrowable = tio.findType("java.lang", "Throwable");
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

      method = type.getMethod("overloaded", new JType[]{JPrimitiveType.INT, javaLangObject});
      assertSame(JPrimitiveType.VOID, method.getReturnType());
      thrownTypes = method.getThrows();
      assertEquals(1, thrownTypes.length);
      assertSame(javaLangThrowable, thrownTypes[0]);

      method = type.getMethod("overloaded", new JType[]{JPrimitiveType.INT, JPrimitiveType.CHAR});
      assertSame(javaLangObject, method.getReturnType());
      thrownTypes = method.getThrows();
      assertEquals(0, thrownTypes.length);
    }
  };

  protected CheckedJavaResource CU_NestedGenericInterfaces = new CheckedJavaResource(
      OuterInt.class, "OuterInt.InnerInt") {

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
      assertEquals(getTypeName() + ".InnerInt", type.getQualifiedSourceName());
      assertEquals(getTypeName(), type.getEnclosingType().getQualifiedSourceName());
    }

    public void checkOuter(JClassType type) {
      assertEquals("OuterInt", type.getSimpleSourceName());
      assertEquals(getTypeName(), type.getQualifiedSourceName());
      JClassType[] nested = type.getNestedTypes();
      assertEquals(1, nested.length);
      JClassType inner = nested[0];
      assertEquals(getTypeName() + ".InnerInt", inner.getQualifiedSourceName());
    }
  };

  protected final Set<Resource> resources = new LinkedHashSet<Resource>();

  protected TypeOracle typeOracle;

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
  public void manualTestAbstractRefrenceMap() {

    /*
     * with a HARD -> WEAK map, verify that the entry remains if there is no
     * reference to key, but is deleted when the reference to value is gone
     */
    Map<Integer, Integer> simpleMap = new MapMaker().weakValues().makeMap();
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
    simpleMap = new MapMaker().weakKeys().weakValues().makeMap();
    Map<Integer, Integer> reverseMap = new MapMaker().weakKeys().weakValues().makeMap();
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

  @Override
  public void setUp() {
    resources.clear();
  }

  public void testAssignable() throws TypeOracleException {
    // The order of adding resources is important for testing from byte code
    addTestResource(CU_Object);
    addTestResource(CU_BaseInterface);
    addTestResource(CU_DerivedInterface);
    addTestResource(CU_OuterInner);
    addTestResource(CU_Derived);
    buildTypeOracle();

    Map<JClassType, Set<JClassType>> assignabilityMap = new HashMap<JClassType, Set<JClassType>>();
    JClassType obj = typeOracle.findType(CU_Object.getTypeName());
    assertNotNull(obj);

    JClassType inner = typeOracle.findType(CU_OuterInner.getTypeName() + ".Inner");
    assertNotNull(inner);

    JClassType derived = typeOracle.findType(CU_Derived.getTypeName());
    assertNotNull(derived);

    JClassType nested = typeOracle.findType(CU_Derived.getTypeName() + ".Nested");
    assertNotNull(nested);

    JClassType baseIntf = typeOracle.findType(CU_Derived.getPackageName() + ".BaseInterface");
    assertNotNull(baseIntf);

    JClassType derivedIntf = typeOracle.findType(CU_Derived.getPackageName() + ".DerivedInterface");
    assertNotNull(derivedIntf);

    recordAssignability(assignabilityMap, derivedIntf, baseIntf);
    recordAssignability(assignabilityMap, derived, inner);
    recordAssignability(assignabilityMap, nested, inner);
    recordAssignability(assignabilityMap, nested, derivedIntf);
    recordAssignability(assignabilityMap, nested, baseIntf);

    JClassType[] allTypes = typeOracle.getTypes();
    assertEquals(7, allTypes.length);

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
    addTestResource(CU_Object);
    addTestResource(CU_BeforeAssimilate);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_BeforeAssimilate.getTypeName()));
    assertEquals(2, typeOracle.getTypes().length);

    // Build onto an existing type oracle.
    addTestResource(CU_AfterAssimilate);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_BeforeAssimilate.getTypeName()));
    assertNotNull(typeOracle.findType(CU_AfterAssimilate.getTypeName()));
    assertEquals(3, typeOracle.getTypes().length);
  }

  public void testBindToTypeScope() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_BindToTypeScope);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType("com.google.gwt.dev.javac.mediatortest.BindToTypeScope"));
    assertNotNull(
        typeOracle.findType("com.google.gwt.dev.javac.mediatortest.BindToTypeScope.DerivedObject"));
    assertNotNull(
        typeOracle.findType("com.google.gwt.dev.javac.mediatortest.BindToTypeScope.Object"));
    assertEquals(4, typeOracle.getTypes().length);
  }

  public void testConstrainedField() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_Throwable);
    addTestResource(CU_ConstrainedList);
    addTestResource(CU_ConstrainedListAsField);
    buildTypeOracle();

    // Get the types produced by the TypeOracle
    JClassType type = typeOracle.getType(CU_ConstrainedListAsField.getTypeName());
    assertNull(type.isParameterized());
    JField[] fields = type.getFields();
    assert (fields.length == 1);
    JField field = type.getField("field");
    assertNotNull(field);
    JType fieldType = field.getType();
    JParameterizedType fieldParamType = fieldType.isParameterized();
    assertNotNull(fieldParamType);
    assertNull(fieldParamType.getEnclosingType());
    JGenericType baseType = fieldParamType.getBaseType();
    assertNotNull(baseType);
    assertEquals(CU_ConstrainedList.getTypeName(), baseType.getQualifiedSourceName());
    JClassType[] typeArgs = fieldParamType.getTypeArgs();
    assertEquals(1, typeArgs.length);
    JWildcardType wildcard = typeArgs[0].isWildcard();
    assertNotNull(wildcard);
    JClassType upperBound = wildcard.getUpperBound();
    assertEquals("Throwable", upperBound.getSimpleSourceName());
  }

  public void testConstrainedList() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_Throwable);
    addTestResource(CU_ConstrainedList);

    buildTypeOracle();

    JClassType type = typeOracle.getType(CU_ConstrainedList.getPackageName() + ".ConstrainedList");
    JClassType throwable = typeOracle.getType("java.lang.Throwable");
    assertNotNull(throwable);
    assertEquals("Throwable", throwable.getSimpleSourceName());

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

  public void testConstructors() throws TypeOracleException {
    addTestResource(CU_Object);
    buildTypeOracle();
    JClassType objectType = typeOracle.findType(CU_Object.getTypeName());
    assertNotNull(objectType);
    assertEquals("Object", objectType.getSimpleSourceName());
    JConstructor[] ctors = objectType.getConstructors();
    assertEquals(1, ctors.length);
    JConstructor defaultCtor = ctors[0];
    assertEquals("Object", defaultCtor.getName());
    assertEquals(0, defaultCtor.getParameters().length);
  }

  public void testDefaultClass() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_DefaultClass);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_DefaultClass.getTypeName()));
    assertEquals(2, typeOracle.getTypes().length);
  }

  public void testEnclosingGenericType() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_String);
    addTestResource(CU_List);
    addTestResource(CU_GenericOuterInner);
    addTestResource(CU_ExtendsGenericOuterInner);

    buildTypeOracle();

    // Get the types produced by the TypeOracle
    JClassType outer = typeOracle.getType(CU_GenericOuterInner.getPackageName() + ".GenericOuter");
    JClassType inner =
        typeOracle.getType(CU_GenericOuterInner.getPackageName() + ".GenericOuter.Inner");

    assertNull(outer.getEnclosingType());
    assertEquals(outer, inner.getEnclosingType());
    assertNull(inner.isParameterized());
    assertNotNull(outer.isGenericType());
    assertNotNull(inner.isGenericType());
    JField[] fields = inner.getFields();
    assertEquals(fields.length, 2);
    JField field = inner.getField("field");
    assertNotNull(field);
    JType fieldType = field.getType();
    JTypeParameter typeParam = fieldType.isTypeParameter();
    assertNotNull(typeParam);
    assertEquals("V", typeParam.getName());
    JClassType[] bounds = typeParam.getBounds();
    assertEquals(1, bounds.length);
    assertEquals(typeOracle.getJavaLangObject(), bounds[0]);

    JClassType extendsOuter =
        typeOracle.getType(CU_GenericOuterInner.getPackageName() + ".ExtendsGenericOuter");
    JClassType extendsInner =
        typeOracle.getType(CU_GenericOuterInner.getPackageName()
            + ".ExtendsGenericOuter.ExtendsInner");
    assertNull(extendsOuter.getEnclosingType());
    assertEquals(extendsOuter, extendsInner.getEnclosingType());

    JClassType outerSuper = extendsOuter.getSuperclass();
    JParameterizedType outerSuperParam = outerSuper.isParameterized();
    assertNotNull(outerSuperParam);
    assertEquals(outer, outerSuperParam.getBaseType());
    JClassType innerSuper = extendsInner.getSuperclass();
    assertEquals("GenericOuter.Inner", innerSuper.getName());
    field = inner.getField("field");
    assertNotNull(field);

    /*
     * This test fails for OpenJDK compiled classes compared to JDT classes. The
     * reason is that the superclass of this type doesn't contain a type
     * signature for OpenJDK byte code.
     *
     * Commenting out this code for the tests: I'm not sure any generators
     * depend on this subtle difference.
     */
    // assertEquals("java.lang.String",
    // field.getType().getQualifiedSourceName());
    // JParameterizedType innerSuperParam = innerSuper.isParameterized();
    // assertNotNull(innerSuperParam);
    // assertEquals(inner, innerSuperParam.getBaseType());
  }

  public void testEnclosingType() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_List);
    addTestResource(CU_ListAsField);

    buildTypeOracle();

    JClassType listAsField = typeOracle.getType(CU_ListAsField.getTypeName());
    assertNotNull(listAsField);
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
    addTestResource(CU_Object);
    addTestResource(CU_DefaultClass);
    addTestResource(CU_FieldsAndTypes);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_DefaultClass.getTypeName()));
    assertNotNull(typeOracle.findType(CU_FieldsAndTypes.getTypeName()));
    assertEquals(3, typeOracle.getTypes().length);
  }

  // Check that anonymous classes are not reflected in TypeOracle
  public void testLocal() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_EnclosingLocalClass);
    buildTypeOracle();
    JClassType[] types = typeOracle.getTypes();
    assertEquals(2, types.length);
  }

  public void testLocalWithSynthetic() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_EnclosingLocalWithMember);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_EnclosingLocalWithMember.getTypeName()));
    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertEquals(2, typeOracle.getTypes().length);
  }

  public void testMethodsAndParams() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_Throwable);
    addTestResource(CU_MethodsAndParams);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_Throwable.getTypeName()));
    assertNotNull(typeOracle.findType(CU_MethodsAndParams.getTypeName()));
    // Can't make an assertion about the # of loaded types in this test since the results are
    // different for source versus bytecode.
  }

  public void testOuterInner() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_OuterInner);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_OuterInner.getTypeName()));
    JClassType outer = typeOracle.findType("com.google.gwt.dev.javac.mediatortest.Outer");
    assertEquals(3, typeOracle.getTypes().length);
    assertNotNull(outer);
    assertEquals("Outer", outer.getSimpleSourceName());
    JClassType superclass = outer.getSuperclass();
    JType objectRef = typeOracle.getJavaLangObject();
    assertNotNull(objectRef);
    assertEquals(objectRef, superclass);
  }

  /**
   * Tests that we can build nested parameterized types even if that happens
   * while the type oracle is being built. This test assumes that
   * CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed will
   * cause a parameterized form of CU_DeclaresInnerGenericInterface to be
   * created before the type oracle has had a chance to resolve
   * CU_DeclaresInnerGenericInterface.
   */
  public void testParameterizedTypeBuildDependencies() throws TypeOracleException {
    addTestResource(CU_ReferencesParameterizedTypeBeforeItsGenericFormHasBeenProcessed);
    // Intentionally omitting the ExtendsParameterizedInterface resource
    // addResource(CU_ExtendsParameterizedInterface);
    addTestResource(CU_DeclaresInnerGenericInterface);
    addTestResource(CU_Object);

    buildTypeOracle();

    assertNull(typeOracle.findType(CU_ExtendsParameterizedInterface.getTypeName()));
  }

  public void testPseudoInnerSourceName() throws TypeOracleException {
    addTestResource(CU_Object);
    resources.add(CU_PseudoInner);
    buildTypeOracle();

    JClassType pseudoInnerType = typeOracle.findType(CU_PseudoInner.getTypeName());
    assertNotNull(pseudoInnerType);
    assertEquals("Pseudo$Inner", pseudoInnerType.getSimpleSourceName());
    assertEquals(CU_PseudoInner.getTypeName(), pseudoInnerType.getQualifiedSourceName());
  }

  /**
   * Test that modifying a type will cause any types that depend on it to be
   * rebuilt by the TypeOracleUpdater during a refresh.
   *
   * @throws NotFoundException
   */
  public void testRefresh() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_ExtendsGenericList);
    addTestResource(CU_GenericList);
    addTestResource(CU_ReferencesGenericListConstant);

    buildTypeOracle();

    // Get the types produced by the TypeOracle
    JClassType extendsGenericListType = typeOracle.getType(CU_ExtendsGenericList.getTypeName());
    JClassType genericListType = typeOracle.getType(CU_GenericList.getTypeName());
    JClassType referencesGenericListConstantType =
        typeOracle.getType(CU_ReferencesGenericListConstant.getTypeName());

    /*
     * Invalidate CU_GenericList and simulate a refresh. This should cause
     * anything that depends on GenericList to be rebuilt by the type oracle.
     */
    CU_GenericList.touch();
    buildTypeOracle();

    assertNotSame(genericListType.getQualifiedSourceName() + "; ", typeOracle
        .getType(CU_GenericList.getTypeName()), genericListType);
    assertNotSame(extendsGenericListType.getQualifiedSourceName() + "; ", typeOracle
        .getType(CU_ExtendsGenericList.getTypeName()), extendsGenericListType);

    /*
     * Make sure that referencing a constant field will cause a type to be
     * rebuilt if the constant changes.
     */
    assertNotSame(referencesGenericListConstantType.getQualifiedSourceName(), typeOracle
        .getType(CU_ReferencesGenericListConstant.getTypeName()), referencesGenericListConstantType);
  }

  public void testTypeParams() throws TypeOracleException {
    addTestResource(CU_Object);
    addTestResource(CU_NestedGenericInterfaces);
    addTestResource(CU_UnnestedImplementations);
    buildTypeOracle();

    assertNotNull(typeOracle.findType(CU_Object.getTypeName()));
    assertNotNull(typeOracle.findType(CU_NestedGenericInterfaces.getTypeName()));
    assertNotNull(typeOracle.findType("com.google.gwt.dev.javac.mediatortest.OuterInt"));
    assertNotNull(typeOracle.findType(CU_UnnestedImplementations.getTypeName()));
    assertNotNull(typeOracle.findType(CU_UnnestedImplementations.getTypeName() + ".OuterImpl"));

    JClassType innerImplType =
        typeOracle.findType(CU_UnnestedImplementations.getTypeName() + ".InnerImpl");
    assertNotNull(innerImplType);
    assertEquals(6, typeOracle.getTypes().length);
    JClassType[] interfaces = innerImplType.getImplementedInterfaces();
    assertEquals(1, interfaces.length);
    JClassType intf = interfaces[0];
    JParameterizedType intfParam = intf.isParameterized();
    assertNotNull(intfParam);
    JClassType intfEnclosing = intf.getEnclosingType();
    assertNotNull(intfEnclosing.isRawType());
  }

  /**
   * Creates a {@link Resource} and adds it the set of resources.
   */
  protected void addResource(String qualifiedTypeName, CharSequence source) {
    resources.add(new StaticJavaResource(qualifiedTypeName, source));
  }

  protected abstract void buildTypeOracle() throws TypeOracleException;

  /**
   * Tweak this if you want to see the log output.
   */
  protected TreeLogger createTreeLogger() {
    boolean reallyLog = false;
    if (reallyLog) {
      AbstractTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ALL);
      return logger;
    } else {
      return TreeLogger.NULL;
    }
  }

  protected void addTestResource(CheckedJavaResource checkedResource) {
    resources.add(checkedResource);
    for (String typeName : checkedResource.getTypeNames()) {
      register(typeName, checkedResource);
    }
  }

  private void check(JClassType classInfo) throws NotFoundException {
    final String qName = classInfo.getQualifiedSourceName();
    CheckedJavaResource cup = publicTypeNameToTestCupMap.get(qName);
    if (cup != null) {
      cup.check(classInfo);
    }
  }

  private void register(String qualifiedTypeName, CheckedJavaResource cup) {
    assertFalse(publicTypeNameToTestCupMap.containsKey(qualifiedTypeName));
    publicTypeNameToTestCupMap.put(qualifiedTypeName, cup);
  }
}
