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

import com.google.gwt.validation.client.constraints.NullValidator;

import javax.validation.constraints.Null;

/**
 * Tests for {@link NullValidator}.
 */
public class NullValidatorTest extends
    ConstraintValidatorTestCase<Null, Object> {

  @SuppressWarnings("unused")
  @Null
  private Object defaultField;

  protected NullValidator createValidator() {
    return new NullValidator();
  }

  public void testIsValid_notNull() {
    assertConstraintValidator("this is not null", false);
  }

  @Override
  protected Class<Null> getAnnotationClass() {
    return Null.class;
  }

}
