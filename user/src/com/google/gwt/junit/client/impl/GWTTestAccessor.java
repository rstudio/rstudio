/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A helper to provide create/execute functionality over GWTTestCase using class and method names
 * via reflection. This class is super-sourced for production mode.
 */
public class GWTTestAccessor {

  public GWTTestCase newInstance(String className) throws Throwable {
    return (GWTTestCase) Class.forName(className).newInstance();
  }

  public Object invoke(GWTTestCase test, String className, String methodName) throws Throwable {
    assert test.getClass().getName().equals(className);

    Method m = test.getClass().getMethod(methodName);
    m.setAccessible(true);
    try {
      return m.invoke(test);
    } catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }
}