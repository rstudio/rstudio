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

package com.google.gwt.logging.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A representation of a Throwable which can be used by GWT RPC. Although
 * Throwables are serializable, we don't want to use them directly in the
 * SerializableLogRecord since including a class with so many possible
 * subclasses will cause the client side JS to be very big.
 */
public class SerializableThrowable implements IsSerializable {
  private SerializableThrowable cause = null;
  private String message;
  private StackTraceElement[] stackTrace;
  
  /**
   * Create a new SerializableThrowable from a Throwable.
   */
  public SerializableThrowable(Throwable t) {
    message = t.getMessage();
    if (t.getCause() != null) {
      cause = new SerializableThrowable(t.getCause());
    }
    stackTrace = t.getStackTrace();
  }
  
  public SerializableThrowable(String message, SerializableThrowable cause,
      StackTraceElement[] stackTrace) {
    this.message = message;
    this.cause = cause;
    this.stackTrace = stackTrace;
  }
  
  protected SerializableThrowable() {
    // for serialization
  }
  
  public SerializableThrowable getCause() {
    return cause;
  }

  public String getMessage() {
    return message;
  }
  
  public StackTraceElement[] getStackTrace() {
    return stackTrace;
  }
  
  /**
   * Create a new Throwable from this SerializableThrowable.
   */
  public Throwable getThrowable() {
    Throwable t;
    if (cause != null) {
      t = new Throwable(message, cause.getThrowable());
    } else {
      t = new Throwable(message);
    }
    t.setStackTrace(stackTrace);
    return t;
  }
}
