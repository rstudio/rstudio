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
 * The superclass of exceptions thrown by {@link TypeOracle}.
 */
public class TypeOracleException extends Exception {

  public TypeOracleException() {
    super();
  }

  public TypeOracleException(String message) {
    super(message);
  }

  public TypeOracleException(String message, Throwable cause) {
    super(message, cause);
  }

  public TypeOracleException(Throwable cause) {
    super(cause);
  }

}
