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

import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Base test cases for all {@link JDelegatingClassType}s.
 */
public abstract class JDelegatingClassTypeTestBase extends TestCase {

  private static final Comparator<? super Annotation> ANNOTATION_COMPARATOR = new Comparator<Annotation>() {
    public int compare(Annotation o1, Annotation o2) {
      // Just use toString, it contains all info.
      return o1.toString().compareTo(o2.toString());
    }
  };

  protected static void assertArraysEqual(Object[] expected, Object[] actual) {
    assertTrue("Expected: \n" + Arrays.toString(expected) + ",\n Actual: \n"
        + Arrays.toString(actual), Arrays.equals(expected, actual));
  }

  protected static void validateAbstractMethodSubstitution(
      JAbstractMethod preSubMethod, JAbstractMethod postSubMethod,
      Substitution substitution) {

    assertEquals(preSubMethod.getName(), postSubMethod.getName());

    assertEquals(preSubMethod.getModifierBits(),
        postSubMethod.getModifierBits());

    validateAnnotations(preSubMethod.getAnnotations(),
        postSubMethod.getAnnotations());

    JParameter[] preSubParams = preSubMethod.getParameters();
    JParameter[] postSubParams = postSubMethod.getParameters();

    assertEquals(preSubParams.length, postSubParams.length);

    for (int j = 0; j < preSubParams.length; ++j) {
      JParameter preSubParam = preSubParams[j];
      JParameter postSubParam = postSubParams[j];

      validateAnnotations(preSubParam.getAnnotations(),
          postSubParam.getAnnotations());

      assertEquals(substitute(substitution, preSubParam.getType()),
          postSubParam.getType());
    }

    JClassType[] preSubThrows = preSubMethod.getThrows();
    JClassType[] postSubThrows = postSubMethod.getThrows();
    assertArraysEqual(preSubThrows, postSubThrows);
  }

  private static JType substitute(Substitution substitution, JType type) {
    if (!(type instanceof JClassType)) {
      return type;
    }
    JClassType t2 = (JClassType) type;
    JType substitution2 = substitution.getSubstitution(t2);
    return substitution2;
  }

  protected static void validateAnnotations(Annotation[] expected,
      Annotation[] actual) {
    Arrays.sort(expected, ANNOTATION_COMPARATOR);
    Arrays.sort(actual, ANNOTATION_COMPARATOR);
    assertArraysEqual(expected, actual);
  }

  protected static void validateConstructorSubstitutions(
      JClassType preSubstitution, JClassType postSubstituion,
      Substitution substitution) {
    // Check the constructors
    JConstructor[] preSubCtors = preSubstitution.getConstructors();
    JConstructor[] postSubCtors = postSubstituion.getConstructors();
    assertEquals(preSubCtors.length, postSubCtors.length);
    for (int i = 0; i < preSubCtors.length; ++i) {
      validateAbstractMethodSubstitution(preSubCtors[i], postSubCtors[i],
          substitution);
    }
  }

  protected static void validateDeclaredAnnotations(Annotation[] expected,
      Annotation[] actual) {
    Arrays.sort(expected, ANNOTATION_COMPARATOR);
    Arrays.sort(actual, ANNOTATION_COMPARATOR);
    assertArraysEqual(expected, actual);
  }

  protected static void validateEquals(JClassType[] expectedTypes,
      JClassType actualTypes[]) {
    TypeOracle.sort(expectedTypes);
    TypeOracle.sort(actualTypes);

    assertArraysEqual(expectedTypes, actualTypes);
  }

  protected static void validateFieldSubstitutions(JClassType preSubstitution,
      JClassType postSubstituion, Substitution substitution) {
    // Check the fields
    JField[] preSubfields = preSubstitution.getFields();
    JField[] postSubFields = postSubstituion.getFields();
    assertEquals(preSubfields.length, postSubFields.length);
    for (int i = 0; i < preSubfields.length; ++i) {
      JField postSubField = postSubstituion.getField(preSubfields[i].getName());
      assertNotNull(postSubField);
      assertEquals(substitute(substitution, preSubfields[i].getType()),
          postSubField.getType());
    }
  }

