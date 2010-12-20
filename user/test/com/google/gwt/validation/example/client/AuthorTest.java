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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.validation.example.client.ExampleValidatorFactory.ClientGroup;
import com.google.gwt.validation.example.client.ExampleValidatorFactory.ServerGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

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

  public void testGroup_clientGroup() throws Exception {
    initValidAuthor();
    Set<ConstraintViolation<Author>> violations = validator.validate(author,
        ClientGroup.class);
    assertContentsAnyOrder("valid author", violations);
  }

  public void testGroup_default() throws Exception {
    initValidAuthor();
    Set<ConstraintViolation<Author>> violations = validator.validate(author,
        Default.class);
    assertContentsAnyOrder("valid author", violations);
  }

  public void testGroup_empty() throws Exception {
    initValidAuthor();
    Set<ConstraintViolation<Author>> violations = validator.validate(author);
    assertContentsAnyOrder("valid author", violations);
  }

  public void testGroup_serverGroup() throws Exception {
    initValidAuthor();
    try {
      validator.validate(author, ServerGroup.class);
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  public void testValidate_companySize31() {
    initValidAuthor();
    author.setCompany("1234567890123456789012345678901");
    Set<ConstraintViolation<Author>> violations = validator.validate(author);
    assertContentsAnyOrder("company size 31", toMessage(violations),
        "size must be between 0 and 30"
        );
  }

  public void testValidate_string() {
    try {
      validator.validate("some string");
      fail("Expected a " + IllegalArgumentException.class);
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testValidate_valid() {
    initValidAuthor();
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
    } catch (IllegalArgumentException expected) {
    }
  }

  @Override
  protected final void gwtSetUp() throws Exception {
    super.gwtSetUp();
    author = new Author();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  protected void initValidAuthor() {
    author.setFirstName("John");
    author.setLastName("Smith");
    author.setCompany("Google");
  }

  private <T> void assertContentsAnyOrder(String message,
      Iterable<T> actual, T... expected) {

    List<T> expectedList = new ArrayList<T>(Arrays.asList(expected));
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

  private <T> List<String> toMessage(Set<ConstraintViolation<T>> violations) {
    List<String> messages = new ArrayList<String>();
    for (ConstraintViolation<T> violation : violations) {
      messages.add(violation.getMessage());
    }
    return messages;
  }
}
