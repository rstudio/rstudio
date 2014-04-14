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
import com.google.gwt.dev.javac.typemodel.test.GenericClassWithDependentTypeBounds;
import com.google.gwt.dev.javac.typemodel.test.GenericClassWithTypeBound;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.Serializable;

/**
 * Tests for {@link JGenericType}.
 */
public class JGenericTypeTest extends TestCase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JGenericTypeTest() throws UnableToCompleteException {
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#getTypeParameters()}.
   * This test goes beyond
   * {@link com.google.gwt.core.ext.typeinfo.JGenericTypeTest#testGetTypeParameters()}
   * by testing generic types that have type parameters which are dependent on
   * one another.
   *
   * NOTE: This test does not make use of the
   * {@link com.google.gwt.core.ext.typeinfo.JGenericTypeTest#getTestType()}
   * method. The test types used are:
   * {@link GenericClassWithDependentTypeBounds}
   * {@link GenericClassWithTypeBound}
   *
   * @throws NotFoundException
   */
  public void testGetDependentTypeParameters() throws NotFoundException {

    TypeOracle oracle = moduleContext.getOracle();

    // Get the generic type

    JClassType type = oracle.getType(GenericClassWithDependentTypeBounds.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);

    // Get its type parameters

    JTypeParameter[] typeParameters = genericType.getTypeParameters();
    assertEquals(2, typeParameters.length);

    // Examine the first type parameter. Its name should be 'C'.

    JTypeParameter typeParameter = typeParameters[0];
    assertEquals("C", typeParameter.getName());

    // Check the bound of the first type parameter. It should be a single
    // upper bound.
    JClassType[] genericTypeBounds = typeParameter.getBounds();
    assertEquals(1, genericTypeBounds.length);

    // Check to see that the upper bound is a parameterized type.

    JClassType upperBoundType = genericTypeBounds[0];
    JParameterizedType upperBoundParameterizedType = upperBoundType.isParameterized();
    assertNotNull(upperBoundParameterizedType);

    // Examine the parameterized type. Its name should be
    // 'GenericClassWithTypeBound'. The base type of the parameterized type
    // should be a reference to the class 'GenericClassWithTypeBound'.

    assertEquals("GenericClassWithTypeBound",
        upperBoundParameterizedType.getName());
    assertEquals(upperBoundParameterizedType.getBaseType(),
        oracle.getType(GenericClassWithTypeBound.class.getName()));

    // Check the type arguments for the parameterized type. There should be a
    // single type argument.

    JClassType[] typeArgs = upperBoundParameterizedType.getTypeArgs();
    assertEquals(1, typeArgs.length);

    // Examine the first type argument. It should be a type parameter.

    JClassType typeArg = typeArgs[0];
    JTypeParameter typeArgTypeParameter = typeArg.isTypeParameter();
    assertNotNull(typeArgTypeParameter);

    // Check the name of the type parameter. It should be 'M'.

    assertEquals("M", typeArgTypeParameter.getName());

    // Check the bound of the type parameter. It should have a single upper
    // bound.
    JClassType[] typeArgBounds = typeArgTypeParameter.getBounds();
    assertEquals(1, typeArgBounds.length);

    // Verify that the bound type is actually a reference to
    // java.io.Serializable.

    JClassType typeArgUpperBoundType = typeArgBounds[0];
    assertEquals(typeArgUpperBoundType,
        oracle.getType(Serializable.class.getName()));

    // Now look at the second type parameter on the generic type. It should
    // be identical to the type argument of the first type parameter
    // (remember, the first type parameter was a paramaterized type).

    JTypeParameter secondTypeParameter = typeParameters[1];
    assertEquals(secondTypeParameter, typeArgTypeParameter);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#getErasedType()}.
   *
   * @throws NotFoundException
   */
  public void testGetErasedType() throws NotFoundException {
    JGenericType genericClass = getTestType();

    assertEquals(genericClass.getRawType(), genericClass.getErasedType());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#getRawType()}.
   *
   * @throws NotFoundException
   */
  public void testGetRawType() throws NotFoundException {
    JGenericType genericClass = getTestType();

    JDelegatingClassTypeTestBase.validateTypeSubstitution(genericClass,
        genericClass.getRawType(), new Substitution() {
          @Override
          public JClassType getSubstitution(JClassType type) {
            return type.getErasedType();
          }
        });
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#getTypeParameters()}.
   *
   * @throws NotFoundException
   */
  public void testGetTypeParameters() throws NotFoundException {
    JGenericType genericType = getTestType();
    JTypeParameter[] typeParameters = genericType.getTypeParameters();

    assertEquals(1, typeParameters.length);

    JTypeParameter typeParameter = typeParameters[0];
    assertEquals("T", typeParameter.getName());

    JClassType[] bounds = typeParameter.getBounds();
    assertEquals(1, bounds.length);
    assertEquals(
        moduleContext.getOracle().getType(Serializable.class.getName()),
        bounds[0]);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#isAssignableFrom(JClassType)}
   * .
   *
   * @throws NotFoundException
   */
  public void testIsAssignableFrom() throws NotFoundException {
    JGenericType genericType = getTestType();

    // Check that the generic type's superclass is assignable from the generic
    // type
    assertTrue(genericType.getSuperclass().isAssignableFrom(genericType));

    // Check that each implemented interface is assignable from the generic type
    JClassType[] implementedInterfaces = genericType.getImplementedInterfaces();
    for (JClassType implementedInterface : implementedInterfaces) {
      assertTrue(implementedInterface.isAssignableFrom(genericType));
      assertTrue(implementedInterface.isAssignableFrom(genericType.getRawType()));
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JGenericType#isAssignableTo(JClassType)}
   * .
   *
   * @throws NotFoundException
   */
  public void testIsAssignableTo() throws NotFoundException {
    JGenericType genericType = getTestType();

    // Check that generic type is assignable to its superclass
    assertTrue(genericType.isAssignableTo(genericType.getSuperclass()));

    // Check that the generic class is assignable to any implemented interface
    JClassType[] implementedInterfaces = genericType.getImplementedInterfaces();
    for (JClassType implementedInterface : implementedInterfaces) {
      assertTrue(genericType.isAssignableTo(implementedInterface));

      if (implementedInterface.isParameterized() != null) {
        assertTrue(genericType.isAssignableTo(implementedInterface.isParameterized().getRawType()));
      }
    }
  }

  /**
   * Returns the generic version of {@link GenericClass}.
   */
  protected JGenericType getTestType() throws NotFoundException {
    JClassType type = moduleContext.getOracle().getType(
        GenericClass.class.getName());
    JGenericType genericType = type.isGenericType();
    assertNotNull(genericType);
    return genericType;
  }
}
