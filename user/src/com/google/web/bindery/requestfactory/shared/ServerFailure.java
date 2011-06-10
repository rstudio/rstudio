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
package com.google.web.bindery.requestfactory.shared;

/**
 * Describes a request failure on the server.
 * <p>
 * This error reporting mechanism is adequate at best. When RequestFactory is
 * extended to handle polymorphic types, this class will likely be replaced with
 * something more expressive.
 */
public class ServerFailure {
  private final String message;
  private final String stackTraceString;
  private final String exceptionType;
  private final boolean fatal;
  private RequestContext requestContext;

  /**
   * Constructs a ServerFailure with null properties.
   */
  public ServerFailure() {
    this(null);
  }

  /**
   * Constructs a fatal ServerFailure with null type and null stack trace.
   */
  public ServerFailure(String message) {
    this(message, null, null, true);
  }

  /**
   * Constructs a ServerFailure object.
   * 
   * @param message a String containing the failure message
   * @param exceptionType a String containing the exception type
   * @param stackTraceString a String containing the stack trace
   */
  public ServerFailure(String message, String exceptionType, String stackTraceString, boolean fatal) {
    this.message = message;
    this.exceptionType = exceptionType;
    this.stackTraceString = stackTraceString;
    this.fatal = fatal;
  }

  /**
   * Return the exception type.
   * 
   * @return the exception type as a String
   */
  public String getExceptionType() {
    return exceptionType;
  }

  /**
   * Return the failure message.
   * 
   * @return the message as a String
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the RequestContext that triggered the ServerFailure.
   */
  public RequestContext getRequestContext() {
    return requestContext;
  }

  /**
   * Return the failure stack trace.
   * 
   * @return the stack trace as a String
   */
  public String getStackTraceString() {
    return stackTraceString;
  }

  /**
   * Return true if this is a fatal error. The default implementation of
   * {@link Receiver#onFailure} throws a runtime exception for fatal failures.
   * 
   * @return whether this is a fatal failure
   */
  public boolean isFatal() {
    return fatal;
  }

  /**
   * Sets the RequestContext to return via {@link #getRequestContext()}.
   */
  public void setRequestContext(RequestContext requestContext) {
    this.requestContext = requestContext;
  }
}
