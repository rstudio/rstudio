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
package java.lang;

import com.google.gwt.core.client.GWT;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.4.2/docs/api/java/lang/Throwable.html">the
 * official Java API doc</a> for details.
 */
public class Throwable {

  private static final StackTraceElement[] NO_STACK_TRACE = new StackTraceElement[0];

  private Throwable cause;
  private String message;

  public Throwable() {
  }

  public Throwable(String message) {
    this.message = message;
  }

  public Throwable(String message, Throwable cause) {
    this.cause = cause;
    this.message = message;
  }

  public Throwable(Throwable cause) {
    this.message = (cause == null) ? null : cause.toString();
    this.cause = cause;
  }

  /**
   * Stack traces are not currently supported.
   * 
   * @return this
   */
  public Throwable fillInStackTrace() {
    return this;
  }

  public Throwable getCause() {
    return cause;
  }

  public String getLocalizedMessage() {
    return getMessage();
  }

  public String getMessage() {
    return message;
  }

  /**
   * Stack traces are not currently supported.
   * 
   * @return always a zero-length array
   */
  public StackTraceElement[] getStackTrace() {
    return NO_STACK_TRACE;
  }

  public Throwable initCause(Throwable cause) {
    if (this.cause != null) {
      throw new IllegalStateException("Can't overwrite cause");
    }
    if (cause == this) {
      throw new IllegalArgumentException("Self-causation not permitted");
    }
    this.cause = cause;
    return this;
  }

  public void printStackTrace() {
    StringBuffer msg = new StringBuffer();
    Throwable currentCause = this;
    while (currentCause != null) {
      String causeMessage = currentCause.getMessage();
      if (currentCause != this) {
        msg.append("Caused by: ");
      }
      msg.append(currentCause.typeName);
      msg.append(": ");
      msg.append(causeMessage == null ? "(No exception detail)" : causeMessage);
      msg.append("\n");
      currentCause = currentCause.getCause();
    }
    System.err.println(msg);
  }

  /**
   * Stack traces are not currently supported.
   */
  public void setStackTrace(StackTraceElement[] stackTrace) {
  }

  public String toString() {
    String className = GWT.getTypeName(this);
    String msg = getMessage();
    if (msg != null) {
      return className + ": " + msg;
    } else {
      return className;
    }
  }

}
