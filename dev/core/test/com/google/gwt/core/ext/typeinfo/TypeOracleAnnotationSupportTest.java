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
import com.google.gwt.core.ext.typeinfo.test.AnnotatedClass;
import com.google.gwt.core.ext.typeinfo.test.TestAnnotation;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Test cases for the {@link TypeOracle}'s {@link Annotation} support.
 * 
 * Array annotations Enum annotations String from field annotations
 */
public class TypeOracleAnnotationSupportTest extends TestCase {
  static {
    ModuleDefLoader.setEnableCachingModules(true);
  }

  private static void validateAnnotation(HasAnnotations annotatedElement,
      String testAnnotationValue, String nestedAnnotationValue,
      TestAnnotation realAnnotation) {
    assertNotNull(annotatedElement);

    TestAnnotation testAnnotation = annotatedElement.getAnnotation(TestAnnotation.class);
    assertNotNull(testAnnotation);

    // Check our proxy objects against the real thing.
    assertEquals(realAnnotation, testAnnotation);
    assertEquals(realAnnotation.hashCode(), testAnnotation.hashCode());

    // tobyr doesn't like this.
    // assertEquals(realAnnotation.toString(), testAnnotation.toString());

    // checks default value
    assertEquals(testAnnotationValue, testAnnotation.value());
    assertEquals(nestedAnnotationValue,
        testAnnotation.nestedAnnotation().value());
  }

  private final TreeLogger logger = TreeLogger.NULL;
  private ModuleDef moduleDef;

  private final TypeOracle typeOracle;

  public TypeOracleAnnotationSupportTest() throws UnableToCompleteException {
    moduleDef = ModuleDefLoader.loadFromClassPath(logger,
        "com.google.gwt.core.ext.typeinfo.TypeOracleTest");
    typeOracle = moduleDef.getTypeOracle(logger);
  }

  /**
   * Test that a class can be annotated.
   */
  public void testAnnotatedClass() throws NotFoundException {
    JClassType annotatedClass = typeOracle.getType(AnnotatedClass.class.getName());

    TestAnnotation realAnnotation = AnnotatedClass.class.getAnnotation(TestAnnotation.class);
    validateAnnotation(annotatedClass, "Class", "Foo", realAnnotation);

    assertEquals(1, annotatedClass.getAnnotations().length);
  }

  /**
   * Test that a constructor can be annotated.
   */
  public void testAnnotatedConstructor() throws NotFoundException,
      SecurityException, NoSuchMethodException {
    JClassType annotatedClass = typeOracle.getType(AnnotatedClass.class.getName());
    JConstructor ctor = annotatedClass.getConstructor(new JType[0]);

    Constructor<AnnotatedClass> constructor = AnnotatedClass.class.getConstructor();
    TestAnnotation realAnnotation = constructor.getAnnotation(TestAnnotation.class);

    validateAnnotation(ctor, "Constructor", "Not assigned", realAnnotation);
  }

  /**
   * Test that a field can be annotated.
   */
  public void testAnnotatedField() throws NotFoundException, SecurityException,
      NoSuchFieldException {
    JClassType annotatedClass = typeOracle.getType(AnnotatedClass.class.getName());
    JField annotatedField = annotatedClass.getField("annotatedField");

    Field field = AnnotatedClass.class.getDeclaredField("annotatedField");
    TestAnnotation realAnnotation = field.getAnnotation(TestAnnotation.class);

    validateAnnotation(annotatedField, "Field", "Not assigned", realAnnotation);
  }

  /**
   * Tests that methods can be annotated.
   */
  public void testAnnotatedMethod() throws NotFoundException,
      SecurityException, NoSuchMethodException {
    JClassType annotatedClass = typeOracle.getType(AnnotatedClass.class.getName());
    JMethod annotatedMethod = annotatedClass.getMethod("annotatedMethod",
        new JType[0]);

    Method method = AnnotatedClass.class.getDeclaredMethod("annotatedMethod");
    TestAnnotation realAnnotation = method.getAnnotation(TestAnnotation.class);

    validateAnnotation(annotatedMethod, "Method", "Not assigned",
        realAnnotation);
  }

  /**
   * Tests that packages can be annotated. This necessitates the existence of a
   * package-info.java file in the package that you wish to annotate.
   */
  public void testAnnotatedPackage() throws NotFoundException,
      ClassNotFoundException {
    JPackage annotatedPackage = typeOracle.getPackage("com.google.gwt.core.ext.typeinfo.test");
    assertNotNull(annotatedPackage);

    TestAnnotation realAnnotation = Class.forName(
        "com.google.gwt.core.ext.typeinfo.test.package-info").getAnnotation(
        TestAnnotation.class);

    validateAnnotation(annotatedPackage, "Package", "Not assigned",
        realAnnotation);
  }

  /**
   * Tests that parameters can be annotated.
   */
  public void testAnnotatedParameter() throws NotFoundException,
      SecurityException, NoSuchMethodException {
    JClassType annotatedClass = typeOracle.getType(AnnotatedClass.class.getName());
    JMethod jmethod = annotatedClass.getMethod("methodWithAnnotatedParameter",
        new JType[] {JPrimitiveType.INT});
    JParameter parameter = jmethod.getParameters()[0];

    Method method = AnnotatedClass.class.getDeclaredMethod(
        "methodWithAnnotatedParameter", int.class);
    Annotation[][] paramAnnotations = method.getParameterAnnotations();
    TestAnnotation realAnnotation = (TestAnnotation) paramAnnotations[0][0];

    validateAnnotation(parameter, "Parameter", "Not assigned", realAnnotation);
  }
}
