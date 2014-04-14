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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.typemodel.test.Base;
import com.google.gwt.dev.javac.typemodel.test.Derived;
import com.google.gwt.dev.javac.typemodel.test.GenericClass;
import com.google.gwt.dev.javac.typemodel.test.GenericClass.GenericInnerClass;
import com.google.gwt.dev.javac.typemodel.test.MyCustomList;
import com.google.gwt.dev.javac.typemodel.test.MyIntegerList;
import com.google.gwt.dev.javac.typemodel.test.MyList;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test for {@link JParameterizedType}.
 */
public class JParameterizedTypeTest extends JDelegatingClassTypeTestBase {
  /**
   * Helper for verifying parameterized substitutions.
   */
  static class ParameterizedSubstitution implements Substitution {
    private final JParameterizedType parameterizedType;

    public ParameterizedSubstitution(JParameterizedType parameterizedType) {
      this.parameterizedType = parameterizedType;
    }

    @Override
    public JClassType getSubstitution(JClassType type) {
      return type.getSubstitutedType(parameterizedType);
    }
  }

  private static TreeLogger createTreeLogger() {
    final boolean logToConsole = false;
    return logToConsole ? new PrintWriterTreeLogger() : TreeLogger.NULL;
  }

  private final JClassType integerType;
  private final ModuleContext moduleContext = new ModuleContext(createTreeLogger(),
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JParameterizedTypeTest() throws UnableToCompleteException,
      NotFoundException {
    integerType = moduleContext.getOracle().getType(Integer.class.getName());
  }

  @Override
  public void testFindNestedType() {
    // TODO: complete this test method
  }

  /**
   * Checks that GenericClass<Integer> ends up with the correct substitutions.
   *
   * @throws NotFoundException
   */
  public void testGenericClass_Integer() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[]{integerType});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_LowerBoundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType lowerBoundWildcard = oracle.getWildcardType(BoundType.SUPER,
        integerType);

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[]{lowerBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_UnboundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType upperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, oracle.getJavaLangObject());

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[]{upperBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_UpperBoundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType upperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, integerType);

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[]{upperBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JParameterizedType#getEnclosingType()}
   * .
   *
   * @throws NotFoundException
   */
  @Override
  public void testGetEnclosingType() throws NotFoundException {
    JParameterizedType testType = getTestType();

    // Check that GenericClass<Integer> is not nested
    assertNull(testType.getEnclosingType());

    /*
     * Check that GenericClass<Integer>.GenericInnerClass<Boolean> has //
     * GenericClass<Integer> as its // enclosing type
     */
    JParameterizedType parameterizedInnerClass = getInnerParameterizedType();

    assertEquals(testType, parameterizedInnerClass.getEnclosingType());
  }

  @Override
  public void testGetInheritableMethods() throws NotFoundException {
    // Tested via testOverridableMethods_Base, testOverridableMethods_Derived,
    // testOverridableMethods_Derived_Integer
  }

  @Override
  public void testGetNestedType() {
    // TODO: complete this test method
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JParameterizedType#getNestedTypes()}
   * .
   *
   * @throws NotFoundException
   */
  @Override
  public void testGetNestedTypes() throws NotFoundException {
    JParameterizedType cut = getTestType();
    JParameterizedType innerCut = getInnerParameterizedType();

    // Check that inner parameterized types don't appear in the parent's nested
    // type set
    assertEquals(0, cut.getNestedTypes().length);

    try {
      cut.getNestedType(innerCut.getSimpleSourceName());
      fail("Type " + cut.getQualifiedSourceName()
          + " should report that it has no nested types");
    } catch (NotFoundException ex) {
      // Expected to get here
    }
  }

  @Override
  public void testGetOverridableMethods() throws NotFoundException {
    // Tested via testOverridableMethods_Base, testOverridableMethods_Derived,
    // testOverridableMethods_Derived_Integer
  }

  /**
   * Tests the subtypes of MyList<Integer>. These should be:
   * <ul>
   * <li><code>MyIntegerList</code></li>
   * <li><code>MyCustomList&lt;? extends Serializable, Integer&gt;</code></li>
   * </ul>
   *
   * @throws NotFoundException
   */
  @Override
  public void testGetSubtypes() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = oracle.getType(MyList.class.getName()).isGenericType();

    JParameterizedType parameterizedType = oracle.getParameterizedType(
        genericType, new JClassType[]{integerType});
    JClassType[] actualSubtypes = parameterizedType.getSubtypes();

    JGenericType myCustomListType = oracle.getType(MyCustomList.class.getName()).isGenericType();
    JParameterizedType parameterizedMyCustomList = oracle.getParameterizedType(
        myCustomListType, new JClassType[]{
            oracle.getWildcardType(BoundType.EXTENDS,
                oracle.getType(Serializable.class.getName())), integerType});
    JClassType[] expected = {
        oracle.getType(MyIntegerList.class.getName()),
        parameterizedMyCustomList};

    validateEquals(expected, actualSubtypes);
  }

  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    assertType("List").isAssignableFrom("List");
    assertType("List").isAssignableFrom("List<?>");
    assertType("List").isAssignableFrom("List<? extends Number>");
    assertType("List").isAssignableFrom("List<String>");
    assertType("List").isAssignableFrom("ExtendsRawList");

    assertType("List<?>").isAssignableFrom("List");
    assertType("List<?>").isAssignableFrom("List<?>");
    assertType("List<?>").isAssignableFrom("List<? extends Number>");
    assertType("List<?>").isAssignableFrom("List<String>");
    assertType("List<?>").isAssignableFrom("ExtendsRawList");

    assertType("List<? extends Number>").isAssignableFrom("List");
    assertType("List<? extends Number>").isAssignableFrom("List<Integer>");
    assertType("List<? extends Number>").isAssignableFrom("List<? extends Integer>");
    assertType("List<? extends Number>").isNOTAssignableFrom("List<?>");
    assertType("List<? extends Number>").isNOTAssignableFrom("List<Object>");

    assertType("List<? extends Integer>").isAssignableFrom("List<? extends Integer>");
    assertType("List<? extends Integer>").isNOTAssignableFrom("List<? extends Number>");

    assertType("List<? super Number>").isAssignableFrom("List");
    assertType("List<? super Number>").isAssignableFrom("List<Object>");
    assertType("List<? super Number>").isAssignableFrom("List<Number>");
    assertType("List<? super Number>").isAssignableFrom("List<? super Number>");
    assertType("List<? super Number>").isNOTAssignableFrom("List<Integer>");
    assertType("List<? super Number>").isNOTAssignableFrom("List<String>");
    assertType("List<? super Number>").isNOTAssignableFrom("List<?>");
    assertType("List<? super Number>").isNOTAssignableFrom("List<? super Integer>");

    assertType("List<? super Integer>").isAssignableFrom("List<? super Object>");
    assertType("List<? super Integer>").isAssignableFrom("List<? super Number>");

    assertType("List<Object>").isAssignableFrom("List");
    assertType("List<Object>").isAssignableFrom("List<Object>");
    assertType("List<String>").isAssignableFrom("ExtendsRawList");
    assertType("List<Object>").isNOTAssignableFrom("List<String>");
    assertType("List<String>").isNOTAssignableFrom("List<Object>");

    assertType("List<List>").isAssignableFrom("List");
    assertType("List<List>").isAssignableFrom("List<List>");
    assertType("List<List>").isNOTAssignableFrom("List<List<?>>");
    assertType("List<List>").isNOTAssignableFrom("List<List<String>>");
    assertType("List<List>").isNOTAssignableFrom("List<? extends List<String>>");

    assertType("List<List<?>>").isAssignableFrom("List");
    assertType("List<List<?>>").isAssignableFrom("List<List<?>>");
    assertType("List<List<?>>").isAssignableFrom("List<List<? extends Object>>");
    assertType("List<List<?>>").isNOTAssignableFrom("List<List>");
    assertType("List<List<?>>").isNOTAssignableFrom("List<List<String>>");

    assertType("List<List<String>>").isAssignableFrom("List");
    assertType("List<List<String>>").isAssignableFrom("List<List<String>>");
    assertType("List<List<String>>").isNOTAssignableFrom("List<List>");
    assertType("List<List<String>>").isNOTAssignableFrom("List<List<?>>");
    assertType("List<List<String>>").isNOTAssignableFrom("List<List<Object>>");

    assertType("List<Collection<String>>").isNOTAssignableFrom("List<List<String>>");

    assertType("List<? extends Collection<String>>").isAssignableFrom("List<List<String>>");
    assertType("List<List<? extends Number>>").isNOTAssignableFrom("List<List<Integer>>");

    assertType("Map<?, ?>").isAssignableFrom("Map");
    assertType("Map<?, ?>").isAssignableFrom("Map<String, String>");
    assertType("Map<?, ?>").isAssignableFrom("Map<String, Integer>");

    assertType("Map<?, String>").isAssignableFrom("Map");
    assertType("Map<?, String>").isAssignableFrom("Map<String, String>");
    assertType("Map<?, String>").isNOTAssignableFrom("Map<String, Integer>");

    assertType("Map<String, String>").isAssignableFrom("Map");
    assertType("Map<String, String>").isAssignableFrom("Map<String, String>");
    assertType("Map<String, String>").isNOTAssignableFrom("Map<String, Integer>");
  }

