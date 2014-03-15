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
package com.google.gwt.validation.example.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import javax.validation.Validator;
import javax.validation.groups.Default;

/**
 * Factory to create the Validator specified by {@link GwtValidator}.
 *
 * GWT.create instances of this class
 */
public class ExampleValidatorFactory extends AbstractGwtValidatorFactory {

  /**
   * Marks constraints that should run on the client.
   */
  public interface ClientGroup {
  }

  /**
   * Validator Interface annotated with the list of classes to validate on the
   * client.
   */
  @GwtValidation(
      value = {Author.class, Book.class, NotSpecifiedGroupsTest.MyClass.class},
      groups = {Default.class, ClientGroup.class})
  public interface GwtValidator extends Validator {
  }

  /**
   * Marks constraints that should run on the server.
   */
  public interface ServerGroup {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(GwtValidator.class);
  }
}
