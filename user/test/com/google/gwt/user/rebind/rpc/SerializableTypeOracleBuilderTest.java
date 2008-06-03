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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JavaSourceCodeBase;
import com.google.gwt.dev.javac.MockCompilationUnit;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.impl.SourceFileCompilationUnit;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.user.rebind.rpc.testcases.client.AbstractSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.ClassWithTypeParameterThatErasesToObject;
import com.google.gwt.user.rebind.rpc.testcases.client.CovariantArrays;
import com.google.gwt.user.rebind.rpc.testcases.client.ManualSerialization;
import com.google.gwt.user.rebind.rpc.testcases.client.NoSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.NotAllSubtypesAreSerializable;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used to test the {@link SerializableTypeOracleBuilder}.
 */
public class SerializableTypeOracleBuilderTest extends TestCase {

  /**
   * Used to test the results produced by the {@link SerializableTypeOracle}.
   */
  static class TypeInfo {
    boolean maybeInstantiated;
    final String sourceName;

    TypeInfo(String binaryName, boolean maybeInstantiated) {
      this.sourceName = makeSourceName(binaryName);
      this.maybeInstantiated = maybeInstantiated;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof TypeInfo)) {
        return false;
      }

      TypeInfo other = (TypeInfo) obj;
      return sourceName.equals(other.sourceName)
          && maybeInstantiated == other.maybeInstantiated;
    }

    @Override
    public String toString() {
      return "{ " + sourceName + ", " + Boolean.toString(maybeInstantiated)
          + " }";
    }
  }

  private static final int EXPOSURE_DIRECT = TypeParameterExposureComputer.EXPOSURE_DIRECT;

  private static final int EXPOSURE_NONE = TypeParameterExposureComputer.EXPOSURE_NONE;

  private static void addICRSE(Set<CompilationUnit> units) {
    StringBuffer code = new StringBuffer();
    code.append("package com.google.gwt.user.client.rpc;\n");
    code.append("public class IncompatibleRemoteServiceException extends Throwable {\n");
    code.append("}\n");
    units.add(createMockCompilationUnit(
        "com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException",
        code));
  }

  private static void addIsSerializable(Set<CompilationUnit> units) {
    StringBuffer code = new StringBuffer();
    code.append("package com.google.gwt.user.client.rpc;\n");
    code.append("public interface IsSerializable {\n");
    code.append("}\n");
    units.add(createMockCompilationUnit(
        "com.google.gwt.user.client.rpc.IsSerializable", code));
  }

  private static void addJavaIoSerializable(Set<CompilationUnit> units) {
    units.add(new SourceFileCompilationUnit(JavaSourceCodeBase.SERIALIZABLE));
  }

  private static void addJavaLangException(Set<CompilationUnit> units) {
    StringBuffer code = new StringBuffer();
    code.append("package java.lang;\n");
    code.append("public class Exception extends Throwable {\n");
    code.append("}\n");

    units.add(createMockCompilationUnit("java.lang.Exception", code));
  }

  private static void addJavaLangObject(Set<CompilationUnit> units) {
    units.add(new SourceFileCompilationUnit(JavaSourceCodeBase.OBJECT));
  }

  private static void addJavaLangString(Set<CompilationUnit> units) {
    units.add(new SourceFileCompilationUnit(JavaSourceCodeBase.STRING));
  }

  private static void addJavaLangThrowable(Set<CompilationUnit> units) {
    StringBuffer code = new StringBuffer();
    code.append("package java.lang;\n");
    code.append("import java.io.Serializable;\n");
    code.append("public class Throwable implements Serializable {\n");
    code.append("}\n");
    units.add(createMockCompilationUnit("java.lang.Throwable", code));
  }

  private static void addJavaUtilCollection(Set<CompilationUnit> units) {
    StringBuffer code = new StringBuffer();
    code.append("package java.util;\n");
    code.append("public interface Collection<E> {\n");
    code.append("}\n");
    units.add(createMockCompilationUnit("java.util.Collection", code));
  }

  private static void addJavaUtilMap(Set<CompilationUnit> units) {
    units.add(new SourceFileCompilationUnit(JavaSourceCodeBase.MAP));
  }

  private static void addStandardClasses(Set<CompilationUnit> units) {
    addJavaIoSerializable(units);
    addJavaLangObject(units);
    addJavaLangString(units);
    addJavaUtilMap(units);
    addICRSE(units);
    addJavaLangException(units);
    addJavaLangThrowable(units);
    addJavaUtilCollection(units);
    addIsSerializable(units);
  }

  private static void assertFieldSerializable(SerializableTypeOracle so,
      JClassType type) {
    assertTrue(so.isSerializable(type));
  }

  private static void assertInstantiable(SerializableTypeOracle so,
      JClassType type) {
    assertTrue(so.maybeInstantiated(type));
    assertFieldSerializable(so, type);
  }

  private static void assertNotFieldSerializable(SerializableTypeOracle so,
      JClassType type) {
    assertFalse(so.isSerializable(type));
  }

  private static void assertNotInstantiable(SerializableTypeOracle so,
      JClassType type) {
    assertFalse(so.maybeInstantiated(type));
  }

  private static void assertNotInstantiableOrFieldSerializable(
      SerializableTypeOracle so, JClassType type) {
    assertNotInstantiable(so, type);
    assertNotFieldSerializable(so, type);
  }

  private static void assertSerializableTypes(SerializableTypeOracle so,
      JClassType... expectedTypes) {
    Set<JType> expectedSet = new TreeSet<JType>(
        SerializableTypeOracleBuilder.JTYPE_COMPARATOR);
    expectedSet.addAll(Arrays.asList(expectedTypes));

    Set<JType> actualSet = new TreeSet<JType>(
        SerializableTypeOracleBuilder.JTYPE_COMPARATOR);
    JType[] actualTypes = so.getSerializableTypes();
    actualSet.addAll(Arrays.asList(actualTypes));

    assertTrue("Sets not equal.  Expected=\n" + expectedSet + ", \nactual=\n"
        + actualSet, expectedSet.containsAll(actualSet)
        && actualSet.containsAll(expectedSet));
  }

  private static TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private static CompilationUnit createMockCompilationUnit(String qname,
      CharSequence code) {
    return new MockCompilationUnit(qname, code.toString());
  }

  private static TypeInfo[] getActualTypeInfo(SerializableTypeOracle sto) {
    JType[] types = sto.getSerializableTypes();
    TypeInfo[] actual = new TypeInfo[types.length];
    for (int i = 0; i < types.length; ++i) {
      JType type = types[i];
      actual[i] = new TypeInfo(type.getParameterizedQualifiedSourceName(),
          sto.maybeInstantiated(type));
    }
    sort(actual);
    return actual;
  }

  private static String makeSourceName(String binaryName) {
    return binaryName.replace('$', '.');
  }

  private static void sort(TypeInfo[] typeInfos) {
    Arrays.sort(typeInfos, new Comparator<TypeInfo>() {
      public int compare(TypeInfo ti1, TypeInfo ti2) {
        if (ti1 == ti2) {
          return 0;
        }

        return ti1.sourceName.compareTo(ti2.sourceName);
      }
    });
  }

  private static String toString(TypeInfo[] typeInfos) {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    for (int i = 0; i < typeInfos.length; ++i) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append(typeInfos[i].toString());
      sb.append("\n");
    }

    sb.append("]");
    return sb.toString();
  }

  private static void validateSTO(SerializableTypeOracle sto,
      TypeInfo[] expected) {
    sort(expected);
    TypeInfo[] actual = getActualTypeInfo(sto);

    assertTrue("Expected: \n" + toString(expected) + ",\n Actual: \n"
        + toString(actual), Arrays.equals(expected, actual));
  }

  private final ModuleDef moduleDef;

  private final TypeOracle typeOracle;

  public SerializableTypeOracleBuilderTest() throws UnableToCompleteException {
    TreeLogger logger = createLogger();

    moduleDef = ModuleDefLoader.createSyntheticModule(logger,
        "com.google.gwt.user.rebind.rpc.testcases.RebindRPCTestCases.JUnit",
        new String[] {
            "com.google.gwt.user.rebind.rpc.testcases.RebindRPCTestCases",
            "com.google.gwt.junit.JUnit"}, true);
    typeOracle = moduleDef.getTypeOracle(logger);
  }

  /**
   * Test with a generic class whose type parameter is exposed only in certain
   * subclasses.
   * 
   * NOTE: This test has been disabled because it requires a better pruner in
   * STOB. See SerializableTypeOracleBuilder.pruneUnreachableTypes().
   */
  public void disabledTestMaybeExposedParameter()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class List<T> implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("List", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class EmptyList<T> extends List<T> {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("EmptyList", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class LinkedList<T> extends List<T> {\n");
      code.append("  T head;\n");
      code.append("  LinkedList<T> next;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("LinkedList", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class CantSerialize {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("CantSerialize", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType list = to.getType("List").isGenericType();
    JGenericType emptyList = to.getType("EmptyList").isGenericType();
    JClassType cantSerialize = to.getType("CantSerialize");

    JParameterizedType listOfCantSerialize = to.getParameterizedType(list,
        makeArray(cantSerialize));

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, listOfCantSerialize);
    SerializableTypeOracle so = sob.build(logger);

    assertFieldSerializable(so, listOfCantSerialize);
    assertSerializableTypes(so, list.getRawType(), emptyList.getRawType());
  }

  /**
   * Tests abstract root types that are field serializable.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testAbstractFieldSerializableRootType()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class A implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class B extends A {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends B {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JClassType a = to.getType("A");
    JClassType b = to.getType("B");
    JClassType c = to.getType("C");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, b);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, c);
    assertFieldSerializable(so, a);
    assertFieldSerializable(so, b);
    assertSerializableTypes(so, a, b, c);
  }

  /**
   * Tests that we do not violate java package restrictions when computing
   * serializable types.
   */
  public void testAccessLevelsInJavaPackage() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("java.A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("class B extends A {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("java.B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends A {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("java.C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JClassType a = to.getType("java.A");
    JArrayType arrayOfA = to.getArrayType(a);

    JClassType c = to.getType("java.C");
    JArrayType arrayOfC = to.getArrayType(c);

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, arrayOfA);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, arrayOfA, arrayOfC, a, c);
  }

  /*
   * Tests arrays of parameterized types.
   */
  public void testArrayOfParameterizedTypes() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      // A<T> exposes its param
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class AList<T> implements Serializable {\n");
      code.append("  A<T>[] as;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("AList", code));
    }

    {
      // B<T> does not expose its param
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class BList<T> implements Serializable {\n");
      code.append("  B<T>[] bs;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("BList", code));
    }

    {
      // A random serializable class
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser1 implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Ser1", code));
    }

    {
      // A random serializable class
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser2 implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Ser2", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Root implements Serializable {\n");
      code.append("  AList<Ser1> alist;\n");
      code.append("  BList<Ser2> blist;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Root", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JGenericType alist = to.getType("AList").isGenericType();
    JGenericType blist = to.getType("BList").isGenericType();
    JClassType ser1 = to.getType("Ser1");
    JClassType ser2 = to.getType("Ser2");
    JClassType root = to.getType("Root");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, root);

    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(a, 0));
    assertEquals(EXPOSURE_NONE, sob.getTypeParameterExposure(b, 0));
    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(alist, 0));
    assertEquals(EXPOSURE_NONE, sob.getTypeParameterExposure(blist, 0));

    SerializableTypeOracle so = sob.build(logger);

    JArrayType aArray = to.getArrayType(a.getRawType());
    JArrayType bArray = to.getArrayType(b.getRawType());

    assertSerializableTypes(so, root, alist.getRawType(), blist.getRawType(),
        aArray, bArray, a.getRawType(), b.getRawType(), ser1);

    assertInstantiable(so, alist.getRawType());
    assertInstantiable(so, blist.getRawType());
    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());
    assertInstantiable(so, aArray);
    assertInstantiable(so, bArray);
    assertInstantiable(so, ser1);
    assertNotInstantiableOrFieldSerializable(so, ser2);
  }

  /*
   * Tests arrays of type variables that do not cause infinite expansion.
   */
  public void testArrayOfTypeParameter() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  T[][] t;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C<T> implements Serializable {\n");
      code.append("  A<T[]> a1;\n");
      code.append("  A<Ser> a2;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("C", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JGenericType c = to.getType("C").isGenericType();
    JClassType ser = to.getType("Ser");

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType cOfString = to.getParameterizedType(c,
        makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, cOfString);

    assertEquals(2, sob.getTypeParameterExposure(a, 0));
    assertEquals(2, sob.getTypeParameterExposure(b, 0));
    assertEquals(3, sob.getTypeParameterExposure(c, 0));

    SerializableTypeOracle so = sob.build(logger);

    JArrayType stringArray = to.getArrayType(javaLangString);
    JArrayType stringArrayArray = to.getArrayType(stringArray);
    JArrayType stringArrayArrayArray = to.getArrayType(stringArrayArray);
    JArrayType serArray = to.getArrayType(ser);
    JArrayType serArrayArray = to.getArrayType(serArray);

    assertSerializableTypes(so, a.getRawType(), b.getRawType(), c,
        javaLangString, stringArray, stringArrayArray, stringArrayArrayArray,
        ser, serArray, serArrayArray);

    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());
    assertInstantiable(so, c);
    assertInstantiable(so, stringArray);
    assertInstantiable(so, stringArrayArray);
    assertInstantiable(so, stringArrayArrayArray);
    assertInstantiable(so, serArray);
    assertInstantiable(so, serArrayArray);
  }

  /**
   * Tests the rules that govern whether a type qualifies for serialization.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testClassQualifiesForSerialization()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("public class NotSerializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("NotSerializable", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class AutoSerializable {\n");
      code.append("  interface IFoo extends Serializable {};\n");
      code.append("  IFoo createFoo() { return new IFoo(){};}\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("AutoSerializable", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class OuterClass {\n");
      code.append("  static class StaticNested implements Serializable {};\n");
      code.append("  class NonStaticNested implements Serializable {};\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("OuterClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class AbstractSerializableClass implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("AbstractSerializableClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class NonDefaultInstantiableSerializable implements Serializable {\n");
      code.append("  NonDefaultInstantiableSerializable(int i) {}\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("NonDefaultInstantiableSerializable",
          code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class PublicOuterClass {\n");
      code.append("  private static class PrivateStaticInner {\n");
      code.append("    public static class PublicStaticInnerInner implements Serializable {\n");
      code.append("    }\n");
      code.append("  }\n");
      code.append("  static class DefaultStaticInner {\n");
      code.append("    static class DefaultStaticInnerInner implements Serializable {\n");
      code.append("    }\n");
      code.append("  }\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("PublicOuterClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public enum EnumWithSubclasses {\n");
      code.append("  A {\n");
      code.append("    @Override\n");
      code.append("    public String value() {\n");
      code.append("      return \"X\";\n");
      code.append("    }\n");
      code.append("  },\n");
      code.append("  B {\n");
      code.append("    @Override\n");
      code.append("    public String value() {\n");
      code.append("      return \"Y\";\n");
      code.append("    };\n");
      code.append("  };\n");
      code.append("  public abstract String value();\n");
      code.append("};\n");
      units.add(createMockCompilationUnit("EnumWithSubclasses", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public enum EnumWithNonDefaultCtors {\n");
      code.append("  A(\"X\"), B(\"Y\");\n");
      code.append("  String value;");
      code.append("  private EnumWithNonDefaultCtors(String value) {\n");
      code.append("    this.value = value;\n");
      code.append("  }\n");
      code.append("};\n");
      units.add(createMockCompilationUnit("EnumWithNonDefaultCtors", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("package java.lang;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class Enum<E extends Enum<E>> implements Serializable {\n");
      code.append("  protected Enum(String name, int ordinal) {\n");
      code.append("  }\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("java.lang.Enum", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);

    // Does not qualify because it is not declared to be auto or manually
    // serializable
    JClassType notSerializable = to.getType("NotSerializable");
    assertFalse(sob.shouldConsiderFieldsForSerialization(logger,
        notSerializable, false));

    // Local types should not qualify for serialization
    JClassType iFoo = to.getType("AutoSerializable.IFoo");
    assertFalse(sob.shouldConsiderFieldsForSerialization(logger,
        iFoo.getSubtypes()[0], false));

    // Static nested types qualify for serialization
    JClassType staticNested = to.getType("OuterClass.StaticNested");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger, staticNested,
        false));

    // Non-static nested types do not qualify for serialization
    JClassType nonStaticNested = to.getType("OuterClass.NonStaticNested");
    assertFalse(sob.shouldConsiderFieldsForSerialization(logger,
        nonStaticNested, false));

    // Abstract classes that implement Serializable should not qualify
    JClassType abstractSerializableClass = to.getType("AbstractSerializableClass");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger,
        abstractSerializableClass, false));
    assertFalse(SerializableTypeOracleBuilder.canBeInstantiated(
        TreeLogger.NULL, abstractSerializableClass, TreeLogger.DEBUG));

    // Non-default instantiable types should not qualify
    JClassType nonDefaultInstantiableSerializable = to.getType("NonDefaultInstantiableSerializable");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger,
        nonDefaultInstantiableSerializable, false));
    assertFalse(SerializableTypeOracleBuilder.canBeInstantiated(
        TreeLogger.NULL, nonDefaultInstantiableSerializable, TreeLogger.DEBUG));

    // SPublicStaticInnerInner is not accessible to classes in its package
    JClassType publicStaticInnerInner = to.getType("PublicOuterClass.PrivateStaticInner.PublicStaticInnerInner");
    assertFalse(sob.shouldConsiderFieldsForSerialization(logger,
        publicStaticInnerInner, false));

    // DefaultStaticInnerInner is visible to classes in its package
    JClassType defaultStaticInnerInner = to.getType("PublicOuterClass.DefaultStaticInner.DefaultStaticInnerInner");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger,
        defaultStaticInnerInner, false));

    // Enum with subclasses should qualify, but their subtypes should not
    JClassType enumWithSubclasses = to.getType("EnumWithSubclasses");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger,
        enumWithSubclasses, false));
    assertFalse(sob.shouldConsiderFieldsForSerialization(logger,
        enumWithSubclasses.getSubtypes()[0], false));

    // Enum that are not default instantiable should qualify
    JClassType enumWithNonDefaultCtors = to.getType("EnumWithNonDefaultCtors");
    assertTrue(sob.shouldConsiderFieldsForSerialization(logger,
        enumWithNonDefaultCtors, false));
  }

  /**
   * Tests that both the generic and raw forms of type that has a type parameter
   * that erases to object are not serializable.
   * 
   * @throws NotFoundException
   */
  public void testClassWithTypeParameterThatErasesToObject()
      throws NotFoundException, UnableToCompleteException {
    TreeLogger logger = createLogger();

    JRawType rawType = typeOracle.getType(
        ClassWithTypeParameterThatErasesToObject.class.getCanonicalName()).isGenericType().getRawType();

    // The raw form of the type should not be serializable.
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, rawType);
    try {
      stob.build(logger);
      fail("Expected an " + UnableToCompleteException.class.getSimpleName());
    } catch (UnableToCompleteException ex) {
      // Expected to reach here
    }
  }

  /**
   * Tests that a method signature which returns an Array type also includes the
   * possible covariant array types which could contain a serializable type.
   */
  public void testCovariantArrays() throws UnableToCompleteException,
      NotFoundException {
    TreeLogger logger = createLogger();

    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    JClassType rootType = typeOracle.getArrayType(typeOracle.getType(CovariantArrays.AA.class.getCanonicalName()));
    stob.addRootType(logger, rootType);
    SerializableTypeOracle sto = stob.build(logger);

    TypeInfo[] expected = new TypeInfo[] {
        new TypeInfo(CovariantArrays.AA.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.BB.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.CC.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.DD.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.A.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.B.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.B.class.getName(), true),
        new TypeInfo(CovariantArrays.C.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.D.class.getName() + "[]", true),
        new TypeInfo(CovariantArrays.D.class.getName(), true),};
    validateSTO(sto, expected);
  }

  /**
   * Expansion via parameterized types, where the type is exposed.
   */
  public void testInfiniteParameterizedTypeExpansionCase1()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("  T x;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<B<T>> ab;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerializableArgument implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("SerializableArgument", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JClassType serializableArgument = to.getType("SerializableArgument");

    JParameterizedType aOfString = to.getParameterizedType(a,
        makeArray(serializableArgument));
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, aOfString);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());
    assertInstantiable(so, serializableArgument);
    assertSerializableTypes(so, a.getRawType(), b.getRawType(),
        serializableArgument);
  }

  /**
   * Expansion via parameterized types, where the type is not actually exposed.
   */
  public void testInfiniteParameterizedTypeExpansionCase2()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<B<T>> ab;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class UnusedSerializableArgument implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("UnusedSerializableArgument", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JClassType unusedSerializableArgument = to.getType("UnusedSerializableArgument");

    JParameterizedType aOfString = to.getParameterizedType(a,
        makeArray(unusedSerializableArgument));
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, aOfString);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());

    assertNotInstantiableOrFieldSerializable(so, unusedSerializableArgument);
    assertSerializableTypes(so, a.getRawType(), b.getRawType());
  }

  /*
   * Case 3: Expansion via array dimensions, but the type arguments are not
   * exposed so this case will succeed.
   */
  public void testInfiniteParameterizedTypeExpansionCase3()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<T[]> ab;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a,
        makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, aOfString);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());

    assertNotInstantiableOrFieldSerializable(so, javaLangString);
    assertSerializableTypes(so, a.getRawType(), b.getRawType());
  }

  /*
   * Case 4: Expansion via array dimensions, but the type arguments are exposed
   * so this case will fail.
   */
  public void testInfiniteParameterizedTypeExpansionCase4()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<T[]> ab;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a,
        makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);

    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(a, 0));
    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(b, 0));

    sob.addRootType(logger, aOfString);
    SerializableTypeOracle so = sob.build(logger);

    assertNotFieldSerializable(so, b.getRawType());
    assertNotInstantiable(so, b.getRawType());

    assertSerializableTypes(so, a.getRawType(), javaLangString);
    assertInstantiable(so, a.getRawType());
    assertNotInstantiable(so, b.getRawType());
    assertInstantiable(so, javaLangString);
  }

  /**
   * Tests that a manually serialized type with a field that is not serializable
   * does not cause the generator to fail.
   */
  public void testManualSerialization() throws NotFoundException,
      UnableToCompleteException {
    TreeLogger logger = createLogger();

    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    JClassType a = typeOracle.getType(ManualSerialization.A.class.getCanonicalName());
    JClassType b = typeOracle.getType(ManualSerialization.B.class.getCanonicalName());
    stob.addRootType(logger, a);
    SerializableTypeOracle sto = stob.build(logger);
    assertInstantiable(sto, a);
    assertNotInstantiableOrFieldSerializable(sto, b);
  }

  /**
   * Tests that a raw List (missing gwt.typeArgs) will not result in a failure.
   * The set of types is not currently being checked.
   */
  public void testMissingGwtTypeArgs() throws NotFoundException,
      UnableToCompleteException {
    TreeLogger logger = createLogger();

    JClassType rawList = typeOracle.getType(List.class.getName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, rawList);
    SerializableTypeOracle sto = stob.build(logger);

    // TODO: This test should should be updated to use a controlled type oracle
    // then we can check the types.
    assertNotInstantiable(sto, rawList);
  }

  /**
   * Tests that a method signature which has no serializable types will result
   * in a failure.
   */
  public void testNoSerializableTypes() throws NotFoundException,
      UnableToCompleteException {
    TreeLogger logger = createLogger();

    JClassType a = typeOracle.getType(NoSerializableTypes.A.class.getCanonicalName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, a);
    try {
      stob.build(logger);
      fail("Should have thrown an UnableToCompleteException");
    } catch (UnableToCompleteException ex) {
      // expected to get here
    }
  }

  /**
   * Tests that a method signature which only has type whose inheritance
   * hiearchy has a mix of serializable and unserializable types will not cause
   * the generator fail. It also checks for the set of serializable types.
   */
  public void testNotAllSubtypesAreSerializable()
      throws UnableToCompleteException, NotFoundException {
    TreeLogger logger = createLogger();

    JClassType a = typeOracle.getType(NotAllSubtypesAreSerializable.A.class.getCanonicalName());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, a);
    SerializableTypeOracle sto = stob.build(logger);

    TypeInfo[] expected = new TypeInfo[] {
        new TypeInfo(
            makeSourceName(NotAllSubtypesAreSerializable.B.class.getName()),
            true),
        new TypeInfo(
            makeSourceName(NotAllSubtypesAreSerializable.D.class.getName()),
            true)};
    validateSTO(sto, expected);
  }

  /**
   * Tests that Object[] is not instantiable.
   */
  public void testObjectArrayNotInstantiable() throws UnableToCompleteException {
    TreeLogger logger = createLogger();

    JArrayType objectArray = typeOracle.getArrayType(typeOracle.getJavaLangObject());
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, objectArray);
    try {
      stob.build(logger);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that Object is not considered instantiable.
   */
  public void testObjectNotInstantiable() throws UnableToCompleteException {
    TreeLogger logger = createLogger();

    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(logger, typeOracle.getJavaLangObject());
    try {
      stob.build(logger);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that a method signature which only has abstract serializable types
   * fails.
   */
  public void testOnlyAbstractSerializableTypes()
      throws UnableToCompleteException, NotFoundException {
    TreeLogger logger = createLogger();

    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    stob.addRootType(
        logger,
        typeOracle.getType(AbstractSerializableTypes.IFoo.class.getCanonicalName()));
    stob.addRootType(
        logger,
        typeOracle.getType(AbstractSerializableTypes.AbstractClass.class.getCanonicalName()));

    try {
      stob.build(logger);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests that raw type with type parameters that are instantiable are
   * themselves instantiable.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testRawTypes() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T extends SerializableClass> implements Serializable {\n");
      code.append("  T x;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerializableClass implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("SerializableClass", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JRawType rawA = a.getRawType();

    JClassType serializableClass = to.getType("SerializableClass");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, rawA);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertSerializableTypes(so, rawA, serializableClass);
  }

  /*
   * Tests the isAssignable test for deciding whether a subclass should be
   * pulled in.
   */
  public void testSubclassIsAssignable() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B extends A<String> implements Serializable {\n");
      code.append("  Object o;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends A<Ser> implements Serializable {\n");
      // TODO: rejecting Ser requires a better pruner in STOB
      // code.append(" Ser ser;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("C", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JClassType b = to.getType("B");
    JClassType c = to.getType("C");
    JClassType ser = to.getType("Ser");

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a,
        makeArray(javaLangString));
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, to);
    stob.addRootType(logger, aOfString);

    assertEquals(EXPOSURE_NONE, stob.getTypeParameterExposure(a, 0));

    SerializableTypeOracle so = stob.build(logger);

    assertSerializableTypes(so, a.getRawType());

    assertInstantiable(so, a.getRawType());
    assertNotInstantiableOrFieldSerializable(so, b);
    assertNotInstantiableOrFieldSerializable(so, c);
    assertNotInstantiableOrFieldSerializable(so, javaLangString);
    assertNotInstantiableOrFieldSerializable(so, ser);
  }

  /**
   * Tests subtypes that introduce new instantiable type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testSubclassWithNewInstantiableTypeParameters()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T extends C> extends A {\n");
      code.append("  T c;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JClassType a = to.getType("A");
    JRawType rawB = to.getType("B").isGenericType().getRawType();
    JClassType c = to.getType("C");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, a);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a);
    assertSerializableTypes(so, a, rawB, c);
  }

  /**
   * Tests subtypes that introduce new uninstantiable type parameters as
   * compared to an implemented interface, where the root type is the interface.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testSubclassWithNewTypeParameterComparedToAnImplementedInterface()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface Intf<T> extends Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Intf", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Bar<T> implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Bar", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Foo<T extends Ser> extends Bar<T> implements Intf<String> {\n");
      code.append("  T x;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Foo", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType intf = to.getType("Intf").isGenericType();
    JClassType foo = to.getType("Foo");
    JClassType bar = to.getType("Bar");
    JClassType intfOfString = to.getParameterizedType(intf,
        new JClassType[] {to.getType(String.class.getName())});
    JClassType ser = to.getType("Ser");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, intfOfString);
    SerializableTypeOracle so = sob.build(logger);

    /*
     * TODO(spoon): should also check that Intf<String> has instantiable
     * subclasses; currently the APIs for STOB and STO do not make this possible
     * to test
     */
    assertInstantiable(so, ser);
    assertSerializableTypes(so, foo.getErasedType(), bar.getErasedType(),
        ser.getErasedType());
  }

  /**
   * Tests subtypes that introduce new uninstantiable type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testSubclassWithNewUninstantiableTypeParameters()
      throws UnableToCompleteException, NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A {\n");
      code.append("  T x;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JClassType a = to.getType("A");

    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, a);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a);
    assertSerializableTypes(so, a);
  }

  /**
   * Tests root types that have type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testTypeParametersInRootTypes() throws UnableToCompleteException,
      NotFoundException {
    Set<CompilationUnit> units = new HashSet<CompilationUnit>();
    addStandardClasses(units);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B implements Serializable {\n");
      code.append("}\n");
      units.add(createMockCompilationUnit("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, units);

    JGenericType a = to.getType("A").isGenericType();
    JRawType rawA = a.getRawType();
    JClassType b = to.getType("B");

    JTypeParameter syntheticTypeParam = new JTypeParameter("U", 0);
    syntheticTypeParam.setBounds(makeArray(b));

    JParameterizedType parameterizedType = to.getParameterizedType(a,
        new JClassType[] {syntheticTypeParam});
    SerializableTypeOracleBuilder sob = new SerializableTypeOracleBuilder(
        logger, to);
    sob.addRootType(logger, parameterizedType);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, rawA);
    assertInstantiable(so, b);
    assertSerializableTypes(so, rawA, b);
  }

  private JClassType[] makeArray(JClassType... elements) {
    return elements;
  }
}
