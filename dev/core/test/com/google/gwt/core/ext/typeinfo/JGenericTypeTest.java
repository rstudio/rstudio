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
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Tests for {@link JGenericType}.
 */
public class JGenericTypeTest extends TestCase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.core.ext.typeinfo.TypeOracleTest");

  public JGenericTypeTest() throws UnableToCompleteException {
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

    JDelegatingClassTypeTest.validateTypeSubstitution(genericClass,
        genericClass.getRawType(), new Substitution() {
          public JType getSubstitution(JType type) {
            return type.getErasedType();
          }
        });
  }

  /**
   * Test method for {@link
   * com.google.gwt.core.ext.typeinfo.JGenericType#getTypeParameters()}.
   * 
   * @throws NotFoundException
   */
  public void testGetTypeParameters() throws NotFoundException {
    JGenericType genericType = getTestType();
    JTypeParameter[] typeParameters = genericType.getTypeParameters();

    assertEquals(1, typeParameters.length);

    JTypeParameter typeParameter = typeParameters[0];
    assertEquals("T", typeParameter.getName());

    JBound bound = typeParameter.getBounds();
    assertNotNull(bound.isUpperBound());

    JClassType[] bounds = bound.getBounds();
    assertEquals(1, bounds.length);
    assertEquals(moduleContext.getOracle().getJavaLangObject(), bounds[0]);
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
