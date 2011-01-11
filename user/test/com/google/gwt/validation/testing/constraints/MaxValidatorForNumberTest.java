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

import com.google.gwt.validation.client.constraints.MaxValidatorForNumber;

import javax.validation.constraints.Max;

/**
 * Tests for {@link MaxValidatorForNumber}.
 */
public class MaxValidatorForNumberTest extends
    ConstraintValidatorTestCase<Max, Number> {
  private static long SAME = 123456789L;
  private static long SMALLER = SAME - 1L;
  private static long BIGGER = SAME + 1L;

  @Max(123456789L)
  public long defaultField;

  public void testIsValid_same() {
    assertConstraintValidator(Long.valueOf(SAME), true);
  }

  public void testIsValid_smaller() {
    assertConstraintValidator(Long.valueOf(SMALLER), true);
  }

  public void testIsValid_bigger() {
    assertConstraintValidator(Long.valueOf(BIGGER), false);
  }

  public void testIsValid_minValue() {
    assertConstraintValidator(Long.valueOf(Long.MIN_VALUE), true);
  }

  public void testIsValid_maxValue() {
    assertConstraintValidator(Long.valueOf(Long.MAX_VALUE), false);
  }

  @Override
  protected MaxValidatorForNumber createValidator() {
    return new MaxValidatorForNumber();
  }

  @Override
  protected Class<Max> getAnnotationClass() {
    return Max.class;
  }
}
