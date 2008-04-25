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
package com.google.gwt.dev.shell;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Encodes all JSNI methods into a compiled hosted mode class file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsniMethods {
  
  /**
   * Encodes a JSNI method into a compiled hosted mode class file.
   */
  @Target({})
  public @interface JsniMethod {
    /**
     * Source file of the method.
     */
    String file();

    /**
     * Starting line number of the method.
     */
    int line();

    /**
     * The mangled method name (a jsni signature).
     */
    String name();

    /**
     * The parameter names.
     */
    String[] paramNames();

    /**
     * The script body.
     */
    String body();
  }

  /**
   * The set of all methods.
   */
  JsniMethod[] value();
}
