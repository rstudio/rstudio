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

import com.google.gwt.validation.client.constraints.AssertTrueValidator;

import javax.validation.constraints.AssertTrue;

/**
 * Tests for {@link AssertTrueValidator}.
 */
public class AssertTrueValidatorTest extends
    ConstraintValidatorTestCase<AssertTrue, Boolean> {

  @SuppressWarnings("unused")
  @AssertTrue
  private Boolean defaultField;

  @Override
  protected AssertTrueValidator createValidator() {
    return new AssertTrueValidator();
  }

  public void testIsValid_false() {
    assertConstraintValidator(Boolean.FALSE, false);
  }

  public void testIsValid_true() {
    assertConstraintValidator(Boolean.TRUE, true);
  }

  @Override
  protected Class<AssertTrue> getAnnotationClass() {
    return AssertTrue.class;
  }
}
