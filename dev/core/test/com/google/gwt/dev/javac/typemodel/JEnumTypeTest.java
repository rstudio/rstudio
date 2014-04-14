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
import com.google.gwt.dev.javac.typemodel.test.EnumInterface;
import com.google.gwt.dev.javac.typemodel.test.EnumOfInterface;
import com.google.gwt.dev.javac.typemodel.test.MyEnum;
import com.google.gwt.dev.javac.typemodel.test.TestAnnotation;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Tests for {@link JEnumType}.
 */
public class JEnumTypeTest extends TestCase {

  private final boolean logToConsole = false;

  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JEnumTypeTest() throws UnableToCompleteException {
  }

  private final TypeOracle typeOracle = moduleContext.getOracle();

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JEnumType#getEnumConstants()}.
   *
   * @throws NotFoundException
   */
  public void testGetEnumConstants() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());
    JEnumType enumType = type.isEnum();
    assertNotNull(enumType);

    JEnumConstant[] enumConstants = enumType.getEnumConstants();
    assertEquals(3, enumConstants.length);

    for (int i = 0; i < enumConstants.length; ++i) {
      JEnumConstant enumConstant = enumConstants[i];
      // Check the ordinal
      assertEquals(i, enumConstant.getOrdinal());

      // Check that the name matches what reflection expects at the current
      // ordinal.
      assertEquals(MyEnum.class.getEnumConstants()[i].name(),
          enumConstant.getName());
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JClassType#getFields()}.
   *
   * @throws NotFoundException
   */
  public void testGetFields() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());
    JEnumType enumType = validateTypeIsEnum(type);

    assertEquals(5, enumType.getFields().length);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JClassType#getField(String)}.
   *
   * @throws NotFoundException
   */
  public void testGetFieldString() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());

    validateTypeIsEnum(type);

    type.getField("VAL0");
    type.getField("VAL1");
    type.getField("VAL2");
    type.getField("instanceField");
    type.getField("e");
  }

  /**
   * Test method for {@link JEnumType#getConstructors()}
   *
   * @throws NotFoundException
   * @throws NotFoundException
   */
  public void testGetConstructors() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());
    JEnumType enumType = validateTypeIsEnum(type);

    // Enum constructors are not reflected.
    assertEquals(0, enumType.getConstructors().length);
  }

  /**
   * Test method for {@link JEnumType#getMethods()}
   *
   * @throws NotFoundException
   */
  public void testGetMethods() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());
    JEnumType enumType = validateTypeIsEnum(type);

    assertEquals(3, enumType.getMethods().length);
  }

  /**
   * Test an enum that implements an interface.
   *
   * @throws NotFoundException
   */
  public void testInterface() throws NotFoundException {
    JClassType type = typeOracle.getType(EnumOfInterface.class.getName());
    JEnumType enumType = validateTypeIsEnum(type);
    JClassType[] intf = enumType.getImplementedInterfaces();
    assertEquals(1, intf.length);
    assertEquals(EnumInterface.class.getName(),
        intf[0].getQualifiedSourceName());
    JMethod getExtra = intf[0].getMethod("getExtra", TypeOracle.NO_JTYPES);
    TestAnnotation annotation = getExtra.getAnnotation(TestAnnotation.class);
    assertNotNull(annotation);
    assertEquals("EnumInterface getExtra", annotation.value());
    JEnumConstant[] constants = enumType.getEnumConstants();
    assertEquals(2, constants.length);
    assertEquals("A", constants[0].getName());
    annotation = constants[0].getAnnotation(TestAnnotation.class);
    assertNotNull(annotation);
    assertEquals("A", annotation.value());
    JClassType aClass = (JClassType) constants[0].getType().isClass();
    {
      JMethod[] methods = aClass.getInheritableMethods();
      assertEquals(11, methods.length);
      boolean found = false;
      for (JMethod method : methods) {
        if ("name".equals(method.getName())) {
          found = true;
          // TODO(jat); any other verification here?
        }
      }
      assertTrue(found);
    }
    {
      JMethod[] methods = aClass.getOverridableMethods();
      assertEquals(4, methods.length);
      // TODO(jat): verify getExtra is from A's anonymous subclass of
      // EnumInterface when/if that is implemented.
      boolean found = false;
      for (JMethod method : methods) {
        if ("getExtra".equals(method.getName())) {
          found = true;
          // TODO(jat); any other verification here?
        }
      }
      assertTrue(found);
    }
  }

  /**
   * Test method for {@link com.google.gwt.core.ext.typeinfo.JEnumType#isEnum()}
   * .
   *
   * @throws NotFoundException
   */
  public void testIsEnum() throws NotFoundException {
    JClassType type = typeOracle.getType(MyEnum.class.getName());
    validateTypeIsEnum(type);
  }

  private static JEnumType validateTypeIsEnum(JClassType type)
      throws NotFoundException {
    JEnumType maybeEnum = type.isEnum();
    assertNotNull(maybeEnum);

    JClassType enumType = type.getOracle().getType(Enum.class.getName());

    assertTrue(enumType.isAssignableFrom(maybeEnum));

    return maybeEnum;
  }
}
