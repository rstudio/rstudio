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

import com.google.gwt.validation.client.constraints.SizeValidatorForString;

import java.util.Date;

import javax.validation.constraints.Size;

/**
 * Tests for {@link SizeValidatorForString}.
 */
public class SizeValidatorForStringTest extends
    ConstraintValidatorTestCase<Size, String> {

  @SuppressWarnings("unused")
  @Size(min = 2, max = 5)
  private Date defaultField;

  protected SizeValidatorForString createValidator() {
    return new SizeValidatorForString();
  }

  public void testAssertIsValid_short() {
    assertConstraintValidator("1", false);
  }

  public void testAssertIsValid_min() {
    assertConstraintValidator("12", true);
  }

  public void testAssertIsValid_max() {
    assertConstraintValidator("12345", true);
  }

  public void testAssertIsValid_long() {
    assertConstraintValidator("123456", false);
  }

  @Override
  protected Class<Size> getAnnotationClass() {
    return Size.class;
  }
}
