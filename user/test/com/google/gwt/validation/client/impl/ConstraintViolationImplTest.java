/*
 * Copyright 2016 Google Inc.
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
package com.google.gwt.validation.client.impl;

import junit.framework.TestCase;

import javax.validation.ConstraintViolation;

/**
 * Tests for {@link ConstraintViolationImpl}.
 */
public class ConstraintViolationImplTest extends TestCase {

  public <T> void testEquals() throws Exception {
    String constraintMessage = "May not be null";
    String path = "path";
    ConstraintViolation<T> a = createViolation(constraintMessage, path);
    ConstraintViolation<T> b = createViolation(constraintMessage, path);
    assertTrue(a.equals(b));
  }

  public <T> void testNotEquals() throws Exception {
    String constraintMessage = "May not be null";
    ConstraintViolation<T> a = createViolation(constraintMessage, "path 1");
    ConstraintViolation<T> b = createViolation(constraintMessage, "path 2");
    assertFalse(a.equals(b));
  }

  private <T> ConstraintViolation<T> createViolation(String msg, final String path) {
    return new ConstraintViolationImpl.Builder<T>()
        .setMessage(msg)
        .setRootBean(null)
        .setPropertyPath(new PathImpl().append(path))
        .build();
  }

}
