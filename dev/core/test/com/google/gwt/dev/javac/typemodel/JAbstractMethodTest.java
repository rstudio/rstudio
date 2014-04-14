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
import com.google.gwt.dev.javac.typemodel.test.GenericClassWithDependentTypeBounds;
import com.google.gwt.dev.javac.typemodel.test.GenericClassWithTypeBound;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.Serializable;

/**
 * Tests for {@link JAbstractMethod}.
 */
public class JAbstractMethodTest extends TestCase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JAbstractMethodTest() throws UnableToCompleteException {
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JAbstractMethod#getTypeParameters()}
   * . This test is similar in nature to
   * {@link com.google.gwt.core.ext.typeinfo.JGenericTypeTest#testGetDependentTypeParameters()}
   * , except that it is verifying the type parameters on a method declaration,
   * as opposed to a class declaration.
   *
   * NOTE: The test types used are: {@link GenericClassWithDependentTypeBounds}
   * {@link GenericClassWithTypeBound}
   *
   * @throws NotFoundException
   */
  public void testGenericMethodWithDependentTypeParameters()
      throws NotFoundException {

    TypeOracle oracle = moduleContext.getOracle();

    // Get the type.

    JClassType type = oracle.getType(GenericClassWithDependentTypeBounds.class.getName());

    // Get its methods (not constructors). There should be only one method.

    JMethod[] methods = type.getMethods();
    assertEquals(1, methods.length);

    // Make sure this method is a generic method by checking its type
    // parameters. The method should have two type parameters.

    JMethod method = methods[0];
    JTypeParameter[] typeParameters = method.getTypeParameters();
    assertEquals(2, typeParameters.length);

    // Examine the first type parameter. Its name should be 'Q'.

    JTypeParameter typeParameter = typeParameters[0];
    assertEquals("Q", typeParameter.getName());

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

    // Check the name of the type parameter. It should be 'P'.

    assertEquals("P", typeArgTypeParameter.getName());

    // Check the bound of the type parameter. It should have a single upper
    // bound.
    JClassType[] typeArgBounds = typeArgTypeParameter.getBounds();
    assertEquals(1, typeArgBounds.length);

    // Verify that the bound type is actually a reference to
    // java.io.Serializable.

    JClassType typeArgUpperBoundType = typeArgBounds[0];
    assertEquals(typeArgUpperBoundType,
        oracle.getType(Serializable.class.getName()));

    // Now look at the second type parameter on the generic method. It should
    // be identical to the type argument of the first type parameter
    // (remember, the first type parameter was a paramaterized type).

    JTypeParameter secondTypeParameter = typeParameters[1];
    assertEquals(secondTypeParameter, typeArgTypeParameter);
  }
}
