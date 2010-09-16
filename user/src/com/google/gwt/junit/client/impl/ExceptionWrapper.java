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
package com.google.gwt.junit.client.impl;

import java.io.Serializable;

/**
 * Wraps a {@link Throwable}, and explicitly serializes cause and stack trace.
 */
final class ExceptionWrapper implements Serializable {

  /**
   * Stand-in for the transient {@link Throwable#getCause()} in GWT JRE.
   */
  ExceptionWrapper causeWrapper;

  /**
   * The wrapped exception.
   */
  Throwable exception;

  /**
   * Stand-in for the transient {@link Throwable#getStackTrace()} in GWT JRE.
   */
  StackTraceElement[] stackTrace;

  /**
   * If true, the exception's inner stack trace and cause have been initialized.
   * Defaults to false immediate after deserialization.
   */
  private transient boolean isExceptionInitialized;

  /**
   * Creates an {@link ExceptionWrapper} around an existing {@link Throwable}.
   * 
   * @param exception the {@link Throwable} to wrap.
   */
  public ExceptionWrapper(Throwable exception) {
    this.exception = exception;
    this.stackTrace = exception.getStackTrace();
    Throwable cause = exception.getCause();
    if (cause != null) {
      this.causeWrapper = new ExceptionWrapper(cause);
    }
    this.isExceptionInitialized = true;
  }

  /**
   * Deserialization constructor.
   */
  ExceptionWrapper() {
    this.isExceptionInitialized = false;
  }

  public Throwable getException() {
    if (!isExceptionInitialized) {
      exception.setStackTrace(stackTrace);
      if (causeWrapper != null) {
        exception.initCause(causeWrapper.getException());
      }
      isExceptionInitialized = true;
    }
    return exception;
  }
}

