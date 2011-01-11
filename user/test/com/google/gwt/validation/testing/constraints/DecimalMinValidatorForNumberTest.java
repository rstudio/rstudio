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

import com.google.gwt.validation.client.constraints.DecimalMinValidatorForNumber;

import javax.validation.constraints.DecimalMin;

/**
 * Tests for {@link DecimalMinValidatorForNumber}.
 */
public class DecimalMinValidatorForNumberTest extends
    ConstraintValidatorTestCase<DecimalMin, Number> {
  private static Double BELOW = Double.valueOf(922392239223.08);
  private static Double ABOVE = Double.valueOf(922392239223.10);

  @SuppressWarnings("unused")
  @DecimalMin("922392239223.09")
  private double defaultField;

  protected DecimalMinValidatorForNumber createValidator() {
    return new DecimalMinValidatorForNumber();
  }

  public void testIsValid_below() {
    assertConstraintValidator(BELOW, false);
  }

  // Because of rounding error we can't actually test for the exact vale

  public void testIsValid_above() {
    assertConstraintValidator(ABOVE, true);
  }

  @Override
  protected Class<DecimalMin> getAnnotationClass() {
    return DecimalMin.class;
  }

}
