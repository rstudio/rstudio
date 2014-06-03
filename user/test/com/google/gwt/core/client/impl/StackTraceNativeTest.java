/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.client.impl;

import static com.google.gwt.core.client.impl.StackTraceExamples.TYPE_ERROR;

import com.google.gwt.core.client.impl.StackTraceCreator.CollectorLegacy;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;

/**
 * Tests {@link StackTraceCreator} in the native mode.
 */
@DoNotRunWith(Platform.Devel)
public class StackTraceNativeTest extends StackTraceTestBase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.StackTraceNative";
  }

  @Override
  protected String[] getTraceJava() {
    return new String[] {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@java.lang.Exception::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::getLiveException(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceTestBase::testTraceJava()"),
    };
  }

  @Override
  protected String[] getTraceRecursion() {
    final String[] expectedModern = {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@java.lang.Exception::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwRecursive(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::getLiveException(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceTestBase::testTraceRecursion()"),
    };

    final String[] expectedLegacy = {
        Impl.getNameOf("@java.lang.Throwable::new(Ljava/lang/String;)"),
        Impl.getNameOf("@java.lang.Exception::new(Ljava/lang/String;)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwRecursive(*)"),
    };

    return isLegacyCollector() ? expectedLegacy : expectedModern;
  }

  @Override
  protected String[] getTraceJse(Object thrown) {
    String[] nativeMethodNames = StackTraceExamples.getNativeMethodNames();
    final String[] full = {
        nativeMethodNames[0],
        nativeMethodNames[1],
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwJse(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException2(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::throwException1(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceExamples::getLiveException(*)"),
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceTestBase::assertJse(*)"),
    };

    final String[] limited = {
        Impl.getNameOf("@com.google.gwt.core.client.impl.StackTraceTestBase::assertJse(*)"),
    };

    // For legacy browsers and non-error javascript exceptions (e.g. throw "string"), we can only
    // construct stack trace from the catch block and below.

    return (isLegacyCollector() || thrown != TYPE_ERROR) ? limited : full;
  }

  private static boolean isLegacyCollector() {
    return StackTraceCreator.collector instanceof CollectorLegacy;
  }
}
