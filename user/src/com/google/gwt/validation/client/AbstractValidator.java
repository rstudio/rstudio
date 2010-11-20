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

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;

/**
 * Abstract {@link Validator} that delegates to a GWT generated validator.
 * <p>
 * Extend this class create a default no arg constructor like.
 *
 * <pre>
 * public class MyValidator {
 *   @GwtValidation(value = {Pojo.class,Other.class})
 *   public static interface GwtValidator extends Validator {
 *   }
 *
 *   MyValidator() {
 *     super((Validator) GWT.create(GwtValidator.class));
 *   }
 * }
 * </pre>
 * <p>
 * Then add a line like this to your Gwt Module config (gwt.xml) file.
 *
 * <pre>
 * &lt;replace-with class="com.example.MyValidator.GwtValidator">
 *   &lt;when-type-is class="javax.validation.Validator"/>
 * &lt;/replace-with>
 * </pre>
 */
public abstract class AbstractValidator implements Validator {

  private final Validator validator;

  /**
   * Pass a GWT created {@link Validator} to this constructor.
   */
  public AbstractValidator(Validator validator) {
    super();
    this.validator = validator;
  }

  public final BeanDescriptor getConstraintsForClass(Class<?> clazz) {
    return validator.getConstraintsForClass(clazz);
  }

  public final <T> T unwrap(Class<T> type) {
    return validator.unwrap(type);
  }

  public final <T> Set<ConstraintViolation<T>> validate(T object,
      Class<?>... groups) {
    return validator.validate(object, groups);
  }

  public final <T> Set<ConstraintViolation<T>> validateProperty(T object,
      String propertyName, Class<?>... groups) {
    return validator.validateProperty(object, propertyName, groups);
  }

  public final <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
      String propertyName, Object value,
      Class<?>... groups) {
        return validator.validateValue(beanType, propertyName, value, groups);
      }
}