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
package com.google.gwt.core.ext.typeinfo;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.test.Base;
import com.google.gwt.core.ext.typeinfo.test.Derived;
import com.google.gwt.core.ext.typeinfo.test.GenericClass;
import com.google.gwt.core.ext.typeinfo.test.MyCustomList;
import com.google.gwt.core.ext.typeinfo.test.MyIntegerList;
import com.google.gwt.core.ext.typeinfo.test.MyList;
import com.google.gwt.core.ext.typeinfo.test.GenericClass.GenericInnerClass;
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

    public JType getSubstitution(JType type) {
      return type.getSubstitutedType(parameterizedType);
    }
  }

  private final JClassType integerType;
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.core.ext.typeinfo.TypeOracleTest");

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
        new JClassType[] {integerType});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_LowerBoundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType lowerBoundWildcard = oracle.getWildcardType(new JLowerBound(
        new JClassType[] {integerType}));

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[] {lowerBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_UnboundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType upperBoundWildcard = oracle.getWildcardType(new JUpperBound(
        new JClassType[] {oracle.getJavaLangObject()}));

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[] {upperBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  public void testGenericClass_UpperBoundWildcard() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericType = getGenericTestType();
    JWildcardType upperBoundWildcard = oracle.getWildcardType(new JUpperBound(
        new JClassType[] {integerType}));

    JClassType type = oracle.getParameterizedType(genericType,
        new JClassType[] {upperBoundWildcard});
    JParameterizedType parameterizedType = type.isParameterized();

    validateTypeSubstitution(genericType, parameterizedType,
        new ParameterizedSubstitution(parameterizedType));
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JParameterizedType#getEnclosingType()}.
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
  public void testGetNestedType() {
    // TODO: complete this test method
  }

  /**
   * Test method for {@link
   * com.google.gwt.core.ext.typeinfo.JParameterizedType#getNestedTypes()}.
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
        genericType, new JClassType[] {integerType});
    JClassType[] actualSubtypes = parameterizedType.getSubtypes();

    JGenericType myCustomListType = oracle.getType(MyCustomList.class.getName()).isGenericType();
    JParameterizedType parameterizedMyCustomList = oracle.getParameterizedType(
        myCustomListType, new JClassType[] {
            oracle.getWildcardType(new JUpperBound(
                oracle.getType(Serializable.class.getName()))), integerType});
    JClassType[] expected = {
        oracle.getType(MyIntegerList.class.getName()),
        parameterizedMyCustomList};

    validateEquals(oracle, expected, actualSubtypes);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JParameterizedType#isAssignableFrom(JClassType)}.
   */
  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    // Check that raw types can be assigned to a parameterized type
    JParameterizedType testType = getTestType();
    JClassType rawType = testType.getRawType();

    assertTrue(testType.isAssignableFrom(rawType));

    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericList = (JGenericType) oracle.getType(List.class.getName());

    JWildcardType unboundWildcard = oracle.getWildcardType(new JUpperBound(
        oracle.getJavaLangObject()));
    JWildcardType numUpperBoundWildcard = oracle.getWildcardType(new JUpperBound(
        oracle.getType(Number.class.getName())));

    // List<?> should be assignable from List<? extends Number>
    JParameterizedType unboundList = oracle.getParameterizedType(genericList,
        new JClassType[] {unboundWildcard});
    JParameterizedType numUpperBoundList = oracle.getParameterizedType(
        genericList, new JClassType[] {numUpperBoundWildcard});
    assertTrue(unboundList.isAssignableFrom(numUpperBoundList));
    assertFalse(unboundList.isAssignableTo(numUpperBoundList));

    assertFalse(numUpperBoundList.isAssignableFrom(unboundList));
    assertTrue(numUpperBoundList.isAssignableTo(unboundList));

    // List<? extends Number> should be assignable from List<? extends Integer>
    JWildcardType intUpperBoundWildcard = oracle.getWildcardType(new JUpperBound(
        integerType));

    JParameterizedType intUpperBoundList = oracle.getParameterizedType(
        genericList, new JClassType[] {intUpperBoundWildcard});
    assertTrue(numUpperBoundList.isAssignableFrom(intUpperBoundList));
    assertFalse(numUpperBoundList.isAssignableTo(intUpperBoundList));

    assertFalse(intUpperBoundList.isAssignableFrom(numUpperBoundList));
    assertTrue(intUpperBoundList.isAssignableTo(numUpperBoundList));

    // List<? super Integer> should be assignable from List<? super Number>
    JWildcardType numLowerBoundWildcard = oracle.getWildcardType(new JLowerBound(
        oracle.getType(Number.class.getName())));
    JWildcardType intLowerBoundWildcard = oracle.getWildcardType(new JLowerBound(
        integerType));

    JParameterizedType numLowerBoundList = oracle.getParameterizedType(
        genericList, new JClassType[] {numLowerBoundWildcard});
    JParameterizedType intLowerBoundList = oracle.getParameterizedType(
        genericList, new JClassType[] {intLowerBoundWildcard});

    assertTrue(intLowerBoundList.isAssignableFrom(numLowerBoundList));
    assertFalse(intLowerBoundList.isAssignableTo(numLowerBoundList));
    assertFalse(numLowerBoundList.isAssignableFrom(intLowerBoundList));
    assertTrue(numLowerBoundList.isAssignableTo(intLowerBoundList));
  }

  public void testOverridableMethods_Base() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(Base.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    List<JMethod> expected = new ArrayList<JMethod>(
        Arrays.asList(type.getOverloads("m")));
    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(type.getOverridableMethods()));

    validateOverridableMethods(expected, actual, true);
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
        new JType[] {paramType.getTypeArgs()[0]}));

    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(type.getOverridableMethods()));

    validateOverridableMethods(expected, actual, true);
  }

  public void testOverridableMethods_Derived_Integer() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType type = oracle.getType(Derived.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    JParameterizedType paramType = oracle.getParameterizedType(genericType,
        new JClassType[] {integerType});

    List<JMethod> expected = new ArrayList<JMethod>();
    expected.addAll(Arrays.asList(paramType.getOverloads("m")));

    List<JMethod> actual = new ArrayList<JMethod>(
        Arrays.asList(paramType.getOverridableMethods()));

    validateOverridableMethods(expected, actual, true);
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
        new JClassType[] {integerType});
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
        innerGenericClass, cut, new JClassType[] {booleanType});

    return parameterizedInnerClass;
  }

  private void validateOverridableMethods(List<JMethod> expected,
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

  @Override
  public void testIsAssignableTo() throws NotFoundException {
    // This is covered as part of testIsAssignableFrom
  }
}
