/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

/**
 * Thrown when an invalid type is specified as part of a
 * <code>gwt.typeArgs</code> tag within a doc comment.
 */
public class BadTypeArgsException extends TypeOracleException {

  public BadTypeArgsException(String message) {
    super(message);
  }

  public BadTypeArgsException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadTypeArgsException(Throwable cause) {
    super(cause);
  }

}
