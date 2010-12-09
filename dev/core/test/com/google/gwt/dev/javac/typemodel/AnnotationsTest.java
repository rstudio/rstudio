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

import com.google.gwt.dev.util.collect.IdentityHashMap;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * Test cases for the {@link Annotations} class.
 */
public class AnnotationsTest extends TestCase {

  @TestAnnotation1("1")
  private static class AnnotatedClass1 {
  }

  @TestAnnotation2("2")
  private static class AnnotatedClass2 extends AnnotatedClass1 {
  }

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface TestAnnotation1 {
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface TestAnnotation2 {
    String value();
  }

  @Inherited
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface UnusedAnnotation {
    String value();
  }

  /**
   * Initializes an {@link Annotations} instance from a given {@link Class}.
   */
  private static Annotations initializeAnnotationsFromClass(
      Class<?> annotatedClass, Annotations parent) {
    Annotation[] jAnnotations = annotatedClass.getDeclaredAnnotations();

    Map<Class<? extends Annotation>, Annotation> map = new IdentityHashMap<Class<? extends Annotation>, Annotation>();
    for (Annotation annotation : jAnnotations) {
      map.put(annotation.annotationType(), annotation);
    }
    Annotations annotations = new Annotations(map);

    if (parent != null) {
      annotations.setParent(parent);
    }

    return annotations;
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#addAnnotations(java.util.Map)}
   * .
   */
  public void testAddAnnotations() {
    Annotations annotations = new Annotations();
    Map<Class<? extends Annotation>, Annotation> entries = new HashMap<Class<? extends Annotation>, Annotation>();
    entries.put(TestAnnotation1.class,
        AnnotatedClass1.class.getAnnotation(TestAnnotation1.class));
    annotations.addAnnotations(entries);
    assertNotNull(annotations.getAnnotation(TestAnnotation1.class));
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#getAnnotation(java.lang.Class)}
   * .
   * 
   * case 1: annotation is a declared case 2: annotation is inherited case 3:
   * annotation is not found
   */
  public void testGetAnnotationDeclared() {
    Annotations annotations = initializeAnnotationsFromClass(
        AnnotatedClass1.class, null);
    assertNotNull(annotations.getAnnotation(TestAnnotation1.class));
    assertNull(annotations.getAnnotation(UnusedAnnotation.class));
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#getAnnotation(java.lang.Class)}
   * .
   * 
   * case 1: annotation is a declared case 2: annotation is inherited case 3:
   * annotation is not found
   */
  public void testGetAnnotationInherited() {
    Annotations annotations1 = initializeAnnotationsFromClass(
        AnnotatedClass1.class, null);
    Annotations annotations2 = initializeAnnotationsFromClass(
        AnnotatedClass2.class, annotations1);

    assertNotNull(annotations2.getAnnotation(TestAnnotation1.class));
    assertNull(annotations2.getAnnotation(UnusedAnnotation.class));
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#getAnnotations()}.
   */
  public void testGetAnnotations() {
    Annotations annotations1 = initializeAnnotationsFromClass(
        AnnotatedClass1.class, null);
    Annotations annotations2 = initializeAnnotationsFromClass(
        AnnotatedClass2.class, annotations1);

    assertEquals(2, annotations2.getAnnotations().length);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#getDeclaredAnnotations()}
   * .
   */
  public void testGetDeclaredAnnotations() {
    Annotations annotations1 = initializeAnnotationsFromClass(
        AnnotatedClass1.class, null);
    Annotations annotations2 = initializeAnnotationsFromClass(
        AnnotatedClass2.class, annotations1);

    assertEquals(1, annotations2.getDeclaredAnnotations().length);
    assertEquals(1, annotations1.getDeclaredAnnotations().length);
  }

  /**
   * Test method for
   * {@link com.google.gwt.core.ext.typeinfo.Annotations#isAnnotationPresent(java.lang.Class)}
   * .
   */
  public void testIsAnnotationPresent() {
    Annotations annotations1 = initializeAnnotationsFromClass(
        AnnotatedClass1.class, null);
    Annotations annotations2 = initializeAnnotationsFromClass(
        AnnotatedClass2.class, annotations1);

    assertTrue(annotations2.isAnnotationPresent(TestAnnotation1.class));
    assertTrue(annotations2.isAnnotationPresent(TestAnnotation2.class));
    assertFalse(annotations2.isAnnotationPresent(UnusedAnnotation.class));
  }
}
