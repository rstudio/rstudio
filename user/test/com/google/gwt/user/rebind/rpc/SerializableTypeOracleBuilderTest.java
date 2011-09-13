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

import com.google.gwt.core.ext.StubGeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.cfg.BindingProperty;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.StaticPropertyOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.StaticJavaResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.user.rebind.rpc.testcases.client.AbstractSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.ClassWithTypeParameterThatErasesToObject;
import com.google.gwt.user.rebind.rpc.testcases.client.ManualSerialization;
import com.google.gwt.user.rebind.rpc.testcases.client.NoSerializableTypes;
import com.google.gwt.user.rebind.rpc.testcases.client.NotAllSubtypesAreSerializable;

import junit.framework.TestCase;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Used to test the {@link SerializableTypeOracleBuilder}.
 */
public class SerializableTypeOracleBuilderTest extends TestCase {
  
  /**
   * Just enough of a {@code GeneratorContext} to satisfy
   * {@code SerializableTypeOracleBuilder}.
   */
  static class MockContext extends StubGeneratorContext {
    private TypeOracle typeOracle;

    MockContext(TypeOracle typeOracle) {
      this.typeOracle = typeOracle;
    }

    @Override
    public TypeOracle getTypeOracle() {
      return typeOracle;
    }

    @Override
    public boolean isProdMode() {
      return true;
    }
  }

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
      return sourceName.equals(other.sourceName) && maybeInstantiated == other.maybeInstantiated;
    }

    @Override
    public int hashCode() {
      return sourceName.hashCode() * (maybeInstantiated ? 17 : 43);
    }

    @Override
    public String toString() {
      return "{ " + sourceName + ", " + Boolean.toString(maybeInstantiated) + " }";
    }
  }

  private static final int EXPOSURE_DIRECT = TypeParameterExposureComputer.EXPOSURE_DIRECT;

  private static final int EXPOSURE_NONE = TypeParameterExposureComputer.EXPOSURE_NONE;

  private static TypeOracle sTypeOracle;

  /**
   * Mocks the source of the {@link GwtTransient} type in this package. 
   */
  private static void addCustomGwtTransient(Set<Resource> resources) {
    StringBuffer code = new StringBuffer();
    code.append("package com.google.gwt.user.rebind.rpc;\n");
    code.append("import java.lang.annotation.Retention;");
    code.append("import java.lang.annotation.RetentionPolicy;");
    code.append("@Retention(RetentionPolicy.RUNTIME)");
    code.append("public @interface GwtTransient { }\n");
    resources.add(new StaticJavaResource("com.google.gwt.user.rebind.rpc.GwtTransient", code));
  }

  private static void addGwtTransient(Set<Resource> resources) {
    StringBuffer code = new StringBuffer();
    code.append("package com.google.gwt.user.client.rpc;\n");
    code.append("public @interface GwtTransient { }\n");
    resources.add(new StaticJavaResource("com.google.gwt.user.client.rpc.GwtTransient", code));
  }

  private static void addICRSE(Set<Resource> resources) {
    StringBuffer code = new StringBuffer();
    code.append("package com.google.gwt.user.client.rpc;\n");
    code.append("public class IncompatibleRemoteServiceException extends Throwable {\n");
    code.append("}\n");
    resources.add(new StaticJavaResource(
        "com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException", code));
  }

  private static void addStandardClasses(Set<Resource> resources) {
    for (MockJavaResource resource : JavaResourceBase.getStandardResources()) {
      resources.add(resource);
    }
    addGwtTransient(resources);
    addICRSE(resources);
  }

  private static void assertFieldSerializable(SerializableTypeOracle so, JClassType type) {
    assertTrue(so.isSerializable(type));
  }

  private static void assertInstantiable(SerializableTypeOracle so, JClassType type) {
    assertTrue(so.maybeInstantiated(type));
    assertFieldSerializable(so, type);
  }

  private static void assertNotFieldSerializable(SerializableTypeOracle so, JClassType type) {
    assertFalse(so.isSerializable(type));
  }

  private static void assertNotInstantiable(SerializableTypeOracle so, JClassType type) {
    assertFalse(so.maybeInstantiated(type));
  }

  private static void assertNotInstantiableOrFieldSerializable(SerializableTypeOracle so,
      JClassType type) {
    assertNotInstantiable(so, type);
    assertNotFieldSerializable(so, type);
  }

  private static void assertSerializableTypes(SerializableTypeOracle so,
      JClassType... expectedTypes) {
    Set<JType> expectedSet = new TreeSet<JType>(SerializableTypeOracleBuilder.JTYPE_COMPARATOR);
    expectedSet.addAll(Arrays.asList(expectedTypes));

    Set<JType> actualSet = new TreeSet<JType>(SerializableTypeOracleBuilder.JTYPE_COMPARATOR);
    JType[] actualTypes = so.getSerializableTypes();
    actualSet.addAll(Arrays.asList(actualTypes));

    assertTrue("Sets not equal.  Expected=\n" + expectedSet + ", \nactual=\n" + actualSet,
        expectedSet.containsAll(actualSet) && actualSet.containsAll(expectedSet));
  }

  private static TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private static SerializableTypeOracleBuilder createSerializableTypeOracleBuilder(
      TreeLogger logger, TypeOracle to) throws UnableToCompleteException {
    // Make an empty property oracle.
    StaticPropertyOracle propertyOracle =
        new StaticPropertyOracle(new BindingProperty[0], new String[0],
            new ConfigurationProperty[0]);
    return new SerializableTypeOracleBuilder(logger, propertyOracle, new MockContext(to));
  }

  private static TypeInfo[] getActualTypeInfo(SerializableTypeOracle sto) {
    JType[] types = sto.getSerializableTypes();
    TypeInfo[] actual = new TypeInfo[types.length];
    for (int i = 0; i < types.length; ++i) {
      JType type = types[i];
      actual[i] =
          new TypeInfo(type.getParameterizedQualifiedSourceName(), sto.maybeInstantiated(type));
    }
    sort(actual);
    return actual;
  }

  private static TypeOracle getTestTypeOracle() throws UnableToCompleteException {
    if (sTypeOracle == null) {
      TreeLogger logger = createLogger();
      ModuleDef moduleDef =
          ModuleDefLoader.createSyntheticModule(logger,
              "com.google.gwt.user.rebind.rpc.testcases.RebindRPCTestCases.JUnit", new String[] {
                  "com.google.gwt.user.rebind.rpc.testcases.RebindRPCTestCases",
                  "com.google.gwt.junit.JUnit"}, true);
      sTypeOracle = moduleDef.getCompilationState(logger).getTypeOracle();
    }
    return sTypeOracle;
  }

  private static String makeSourceName(String binaryName) {
    return binaryName.replace('$', '.');
  }

  private static void sort(TypeInfo[] typeInfos) {
    Arrays.sort(typeInfos, new Comparator<TypeInfo>() {
      @Override
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

  private static void validateSTO(SerializableTypeOracle sto, TypeInfo[] expected) {
    sort(expected);
    TypeInfo[] actual = getActualTypeInfo(sto);

    assertTrue("Expected: \n" + toString(expected) + ",\n Actual: \n" + toString(actual), Arrays
        .equals(expected, actual));
  }

  /**
   * Test with a generic class whose type parameter is exposed only in certain
   * subclasses.
   * 
   * NOTE: This test has been disabled because it requires a better pruner in
   * STOB. See SerializableTypeOracleBuilder.pruneUnreachableTypes().
   */
  public void disabledTestMaybeExposedParameter() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class List<T> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("List", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class EmptyList<T> extends List<T> {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("EmptyList", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class LinkedList<T> extends List<T> {\n");
      code.append("  T head;\n");
      code.append("  LinkedList<T> next;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("LinkedList", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("public class CantSerialize {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("CantSerialize", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType list = to.getType("List").isGenericType();
    JGenericType emptyList = to.getType("EmptyList").isGenericType();
    JClassType cantSerialize = to.getType("CantSerialize");

    JParameterizedType listOfCantSerialize =
        to.getParameterizedType(list, makeArray(cantSerialize));

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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
  public void testAbstractFieldSerializableRootType() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class A implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class B extends A {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends B {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType a = to.getType("A");
    JClassType b = to.getType("B");
    JClassType c = to.getType("C");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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
  public void testAccessLevelsInJavaPackage() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("java.A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("class B extends A {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("java.B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("package java;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends A {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("java.C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType a = to.getType("java.A");
    JArrayType arrayOfA = to.getArrayType(a);

    JClassType c = to.getType("java.C");
    JArrayType arrayOfC = to.getArrayType(c);

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, arrayOfA);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, arrayOfA, arrayOfC, a, c);
  }

  /*
   * Tests arrays of parameterized types.
   */
  public void testArrayOfParameterizedTypes() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      // A<T> exposes its param
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class AList<T> implements Serializable {\n");
      code.append("  A<T>[] as;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("AList", code));
    }

    {
      // B<T> does not expose its param
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class BList<T> implements Serializable {\n");
      code.append("  B<T>[] bs;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("BList", code));
    }

    {
      // A random serializable class
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser1 implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Ser1", code));
    }

    {
      // A random serializable class
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser2 implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Ser2", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Root implements Serializable {\n");
      code.append("  AList<Ser1> alist;\n");
      code.append("  BList<Ser2> blist;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Root", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JGenericType alist = to.getType("AList").isGenericType();
    JGenericType blist = to.getType("BList").isGenericType();
    JClassType ser1 = to.getType("Ser1");
    JClassType ser2 = to.getType("Ser2");
    JClassType root = to.getType("Root");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, root);

    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(a, 0));
    assertEquals(EXPOSURE_NONE, sob.getTypeParameterExposure(b, 0));
    assertEquals(EXPOSURE_DIRECT, sob.getTypeParameterExposure(alist, 0));
    assertEquals(EXPOSURE_NONE, sob.getTypeParameterExposure(blist, 0));

    SerializableTypeOracle so = sob.build(logger);

    JArrayType aArray = to.getArrayType(a.getRawType());
    JArrayType bArray = to.getArrayType(b.getRawType());

    assertSerializableTypes(so, root, alist.getRawType(), blist.getRawType(), aArray, bArray, a
        .getRawType(), b.getRawType(), ser1);

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
  public void testArrayOfTypeParameter() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  T[][] t;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C<T> implements Serializable {\n");
      code.append("  A<T[]> a1;\n");
      code.append("  A<Ser> a2;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JGenericType c = to.getType("C").isGenericType();
    JClassType ser = to.getType("Ser");

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType cOfString = to.getParameterizedType(c, makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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

    assertSerializableTypes(so, a.getRawType(), b.getRawType(), c, javaLangString, stringArray,
        stringArrayArray, stringArrayArrayArray, ser, serArray, serArrayArray);

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
  public void testClassQualifiesForSerialization() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("public class NotSerializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("NotSerializable", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class OuterClass {\n");
      code.append("  static class StaticNested implements Serializable {};\n");
      code.append("  class NonStaticNested implements Serializable {};\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("OuterClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class AbstractSerializableClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("AbstractSerializableClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class NonDefaultInstantiableSerializable implements Serializable {\n");
      code.append("  NonDefaultInstantiableSerializable(int i) {}\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("NonDefaultInstantiableSerializable", code));
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
      resources.add(new StaticJavaResource("PublicOuterClass", code));
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
      resources.add(new StaticJavaResource("EnumWithSubclasses", code));
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
      resources.add(new StaticJavaResource("EnumWithNonDefaultCtors", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);

    // Does not qualify because it is not declared to be auto or manually
    // serializable
    JClassType notSerializable = to.getType("NotSerializable");
    ProblemReport problems = new ProblemReport();
    assertFalse(sob.shouldConsiderFieldsForSerialization(notSerializable, problems));

    // Static nested types qualify for serialization
    JClassType staticNested = to.getType("OuterClass.StaticNested");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(staticNested, problems));

    // Non-static nested types do not qualify for serialization
    JClassType nonStaticNested = to.getType("OuterClass.NonStaticNested");
    problems = new ProblemReport();
    assertFalse(sob.shouldConsiderFieldsForSerialization(nonStaticNested, problems));

    // Abstract classes that implement Serializable should not qualify
    JClassType abstractSerializableClass = to.getType("AbstractSerializableClass");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(abstractSerializableClass, problems));

    problems = new ProblemReport();
    assertFalse(SerializableTypeOracleBuilder
        .canBeInstantiated(abstractSerializableClass, problems));

    // Non-default instantiable types should not qualify
    JClassType nonDefaultInstantiableSerializable =
        to.getType("NonDefaultInstantiableSerializable");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(nonDefaultInstantiableSerializable,
        problems));

    problems = new ProblemReport();
    assertFalse(SerializableTypeOracleBuilder.canBeInstantiated(nonDefaultInstantiableSerializable,
        problems));

    // SPublicStaticInnerInner is not accessible to classes in its package
    JClassType publicStaticInnerInner =
        to.getType("PublicOuterClass.PrivateStaticInner.PublicStaticInnerInner");
    problems = new ProblemReport();
    assertFalse(sob.shouldConsiderFieldsForSerialization(publicStaticInnerInner, problems));

    // DefaultStaticInnerInner is visible to classes in its package
    JClassType defaultStaticInnerInner =
        to.getType("PublicOuterClass.DefaultStaticInner.DefaultStaticInnerInner");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(defaultStaticInnerInner, problems));

    // Enum with subclasses should qualify, but their subtypes should not
    JClassType enumWithSubclasses = to.getType("EnumWithSubclasses");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(enumWithSubclasses, problems));

    // There are no longer any enum subclasses in TypeOracle
    assertEquals(0, enumWithSubclasses.getSubtypes().length);

    // Enum that are not default instantiable should qualify
    JClassType enumWithNonDefaultCtors = to.getType("EnumWithNonDefaultCtors");
    problems = new ProblemReport();
    assertTrue(sob.shouldConsiderFieldsForSerialization(enumWithNonDefaultCtors, problems));
  }

  /**
   * Tests that both the generic and raw forms of type that has a type parameter
   * that erases to object are not serializable.
   * 
   * @throws NotFoundException
   */
  public void testClassWithTypeParameterThatErasesToObject() throws NotFoundException,
      UnableToCompleteException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    JRawType rawType =
        typeOracle.getType(ClassWithTypeParameterThatErasesToObject.class.getCanonicalName())
            .isGenericType().getRawType();

    // The raw form of the type should not be serializable.
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
    stob.addRootType(logger, rawType);
    try {
      stob.build(logger);
      fail("Expected an " + UnableToCompleteException.class.getSimpleName());
    } catch (UnableToCompleteException ex) {
      // Expected to reach here
    }
  }

  /**
   * Test the situation where an abstract class has an unconstrained type
   * parameter but all of its concrete subclasses add helpful constraints to it.
   * 
   * @throws NotFoundException
   * @throws UnableToCompleteException
   */
  public void testConcreteClassesConstrainATypeParameter() throws NotFoundException,
      UnableToCompleteException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class Holder<T extends Serializable> implements Serializable {\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Holder", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class StringHolder extends Holder<String> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("StringHolder", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class DateHolder extends Holder<Date> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("DateHolder", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Date implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Date", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class UnrelatedClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("UnrelatedClass", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType holder = to.getType("Holder").isGenericType();
    JClassType stringHolder = to.getType("StringHolder");
    JClassType dateHolder = to.getType("DateHolder");
    JClassType unrelatedClass = to.getType("UnrelatedClass");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, holder.getRawType());
    SerializableTypeOracle so = sob.build(logger);

    JClassType string = to.getType(String.class.getCanonicalName());
    JClassType date = to.getType("Date");

    assertSerializableTypes(so, holder.getRawType(), stringHolder, dateHolder, string, date);
    assertFieldSerializable(so, holder.getRawType());
    assertInstantiable(so, stringHolder);
    assertInstantiable(so, dateHolder);
    assertInstantiable(so, string);
    assertInstantiable(so, date);
    assertNotInstantiableOrFieldSerializable(so, unrelatedClass);
  }

  /**
   * Tests that a method signature which returns an Array type also includes the
   * possible covariant array types which could contain a serializable type.
   */
  public void testCovariantArrays() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Sup implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Sup", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Sub extends Sup {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Sub", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType sup = to.getType("Sup");
    JClassType sub = to.getType("Sub");
    JPrimitiveType primFloat = JPrimitiveType.FLOAT;

    JArrayType subArray = to.getArrayType(sub);
    JArrayType subArrayArray = to.getArrayType(subArray);
    JArrayType supArray = to.getArrayType(sup);
    JArrayType supArrayArray = to.getArrayType(supArray);
    JArrayType primFloatArray = to.getArrayType(primFloat);
    JArrayType primFloatArrayArray = to.getArrayType(primFloatArray);

    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, to);
    // adding Sub first exercises an extra code path in STOB
    stob.addRootType(logger, sub);
    stob.addRootType(logger, supArrayArray);
    stob.addRootType(logger, primFloatArrayArray);
    SerializableTypeOracle sto = stob.build(logger);

    assertSerializableTypes(sto, sup, sub, supArray, subArray, primFloatArray, supArrayArray,
        subArrayArray, primFloatArrayArray);
    assertInstantiable(sto, primFloatArrayArray);
    assertInstantiable(sto, primFloatArray);
    assertInstantiable(sto, subArrayArray);
    assertInstantiable(sto, subArray);
    assertInstantiable(sto, supArrayArray);
    assertInstantiable(sto, supArray);
  }

  /**
   * If the query type extends a raw type, be sure to pick up the parameters of
   * the raw subertype.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testExtensionFromRaw1() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class HashSet<T extends SerClass> implements Serializable {\n");
      code.append("  T[] x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("HashSet", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class NameSet extends HashSet implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("NameSet", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerClassSub extends SerClass {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerClassSub", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType hashSet = to.getType("HashSet").isGenericType();
    JClassType nameSet = to.getType("NameSet");
    JClassType serClass = to.getType("SerClass");
    JClassType serClassSub = to.getType("SerClassSub");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, nameSet);
    SerializableTypeOracle so = sob.build(logger);

    JArrayType arrayOfSerClass = to.getArrayType(serClass);
    JArrayType arrayOfSerClassSub = to.getArrayType(serClassSub);

    assertSerializableTypes(so, hashSet.getRawType(), nameSet, serClass, serClassSub,
        arrayOfSerClass, arrayOfSerClassSub);
    assertFieldSerializable(so, hashSet.getRawType());
    assertNotInstantiable(so, hashSet.getRawType());
    assertInstantiable(so, nameSet);
    assertInstantiable(so, serClass);
    assertInstantiable(so, serClassSub);
    assertInstantiable(so, arrayOfSerClass);
    assertInstantiable(so, arrayOfSerClassSub);
  }

  /**
   * If a subtype of a root type extends from the raw version of that root type,
   * then when visiting the fields of the raw version, take advantage of
   * information from the original root type.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testExtensionFromRaw2() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class HashSet<T extends Serializable> implements Serializable {\n");
      code.append("  T[] x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("HashSet", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class NameSet<T extends SerClass> extends HashSet implements Serializable {\n");
      code.append("  T exposed;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("NameSet", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerClass", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerClassSub extends SerClass {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerClassSub", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class UnrelatedClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("UnrelatedClass", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType string = to.getType(String.class.getCanonicalName());
    JGenericType hashSet = to.getType("HashSet").isGenericType();
    JGenericType nameSet = to.getType("NameSet").isGenericType();
    JClassType unrelatedClass = to.getType("UnrelatedClass");
    JClassType serClass = to.getType("SerClass");
    JClassType serClassSub = to.getType("SerClassSub");
    JParameterizedType hashSetOfString = to.getParameterizedType(hashSet, makeArray(string));

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, hashSetOfString);
    SerializableTypeOracle so = sob.build(logger);

    JArrayType arrayOfString = to.getArrayType(string);

    assertSerializableTypes(so, hashSet.getRawType(), nameSet.getRawType(), string, arrayOfString,
        serClass, serClassSub);
    assertInstantiable(so, hashSet.getRawType());
    assertInstantiable(so, nameSet.getRawType());
    assertInstantiable(so, string);
    assertInstantiable(so, serClass);
    assertInstantiable(so, serClassSub);
    assertNotInstantiable(so, unrelatedClass);
  }

  /**
   * Expansion via parameterized types, where the type is exposed.
   */
  public void testInfiniteParameterizedTypeExpansionCase1() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<B<T>> ab;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerializableArgument implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerializableArgument", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JClassType serializableArgument = to.getType("SerializableArgument");

    JParameterizedType aOfString = to.getParameterizedType(a, makeArray(serializableArgument));
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, aOfString);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertInstantiable(so, b.getRawType());
    assertInstantiable(so, serializableArgument);
    assertSerializableTypes(so, a.getRawType(), b.getRawType(), serializableArgument);
  }

  /**
   * Expansion via parameterized types, where the type is not actually exposed.
   */
  public void testInfiniteParameterizedTypeExpansionCase2() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<B<T>> ab;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class UnusedSerializableArgument implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("UnusedSerializableArgument", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();
    JClassType unusedSerializableArgument = to.getType("UnusedSerializableArgument");

    JParameterizedType aOfString =
        to.getParameterizedType(a, makeArray(unusedSerializableArgument));
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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
  public void testInfiniteParameterizedTypeExpansionCase3() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  B<T> b;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<T[]> ab;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a, makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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
  public void testInfiniteParameterizedTypeExpansionCase4() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A<T> implements Serializable {\n");
      code.append("  A<T[]> ab;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JGenericType b = to.getType("B").isGenericType();

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a, makeArray(javaLangString));
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);

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
  public void testManualSerialization() throws NotFoundException, UnableToCompleteException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
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
  public void testMissingGwtTypeArgs() throws NotFoundException, UnableToCompleteException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    JClassType rawList = typeOracle.getType(List.class.getName());
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
    stob.addRootType(logger, rawList);
    SerializableTypeOracle sto = stob.build(logger);

    // TODO: This test should should be updated to use a controlled type oracle
    // then we can check the types.
    assertNotInstantiable(sto, rawList);
  }

  /**
   * Tests that the type constrainer can accurately detect when an interface
   * matches another type.
   */
  public void testNonOverlappingInterfaces() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface Intf1 extends Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf1", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface Intf2 extends Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf2", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface Intf3 extends Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf3", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Implements12 implements Intf1, Intf2 {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Implements12", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ImplementsNeither implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ImplementsNeither", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface List<T> extends Serializable  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("List", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ListOfIntf1 implements List<Intf1>  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ListOfIntf1", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ListOfIntf2 implements List<Intf2>  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ListOfIntf2", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ListOfIntf3 implements List<Intf3>  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ListOfIntf3", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ListOfImplements12 implements List<Implements12>  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ListOfImplements12", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ListOfImplementsNeither implements List<ImplementsNeither>  {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ListOfImplementsNeither", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType intf1 = to.getType("Intf1");
    JClassType intf2 = to.getType("Intf2");
    JClassType intf3 = to.getType("Intf3");
    JClassType implements12 = to.getType("Implements12");
    JClassType implementsNeither = to.getType("ImplementsNeither");
    JGenericType list = to.getType("List").isGenericType();
    JClassType listOfIntf1 = to.getType("ListOfIntf1");
    JClassType listOfIntf2 = to.getType("ListOfIntf2");
    JClassType listOfIntf3 = to.getType("ListOfIntf3");
    JClassType listOfImplements12 = to.getType("ListOfImplements12");
    JClassType listOfImplementsNeither = to.getType("ListOfImplementsNeither");

    TypeConstrainer typeConstrainer = new TypeConstrainer(to);
    Map<JTypeParameter, JClassType> emptyConstraints = new HashMap<JTypeParameter, JClassType>();
    assertTrue(typeConstrainer.typesMatch(intf1, intf2, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(intf1, intf3, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(implements12, intf1, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(implements12, intf3, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(implementsNeither, intf1, emptyConstraints));

    JParameterizedType parameterizedListOfIntf1 = to.getParameterizedType(list, makeArray(intf1));

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, parameterizedListOfIntf1);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, listOfIntf1, listOfIntf2, listOfImplements12);
    assertNotFieldSerializable(so, listOfIntf3);
    assertNotFieldSerializable(so, listOfImplementsNeither);
  }

  /**
   * Tests that a method signature which has no serializable types will result
   * in a failure.
   */
  public void testNoSerializableTypes() throws NotFoundException, UnableToCompleteException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    JClassType a = typeOracle.getType(NoSerializableTypes.A.class.getCanonicalName());
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
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
  public void testNotAllSubtypesAreSerializable() throws UnableToCompleteException,
      NotFoundException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    JClassType a = typeOracle.getType(NotAllSubtypesAreSerializable.A.class.getCanonicalName());
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
    stob.addRootType(logger, a);
    SerializableTypeOracle sto = stob.build(logger);

    TypeInfo[] expected =
        new TypeInfo[] {
            new TypeInfo(makeSourceName(NotAllSubtypesAreSerializable.B.class.getName()), true),
            new TypeInfo(makeSourceName(NotAllSubtypesAreSerializable.D.class.getName()), true)};
    validateSTO(sto, expected);
  }

  /**
   * Tests that Object[] is not instantiable.
   */
  public void testObjectArrayNotInstantiable() throws UnableToCompleteException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    JArrayType objectArray = typeOracle.getArrayType(typeOracle.getJavaLangObject());
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
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

    TypeOracle typeOracle = getTestTypeOracle();
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
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
  public void testOnlyAbstractSerializableTypes() throws UnableToCompleteException,
      NotFoundException {
    TreeLogger logger = createLogger();

    TypeOracle typeOracle = getTestTypeOracle();
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, typeOracle);
    stob.addRootType(logger, typeOracle.getType(AbstractSerializableTypes.IFoo.class
        .getCanonicalName()));
    stob.addRootType(logger, typeOracle.getType(AbstractSerializableTypes.AbstractClass.class
        .getCanonicalName()));

    try {
      stob.build(logger);
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      // Should get here
    }
  }

  /**
   * Tests a hierarchy blending various serializable and non-serializable types.
   */
  public void testProblemReporting() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("public interface TopInterface {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("TopInterface", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class AbstractSerializable implements\n");
      code.append("    Serializable, TopInterface {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("AbstractSerializable", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface PureAbstractSerializable extends \n");
      code.append("    Serializable, TopInterface {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("PureAbstractSerializable", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public abstract class PureAbstractClass implements \n");
      code.append("    PureAbstractSerializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("PureAbstractClass", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public abstract class PureAbstractClassTwo extends \n");
      code.append("    PureAbstractClass {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("PureAbstractClassTwo", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class ConcreteNonSerializable implements\n");
      code.append("    TopInterface {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ConcreteNonSerializable", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class ConcreteSerializable implements\n");
      code.append("    Serializable, TopInterface {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ConcreteSerializable", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SubSerializable extends\n");
      code.append("    ConcreteNonSerializable implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SubSerializable", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class ConcreteBadCtor extends\n");
      code.append("    AbstractSerializable {\n");
      code.append("  public ConcreteBadCtor(int i) {\n");
      code.append("  }\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ConcreteBadCtor", code));
    }
    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, to);
    JClassType topInterface = to.getType("TopInterface");
    stob.addRootType(logger, topInterface);

    ProblemReport problems = new ProblemReport();
    assertTrue("TopInterface should be (partially) serializable", stob.computeTypeInstantiability(
        logger, topInterface, null, problems).hasInstantiableSubtypes());
    assertTrue("TopInterface should be a serializable type", problems.getProblemsForType(
        topInterface).isEmpty());
    assertTrue("AbstractSerializable should not be reported on", problems.getProblemsForType(
        to.getType("AbstractSerializable")).isEmpty());
    assertTrue("PureAbstractSerializable should not be reported on", problems.getProblemsForType(
        to.getType("PureAbstractSerializable")).isEmpty());
    assertTrue("PureAbstractClass should not be reported on", problems.getProblemsForType(
        to.getType("PureAbstractClass")).isEmpty());
    assertFalse("ConcreteBadCtor should not be a serializable type", problems.getProblemsForType(
        to.getType("ConcreteBadCtor")).isEmpty());
    assertFalse("ConcreteNonSerializable should not be a serializable type", problems
        .getProblemsForType(to.getType("ConcreteNonSerializable")).isEmpty());
    assertTrue("ConcreteSerializable should be a serializable type", problems.getProblemsForType(
        to.getType("ConcreteSerializable")).isEmpty());
    assertTrue("SubSerializable should be a serializable type", problems.getProblemsForType(
        to.getType("SubSerializable")).isEmpty());

    problems = new ProblemReport();
    assertFalse("PureAbstractClass should have no possible concrete implementation", stob
        .computeTypeInstantiability(logger, to.getType("PureAbstractClass"), null, problems)
        .hasInstantiableSubtypes());
    assertTrue("PureAbstractClass should have a problem entry as the tested class",
        null != problems.getProblemsForType(to.getType("PureAbstractClass")));

    problems = new ProblemReport();
    assertFalse("PureAbstractSerializable should have no possible concrete implementation", stob
        .computeTypeInstantiability(logger, to.getType("PureAbstractSerializable"), null, problems)
        .hasInstantiableSubtypes());
    assertFalse("PureAbstractSerializable should have a problem entry", problems
        .getProblemsForType(to.getType("PureAbstractSerializable")).isEmpty());
    assertTrue("PureAbstractClassTwo should not have a problem entry as the middle class", problems
        .getProblemsForType(to.getType("PureAbstractClassTwo")).isEmpty());
    assertTrue("PureAbstractClassTwo should not have an auxiliary entry as the middle class",
        problems.getAuxiliaryMessagesForType(to.getType("PureAbstractClassTwo")).isEmpty());
    assertTrue("PureAbstractClass should not have a problem entry as the child class", problems
        .getProblemsForType(to.getType("PureAbstractClass")).isEmpty());
    assertTrue("PureAbstractClass should not have an auxiliary entry as the child class", problems
        .getAuxiliaryMessagesForType(to.getType("PureAbstractClass")).isEmpty());
  }

  /**
   * Tests that adding a raw collection as a root type pulls in the world.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testRawCollection() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("import java.util.Collection;\n");
      code.append("public interface List<T> extends Serializable, Collection<T> {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("List", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("import java.util.Collection;\n");
      code.append("public class LinkedList<T> implements List<T> {\n");
      code.append("  T head;");
      code.append("}\n");
      resources.add(new StaticJavaResource("LinkedList", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("import java.util.Collection;\n");
      code.append("public class RandomClass implements Serializable {\n");
      // TODO(mmendez): Lex, this should fail, but your fix will allow it if
      // we get here from a raw collection.
      // code.append(" Object obj;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("RandomClass", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType list = to.getType("List").isGenericType();
    JGenericType linkedList = to.getType("LinkedList").isGenericType();
    JClassType randomClass = to.getType("RandomClass");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, list.getRawType());
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, linkedList.getRawType());
    assertInstantiable(so, randomClass);
  }

  /**
   * Tests that raw type with type parameters that are instantiable are
   * themselves instantiable.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testRawTypes() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T extends SerializableClass> implements Serializable {\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class SerializableClass implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("SerializableClass", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JRawType rawA = a.getRawType();

    JClassType serializableClass = to.getType("SerializableClass");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, rawA);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a.getRawType());
    assertSerializableTypes(so, rawA, serializableClass);
  }

  /**
   * Tests that a type paramter that occurs within its bounds will not result in
   * infinite recursion.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testRootTypeParameterWithSelfInBounds() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<Ta extends A<Ta>> implements Serializable {\n");
      code.append("  Ta ta;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JClassType rawA = a.getRawType();
    JTypeParameter ta = a.getTypeParameters()[0];

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, ta);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, rawA);
  }

  /**
   * Tests that type String[][] also pulls in String[].
   */
  public void testStringArrayArray() throws NotFoundException, UnableToCompleteException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Data implements Serializable {\n");
      code.append("  String justOneString;");
      code.append("  String[][] stringsGalore;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Data", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType data = to.getType("Data");
    JClassType string = to.getType(String.class.getCanonicalName());
    JArrayType stringArray = to.getArrayType(string);
    JArrayType stringArrayArray = to.getArrayType(stringArray);

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, data);

    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, data, string, stringArray, stringArrayArray);
    assertInstantiable(so, data);
    assertInstantiable(so, stringArrayArray);
    assertInstantiable(so, stringArray);
  }

  /*
   * Tests the isAssignable test for deciding whether a subclass should be
   * pulled in.
   */
  public void testSubclassIsAssignable() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B extends A<String> implements Serializable {\n");
      code.append("  Object o;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C extends A<Ser> implements Serializable {\n");
      // TODO: rejecting Ser requires a better pruner in STOB
      // code.append(" Ser ser;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JClassType b = to.getType("B");
    JClassType c = to.getType("C");
    JClassType ser = to.getType("Ser");

    JClassType javaLangString = to.getType(String.class.getName());
    JParameterizedType aOfString = to.getParameterizedType(a, makeArray(javaLangString));
    SerializableTypeOracleBuilder stob = createSerializableTypeOracleBuilder(logger, to);
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
  public void testSubclassWithNewInstantiableTypeParameters() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T extends C> extends A {\n");
      code.append("  T c;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType a = to.getType("A");
    JRawType rawB = to.getType("B").isGenericType().getRawType();
    JClassType c = to.getType("C");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
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
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public interface Intf<T> extends Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Bar<T> implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Bar", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Foo<T extends Ser> extends Bar<T> implements Intf<String> {\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Foo", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class Ser implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Ser", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType intf = to.getType("Intf").isGenericType();
    JClassType foo = to.getType("Foo");
    JClassType bar = to.getType("Bar");
    JClassType intfOfString =
        to.getParameterizedType(intf, new JClassType[] {to.getType(String.class.getName())});
    JClassType ser = to.getType("Ser");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, intfOfString);
    SerializableTypeOracle so = sob.build(logger);

    /*
     * TODO(spoon): should also check that Intf<String> has instantiable
     * subclasses; currently the APIs for STOB and STO do not make this possible
     * to test
     */
    assertInstantiable(so, ser);
    assertSerializableTypes(so, foo.getErasedType(), bar.getErasedType(), ser.getErasedType());
  }

  /**
   * Tests subtypes that introduce new uninstantiable type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testSubclassWithNewUninstantiableTypeParameters() throws UnableToCompleteException,
      NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B<T> extends A {\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType a = to.getType("A");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, a);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, a);
    assertSerializableTypes(so, a);
  }

  /**
   * Tests that STOB skips transient fields.
   */
  public void testTransient() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);
    addCustomGwtTransient(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import com.google.gwt.user.client.rpc.GwtTransient;\n");
      code.append("import java.io.Serializable;\n");
      code.append("public class A implements Serializable {\n");
      code.append("  transient ServerOnly1 serverOnly1;\n");
      code.append("  @GwtTransient ServerOnly2 serverOnly2;\n");
      code.append("  @com.google.gwt.user.rebind.rpc.GwtTransient ServerOnly3 serverOnly3;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("class ServerOnly1 implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ServerOnly1", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("class ServerOnly2 implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ServerOnly2", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("class ServerOnly3 implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ServerOnly3", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType a = to.getType("A");
    JClassType serverOnly1 = to.getType("ServerOnly1");
    JClassType serverOnly2 = to.getType("ServerOnly2");
    JClassType serverOnly3 = to.getType("ServerOnly3");

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, a);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, a);
    assertInstantiable(so, a);
    assertNotFieldSerializable(so, serverOnly1);
    assertNotFieldSerializable(so, serverOnly2);
    assertNotFieldSerializable(so, serverOnly3);
  }

  /**
   * Miscellaneous direct tests of {@link TypeConstrainer}.
   * 
   * @throws NotFoundException
   */
  public void testTypeConstrainer() throws NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("public interface Intf1 {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf1", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public interface Intf2 {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf2", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public interface Intf3 {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Intf3", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class Implements12 implements Intf1, Intf2 {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Implements12", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class ImplementsNeither {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("ImplementsNeither", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class Sup {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Sup", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class Sub extends Sup {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Sub", code));
    }
    {
      StringBuilder code = new StringBuilder();
      code.append("public class Holder<T> {\n");
      code.append("  T x;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("Holder", code));
    }
    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JClassType intf1 = to.getType("Intf1");
    JClassType intf2 = to.getType("Intf2");
    JClassType intf3 = to.getType("Intf3");
    JClassType implements12 = to.getType("Implements12");
    JClassType implementsNeither = to.getType("ImplementsNeither");
    JClassType string = to.getType(String.class.getCanonicalName());
    JClassType sub = to.getType("Sub");
    JClassType sup = to.getType("Sup");
    JGenericType holder = to.getType("Holder").isGenericType();

    JClassType arrayOfInt = to.getArrayType(JPrimitiveType.INT);
    JClassType arrayOfFloat = to.getArrayType(JPrimitiveType.FLOAT);
    JClassType arrayOfString = to.getArrayType(string);
    JClassType arrayOfWildExtString =
        to.getArrayType(to.getWildcardType(BoundType.EXTENDS, string));
    JClassType holderOfString =
        to.getParameterizedType(holder, makeArray(to.getWildcardType(BoundType.EXTENDS, string)));
    JClassType holderOfSub =
        to.getParameterizedType(holder, makeArray(to.getWildcardType(BoundType.EXTENDS, sub)));
    JClassType holderOfSup =
        to.getParameterizedType(holder, makeArray(to.getWildcardType(BoundType.EXTENDS, sup)));

    TypeConstrainer typeConstrainer = new TypeConstrainer(to);
    Map<JTypeParameter, JClassType> emptyConstraints = new HashMap<JTypeParameter, JClassType>();

    assertTrue(typeConstrainer.typesMatch(intf1, intf2, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(intf1, intf3, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(implements12, intf1, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(implements12, intf3, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(implementsNeither, intf1, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(to.getJavaLangObject(), arrayOfString, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(arrayOfString, to.getJavaLangObject(), emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(sub, sup, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(sub, sub, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(arrayOfFloat, arrayOfFloat, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(arrayOfFloat, arrayOfInt, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(arrayOfFloat, arrayOfString, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(arrayOfString, arrayOfString, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(arrayOfString, arrayOfWildExtString, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(holderOfSub, holderOfSup, emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(holderOfSub, holderOfString, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(holder.getRawType(), holderOfSub, emptyConstraints));
    assertTrue(typeConstrainer.typesMatch(holderOfSub, holder.getRawType(), emptyConstraints));
    assertFalse(typeConstrainer.typesMatch(holder.getRawType(), intf1, emptyConstraints));

    assertTrue(emptyConstraints.isEmpty());
  }

  /**
   * Tests root types that have type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testTypeParametersInRootTypes1() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class A<T> implements Serializable {\n");
      code.append("  T t;\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C<U extends B> {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JRawType rawA = a.getRawType();
    JClassType b = to.getType("B");
    JGenericType c = to.getType("C").isGenericType();

    JTypeParameter typeParam = c.getTypeParameters()[0];

    JParameterizedType parameterizedType = to.getParameterizedType(a, new JClassType[] {typeParam});
    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, parameterizedType);
    SerializableTypeOracle so = sob.build(logger);

    assertInstantiable(so, rawA);
    assertInstantiable(so, b);
    assertSerializableTypes(so, rawA, b);
  }

  /**
   * Tests root types that <em>are</em> type parameters.
   * 
   * @throws UnableToCompleteException
   * @throws NotFoundException
   */
  public void testTypeParametersInRootTypes2() throws UnableToCompleteException, NotFoundException {
    Set<Resource> resources = new HashSet<Resource>();
    addStandardClasses(resources);

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public abstract class A<U extends B> implements Serializable {\n");
      code.append("  <V extends C>  V getFoo() { return null; }\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("A", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class B implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("B", code));
    }

    {
      StringBuilder code = new StringBuilder();
      code.append("import java.io.Serializable;\n");
      code.append("public class C implements Serializable {\n");
      code.append("}\n");
      resources.add(new StaticJavaResource("C", code));
    }

    TreeLogger logger = createLogger();
    TypeOracle to = TypeOracleTestingUtils.buildTypeOracle(logger, resources);

    JGenericType a = to.getType("A").isGenericType();
    JClassType b = to.getType("B");
    JClassType c = to.getType("C");
    JTypeParameter u = a.getTypeParameters()[0];
    JTypeParameter v = a.getMethod("getFoo", makeArray()).getReturnType().isTypeParameter();

    SerializableTypeOracleBuilder sob = createSerializableTypeOracleBuilder(logger, to);
    sob.addRootType(logger, u);
    sob.addRootType(logger, v);
    SerializableTypeOracle so = sob.build(logger);

    assertSerializableTypes(so, b, c);
    assertInstantiable(so, b);
    assertInstantiable(so, c);
    assertNotInstantiableOrFieldSerializable(so, a.getRawType());
  }

  private JClassType[] makeArray(JClassType... elements) {
    return elements;
  }
}
