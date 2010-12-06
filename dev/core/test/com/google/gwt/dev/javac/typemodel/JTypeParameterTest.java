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
import com.google.gwt.dev.javac.typemodel.test.GenericClass;
import com.google.gwt.dev.javac.typemodel.test.MyCustomList;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

/**
 * Tests for {@link JTypeParameter}.
 */
public class JTypeParameterTest extends JDelegatingClassTypeTestBase {
  private static JClassType findParameterizationOf(JClassType classType,
      JGenericType genericType) {
    Set<JClassType> supertypes = JClassType.getFlattenedSuperTypeHierarchy(classType);
    for (JClassType supertype : supertypes) {
      JMaybeParameterizedType maybeParameterizedType = supertype.isMaybeParameterizedType();
      if (maybeParameterizedType != null
          && maybeParameterizedType.getBaseType() == genericType) {
        return supertype;
      }
    }

    return null;
  }

  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JTypeParameterTest() throws UnableToCompleteException {
  }

  @Override
  public void testFindConstructor() {
  }

  @Override
  public void testFindNestedType() {
  }

  @Override
  public void testGetConstructors() {
  }

  @Override
  public void testGetEnclosingType() throws NotFoundException {
    JTypeParameter testType = getTestType();
    assertNull(testType.getEnclosingType());
  }

  @Override
  public void testGetInheritableMethods() throws NotFoundException {
    JTypeParameter testType = getTestType();

    assertTrue(Arrays.deepEquals(testType.getInheritableMethods(),
        testType.getFirstBound().getInheritableMethods()));
  }

  @Override
  public void testGetName() throws NotFoundException {
    assertEquals("T", getTestType().getName());
  }

  @Override
  public void testGetNestedType() {
  }

  @Override
  public void testGetNestedTypes() throws NotFoundException {
    JTypeParameter testType = getTestType();

    assertTrue(Arrays.deepEquals(testType.getNestedTypes(),
        testType.getFirstBound().getNestedTypes()));
  }

  @Override
  public void testGetOverridableMethods() throws NotFoundException {
    JTypeParameter testType = getTestType();

    assertTrue(Arrays.deepEquals(testType.getOverridableMethods(),
        testType.getFirstBound().getOverridableMethods()));
  }

  /*
   * Checks that all non-local subtypes of the type parameter, T extends
   * Serializable & Comparable<T> are actually assignable to Serializable and
   * the properly parameterized version of Comparable<T>.
   */
  @Override
  public void testGetSubtypes() throws NotFoundException {

    TypeOracle oracle = moduleContext.getOracle();
    JClassType testType = oracle.getType(MyCustomList.class.getName());
    JGenericType genericType = testType.isGenericType();
    JTypeParameter[] typeParameters = genericType.getTypeParameters();
    JTypeParameter typeParameter = typeParameters[0];

    JClassType serializableType = oracle.getType(Serializable.class.getCanonicalName());
    JGenericType comparableType = (JGenericType) oracle.getType(Comparable.class.getCanonicalName());
    JClassType[] computedSubtypes = typeParameter.getSubtypes();

    for (JClassType computedSubtype : computedSubtypes) {
      // Find the parameterized version of the Comparable interface.
      JClassType comparableIntf = findParameterizationOf(computedSubtype,
          comparableType);

      assertTrue(computedSubtype.isAssignableTo(serializableType));
      assertTrue(computedSubtype.isAssignableTo(comparableIntf));
    }
  }

  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    JTypeParameter testType = getTestType();
    assertTrue(testType.isAssignableFrom(moduleContext.getOracle().getType(
        Serializable.class.getName())));

    // Check that the type parameter is assignable from each subtype
    JClassType[] subtypes = testType.getSubtypes();
    for (JClassType subtype : subtypes) {
      assertTrue(testType.isAssignableFrom(subtype));
    }
  }

  @Override
  public void testIsAssignableTo() throws NotFoundException {
    JTypeParameter testType = getTestType();
    assertTrue(testType.isAssignableTo(moduleContext.getOracle().getJavaLangObject()));

    // Check that each bound is assignable to this type parameter.
    JClassType[] typeBounds = testType.getBounds();
    for (JClassType typeBound : typeBounds) {
      // Test that the type parameter is assignable to the type bound.
      assertTrue(testType.isAssignableTo(typeBound));

      // Test that the type parameter is assignable from the type bound.
      assertTrue(typeBound.isAssignableFrom(testType));
    }
  }

  @Override
  protected Substitution getSubstitution() {
    return new Substitution() {
      public JClassType getSubstitution(JClassType type) {
        return type;
      }
    };
  }

  /*
   * NOTE: This method returns the type parameter T from the GenericClass<T>
   * type.
   */
  @Override
  protected JTypeParameter getTestType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType testType = oracle.getType(GenericClass.class.getName());
    JGenericType genericTestType = testType.isGenericType();
    assertNotNull(genericTestType);
    JTypeParameter[] typeParameters = genericTestType.getTypeParameters();
    assertTrue(typeParameters.length > 0);
    return typeParameters[0];
  }
}
