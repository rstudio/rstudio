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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates a {@code javax.validation.Validator} explicitly listing the classes
 * that can be validated in GWT.
 * <p>
 * Define the Validator you want explicitly listing the class you want to
 * validate.
 *
 * <pre>
 * &#064;GwtValidation(MyBean.class, MyOther.class)
 * public interface MyValidator extends javax.validation.Validator {
 * }
 * </pre>
 * Create and use the validator.
 *
 * <pre>
 * MyValidator validator = GWT.create(MyValidator.class);
 * MyBean bean = new MyBean();
 * ...
 * Set&lt;ConstraintViolation&lt;MyBean>> violations = validator.validate(bean);
 * </pre>
 *
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface GwtValidation {

  /**
   * The list of Classes which can be validated by the annotated
   * {@code Validator}.
   */
  Class<?>[] value();

  /**
   * The list of Groups which can be processed by the annotated
   * {@code Validator}, empty means all groups.
   */
  Class<?>[] groups() default {};
}
