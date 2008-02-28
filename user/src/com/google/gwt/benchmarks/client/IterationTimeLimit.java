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
package com.google.gwt.benchmarks.client;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Specifies a custom time limit for iterations on the decorated
 * {@link Benchmark} method. Methods that aren't explicitly decorated with an
 * IterationTimeLimit, receive the default value.
 */
@Target(ElementType.METHOD)
@Documented
public @interface IterationTimeLimit {

  /**
   * The maximum amount of time, in milliseconds, an iteration is pursued before
   * skipping to the next set of values in the {@code Range}. A value of 0
   * means that all values in the {@code Range} will be exhaustively tested.
   * 
   * @return a maximum duration in milliseconds >= 0
   */
  long value() default 1000;
}
