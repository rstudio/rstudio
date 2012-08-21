/*
 * Copyright 2012 Google Inc.
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validates server-side constraints. Will not compile on GWT.
 */
public class ServerValidator
    implements ConstraintValidator<ServerConstraint, NotSpecifiedGroupsTest.MyClass> {

  @Override
  public void initialize(ServerConstraint constraintAnnotation) {
    // Here I do something that will not compile on GWT
    @SuppressWarnings("unused")
    Method[] methods = constraintAnnotation.getClass().getMethods();
  }

  @Override
  public boolean isValid(NotSpecifiedGroupsTest.MyClass obj, ConstraintValidatorContext context) {
    @SuppressWarnings("unused")
    Field[] fields = obj.getClass().getDeclaredFields();
    return false;
  }
}
