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
package com.google.gwt.validation.client.constraints;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Masks a {@link ConstraintValidator} that is not GWT compatible. This
 * validator always fails.
 * <p>
 * Extend this class and implement it as GWT super class. Use validation groups
 * to keep this constraint from being validated on the client.
 * 
 * <p>
 * In a super source directory override your validator like this:
 * 
 * <pre>
 * public class MyValidator extends
 *     NotGwtCompatibleValidator &lt;MyConstraint, MyType&gt;{
 * }
 * </pre>
 * 
 * @param <A> the constraint to validate
 * @param <T> the type to validate
 */
public abstract class NotGwtCompatibleValidator<A extends Annotation, T>
    implements ConstraintValidator<A, T> {

  public final void initialize(A constraintAnnotation) {
  }

  /**
   * Always fails.
   */
  public final boolean isValid(T value, ConstraintValidatorContext context) {
    // TODO (nchalko) add a custom message
    return false;
  }

}
