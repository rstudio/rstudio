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

import com.google.gwt.validation.client.constraints.DigitsValidatorForString;

import javax.validation.constraints.Digits;

/**
 * Tests for {@link DigitsValidatorForString}.
 */
public class DigitsValidatorForStringTest extends
    ConstraintValidatorTestCase<Digits, String> {

  private static String GOOD = "1234.12";
  private static String INT_TO_BIG = "12345.12";
  private static String INT_SMALL = "123.12";
  private static String DECIMAL_TO_BIG = "1234.123";
  private static String DECIMAL_SMALL = "1234.1";

  @SuppressWarnings("unused")
  @Digits(integer = 4, fraction = 2)
  private double defaultField;

  protected DigitsValidatorForString createValidator() {
    return new DigitsValidatorForString();
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

  public void testIsValid_invalid() {
    assertConstraintValidator("invalid", false);
  }

  @Override
  protected Class<Digits> getAnnotationClass() {
    return Digits.class;
  }
}
