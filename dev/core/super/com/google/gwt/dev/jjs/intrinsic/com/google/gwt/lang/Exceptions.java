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
package com.google.gwt.lang;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.impl.StackTraceCreator;

import javaemul.internal.annotations.DoNotInline;

/**
 * This is a magic class the compiler uses to throw and check exceptions.
 */
final class Exceptions {

  @DoNotInline // This frame can be useful in understanding the native stack
  static Object wrap(Object e) {
    // Although this is impossible to happen in code generated from Java (as we always unwrap
    // before throwing), there are code out there where the Java exception is instantiated and
    // thrown in native code, hence we may receive it already wrapped.
    if (e instanceof Throwable) {
      return e;
    }

    Throwable javaException = getJavaException(e);
    if (javaException == null) {
      javaException = new JavaScriptException(e);
      StackTraceCreator.captureStackTrace(javaException);
    }
    return javaException;
  }

  @DoNotInline // This method shouldn't be inlined and pruned as JsStackEmulator needs it.
  static native Object unwrap(Object t)/*-{
    return t.@Throwable::backingJsObject;
  }-*/;

  private static native Throwable getJavaException(Object e)/*-{
    return e && e["__java$exception"];
  }-*/;

  static AssertionError makeAssertionError() {
    return new AssertionError();
  }

  /*
   * We use nonstandard naming here so it's easy for the compiler to map to
   * method names based on primitive type name.
   */
  // CHECKSTYLE_OFF
  static AssertionError makeAssertionError_boolean(boolean message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_char(char message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_double(double message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_float(float message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_int(int message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_long(long message) {
    return new AssertionError(message);
  }

  static AssertionError makeAssertionError_Object(Object message) {
    return new AssertionError(message);
  }

  /**
   * Throws a TypeError if the argument is null. Otherwise,
   * returns the argument.
   *
   * <p>The GWT compiler inserts calls to this method as a debugging aid, but
   * they will be removed in production compiles. We don't throw a
   * NullPointerException to make it harder to accidentally write code that
   * works in development and fails in production.
   *
   * <p> Instead of attempting to catch this exception, we recommend adding
   * an explicit null check and throwing java.lang.NullPointerException,
   * so that the GWT compiler doesn't remove the null check you're depending on.
   */
  static native <T> T checkNotNull(T arg) /*-{
    if (arg == null) {
      throw new TypeError("null pointer");
    }
    return arg;
  }-*/;

  /**
   * Use by the try-with-resources construct. Look at
   * {@link com.google.gwt.dev.jjs.impl.GwtAstBuilder.createCloseBlockFor}.
   *
   * @param resource a resource implementing the AutoCloseable interface.
   * @param mainException  an exception being propagated.
   * @return an exception to propagate or {@code null} if none.
   */
  static Throwable safeClose(AutoCloseable resource, Throwable mainException) {
    if (resource == null) {
      return mainException;
    }

    try {
      resource.close();
    } catch (Throwable e) {
      if (mainException == null) {
        return e;
      }
      mainException.addSuppressed(e);
    }
    return mainException;
  }
  // CHECKSTYLE_ON
}
