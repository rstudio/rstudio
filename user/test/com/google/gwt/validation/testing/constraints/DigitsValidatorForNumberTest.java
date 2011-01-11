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

import com.google.gwt.validation.client.constraints.DigitsValidatorForNumber;

import java.math.BigDecimal;

import javax.validation.constraints.Digits;

/**
 * Tests for {@link DigitsValidatorForNumber}.
 */
public class DigitsValidatorForNumberTest extends
    ConstraintValidatorTestCase<Digits, Number> {

  private static BigDecimal GOOD = new BigDecimal("1234.12");
  private static BigDecimal INT_TO_BIG = new BigDecimal("12345.12");
  private static BigDecimal INT_SMALL = new BigDecimal("123.12");
  private static BigDecimal DECIMAL_TO_BIG = new BigDecimal("1234.123");
  private static BigDecimal DECIMAL_SMALL = new BigDecimal("1234.1");

  @SuppressWarnings("unused")
  @Digits(integer = 4, fraction = 2)
  private double defaultField;

  protected DigitsValidatorForNumber createValidator() {
    return new DigitsValidatorForNumber();
  }

  public void testIsValid_decimalToBig() {
    assertConstraintValidator(DECIMAL_SMALL, true);
  }

  public void testIsValid_decimalToSmall() {
    assertConstraintValidator(DECIMAL_TO_BIG, false);
  }

  public void testIsValid_good() {
    assertConstraintValidator(GOOD, true);
  }

  public void testIsValid_intToBig() {
    assertConstraintValidator(INT_TO_BIG, false);
  }

  public void testIsValid_intToSmall() {
    assertConstraintValidator(INT_SMALL, true);
  }

  @Override
  protected Class<Digits> getAnnotationClass() {
    return Digits.class;
  }
}
