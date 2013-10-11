/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs;

/**
 * Indicates the compiler encountered an IO failure.<br />
 *
 * Generally speaking the exception is fatal and should not be caught except at the top level. As a
 * result the exception is unchecked to avoid lots of unnecessary throws.<br />
 *
 * There is no expectation that a logger entry was created along with this exception, so the
 * exception message should be made as informative as possible.
 */
public class CompilerIoException extends InternalCompilerException {

  public CompilerIoException(String message) {
    super(message);
  }

  public CompilerIoException(String message, Throwable cause) {
    super(message, cause);
  }
}
