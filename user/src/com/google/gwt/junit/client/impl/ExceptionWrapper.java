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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A helper class for converting a generic {@link Throwable} into an Object that
 * can be serialized for RPC.
 */
public final class ExceptionWrapper implements IsSerializable {

  /**
   * Corresponds to {@link Throwable#getCause()}.
   */
  public ExceptionWrapper cause;

  /**
   * Corresponds to {@link Throwable#getMessage()}.
   */
  public String message;

  /**
   * Corresponds to {@link Throwable#getStackTrace()}.
   */
  public StackTraceWrapper[] stackTrace;

  /**
   * The run-time type of the exception.
   */
  public String typeName;

  /**
   * Creates an empty {@link ExceptionWrapper}.
   */
  public ExceptionWrapper() {
  }

  /**
   * Creates an {@link ExceptionWrapper} around an existing {@link Throwable}.
   * 
   * @param e the {@link Throwable} to wrap.
   */
  public ExceptionWrapper(Throwable e) {
    typeName = GWT.getTypeName(e);
    message = e.getMessage();
    stackTrace = StackTraceWrapper.wrapStackTrace(e.getStackTrace());
    Throwable ecause = e.getCause();
    if (ecause != null) {
      cause = new ExceptionWrapper(ecause);
    }
  }

}
