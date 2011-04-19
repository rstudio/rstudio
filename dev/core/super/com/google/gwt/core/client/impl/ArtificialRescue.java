/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.core.client.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * An annotation to specify that if a class is rescued, other types, methods, or
 * fields should be rescued as well. This annotation is an implementation detail
 * of the deRPC code and its use by third parties is not supported.
 */
@Target(ElementType.TYPE)
public @interface ArtificialRescue {
  /*
   * Special compiler support, changing this interface in any way requires
   * changes to ArtificialRescueChecker.
   */

  /**
   * Specifies the elements of a single type to rescue.
   */
  @Target(value = {})
  public @interface Rescue {
    /**
     * The class to be retained. Primitive array types should be referenced
     * using their JSNI type name.
     */
    String className();

    boolean instantiable() default false;

    /**
     * Fields are specified as raw names. That is, <code>fieldName</code>.
     */
    String[] fields() default {};

    /**
     * Methods are specified as unqualified JSNI signatures. That is,
     * <code>methodName(Lsome/Type;...)</code>.
     */
    String[] methods() default {};
  }

  Rescue[] value();
}