  protected static void validateFindConstructor(JClassType preSubstitution,
      JClassType postSubstitution, Substitution substitution) {

    JConstructor[] constructors = preSubstitution.getConstructors();
    for (JConstructor constructor : constructors) {
      JParameter[] params = constructor.getParameters();
      JType[] paramTypes = new JType[params.length];

      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = substitute(substitution, params[i].getType());
      }

      assertNotNull(postSubstitution.findConstructor(paramTypes));
    }
  }

  /**
   * 
   */
  protected static void validateFindField(JClassType preSubstitution,
      JClassType postSubstitution) {
    JField[] fields = preSubstitution.getFields();
    for (JField field : fields) {
      assertNotNull(postSubstitution.findField(field.getName()));
    }
  }

  protected static void validateFindMethod(JClassType preSubstitution,
      JClassType postSubstitution, Substitution substitution) {

    JMethod[] methods = preSubstitution.getMethods();
    for (JMethod method : methods) {
      JParameter[] params = method.getParameters();
      JType[] paramTypes = new JType[params.length];

      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = substitute(substitution, params[i].getType());
      }

      assertNotNull(postSubstitution.findMethod(method.getName(), paramTypes));
    }
  }

  protected static void validateGetConstructor(JClassType preSubstitution,
      JClassType postSubstitution, Substitution substitution)
      throws NotFoundException {

    JConstructor[] constructors = preSubstitution.getConstructors();
    for (JConstructor constructor : constructors) {
      JParameter[] params = constructor.getParameters();
      JType[] paramTypes = new JType[params.length];

      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = substitute(substitution, params[i].getType());
      }

      assertNotNull(postSubstitution.getConstructor(paramTypes));
    }
  }

  /**
   * 
   */
  protected static void validateGetField(JClassType preSubstitution,
      JClassType postSubstitution) {
    JField[] fields = preSubstitution.getFields();
    for (JField field : fields) {
      assertNotNull(postSubstitution.getField(field.getName()));
    }
  }

  protected static void validateGetMethod(JClassType preSubstitution,
      JClassType postSubstitution, Substitution substitution)
      throws NotFoundException {

    JMethod[] methods = preSubstitution.getMethods();
    for (JMethod method : methods) {
      JParameter[] params = method.getParameters();
      JType[] paramTypes = new JType[params.length];

      for (int i = 0; i < params.length; ++i) {
        paramTypes[i] = substitute(substitution, params[i].getType());
      }

      assertNotNull(postSubstitution.getMethod(method.getName(), paramTypes));
    }
  }

  protected static void validateGetOverloads(JClassType preSubstitution,
      JClassType postSubstitution) {
    JMethod[] methods = preSubstitution.getMethods();
    for (JMethod method : methods) {
      assertEquals(preSubstitution.getOverloads(method.getName()).length,
          postSubstitution.getOverloads(method.getName()).length);
    }
  }

  protected static void validateImplementedInterfaceSubstitution(
      JClassType preSubstitution, JClassType postSubstituion,
      Substitution substitution) {
    JClassType[] preSubIntfs = preSubstitution.getImplementedInterfaces();
    JClassType[] postSubIntfs = postSubstituion.getImplementedInterfaces();

    assertEquals(preSubIntfs.length, postSubIntfs.length);

    for (int i = 0; i < preSubIntfs.length; ++i) {
      assertEquals(postSubIntfs[i],
          substitution.getSubstitution(postSubIntfs[i]));
    }
  }

  protected static void validateMethodSubstitution(JMethod preSubMethod,
      JMethod postSubMethod, Substitution substitution) {

    assertEquals(substitute(substitution, preSubMethod.getReturnType()),
        postSubMethod.getReturnType());

    validateAbstractMethodSubstitution(preSubMethod, postSubMethod,
        substitution);
  }

  protected static void validateMethodSubstitutions(JClassType preSubstitution,
      JClassType postSubstituion, Substitution substitution)
      throws NotFoundException {
    // Check the methods
    JMethod[] preSubMethods = preSubstitution.getMethods();
    JMethod[] postSubMethods = postSubstituion.getMethods();
    assertEquals(preSubMethods.length, postSubMethods.length);

    for (int i = 0; i < preSubMethods.length; ++i) {
      JMethod preSubMethod = preSubMethods[i];

      JParameter[] preSubParams = preSubMethod.getParameters();
      JType[] postSubParamTypes = new JType[preSubParams.length];
      for (int j = 0; j < preSubParams.length; ++j) {
        postSubParamTypes[j] = substitute(substitution,
            preSubParams[j].getType());
      }
      JMethod postSubMethod = postSubstituion.getMethod(preSubMethod.getName(),
          postSubParamTypes);

      validateMethodSubstitution(preSubMethod, postSubMethod, substitution);
    }
  }

  protected static void validateTypeSubstitution(JClassType preSubstitution,
      JClassType postSubstituion, Substitution substitution)
      throws NotFoundException {
    if (preSubstitution.isGenericType() == null) {
      return;
    }

    assertEquals(preSubstitution.getModifierBits(),
        postSubstituion.getModifierBits());

    validateAnnotations(preSubstitution.getAnnotations(),
        postSubstituion.getAnnotations());

    assertEquals(preSubstitution.getName(), postSubstituion.getName());
    // assertEquals(preSubstitution.getSubstitution(substitution),
    // postSubstituion);

    // Check superclass
    JClassType superClass = preSubstitution.getSuperclass();
    if (superClass != null) {
      validateTypeSubstitution(superClass, postSubstituion.getSuperclass(),
          substitution);
    }

    // Check interfaces
    validateImplementedInterfaceSubstitution(preSubstitution, postSubstituion,
        substitution);

    validateFieldSubstitutions(preSubstitution, postSubstituion, substitution);

    validateConstructorSubstitutions(preSubstitution, postSubstituion,
        substitution);

    validateMethodSubstitutions(preSubstitution, postSubstituion, substitution);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#findConstructor(JType[])}
   * .
   */
  public void testFindConstructor() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateFindConstructor(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#findField(java.lang.String)}
   * .
   */
  public void testFindField() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateFindField(baseType, testType);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#findMethod(java.lang.String, JType[])}
   * .
   */
  public void testFindMethod() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateFindMethod(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#findNestedType(java.lang.String)}
   * .
   */
  public abstract void testFindNestedType() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getAnnotation(java.lang.Class)}
   * .
   */
  public void testGetAnnotation() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    Annotation[] annotations = baseType.getAnnotations();
    for (Annotation annotation : annotations) {
      assertNotNull(testType.getAnnotation(annotation.annotationType()));
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getAnnotations()}
   * .
   */
  public void testGetAnnotations() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateAnnotations(baseType.getAnnotations(), testType.getAnnotations());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getBaseType()}
   * .
   */
  public void testGetBaseType() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    assertNotNull(testType.getBaseType());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getConstructor(JType[])}
   * .
   */
  public void testGetConstructor() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();
    baseType.getConstructors();
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getConstructors()}
   * .
   */
  public void testGetConstructors() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateConstructorSubstitutions(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getDeclaredAnnotations()}
   * .
   */
  public void testGetDeclaredAnnotations() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateDeclaredAnnotations(baseType.getDeclaredAnnotations(),
        testType.getDeclaredAnnotations());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getEnclosingType()}
   * .
   */
  public abstract void testGetEnclosingType() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getErasedType()}
   * .
   */
  public void testGetErasedType() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getErasedType(), testType.getErasedType());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getField(java.lang.String)}
   * .
   */
  public void testGetField() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateGetField(baseType, testType);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getFields()}.
   */
  public void testGetFields() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateFieldSubstitutions(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getImplementedInterfaces()}
   * .
   */
  public void testGetImplementedInterfaces() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateImplementedInterfaceSubstitution(baseType, testType,
        getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getInheritableMethods()}
   * .
   */
  public abstract void testGetInheritableMethods() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getJNISignature()}
   * .
   */
  public void testGetJNISignature() {
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getMethod(java.lang.String, JType[])}
   * .
   */
  public void testGetMethod() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateGetMethod(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getMethods()}.
   */
  public void testGetMethods() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateMethodSubstitutions(baseType, testType, getSubstitution());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getModifierBits()}
   * .
   */
  public void testGetModifierBits() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getModifierBits(), testType.getModifierBits());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getName()}.
   */
  public void testGetName() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getName(), testType.getName());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getNestedType(java.lang.String)}
   * .
   */
  public abstract void testGetNestedType() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getNestedTypes()}
   * .
   */
  public abstract void testGetNestedTypes() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getOracle()}.
   */
  public void testGetOracle() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getOracle(), testType.getOracle());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getOverloads(java.lang.String)}
   * .
   */
  public void testGetOverloads() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    validateGetOverloads(baseType, testType);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getOverridableMethods()}
   * .
   */
  public abstract void testGetOverridableMethods() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getPackage()}.
   */
  public void testGetPackage() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getPackage(), testType.getPackage());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getSubtypes()}
   * .
   */
  public abstract void testGetSubtypes() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#getSuperclass()}
   * .
   */
  public void testGetSuperclass() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.getSuperclass() != null,
        testType.getSuperclass() != null);

    /*
     * TODO: need to check that the super classes are consistent, if base super
     * is generic then test type super should be parameterized with same super,
     * etc.
     */
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isAbstract()}.
   */
  public void testIsAbstract() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isAbstract(), testType.isAbstract());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isAnnotationPresent(java.lang.Class)}
   * .
   */
  public void testIsAnnotationPresent() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    Annotation[] annotations = baseType.getAnnotations();
    for (Annotation annotation : annotations) {
      assertTrue(testType.isAnnotationPresent(annotation.annotationType()));
    }
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isArray()}.
   */
  public void testIsArray() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isArray(), testType.isArray());

    // TODO: check parameterized arrays
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isAssignableFrom(com.google.gwt.core.ext.typeinfo.JClassType)}
   * .
   */
  public abstract void testIsAssignableFrom() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isAssignableTo(com.google.gwt.core.ext.typeinfo.JClassType)}
   * .
   */
  public abstract void testIsAssignableTo() throws NotFoundException;

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isClass()}.
   */
  public void testIsClass() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isClass() != null, testType.isClass() != null);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isClassOrInterface()}
   * .
   */
  public void testIsClassOrInterface() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isClassOrInterface() != null,
        testType.isClassOrInterface() != null);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isDefaultInstantiable()}
   * .
   */
  public void testIsDefaultInstantiable() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isDefaultInstantiable(),
        testType.isDefaultInstantiable());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isEnum()}.
   */
  public void testIsEnum() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isEnum() != null, testType.isEnum() != null);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isInterface()}
   * .
   */
  public void testIsInterface() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isInterface() != null, testType.isInterface() != null);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isMemberType()}
   * .
   */
  public void testIsMemberType() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isMemberType(), testType.isMemberType());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isPrimitive()}
   * .
   */
  public void testIsPrimitive() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertNull(testType.isPrimitive());
    assertNull(baseType.isPrimitive());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isPrivate()}.
   */
  public void testIsPrivate() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isPrivate(), testType.isPrivate());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isProtected()}
   * .
   */
  public void testIsProtected() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isProtected(), testType.isProtected());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isPublic()}.
   */
  public void testIsPublic() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isPublic(), testType.isPublic());
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.JDelegatingClassType#isStatic()}.
   */
  public void testIsStatic() throws NotFoundException {
    JDelegatingClassType testType = getTestType();
    JClassType baseType = testType.getBaseType();

    assertEquals(baseType.isStatic(), testType.isStatic());
  }

  protected abstract Substitution getSubstitution() throws NotFoundException;

  protected abstract JDelegatingClassType getTestType()
      throws NotFoundException;
}
