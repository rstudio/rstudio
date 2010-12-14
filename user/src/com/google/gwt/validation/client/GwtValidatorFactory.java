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
package com.google.gwt.validation.client;

import com.google.gwt.core.client.GWT;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/**
 * GWT Specific Validator Factory.
 */
public class GwtValidatorFactory implements ValidatorFactory {

  public ConstraintValidatorFactory getConstraintValidatorFactory() {
    return GWT.create(ConstraintValidatorFactory.class);
  }

  public MessageInterpolator getMessageInterpolator() {
    return GWT.create(MessageInterpolator.class);
  }

  public TraversableResolver getTraversableResolver() {
    return GWT.create(TraversableResolver.class);
  }

  public Validator getValidator() {
    return GWT.create(Validator.class);
  }

  public <T> T unwrap(Class<T> type) {
    // TODO(nchalko implement
    return null;
  }

  public ValidatorContext usingContext() {
    return GWT.create(ValidatorContext.class);
  }

}
