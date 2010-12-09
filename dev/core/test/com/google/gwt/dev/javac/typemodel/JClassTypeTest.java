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
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

/**
 * Tests related to JClassType. See individual test methods to details.
 */
public class JClassTypeTest extends TestCase {
  private final boolean logToConsole = false;
  private final ModuleContext moduleContext = new ModuleContext(logToConsole
      ? new PrintWriterTreeLogger() : TreeLogger.NULL,
      "com.google.gwt.dev.javac.typemodel.TypeOracleTest");

  public JClassTypeTest() throws UnableToCompleteException {
  }

  public void testGetInheritableOrOverridableMethods()
      throws TypeOracleException {
    TypeOracle typeOracle = moduleContext.getOracle();
    // TypeOracle typeOracle = buildOracleFromTestPackage(logger);

    String[] noParams = new String[0];
    String[] intObjectParams = new String[]{"int", "java.lang.Object"};

    // Verify IA.
    {
      JClassType type = typeOracle.getType("com.google.gwt.dev.javac.typemodel.test.IA");
      JMethod[] leafMethods = type.getOverridableMethods();
      assertEquals(3, leafMethods.length);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IA", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IA", "ia", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IA", "foo", noParams);
    }

    // Verify IB.
    {
      JClassType type = typeOracle.getType("com.google.gwt.dev.javac.typemodel.test.IB");
      JMethod[] leafMethods = type.getOverridableMethods();
      assertEquals(5, leafMethods.length);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IB", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IB", "ia", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.IB", "ib", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.IB", "ib", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.IB", "foo", noParams);
    }

    // Verify IC.
    {
      JClassType type = typeOracle.getType("com.google.gwt.dev.javac.typemodel.test.IC");
      JMethod[] leafMethods = type.getOverridableMethods();
      assertEquals(7, leafMethods.length);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ia", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ib", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ib", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ic", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.IC", "ic", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.IC", "foo", noParams);
    }

    // Both overloads of ia are only declared in IA, so all searches should find
    // them there.
    {
      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CA", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CB", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ia", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CA", "ia", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CB", "ia", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ia", intObjectParams);
    }

    // Both overloads of ib are declared in both IB and IC, so
    // - searching for ib in CB will return IB
    // - searching for ib in CC will return IC
    {
      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.CB", "ib", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ib", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.CB", "ib", intObjectParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ib", intObjectParams);
    }

    // Both overloads of ic are declared only in IC, but ic() is also declared
    // in CB, so
    // - searching for ic() in CB will return CB
    // - searching for ic() in CC will return CB
    // - searching for ic(int, Object) in CC will return IC
    {
      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CB",
          "com.google.gwt.dev.javac.typemodel.test.CB", "ic", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CB",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ic", noParams);

      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IC",
          "com.google.gwt.dev.javac.typemodel.test.CC", "ic", intObjectParams);
    }

    // Both IA and IB define foo(), and searching for foo() on IC should return
    // IB.foo(). This matters because IC also extends IA, so a naive algorithm
    // for getLeafMethods() might prefer IA.foo() to IB.foo().
    {
      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IB",
          "com.google.gwt.dev.javac.typemodel.test.IC", "foo", noParams);
    }

    // Both IA and CB define foo(), foo() being final in CB, so searching for
    // foo() on CA should return IA.foo() as overridable, while searching for
    // foo() on CB or CC should return CB.foo() as inheritable but not
    // overridable.
    {
      assertMethodOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.IA",
          "com.google.gwt.dev.javac.typemodel.test.CA", "foo", noParams);

      assertMethodInheritableNotOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CB",
          "com.google.gwt.dev.javac.typemodel.test.CB", "foo", noParams);

      assertMethodInheritableNotOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CB",
          "com.google.gwt.dev.javac.typemodel.test.CC", "foo", noParams);

      // Check that we aren't including methods that aren't actually overridable
      // (but are inheritable) because they are final (but non-private).
      assertMethodInheritableNotOverridable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CA",
          "com.google.gwt.dev.javac.typemodel.test.CA",
          "caNotOverridableFinal", noParams);

      // Check that we aren't including methods that aren't actually inheritable
      // because they are private.
      assertMethodNotInheritable(typeOracle,
          "com.google.gwt.dev.javac.typemodel.test.CA",
          "com.google.gwt.dev.javac.typemodel.test.CA",
          "caNotOverridablePrivate", noParams);
    }
  }

  private void assertMethodInheritableNotOverridable(TypeOracle typeOracle,
      String expectedTypeName, String searchTypeName, String methodName,
      String[] paramTypeNames) throws TypeOracleException {
    assertInheritableOrOverridableMethod(true, false, typeOracle,
        expectedTypeName, searchTypeName, methodName, paramTypeNames);
  }

  private void assertMethodNotInheritable(TypeOracle typeOracle,
      String expectedTypeName, String searchTypeName, String methodName,
      String[] paramTypeNames) throws TypeOracleException {
    assertInheritableOrOverridableMethod(false, false, typeOracle,
        expectedTypeName, searchTypeName, methodName, paramTypeNames);
  }

  private void assertMethodOverridable(TypeOracle typeOracle,
      String expectedTypeName, String searchTypeName, String methodName,
      String[] paramTypeNames) throws TypeOracleException {
    assertInheritableOrOverridableMethod(true, true, typeOracle,
        expectedTypeName, searchTypeName, methodName, paramTypeNames);
  }

  private void assertInheritableOrOverridableMethod(
      boolean shouldBeInheritable, boolean shouldBeOverridable,
      TypeOracle oracle, String expectedTypeName, String searchTypeName,
      String methodName, String[] paramTypeNames) throws TypeOracleException {

    JType[] paramTypes = new JType[paramTypeNames.length];
    for (int i = 0; i < paramTypeNames.length; i++) {
      String paramTypeName = paramTypeNames[i];
      paramTypes[i] = oracle.parse(paramTypeName);
    }

    JClassType expectedType = oracle.getType(expectedTypeName);
    JClassType searchType = oracle.getType(searchTypeName);

    assertMethodInclusion(shouldBeInheritable,
        searchType.getInheritableMethods(), methodName, expectedType,
        paramTypes);
    assertMethodInclusion(shouldBeOverridable,
        searchType.getOverridableMethods(), methodName, expectedType,
        paramTypes);
  }

  private void assertMethodInclusion(boolean shouldBeFound,
      JMethod[] leafMethods, String methodName, JClassType expectedType,
      JType[] paramTypes) {
    boolean wasFound = false;

    for (JMethod method : leafMethods) {
      if (method.getName().equals(methodName)) {
        if (method.hasParamTypes(paramTypes)) {
          assertEquals(expectedType, method.getEnclosingType());
          wasFound = true;
          break;
        }
      }
    }

    if (shouldBeFound) {
      if (wasFound) {
        // Good. We wanted to find it and we did.
      } else {
        fail("Did not find expected method '" + methodName + "' on type '"
            + expectedType.getQualifiedSourceName() + "'");
      }
    } else {
      // We want to *not* find it.
      if (wasFound) {
        fail("Did not expect to find method '" + methodName + "' on type '"
            + expectedType.getQualifiedSourceName() + "'");
      } else {
        // Good. We didn't want to find it and didn't.
      }
    }
  }
}
