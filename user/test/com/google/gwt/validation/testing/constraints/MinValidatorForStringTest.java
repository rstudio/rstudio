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

import com.google.gwt.validation.client.constraints.MinValidatorForString;

import javax.validation.constraints.Min;

/**
 * Tests for {@link MinValidatorForString}.
 */
public class MinValidatorForStringTest extends
    ConstraintValidatorTestCase<Min, String> {
  private static String SAME = "123456789";
  private static String SMALLER = "123456788";
  private static String BIGGER = "123456790";

  @Min(123456789)
  public String defaultField;

  public void testIsValid_same() {
    assertConstraintValidator(SAME, true);
  }

  public void testIsValid_smaller() {
    assertConstraintValidator(SMALLER, false);
  }

  public void testIsValid_bigger() {
    assertConstraintValidator(BIGGER, true);
  }

  public void testIsValid_minValue() {
    assertConstraintValidator(Long.valueOf(Long.MIN_VALUE).toString(), false);
  }

  public void testIsValid_maxValue() {
    assertConstraintValidator(Long.valueOf(Long.MAX_VALUE).toString(), true);
  }

  public void testIsValid_invalid() {
    assertConstraintValidator("invalid", false);
  }

  @Override
  protected MinValidatorForString createValidator() {
    return new MinValidatorForString();
  }

  @Override
  protected Class<Min> getAnnotationClass() {
    return Min.class;
  }
}
