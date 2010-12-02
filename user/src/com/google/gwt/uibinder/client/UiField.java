/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks fields in a UiBinder client that must be filled by the binder's
 * {@link UiBinder#createAndBindUi} method. If provided is true the field
 * creation is delegated to the client (owner).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UiField {
  /**
   * If true, the field must be filled before {@link UiBinder#createAndBindUi} is called.
   * If false, {@link UiBinder#createAndBindUi} will fill the field, usually
   * by calling {@link com.google.gwt.core.client.GWT#create}.
   */
  boolean provided() default false;
}
