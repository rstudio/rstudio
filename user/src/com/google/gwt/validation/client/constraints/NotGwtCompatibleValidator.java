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
package com.google.gwt.validation.client.constraints;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Masks a {@link ConstraintValidator} that is not GWT compatible. This
 * validator always fails.
 * 
 * @param <A> the constraint to validate
 * @param <T> the type to validate
 */
public abstract class NotGwtCompatibleValidator<A extends Annotation, T>
    implements ConstraintValidator<A, T> {

  @Override
  public final void initialize(A constraintAnnotation) {
  }

  /**
   * Always fails.
   */
  @Override
  public final boolean isValid(T value, ConstraintValidatorContext context) {
    // TODO (nchalko) add a custom message
    return false;
  }

}
