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

import com.google.gwt.validation.client.constraints.DecimalMaxValidatorForNumber;

import javax.validation.constraints.DecimalMax;

/**
 * Tests for {@link DecimalMaxValidatorForNumber}.
 */
public class DecimalMaxValidatorForNumberTest extends
    ConstraintValidatorTestCase<DecimalMax, Number> {
  private static Double BELOW = Double.valueOf(922392239223.08);
  private static Double SAME = Double.valueOf(922392239223.09);
  private static Double ABOVE = Double.valueOf(922392239223.10);

  @SuppressWarnings("unused")
  @DecimalMax("922392239223.09")
  private double defaultField;

  protected DecimalMaxValidatorForNumber createValidator() {
    return new DecimalMaxValidatorForNumber();
  }

  public void testIsValid_below() {
    assertConstraintValidator(BELOW, true);
  }

  public void testIsValid_same() {
    assertConstraintValidator(SAME, true);
  }

  public void testIsValid_above() {
    assertConstraintValidator(ABOVE, false);
  }

  @Override
  protected Class<DecimalMax> getAnnotationClass() {
    return DecimalMax.class;
  }
}
