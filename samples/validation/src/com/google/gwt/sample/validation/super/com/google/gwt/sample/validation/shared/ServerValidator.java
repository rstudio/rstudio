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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Always passes.
 * <p>
 * TODO(nchalko) change this to extend
 * {@link com.google.gwt.validation.client.constraints.NotGwtCompatibleValidator}
 * when groups are properly handled.
 */
public class ServerValidator implements
    ConstraintValidator<ServerConstraint, Person> {

  public void initialize(ServerConstraint constraintAnnotation) {
  }

  public boolean isValid(Person person, ConstraintValidatorContext context) {
    return true;
  }
}