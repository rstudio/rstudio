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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.dev.javac.typemodel.test.Base;
import com.google.gwt.dev.javac.typemodel.test.Derived;
import com.google.gwt.dev.javac.typemodel.test.ExtendsRawGenericClass;
import com.google.gwt.dev.javac.typemodel.test.GenericClass;
import com.google.gwt.dev.javac.typemodel.test.MyCustomList;
import com.google.gwt.dev.javac.typemodel.test.MyIntegerList;
import com.google.gwt.dev.javac.typemodel.test.MyList;
import com.google.gwt.dev.javac.typemodel.test.GenericClass.GenericInnerClass;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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

    public JClassType getSubstitution(JClassType type) {
      return type.getSubstitutedType(parameterizedType);
    }
  }

  private final JClassType integerType;
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
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

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JParameterizedType#isAssignableFrom(JClassType)}
   * .
   */
  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    // Check that raw types can be assigned to a parameterized type
    JParameterizedType testType = getTestType();
    JClassType rawType = testType.getRawType();

    assertTrue(testType.isAssignableFrom(rawType));

    TypeOracle oracle = moduleContext.getOracle();
    JGenericType genericList = (JGenericType) oracle.getType(List.class.getName());

    // ?
    JWildcardType unboundWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        oracle.getJavaLangObject());

    // ? extends Number
    JWildcardType numUpperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, oracle.getType(Number.class.getName()));

    // List<?>
    JParameterizedType unboundList = oracle.getParameterizedType(genericList,
        new JClassType[]{unboundWildcard});

    // List<? extends Number>
    JParameterizedType listOfExtendsNumber = oracle.getParameterizedType(
        genericList, new JClassType[]{numUpperBoundWildcard});

    // List<?> should be assignable from List<? extends Number>
    assertTrue(unboundList.isAssignableFrom(listOfExtendsNumber));
    assertFalse(unboundList.isAssignableTo(listOfExtendsNumber));

    assertFalse(listOfExtendsNumber.isAssignableFrom(unboundList));
    assertTrue(listOfExtendsNumber.isAssignableTo(unboundList));

    // ? extends Integer
    JWildcardType intUpperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, integerType);

    // List<? extends Integer>
    JParameterizedType listOfExtendsInteger = oracle.getParameterizedType(
        genericList, new JClassType[]{intUpperBoundWildcard});

    // List<? extends Number> should be assignable from List<? extends Integer>
    assertTrue(listOfExtendsNumber.isAssignableFrom(listOfExtendsInteger));
    assertFalse(listOfExtendsNumber.isAssignableTo(listOfExtendsInteger));

    assertFalse(listOfExtendsInteger.isAssignableFrom(listOfExtendsNumber));
    assertTrue(listOfExtendsInteger.isAssignableTo(listOfExtendsNumber));

    // List<? super Integer> should be assignable from List<? super Number>
    JWildcardType numLowerBoundWildcard = oracle.getWildcardType(
        BoundType.SUPER, oracle.getType(Number.class.getName()));
    JWildcardType intLowerBoundWildcard = oracle.getWildcardType(
        BoundType.SUPER, integerType);

    // List<? super Number>
    JParameterizedType listOfSuperNumber = oracle.getParameterizedType(
        genericList, new JClassType[]{numLowerBoundWildcard});

    // List<? super Interger>
    JParameterizedType listOfSuperInteger = oracle.getParameterizedType(
        genericList, new JClassType[]{intLowerBoundWildcard});

    assertTrue(listOfSuperInteger.isAssignableFrom(listOfSuperNumber));
    assertFalse(listOfSuperInteger.isAssignableTo(listOfSuperNumber));
    assertFalse(listOfSuperNumber.isAssignableFrom(listOfSuperInteger));
    assertTrue(listOfSuperNumber.isAssignableTo(listOfSuperInteger));

    JParameterizedType listOfObject = oracle.getParameterizedType(genericList,
        new JClassType[]{oracle.getJavaLangObject()});

    JClassType stringType = oracle.getType(String.class.getName());
    JParameterizedType listOfString = oracle.getParameterizedType(genericList,
        new JClassType[]{stringType});

    // List<Object> is not assignable from List<String>
    assertFalse(listOfObject.isAssignableFrom(listOfString));

    // List<String> is not assignable from List<Object>
    assertFalse(listOfString.isAssignableFrom(listOfObject));

    // List<List<String>> is not assignable from List<Vector<String>>
    JParameterizedType listOfListOfString = oracle.getParameterizedType(
        genericList, new JClassType[]{listOfString});

    JGenericType genericVector = oracle.getType(Vector.class.getName()).isGenericType();
    JParameterizedType vectorOfString = oracle.getParameterizedType(
        genericVector, new JClassType[]{stringType});
    JParameterizedType listOfVectorOfString = oracle.getParameterizedType(
        genericList, new JClassType[]{vectorOfString});

    assertFalse(listOfListOfString.isAssignableFrom(listOfVectorOfString));
    assertFalse(listOfVectorOfString.isAssignableFrom(listOfListOfString));

    // List<List> is not assignable from List<List<String>>
    JClassType listOfRawList = oracle.getParameterizedType(genericList,
        new JClassType[]{genericList.getRawType()});
    assertFalse(listOfRawList.isAssignableFrom(listOfListOfString));
    assertFalse(listOfListOfString.isAssignableFrom(listOfRawList));

    JGenericType genericClass = oracle.getType(GenericClass.class.getName()).isGenericType();
    JParameterizedType parameterizedGenericClass = oracle.getParameterizedType(
        genericClass, new JClassType[]{stringType});
    JClassType extendsRawGenericClass = oracle.getType(ExtendsRawGenericClass.class.getName());

    // GenericClass<String> is assignable from ExtendsRawGenericClass
    assertTrue(parameterizedGenericClass.isAssignableFrom(extendsRawGenericClass));

    // ExtendsRawGenericClass is not assignable from GenericClass<String>
    assertFalse(extendsRawGenericClass.isAssignableFrom(parameterizedGenericClass));

    // List<List<? extends Number>>
    JClassType listOfListOfExtendsNumber = oracle.getParameterizedType(
        genericList, new JClassType[]{listOfExtendsNumber});

    // List<List<? extends Integer>>
    JClassType listOfListOfExtendsInteger = oracle.getParameterizedType(
        genericList, new JClassType[]{listOfExtendsInteger});

    assertFalse(listOfListOfExtendsNumber.isAssignableFrom(listOfListOfExtendsInteger));

    // List<Integer>
    JClassType listOfInteger = oracle.getParameterizedType(genericList,
        new JClassType[]{integerType});

    // List<? extends Number> is assignable from List<Integer>
    assertTrue(listOfExtendsNumber.isAssignableFrom(listOfInteger));
    assertFalse(listOfExtendsNumber.isAssignableFrom(listOfObject));

    // List<? super Number> is not assignable from List<Integer>
    assertFalse(listOfSuperNumber.isAssignableFrom(listOfInteger));

    // List<? super Number> is assignable from List<Object>
    assertTrue(listOfSuperNumber.isAssignableFrom(listOfObject));
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
}
