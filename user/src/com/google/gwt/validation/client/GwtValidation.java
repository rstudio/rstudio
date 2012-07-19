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

import javax.validation.groups.Default;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Annotates a {@code javax.validation.Validator} explicitly listing the classes
 * that can be validated in GWT.
 * <p>
 * Define the Validator you want, explicitly listing the class you want to
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
 * <p>
 * NOTE: Validation is done using only the Constraints found on the Classes
 * listed in the annotation. If you have
 * 
 * <pre>
 * class MyBean {
 *  &#064;Null
 *  String getName(){return name;}
 * }
 * class MySubBean extends MyBean {
 *   &#064;Size(min = 5)
 *   String getName(){return super.getName();}
 * }
 * </pre>
 * 
 * And then create your {@link javax.validation.ValidatorFactory
 * ValidatorFactory} using
 * 
 * <pre>
 * @GwtValidation(MyBean.class, MyOther.class)}
 * </pre>
 * 
 * but call validator with the subclass like
 * 
 * <pre>
 * MySubBean bean = new MySubBean();
 * Set&lt;ConstraintViolation&lt;MyBean>> violations = validator.validate(bean);
 * </pre>
 * 
 * The {@code Size} constraint will not be validated.
 * 
 * Instead make sure you list the all BeanTypes that will be directly validated
 * in the {@link GwtValidation} annotation.
 * 
 * 
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface GwtValidation {

  /**
   * The list of Groups which can be processed by the annotated
   * {@code Validator}. The default value is {@link Default}.
   * An empty array is illegal.
   */
  Class<?>[] groups() default {Default.class};

  /**
   * The list of Classes which can be validated by the annotated
   * {@code Validator}.
   */
  Class<?>[] value();
}
