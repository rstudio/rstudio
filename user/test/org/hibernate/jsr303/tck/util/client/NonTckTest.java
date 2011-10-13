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
 * Marks a testMethod as not part of the TCK. The test is run by default but it
 * is excluded from TCK reports. Used by the {@link TckTestSuiteWrapper}.
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface NonTckTest {
  /**
   * The JVM property name checked by {@link TckTestSuiteWrapper}. If the JVM
   * property {@value} is set to true the {@link TckTestSuiteWrapper} will not
   * run the tests annotated {@link Failing}.
   */
  String EXCLUDE = "com.google.gwt.sample.validationtck.util.NonTckTest.exclude";
}
