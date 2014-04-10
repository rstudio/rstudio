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
 * An annotation to mark a given method as being specialized. If the specified
 * parameters and return context match of a JMethodCall, then the call
 * is retargeted at the specialized version.
 */
@Target(ElementType.METHOD)
@CompilerHint
public @interface SpecializeMethod {

  /**
   * Represents a type that matches any type, even void.
   */
  interface ANY { };

  /**
   * List of parameter types, matched via assignability.
   */
  Class<?>[] params();

  /**
   * List of return types to match, or null if you don't care.
   */
  Class<?> returns() default ANY.class;

  /**
   * The name of the method to target. It must have a signature matching
   * the actual argument types passed to JMethodCall.
   */
  String target();
}
