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
 * Specifies a field containing the entire range of values for a parameter to a
 * {@link Benchmark} method. The field must belong to the same class being
 * decorated by this annotation. The field must be either an Iterable, Enum, or
 * array whose type matches the parameter being annotated.
 * 
 * @see RangeEnum
 */
@Target(ElementType.PARAMETER)
@Documented
public @interface RangeField {

  /**
   * The name of the field that this range refers to.
   * 
   * @return for example, {@code myCommonRange}
   */
  String value();
}
