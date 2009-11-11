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
package com.google.gwt.dev.javac;

import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;

/**
 * Represents a single JsniMethod in a compiled class file.
 */
public abstract class JsniMethod {
  /**
   * If non-null, an anonymous function containing the parameters and body of
   * this JSNI method.
   */
  public abstract JsFunction function();

  /**
   * Starting line number of the method.
   */
  public abstract int line();

  /**
   * Location of the containing compilation unit.
   */
  public abstract String location();

  /**
   * The mangled method name (a jsni signature).
   */
  public abstract String name();

  /**
   * The parameter names.
   */
  public abstract String[] paramNames();

  /**
   * Gets the JsProgram in which {@link #function(TreeLogger)} is located.
   */
  public abstract JsProgram program();
}
