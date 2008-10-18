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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies a teardown method that will be executed after the annotated
 * {@link Benchmark} test method. Teardown methods are automatically executed by
 * the benchmarking framework after their matching test methods. Teardown
 * measurements are excluded from final benchmark reports.
 * 
 * <p>
 * For example, you might annotate a {@code Benchmark} method named
 * <code>testInserts</code> with {@code @Teardown("endTestInserts")} to ensure
 * <code>endTestInserts</code> is always executed after
 * <code>testInserts</code>.
 * </p>
 */
@Target(ElementType.METHOD)
@Documented
public @interface Teardown {

  /**
   * The name of the method to execute after the annotated test method.
   * 
   * @return for example, "endTestInserts"
   */
  String value();
}
