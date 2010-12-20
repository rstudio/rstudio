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
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/**
 * Abstract {@link ValidatorFactory} that delegates to a GWT generated
 * {@link Validator}.
 * <p>
 * Extend this class create and implement createValidator
 *
 * <pre>
 * public class MyValidatorFactory extends AbstractGwtValidatorFactory {
 *   @GwtValidation(value = {Pojo.class,Other.class})
 *   public static interface GwtValidator extends Validator {
 *   }
 *
 *   public AbstractGwtValidator createValidator (){
 *     return GWT.create(GwtValidator.class));
 *   }
 * }
 * </pre>
 * <p>
 * Then add a line like this to your Gwt Module config (gwt.xml) file.
 *
 * <pre>
 * &lt;replace-with class="com.example.MyValidatorFactory">
 *   &lt;when-type-is class="javax.validation.ValidatorFactory"/>
 * &lt;/replace-with>
 * </pre>
 */
public abstract class AbstractGwtValidatorFactory implements ValidatorFactory {

  /**
   * Implement this method to returns a {@link GWT#create}ed {@link Validator}
   * annotated with {@link GwtValidation}.
   *
   * @return newly created Validator
   */
  public abstract AbstractGwtValidator createValidator();

  public final ConstraintValidatorFactory getConstraintValidatorFactory() {
    return GWT.create(ConstraintValidatorFactory.class);
  }

  public final MessageInterpolator getMessageInterpolator() {
    return new GwtMessageInterpolator();
  }

  public final TraversableResolver getTraversableResolver() {
    return GWT.create(TraversableResolver.class);
  }

  public final Validator getValidator() {
    AbstractGwtValidator validator = createValidator();
    validator.init(getConstraintValidatorFactory(), getMessageInterpolator(),
        getTraversableResolver());
    return validator;
  }

  public final <T> T unwrap(Class<T> type) {
    // TODO(nchalko implement
    return null;
  }

  public final ValidatorContext usingContext() {
    return GWT.create(ValidatorContext.class);
  }
}