  @Override
  public void testIsAssignableTo() throws NotFoundException {
    // This is covered as part of testIsAssignableFrom
  }

  public void testOverridableMethods_Base() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(Base.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    List<JMethod> expected = new ArrayList<JMethod>(
        Arrays.asList(type.getOverloads("m")));
    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(type.getInheritableMethods()));
    validateInheritableOrOverridableMethods(expected, actual, true);

    actual = new ArrayList<JMethod>(Arrays.asList(type.getOverridableMethods()));
    validateInheritableOrOverridableMethods(expected, actual, true);
  }

  public void testOverridableMethods_Derived() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(Derived.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    JClassType supertype = type.getSuperclass();
    JParameterizedType paramType = supertype.isParameterized();
    // JGenericType genericSuperType = paramType.getBaseType().isGenericType();
    assertNotNull(paramType);

    List<JMethod> expected = new ArrayList<JMethod>();
    expected.addAll(Arrays.asList(genericType.getOverloads("m")));
    expected.add(paramType.getMethod("m",
        new JType[]{paramType.getTypeArgs()[0]}));

    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(type.getInheritableMethods()));
    validateInheritableOrOverridableMethods(expected, actual, true);

    actual = new ArrayList<JMethod>(Arrays.asList(type.getOverridableMethods()));
    validateInheritableOrOverridableMethods(expected, actual, true);
  }

  public void testOverridableMethods_Derived_Integer() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(Derived.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    JParameterizedType paramType = oracle.getParameterizedType(genericType,
        new JClassType[]{integerType});

    List<JMethod> expected = new ArrayList<JMethod>();
    expected.addAll(Arrays.asList(paramType.getOverloads("m")));

    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(paramType.getInheritableMethods()));
    validateInheritableOrOverridableMethods(expected, actual, true);

    actual = new ArrayList<JMethod>(
        Arrays.asList(paramType.getOverridableMethods()));

    validateInheritableOrOverridableMethods(expected, actual, true);
  }

  /**
   * Returns the <code>TypeOracle</code> type for {@link GenericClass}.
   *
   * @return <code>TypeOracle</code> type for {@link GenericClass}
   * @throws NotFoundException
   */
  protected JGenericType getGenericTestType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(GenericClass.class.getName());
    assertNotNull(type.isGenericType());
    return type.isGenericType();
  }

  @Override
  protected Substitution getSubstitution() throws NotFoundException {
    return new ParameterizedSubstitution(getTestType());
  }

  @Override
  protected JParameterizedType getTestType() throws NotFoundException {
    JGenericType type = getGenericTestType();

    return moduleContext.getOracle().getParameterizedType(type, null,
        new JClassType[]{integerType});
  }

  /**
   * Returns the type for GenericClass<Integer>.GenericInnerClass<Boolean>.
   *
   * @throws NotFoundException
   * @return type for GenericClass<Integer>.GenericInnerClass<Boolean>
   */
  private JParameterizedType getInnerParameterizedType()
      throws NotFoundException {
    JParameterizedType cut = getTestType();
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType innerGenericClass = cut.getBaseType().getNestedType(
        GenericInnerClass.class.getSimpleName()).isGenericType();

    JClassType booleanType = oracle.getType(Boolean.class.getName());

    /*
     * Check that GenericClass<Integer>.GenericInnerClass<Boolean> has
     * GenericClass<Integer> as its enclosing type
     */
    //
    JParameterizedType parameterizedInnerClass = oracle.getParameterizedType(
        innerGenericClass, cut, new JClassType[]{booleanType});

    return parameterizedInnerClass;
  }

  private void validateInheritableOrOverridableMethods(List<JMethod> expected,
      List<JMethod> actual, boolean addObjectMethods) {
    Set<JMethod> expectedMethods = new HashSet<JMethod>();
    expectedMethods.addAll(expected);
    if (addObjectMethods) {
      TypeOracle oracle = moduleContext.getOracle();
      expectedMethods.addAll(Arrays.asList(oracle.getJavaLangObject().getMethods()));
    }

    for (JMethod method : actual) {
      assertEquals("Method " + method.getReadableDeclaration() + " from type "
          + method.getEnclosingType().getQualifiedSourceName()
          + " was not expected", true, expectedMethods.remove(method));
    }

    assertTrue(expectedMethods.isEmpty());
  }

  private static TypeAssignabilityAsserter assertType(final String type) {
    return new TypeAssignabilityAsserter(type);
  }

  // TODO(goktug): make this utilized in more tests
  private static class TypeAssignabilityAsserter {
    private String type;

    public TypeAssignabilityAsserter(String type) {
      this.type = type;
    }

    public void isAssignableFrom(String from) {
      assertTrue(isAssignableFromTo(from, type));
    }

    public void isNOTAssignableFrom(String from) {
      assertFalse(isAssignableFromTo(from, type));
    }

    private boolean isAssignableFromTo(final String fromType, final String toType) {

      // Compile the code snippet to extract the types
      final String helperClassName = "ParameterizedTestHelper";

      Set<Resource> resources = new HashSet<Resource>();
      resources.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
      resources.add(JavaResourceBase.createMockJavaResource(helperClassName,
          "import java.util.*;",
          "public class " + helperClassName + " {",
          fromType + " from() { return null; }",
          toType + " to() { return null; }",
          "}"));
      resources.add(JavaResourceBase.createMockJavaResource("ExtendsRawComparable",
          "public interface ExtendsRawComparable extends Comparable {}"));
      resources.add(JavaResourceBase.createMockJavaResource("NonRecursiveComparable",
          "public interface NonRecursiveComparable extends Comparable<Number> {}"));
      resources.add(JavaResourceBase.createMockJavaResource("ExtendsRawList", "import java.util.*;",
          "public interface ExtendsRawList extends List {}"));

      JClassType to;
      JClassType from;
      try {
        // Compile and get helper type
        JClassType helperType = (JClassType) TypeOracleTestingUtils.buildTypeOracle(
            createTreeLogger(), resources).getType(helperClassName);

        to = (JClassType) helperType.getMethod("to", TypeOracle.NO_JCLASSES).getReturnType();
        from = (JClassType) helperType.getMethod("from", TypeOracle.NO_JCLASSES).getReturnType();
      } catch (NotFoundException e) {
        throw new AssertionError("Possible compilation error. Enable logToConsole for more info");
      }

      // Check the assignability
      boolean assignableFrom = to.isAssignableFrom(from);
      boolean assignableTo = from.isAssignableTo(to);
      assertEquals(assignableFrom, assignableTo);
      return assignableFrom;
    }
  }
}
