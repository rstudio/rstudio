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

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.Min;

/**
 * Abstract {@link Min} constraint validator implementation for a <code>T</code>
 * .
 *
 * @param <T> the type of object to validate
 */
public abstract class AbstractMinValidator<T> implements
    ConstraintValidator<Min, T> {

  private long min;

  public final void initialize(Min constraintAnnotation) {
    min = constraintAnnotation.value();
  }

  protected final boolean isValid(Number value) {
    if (value instanceof BigDecimal) {
      return ((BigDecimal) value).compareTo(BigDecimal.valueOf(min)) >= 0;
    } else if (value instanceof BigInteger) {
      return ((BigInteger) value).compareTo(BigInteger.valueOf(min)) >= 0;
    } else {
      return value.longValue() >= min;
    }
  }
}