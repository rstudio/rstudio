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
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.dev.javac.typemodel.test.CA;
import com.google.gwt.dev.javac.typemodel.test.CB;
import com.google.gwt.dev.javac.typemodel.test.CC;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test for {@link JWildcardType}.
 */
public class JWildcardTypeTest extends JDelegatingClassTypeTestBase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JWildcardTypeTest() throws UnableToCompleteException {
  }

  @Override
  public void testFindConstructor() {
    // Wildcard types do not have constructors
  }

  @Override
  public void testFindNestedType() {
    // Wildcard do not have nested types...
  }

  @Override
  public void testGetConstructors() {
  }

  @Override
  public void testGetEnclosingType() {
    // Wildcard do not have nested types...
  }

  @Override
  public void testGetErasedType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getCanonicalName());

    // Tests that ? extends Number erases to number.
    JWildcardType upperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, numberType);
    assertEquals(numberType, upperBoundWildcard.getErasedType());

    // Tests that ? super Number erases to Object
    JWildcardType lowerBoundWildcard = oracle.getWildcardType(BoundType.SUPER,
        numberType);
    assertEquals(oracle.getJavaLangObject(), lowerBoundWildcard.getErasedType());
  }

  @Override
  public void testGetInheritableMethods() {
    // No inheritable methods
  }

  @Override
  public void testGetMethods() throws NotFoundException {
    super.testGetMethods();
  }

  @Override
  public void testGetNestedType() {
    // No nested types
  }

  @Override
  public void testGetNestedTypes() {
    // No nested types
  }

  @Override
  public void testGetOverridableMethods() {
    // No overridable methods
  }

  /**
   * Tests that {@link JWildcardType#getParameterizedQualifiedSourceName()}
   * returns the expected value. We test this because JSNI code depends on it.
   * 
   * @throws NotFoundException
   */
  public void testGetParameterizedQualifiedSourceName()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());

    JWildcardType numberUpperBound = oracle.getWildcardType(BoundType.EXTENDS,
        numberType);
    assertEquals("? extends " + Number.class.getCanonicalName(),
        numberUpperBound.getParameterizedQualifiedSourceName());

    JWildcardType numberLowerBound = oracle.getWildcardType(BoundType.SUPER,
        numberType);
    assertEquals("? super " + Number.class.getCanonicalName(),
        numberLowerBound.getParameterizedQualifiedSourceName());

    JWildcardType unboundWildcard = oracle.getWildcardType(BoundType.UNBOUND,
        oracle.getJavaLangObject());
    assertEquals("?", unboundWildcard.getParameterizedQualifiedSourceName());
  }

  @Override
  public void testGetSubtypes() {
    // Tested by testGetSubtypes_LowerBound() and testGetSubtypes_UpperBound()
  }

  public void testGetSubtypes_LowerBound() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    // <? super Number>
    JWildcardType lowerBoundWildcard = oracle.getWildcardType(BoundType.SUPER,
        oracle.getType(Number.class.getName()));
    JClassType[] subtypes = lowerBoundWildcard.getSubtypes();
    assertEquals(0, subtypes.length);
    // assertEquals(oracle.getJavaLangObject(), subtypes[0]);
  }

  public void testGetSubtypes_UpperBound() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    // <? extends CA>
    JWildcardType upperBoundWildcard = oracle.getWildcardType(
        BoundType.EXTENDS, oracle.getType(CA.class.getName()));

    JClassType[] expected = new JClassType[]{
        oracle.getType(CB.class.getName()), oracle.getType(CC.class.getName())};
    Set<JClassType> expectedSet = new HashSet<JClassType>();
    expectedSet.addAll(Arrays.asList(expected));

    JClassType[] actual = upperBoundWildcard.getSubtypes();
    assertEquals(expectedSet.size(), actual.length);

    for (int i = 0; i < actual.length; ++i) {
      expectedSet.remove(actual[i]);
    }
    assertTrue(expectedSet.isEmpty());
  }

  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    // Covered by the different testIsAssignableFrom*() variants below.
    TypeOracle oracle = moduleContext.getOracle();

    JClassType integerType = oracle.getType(Integer.class.getName());
    JClassType numberType = oracle.getType(Number.class.getName());

    // ? extends Number
    JClassType extendsNumber = oracle.getWildcardType(BoundType.EXTENDS,
        numberType);

    // ? extends Integer
    JClassType extendsInteger = oracle.getWildcardType(BoundType.EXTENDS,
        integerType);

    // Integer is not assignable from ? extends Number
    assertFalse(integerType.isAssignableFrom(extendsNumber));

    // Integer is assignable from ? extends Integer
    assertTrue(integerType.isAssignableFrom(extendsInteger));

    // Number is assignable from ? extends Integer
    assertTrue(numberType.isAssignableFrom(extendsInteger));

    // ? super Integer
    JClassType superInteger = oracle.getWildcardType(BoundType.SUPER,
        integerType);

    // Integer is assignable from ? super Integer
    assertFalse(integerType.isAssignableFrom(superInteger));

    // ? super Integer is assignable from Number
    assertTrue(superInteger.isAssignableFrom(numberType));

    JClassType superNumber = oracle.getWildcardType(BoundType.SUPER, numberType);

    // ? super Number is assignable from Integer
    assertTrue(superNumber.isAssignableFrom(integerType));

    // ? super Number is assignable from Character
    JClassType characterType = oracle.getType(Character.class.getName());
    assertTrue(superNumber.isAssignableFrom(characterType));
  }

  /**
   * Tests that <? extends Number> is assignable from <? extends Integer> and
   * that the reverse is not <code>true</code>.
   */
  public void testIsAssignableFrom_Extends_Number_To_Extends_Integer()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JWildcardType numberWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        numberType);
    JWildcardType integerWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        integerType);

    assertTrue(numberWildcard.isAssignableFrom(integerWildcard));
    assertFalse(integerWildcard.isAssignableFrom(numberWildcard));
  }

  /**
   * Tests that <? extends Object> is assignable to and from <? super Object>.
   */
  public void testIsAssignableFrom_Extends_Object_From_Super_Object() {
    TypeOracle oracle = moduleContext.getOracle();

    JClassType javaLangObject = oracle.getJavaLangObject();

    // ? super Object
    JWildcardType lowerWildcard = oracle.getWildcardType(BoundType.SUPER,
        javaLangObject);

    // ? extends Object
    JWildcardType upperWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        javaLangObject);

    // ? extends Object assignable from ? super Object
    assertTrue(upperWildcard.isAssignableFrom(lowerWildcard));

    // ? super Object assignable from ? extends Object
    assertTrue(lowerWildcard.isAssignableFrom(upperWildcard));
  }

  /**
   * Tests that <? super Integer> is assignable to and from <? super Number>.
   */
  public void testIsAssignableFrom_Super_Integer_From_Super_Number()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JWildcardType numberWildcard = oracle.getWildcardType(BoundType.SUPER,
        numberType);
    JWildcardType integerWildcard = oracle.getWildcardType(BoundType.SUPER,
        integerType);

    assertTrue(numberWildcard.isAssignableFrom(integerWildcard));
    assertTrue(numberWildcard.isAssignableTo(integerWildcard));
    assertTrue(integerWildcard.isAssignableFrom(numberWildcard));
    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
  }

  /**
   * Tests that <? super Number> is assignable to and from <? super Integer>.
   */
  public void testIsAssignableFrom_Super_Number_To_Super_Integer()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JWildcardType numberWildcard = oracle.getWildcardType(BoundType.SUPER,
        numberType);
    JWildcardType integerWildcard = oracle.getWildcardType(BoundType.SUPER,
        integerType);

    assertTrue(numberWildcard.isAssignableTo(integerWildcard));
    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
  }

  @Override
  public void testIsAssignableTo() {
    // NOTE These cases were tested as part of testIsAssignableFrom.
  }

  /**
   * Tests that <? extends Integer> is assignable to <? extends Number> and that
   * the reverse is not <code>true</code>.
   */
  public void testIsAssignableTo_Extends_Integer_To_Extends_Number()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JWildcardType numberWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        numberType);
    JWildcardType integerWildcard = oracle.getWildcardType(BoundType.EXTENDS,
        integerType);

    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
    assertFalse(numberWildcard.isAssignableTo(integerWildcard));
  }

  /**
   * Tests that <? super Number> is assignable to and from <? super Integer>.
   */
  public void testIsAssignableTo_Super_Number_To_Super_Integer()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JWildcardType numberWildcard = oracle.getWildcardType(BoundType.SUPER,
        numberType);
    JWildcardType integerWildcard = oracle.getWildcardType(BoundType.SUPER,
        integerType);

    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
    assertTrue(numberWildcard.isAssignableTo(integerWildcard));
  }

  @Override
  protected Substitution getSubstitution() {
    return new Substitution() {
      public JClassType getSubstitution(JClassType type) {
        return type;
      }
    };
  }

  @Override
  protected JWildcardType getTestType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    return oracle.getWildcardType(BoundType.EXTENDS,
        oracle.getType(Number.class.getName()));
  }
}
