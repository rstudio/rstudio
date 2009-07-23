/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.impl.StackTraceCreator;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * See <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/lang/Throwable.html">the
 * official Java API doc</a> for details.
 */
public class Throwable implements Serializable {
  /*
   * NOTE: We cannot use custom field serializers because we need the client and
   * server to use different serialization strategies to deal with this type.
   * The client uses the generated field serializers which can use JSNI. That
   * leaves the server free to special case Throwable so that only the
   * detailMessage field is serialized.
   * 
   * Throwable is given special treatment by server's SerializabilityUtil class
   * to ensure that only the detailMessage field is serialized. Changing the
   * field modifiers below may necessitate a change to the server's
   * SerializabilityUtil.fieldQualifiesForSerialization(Field) method.
   */
  private transient Throwable cause;
  private String detailMessage;
  private transient StackTraceElement[] stackTrace;

  {
    fillInStackTrace();
  }

  public Throwable() {
  }

  public Throwable(String message) {
    this.detailMessage = message;
  }

  public Throwable(String message, Throwable cause) {
    this.cause = cause;
    this.detailMessage = message;
  }

  public Throwable(Throwable cause) {
    this.detailMessage = (cause == null) ? null : cause.toString();
    this.cause = cause;
  }

  /**
   * Populates the stack trace information for the Throwable.
   * 
   * @return this
   */
  public Throwable fillInStackTrace() {
    StackTraceCreator.fillInStackTrace(this);
    return this;
  }

  public Throwable getCause() {
    return cause;
  }

  public String getLocalizedMessage() {
    return getMessage();
  }

  public String getMessage() {
    return detailMessage;
  }

  /**
   * Stack traces are not currently populated by GWT. This method will return a
   * zero-length array unless a stack trace has been explicitly set with
   * {@link #setStackTrace(StackTraceElement[])}
   * 
   * @return the current stack trace
   */
  public StackTraceElement[] getStackTrace() {
    if (stackTrace == null) {
      return new StackTraceElement[0];
    }
    return stackTrace;
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
    printStackTrace(System.err);
  }

  public void printStackTrace(PrintStream out) {
    StringBuffer msg = new StringBuffer();
    Throwable currentCause = this;
    while (currentCause != null) {
      String causeMessage = currentCause.getMessage();
      if (currentCause != this) {
        msg.append("Caused by: ");
      }
      msg.append(currentCause.getClass().getName());
      msg.append(": ");
      msg.append(causeMessage == null ? "(No exception detail)" : causeMessage);
      msg.append("\n");
      currentCause = currentCause.getCause();
    }
    out.println(msg);
  }

  public void setStackTrace(StackTraceElement[] stackTrace) {
    StackTraceElement[] copy = new StackTraceElement[stackTrace.length];
    for (int i = 0, c = stackTrace.length; i < c; ++i) {
      if (stackTrace[i] == null) {
        throw new NullPointerException();
      }
      copy[i] = stackTrace[i];
    }
    this.stackTrace = copy;
  }

  @Override
  public String toString() {
    String className = this.getClass().getName();
    String msg = getMessage();
    if (msg != null) {
      return className + ": " + msg;
    } else {
      return className;
    }
  }

}
