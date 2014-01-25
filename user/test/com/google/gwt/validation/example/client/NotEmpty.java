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
package com.google.gwt.validation.example.client;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * The annotated element size must at least 1 and cannot be null. From the JSR
 * 303 spec.
 */
@Documented
@NotNull
@Size(min = 1)
@ReportAsSingleViolation
@Constraint(validatedBy = NotEmpty.NotEmptyValidator.class)
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface NotEmpty {
  String message() default "{com.google.gwt.validation.example.client.NotEmpty.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * From the JSR 303 spec.
   */
  @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
  @Retention(RUNTIME)
  @Documented
  @interface List {
    NotEmpty[] value();
  }
  /**
   * Sample composite validator.
   */
  class NotEmptyValidator implements ConstraintValidator<NotEmpty, String> {
    @Override
    public void initialize(NotEmpty constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
      return true;
    }
  }
}
