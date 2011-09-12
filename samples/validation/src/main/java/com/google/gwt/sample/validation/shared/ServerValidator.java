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
package com.google.gwt.sample.validation.shared;

import java.lang.reflect.Method;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Fails only on the server if the persons name is "Fail".
 */
public class ServerValidator implements
    ConstraintValidator<ServerConstraint, Person> {

  public void initialize(ServerConstraint constraintAnnotation) {
    // Here I do something that will not compile on GWT
    Method[] methods = constraintAnnotation.getClass().getMethods();
  }

  public boolean isValid(Person person, ConstraintValidatorContext context) {
    if (person == null) {
      return true;
    }
    String name = person.getName();
    return name == null || !name.equals("Fail");
  }
}
