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
package com.google.gwt.core.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be placed on a native method to allow it to directly
 * access Java <code>long</code> values. Without this annotation present,
 * accessing a <code>long</code> in any way from a JSNI method is an error.
 * This includes declaring a parameter or return type of <code>long</code>,
 * calling a method that takes or returns a <code>long</code>, or accessing a
 * <code>long</code> field.
 *
 * <p>
 * The reason for the restriction is that Java long values are not represented
 * as numeric values in compiled code, but as opaque Objects. Attempting to
 * perform math operations on them would produce undesirable results.
 * </p>
 * <p>
 * Use this annotation with care; the only safe thing to do with
 * <code>long</code> values in JSNI code is to pass them back into Java
 * unaltered.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface UnsafeNativeLong {
}
