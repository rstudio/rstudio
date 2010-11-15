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
package com.google.gwt.sample.validationtck;

import com.google.gwt.core.client.GWT;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

/**
 * TCK implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}
 */
public class TckTestValidator implements Validator {

  TckValidator validator = GWT.create(TckValidator.class);

  public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
    return validator.getConstraintsForClass(clazz);
  }

  public <T> T unwrap(Class<T> type) {
    return validator.unwrap(type);
  }

  public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
    return validator.validate(object, groups);
  }

  public <T> Set<ConstraintViolation<T>> validateProperty(T object,
      String propertyName, Class<?>... groups) {
    return validator.validateProperty(object, propertyName, groups);
  }

  public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
      String propertyName, Object value, Class<?>... groups) {
    return validator.validateValue(beanType, propertyName, value, groups);
  }
}
