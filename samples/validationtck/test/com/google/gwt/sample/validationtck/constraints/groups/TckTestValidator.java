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
package com.google.gwt.sample.validationtck.constraints.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractValidator;
import com.google.gwt.validation.client.GwtValidation;

import org.hibernate.jsr303.tck.tests.constraints.groups.Animal;
import org.hibernate.jsr303.tck.tests.constraints.groups.Book;
import org.hibernate.jsr303.tck.tests.constraints.groups.Order;
import org.hibernate.jsr303.tck.tests.constraints.groups.User;

import javax.validation.Validator;

/**
 * Validator implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class TckTestValidator extends AbstractValidator {
  /**
   * Marker Interface to {@link GWT#create(Class)}.
   */
  @GwtValidation(value = {Animal.class, Book.class, Order.class, User.class})
  public static interface GwtValidator extends Validator {
  }

  public TckTestValidator() {
    super((Validator) GWT.create(GwtValidator.class));
  }
}
