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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A helper class for converting a generic {@link StackTraceElement} into an
 * Object that can be serialized for RPC.
 */
public final class StackTraceWrapper implements IsSerializable {

  /**
   * Creates a {@link StackTraceWrapper} array around an existing
   * {@link StackTraceElement} array.
   * 
   * @param stackTrace the {@link StackTraceElement} array to wrap.
   */
  public static StackTraceWrapper[] wrapStackTrace(
      StackTraceElement[] stackTrace) {
    int len = stackTrace.length;
    StackTraceWrapper[] result = new StackTraceWrapper[len];
    for (int i = 0; i < len; ++i) {
      result[i] = new StackTraceWrapper(stackTrace[i]);
    }
    return result;
  }

  /**
   * Corresponds to {@link StackTraceElement#getClassName()}.
   */
  public String className;

  /**
   * Corresponds to {@link StackTraceElement#getFileName()}.
   */
  public String fileName;

  /**
   * Corresponds to {@link StackTraceElement#getLineNumber()}.
   */
  public int lineNumber;

  /**
   * Corresponds to {@link StackTraceElement#getMethodName()}.
   */
  public String methodName;

  /**
   * Creates an empty {@link StackTraceWrapper}.
   */
  public StackTraceWrapper() {
  }

  /**
   * Creates a {@link StackTraceWrapper} around an existing
   * {@link StackTraceElement}.
   * 
   * @param ste the {@link StackTraceElement} to wrap.
   */
  public StackTraceWrapper(StackTraceElement ste) {
    className = ste.getClassName();
    fileName = ste.getFileName();
    lineNumber = ste.getLineNumber();
    methodName = ste.getMethodName();
  }
}
