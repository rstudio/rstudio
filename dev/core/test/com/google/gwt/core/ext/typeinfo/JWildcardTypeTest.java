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
import com.google.gwt.core.ext.typeinfo.test.CA;
import com.google.gwt.core.ext.typeinfo.test.CB;
import com.google.gwt.core.ext.typeinfo.test.CC;
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
      "com.google.gwt.core.ext.typeinfo.TypeOracleTest");

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

  @Override
  public void testGetSubtypes() {
    // Tested by testGetSubtypes_LowerBound() and testGetSubtypes_UpperBound()
  }

  public void testGetSubtypes_LowerBound() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    // <? super Number>
    JWildcardType lowerBoundWildcard = oracle.getWildcardType(new JLowerBound(
        oracle.getType(Number.class.getName())));
    JClassType[] subtypes = lowerBoundWildcard.getSubtypes();
    assertEquals(0, subtypes.length);
    // assertEquals(oracle.getJavaLangObject(), subtypes[0]);
  }

  public void testGetSubtypes_UpperBound() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    // <? extends CA>
    JWildcardType upperBoundWildcard = oracle.getWildcardType(new JUpperBound(
        oracle.getType(CA.class.getName())));

    JClassType[] expected = new JClassType[] {
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
  public void testIsAssignableFrom() {
    // Covered by the different testIsAssignableFrom*() variants below.
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

    JUpperBound numberBound = new JUpperBound(numberType);
    JUpperBound integerBound = new JUpperBound(integerType);

    JWildcardType numberWildcard = oracle.getWildcardType(numberBound);
    JWildcardType integerWildcard = oracle.getWildcardType(integerBound);

    assertTrue(numberWildcard.isAssignableFrom(integerWildcard));
    assertFalse(integerWildcard.isAssignableFrom(numberWildcard));
  }

  /**
   * Tests that <? extends Number> is assignable from <? extends Integer> and
   * that the reverse is not <code>true</code>.
   */
  public void testIsAssignableFrom_Extends_Object_From_Super_Object() {
    TypeOracle oracle = moduleContext.getOracle();

    JClassType javaLangObject = oracle.getJavaLangObject();
    JLowerBound lowerBound = new JLowerBound(javaLangObject);
    JUpperBound upperBound = new JUpperBound(javaLangObject);

    JWildcardType lowerWildcard = oracle.getWildcardType(lowerBound);
    JWildcardType upperWildcard = oracle.getWildcardType(upperBound);

    assertTrue(upperWildcard.isAssignableFrom(lowerWildcard));
    assertFalse(lowerWildcard.isAssignableFrom(upperWildcard));
  }

  /**
   * Tests that <? super Integer> is assignable from <? super Number> and that
   * the reverse is not <code>true</code>.
   */
  public void testIsAssignableFrom_Super_Integer_From_Super_Number()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JLowerBound numberBound = new JLowerBound(numberType);
    JLowerBound integerBound = new JLowerBound(integerType);

    JWildcardType numberWildcard = oracle.getWildcardType(numberBound);
    JWildcardType integerWildcard = oracle.getWildcardType(integerBound);

    assertFalse(numberWildcard.isAssignableFrom(integerWildcard));
    assertTrue(integerWildcard.isAssignableFrom(numberWildcard));
  }

  /**
   * Tests that <? super Number> is assignable to <? super Integer> and that the
   * reverse is not <code>true</code>.
   */
  public void testIsAssignableFrom_Super_Number_To_Super_Integer()
      throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType numberType = oracle.getType(Number.class.getName());
    JClassType integerType = oracle.getType(Integer.class.getName());

    JLowerBound numberBound = new JLowerBound(numberType);
    JLowerBound integerBound = new JLowerBound(integerType);

    JWildcardType numberWildcard = oracle.getWildcardType(numberBound);
    JWildcardType integerWildcard = oracle.getWildcardType(integerBound);

    assertFalse(numberWildcard.isAssignableTo(integerWildcard));
    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
  }

  @Override
  public void testIsAssignableTo() {
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

    JUpperBound numberBound = new JUpperBound(numberType);
    JUpperBound integerBound = new JUpperBound(integerType);

    JWildcardType numberWildcard = oracle.getWildcardType(numberBound);
    JWildcardType integerWildcard = oracle.getWildcardType(integerBound);

    assertTrue(integerWildcard.isAssignableTo(numberWildcard));
    assertFalse(numberWildcard.isAssignableTo(integerWildcard));
  }

  @Override
  protected Substitution getSubstitution() {
    return new Substitution() {
      public JType getSubstitution(JType type) {
        return type;
      }
    };
  }

  public void testGetMethods() throws NotFoundException {
    super.testGetMethods();
  }

  @Override
  protected JWildcardType getTestType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    return oracle.getWildcardType(new JUpperBound(
        oracle.getType(Number.class.getName())));
  }
}
