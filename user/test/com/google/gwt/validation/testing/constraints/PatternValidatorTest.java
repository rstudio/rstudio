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

import com.google.gwt.validation.client.constraints.PatternValidator;

import java.util.Date;

import javax.validation.constraints.Pattern;

/**
 * Tests for {@link PatternValidator}.
 */
public class PatternValidatorTest extends
    ConstraintValidatorTestCase<Pattern, String> {

  @SuppressWarnings("unused")
  @Pattern(regexp = "g..d")
  private Date defaultField;

  public void testAssertIsValid_goad() {
    assertConstraintValidator("goad", true);
  }

  public void testAssertIsValid_good() {
    assertConstraintValidator("good", true);
  }

  public void testAssertIsValid_goood() {
    assertConstraintValidator("goood", false);
  }

  public void testAssertIsValid_not_good() {
    assertConstraintValidator("this is not good", false);
  }

  protected PatternValidator createValidator() {
    return new PatternValidator();
  }

  @Override
  protected Class<Pattern> getAnnotationClass() {
    return Pattern.class;
  }
}
