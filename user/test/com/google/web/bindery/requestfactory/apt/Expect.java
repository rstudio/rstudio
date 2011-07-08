/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.apt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is applied to any element that is expected to be the target
 * of an error or a warning from the validator.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@interface Expect {
  /**
   * The arguments to be passed to {@code method}.
   */
  String[] args() default {};

  /**
   * The name of a method defined in {@link Messages}.
   */
  String method();

  /**
   * Specifies whether the diagnostic will be a warning or an error.
   */
  boolean warning() default false;
}