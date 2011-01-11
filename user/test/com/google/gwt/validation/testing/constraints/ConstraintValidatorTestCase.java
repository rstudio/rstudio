/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.testing.constraints;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * TestCase for {@link javax.validation.ConstraintValidator}s.
 * <p>
 * Subclasses must :
 * <ul>
 * <li>implement {@link #getAnnotationClass()}</li>
 * <li>implement {@link #createValidator()}.</li>
 * <li>define a field called <code>defaultField</code> and annotate it with the
 * constraint Annotation under test</li>
 * </ul>
 *
 * <p>
 * If additional instances of the annotation need to be tested you can get there
 * value with {@link #getAnnotation(Class, Class, String)}
 *
 * @param <A> The constraint Annotation of the ConstraintValidator under test
 * @param <T> The bean type of the ConstraintValidator under test
 */
public abstract class ConstraintValidatorTestCase<A extends Annotation, T>
    extends TestCase {

  public final static String DEFAULT_ANNOTATED_FIELD_NAME = "defaultField";

  // Validators are reusable but not thread safe
  protected final ConstraintValidator<A, T> validator = createValidator();

  // private ConstraintValidatorContext context;

  public ConstraintValidatorTestCase() {
    super();
  }

  /**
   * All constraints except {@link NotNull} should return true for a null value.
   */
  public final void testIsValid_null() {
    ConstraintValidatorTestCase.assertConstraintValidator(validator,
        getDefaultAnnotation(), null, null, isNullValid());
  }

  /**
   * Override this if a null value should not be valid. This is called by
   * {@link #testIsValid_null()}
   */
  protected boolean isNullValid() {
    return true;
  }

  /**
   * Get the annotation of type {@link #getAnnotationClass()} on the field named
   * <code>defaultField</code>.
   */
  protected final A getDefaultAnnotation() {
    return getAnnotation(getAnnotationClass(), this.getClass(),
        DEFAULT_ANNOTATED_FIELD_NAME);
  }

  protected abstract Class<A> getAnnotationClass();

  protected A getAnnotation(Class<A> annotationClass, Class<?> objectClass,
      String fieldName) {

    for (Field field : objectClass.getDeclaredFields()) {
      if (field.getName().equals(fieldName)) {
        A annotation = field.getAnnotation(annotationClass);
        if (annotation == null) {
          throw new IllegalArgumentException(objectClass + "." + fieldName
              + " is not annotated with " + annotationClass);
        }
        return annotation;
      }
    }
    throw new IllegalArgumentException(objectClass
        + " does not have a field called " + fieldName);
  }

  protected abstract ConstraintValidator<A, T> createValidator();

  /**
   * Assert result of validating <code>value</code> with <code>validator</code>
   * initialized with the annotation of the <code>defaultField</code>.
   *
   * @param value the Value to validate
   * @param expected the expected result of a calling <code>isValid</code>
   */
  protected void assertConstraintValidator(T value, boolean expected) {
    assertConstraintValidator(validator, getDefaultAnnotation(), null, value,
        expected);
  }

  /**
   * Asserts the validity of a value using the given validator, annotation, and
   * context.
   *
   * @param <T> object type
   * @param <A> the constraint annotation type
   * @param validator the {@link ConstraintValidator} to test
   * @param annotation the constraint annotation to initialize the validator.
   * @param context The context for the validator
   * @param value the value to validate.
   * @param expected the expected result
   */
  public static <T, A extends Annotation> void assertConstraintValidator(
      ConstraintValidator<A, T> validator, A annotation,
      ConstraintValidatorContext context, T value, boolean expected) {
    validator.initialize(annotation);
    String message = validator.getClass().getName() + "(" + annotation
        + ").isValid(" + value + ", " + context + ")";
    assertEquals(message, expected, validator.isValid(value, context));
  }

}
