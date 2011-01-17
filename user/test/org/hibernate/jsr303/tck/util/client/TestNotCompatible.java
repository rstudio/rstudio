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
package org.hibernate.jsr303.tck.util.client;

import org.hibernate.jsr303.tck.util.TckTestSuiteWrapper;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a testMethod as not compatible to prevent it from running in the
 * standard tests. Use this when the test code is not supported by GWT, but the
 * behavior is tested in some other way. Used by the {@link TckTestSuiteWrapper}
 * .
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface TestNotCompatible {
  /**
   * Constants for why a test is not supported.
   */
  public enum Reason {
    REFLECTION
  }

  /**
   * The JVM property name checked by {@link TckTestSuiteWrapper}. If the JVM
   * property {@value} is set to true the {@link TckTestSuiteWrapper} will run
   * tests annotated {@link TestNotCompatible}.
   */
  String INCLUDE = "com.google.gwt.sample.validationtck.util.TestNotCompatible.include";

  Reason reason();

  String whereTested();
}
