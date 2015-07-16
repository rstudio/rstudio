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

import static javaemul.internal.InternalPreconditions.checkCriticalArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

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
   *
   * TODO(rluble): Add remaining functionality for suppressed Exceptions (e.g.
   * printing). Also review the class for missing Java 7 compatibility.
   */
  private transient Throwable cause;
  private String detailMessage;
  private transient Throwable[] suppressedExceptions;
  private transient StackTraceElement[] stackTrace;
  private transient boolean disableSuppression;

  public Throwable() {
    fillInStackTrace();
  }

  public Throwable(String message) {
    this.detailMessage = message;
    fillInStackTrace();
  }

  public Throwable(String message, Throwable cause) {
    this.cause = cause;
    this.detailMessage = message;
    fillInStackTrace();
  }

  public Throwable(Throwable cause) {
    this.detailMessage = (cause == null) ? null : cause.toString();
    this.cause = cause;
    fillInStackTrace();
  }

  /**
   * Constructor that allows subclasses disabling exception suppression and stack traces.
   * Those features should only be disabled in very specific cases.
   */
  protected Throwable(String message, Throwable cause, boolean enableSuppression,
      boolean writetableStackTrace) {
    this.cause = cause;
    this.detailMessage = message;
    this.disableSuppression = !enableSuppression;
    if (writetableStackTrace) {
      fillInStackTrace();
    }
  }

  /**
   * Call to add an exception that was suppressed. Used by try-with-resources.
   */
  public final void addSuppressed(Throwable exception) {
    checkNotNull(exception, "Cannot suppress a null exception.");
    checkCriticalArgument(exception != this, "Exception can not suppress itself.");

    if (disableSuppression) {
      return;
    }

    if (suppressedExceptions == null) {
      suppressedExceptions = new Throwable[] { exception };
    } else {
      // TRICK: This is not correct Java (would give an OOBE, but it works in JS and
      // this code will only be executed in JS.
      suppressedExceptions[suppressedExceptions.length] = exception;
    }
  }

  /**
   * Populates the stack trace information for the Throwable.
   *
   * @return this
   */
  public native Throwable fillInStackTrace() /*-{
    this.@Throwable::stackTrace = null; // Invalidate the cached trace
    @com.google.gwt.core.client.impl.StackTraceCreator::captureStackTrace(*)(this, this.@Throwable::detailMessage);
    return this;
  }-*/;

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
   * Returns the stack trace for the Throwable if it is available.
   * <p> Availability of stack traces in script mode depends on module properties and browser.
   * See: https://code.google.com/p/google-web-toolkit/wiki/WebModeExceptions#Emulated_Stack_Data
   */
  public StackTraceElement[] getStackTrace() {
    if (stackTrace == null) {
      stackTrace = constructJavaStackTrace(this);
    }
    return stackTrace;
  }

  private static native StackTraceElement[] constructJavaStackTrace(Throwable t) /*-{
    return @com.google.gwt.core.client.impl.StackTraceCreator::constructJavaStackTrace(*)(t);
  }-*/;

  /**
   * Returns the array of Exception that this one suppressedExceptions.
   */
  public final Throwable[] getSuppressed() {
    if (suppressedExceptions == null) {
      suppressedExceptions = new Throwable[0];
    }

    return suppressedExceptions;
  }

  public Throwable initCause(Throwable cause) {
    checkState(this.cause == null, "Can't overwrite cause");
    checkCriticalArgument(cause != this, "Self-causation not permitted");
    this.cause = cause;
    return this;
  }

  public void printStackTrace() {
    printStackTrace(System.err);
  }

  public void printStackTrace(PrintStream out) {
    printStackTraceImpl(out, "", "");
  }

  private void printStackTraceImpl(PrintStream out, String prefix, String ident) {
    out.println(ident + prefix + this);
    printStackTraceItems(out, ident);

    for (Throwable t : getSuppressed()) {
      t.printStackTraceImpl(out, "Suppressed: ", "\t" + ident);
    }

    Throwable theCause = getCause();
    if (theCause != null) {
      theCause.printStackTraceImpl(out, "Caused by: ", ident);
    }
  }

  private void printStackTraceItems(PrintStream out, String ident) {
    for (StackTraceElement element : getStackTrace()) {
      out.println(ident + "\tat " + element);
    }
  }

  public void setStackTrace(StackTraceElement[] stackTrace) {
    int length = stackTrace.length;
    StackTraceElement[] copy = new StackTraceElement[length];
    for (int i = 0; i < length; ++i) {
      copy[i] = checkNotNull(stackTrace[i]);
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
