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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.javac.typemodel.test.MyCustomList;
import com.google.gwt.dev.javac.typemodel.test.MyIntegerList;
import com.google.gwt.dev.javac.typemodel.test.MyList;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.util.ArrayList;

/**
 * Test for {@link JRawType}.
 */
public class JRawTypeTest extends JDelegatingClassTypeTestBase {
  private final boolean logToConsole = false;

  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JRawTypeTest() throws UnableToCompleteException {
  }

  @Override
  public void testFindNestedType() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetEnclosingType() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetInheritableMethods() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetNestedType() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetNestedTypes() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetOverridableMethods() {
    // TODO Auto-generated method stub
  }

  @Override
  public void testGetSubtypes() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType testType = oracle.getType(MyList.class.getName());
    JGenericType genericTestType = testType.isGenericType();
    assertNotNull(genericTestType);

    JRawType rawTestType = genericTestType.getRawType();
    JClassType[] expectedTypes = new JClassType[]{
        oracle.getType(MyCustomList.class.getName()).isGenericType().getRawType(),
        oracle.getType(MyIntegerList.class.getName())};
    JClassType[] actualSubtypes = rawTestType.getSubtypes();

    validateEquals(expectedTypes, actualSubtypes);
  }

  @Override
  public void testIsAssignableFrom() throws NotFoundException {
    JRawType rawType = getTestType();
    assertTrue(rawType.isAssignableFrom(rawType.getBaseType()));
  }

  @Override
  public void testIsAssignableTo() throws NotFoundException {
    JRawType rawType = getTestType();
    assertTrue(rawType.getBaseType().isAssignableTo(rawType));
  }

  @Override
  protected Substitution getSubstitution() throws NotFoundException {
    return new Substitution() {
      public JClassType getSubstitution(JClassType type) {
        return type.getErasedType();
      }
    };
  }

  /**
   * Returns the RawType for {@link ArrayList}.
   */
  @Override
  protected JRawType getTestType() throws NotFoundException {
    TypeOracle oracle = moduleContext.getOracle();
    JClassType testType = oracle.getType(ArrayList.class.getName());
    return testType.isGenericType().getRawType();
  }
}
