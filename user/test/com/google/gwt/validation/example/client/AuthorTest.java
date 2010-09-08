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
package com.google.gwt.validation.example.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

/**
 * Tests for {@link Author}.
 */
public class AuthorTest extends GWTTestCase {

  private Author author;

  private Validator validator;

  @Override
  public String getModuleName() {
    return "com.google.gwt.validation.example.ValidationExample";
  }

  public void testValidate_string() {
    try {
      validator.validate("some string");
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testValidate_valid() {
    author.setFirstName("John");
    author.setLastName("Smith");
    author.setCompany("Google");
    Set<ConstraintViolation<Author>> violations = validator.validate(author);
    assertContentsAnyOrder("valid author", violations);
  }

  public void testValidateProperty_object() {
    try {
      validator.validateProperty(new Object(), "foo");
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testValidateValue_string() {
    try {
      validator.validateValue(String.class, "notValid", "value");
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  protected Validator createValidator() {
    return GWT.create(ExampleGwtValidator.class);
  }

  @Override
  protected final void gwtSetUp() throws Exception {
    super.gwtSetUp();
    author = new Author();
    validator = createValidator();
  }

  private <T> void assertContentsAnyOrder(String message,
      Iterable<T> actual, T... expected) {

    List<T> expectedList = Arrays.asList(expected);
    message += "Expected to find " + expectedList + " but found " + actual;
    for (T a : actual) {
      if (expectedList.contains(a)) {
        expectedList.remove(a);
      } else {
        fail(message);
      }
    }
    if (!expectedList.isEmpty()) {
      fail(message);
    }
  }
}
