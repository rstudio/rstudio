/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.codegen.server;

/**
 * Context for code generators.
 * <p>
 * Experimental API - subject to change.
 */
public interface CodeGenContext {

  /**
   * An exception which can be thrown by a code generator to abort - callers of
   * code generators should catch this exception.
   */
  class AbortCodeGenException extends RuntimeException {
    public AbortCodeGenException() {
      super();
    }

    public AbortCodeGenException(String msg) {
      super(msg);
    }

    public AbortCodeGenException(String msg, Throwable cause) {
      super(msg, cause);
    }

    public AbortCodeGenException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Begin generating a new class.
   * 
   * @param pkgName
   * @param className
   * @return a {@link JavaSourceWriterBuilder} for the requested class or null if it
   *     could not be created, such as if it already exists
   */
  JavaSourceWriterBuilder addClass(String pkgName, String className);

  /**
   * Begin generating a new class, possibly using GWT super-source.
   * 
   * @param superPath super-source prefix, or null if a regular class
   * @param pkgName
   * @param className
   * @return a {@link JavaSourceWriterBuilder} for the requested class or null if it
   *     could not be created, such as if it already exists
   */
  JavaSourceWriterBuilder addClass(String superPath, String pkgName, String className);

  /**
   * Log a fatal error during code generation.
   * 
   * @param msg
   */
  void error(String msg);

  /**
   * Log a fatal error during code generation.
   * 
   * @param msg
   * @param cause
   */
  void error(String msg, Throwable cause);

  /**
   * Log a fatal error during code generation.
   * 
   * @param cause
   */
  void error(Throwable cause);

  /**
   * Log a non-fatal warning during code generation.
   * 
   * @param msg
   */
  void warn(String msg);

  /**
   * Log a non-fatal warning during code generation.
   * 
   * @param msg
   * @param cause
   */
  void warn(String msg, Throwable cause);

  /**
   * Log a non-fatal warning during code generation.
   * 
   * @param cause
   */
  void warn(Throwable cause);
}
