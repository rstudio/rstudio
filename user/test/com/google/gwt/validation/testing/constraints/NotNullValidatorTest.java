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

import com.google.gwt.validation.client.constraints.NotNullValidator;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.NotNull;

/**
 * Tests for {@link NotNullValidator}.
 */
public class NotNullValidatorTest extends
    ConstraintValidatorTestCase<NotNull, Object> {

  @SuppressWarnings("unused")
  @NotNull
  private String defaultField;

  public void testIsValid_notNull() {
    ConstraintValidatorTestCase.assertConstraintValidator(validator,
        getDefaultAnnotation(), null, new Object(), true);
  }

  @Override
  protected boolean isNullValid() {
    return false;
  }

  @Override
  protected Class<NotNull> getAnnotationClass() {
    return NotNull.class;
  }

  @Override
  protected ConstraintValidator<NotNull, Object> createValidator() {
    return new NotNullValidator();
  }
}
