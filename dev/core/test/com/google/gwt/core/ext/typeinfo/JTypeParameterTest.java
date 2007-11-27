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
import com.google.gwt.core.ext.typeinfo.test.GenericClass;
import com.google.gwt.core.ext.typeinfo.test.MyCustomList;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.util.Arrays;

/**
 * Tests for {@link JTypeParameter}.
 */
public class JTypeParameterTest extends JDelegatingClassTypeTestBase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.core.ext.typeinfo.TypeOracleTest");

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
    assertEquals(
        moduleContext.getOracle().getType(GenericClass.class.getName()),
        testType.getDeclaringClass());
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

  @Override
  public void testGetSubtypes() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType testType = oracle.getType(MyCustomList.class.getName());
    JGenericType genericType = testType.isGenericType();
    JTypeParameter[] typeParameters = genericType.getTypeParameters();
    JTypeParameter typeParameter = typeParameters[0];

    JClassType[] expected = new JClassType[] {
    /*
     * TODO: Re-eneable this once java.io.Serializable is added to the JRE
     * 
     * emulation classes oracle.getType(Integer.class.getName()),
     * oracle.getType(Float.class.getName()),
     * oracle.getType(Short.class.getName()),
     * oracle.getType(Double.class.getName()),
     * oracle.getType(Number.class.getName()),
     * oracle.getType(Long.class.getName()),
     * oracle.getType(Byte.class.getName()),
     */
    };
    validateEquals(oracle, expected, typeParameter.getSubtypes());
  }

  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    JTypeParameter testType = getTestType();
    assertTrue(testType.isAssignableFrom(moduleContext.getOracle().getJavaLangObject()));
  }

  @Override
  public void testIsAssignableTo() throws NotFoundException {
    JTypeParameter testType = getTestType();
    assertTrue(testType.isAssignableTo(moduleContext.getOracle().getJavaLangObject()));
  }

  @Override
  protected Substitution getSubstitution() {
    return new Substitution() {
      public JType getSubstitution(JType type) {
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
