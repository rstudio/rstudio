/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.requestfactory.shared;

/**
 * Describes a request failure on the server.
 */
public class ServerFailure {
  private final String message;
  private final String stackTraceString;
  private final String exceptionType;

  /**
   * Constructs a ServerFailure with a null message.
   */
  public ServerFailure() {
    this(null, null, null);
  }

  /**
   * Constructs a ServerFailure object.
   *
   * @param message a String containing the failure message
   * @param exceptionType a String containing the exception type
   * @param stackTraceString a String containing the stack trace
   */
  public ServerFailure(String message, String exceptionType,
      String stackTraceString) {
    this.message = message;
    this.exceptionType = exceptionType;
    this.stackTraceString = stackTraceString;
  }

  /**
   * Returns the exception type.
   *
   * @return the exception type as a String
   */
  public String getExceptionType() {
    return exceptionType;
  }

  /**
   * Returns the failure message.
   *
   * @return the message as a String
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the failure stack trace.
   *
   * @return the stack trace as a String
   */
  public String getStackTraceString() {
    return stackTraceString;
  }
}
