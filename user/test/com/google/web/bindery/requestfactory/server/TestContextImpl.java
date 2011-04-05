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
package com.google.web.bindery.requestfactory.server;

/**
 * Bad service method declarations.
 */
public class TestContextImpl {

  public static Class badReturnType() {
    return null;
  }

  public static String mismatchedArityStatic(int x, int y, int z) {
    return null;
  }

  public static String mismatchedParamType(String param) {
    return null;
  }

  public static Integer mismatchedReturnType() {
    return null;
  }

  public static String overloadedMethod() {
    return null;
  }

  public static String overloadedMethod(String foo) {
    return null;
  }
  public String getId() {
    return null;
  }

  public String getVersion() {
    return null;
  }

  public String mismatchedArityInstance(int x, int y) {
    return null;
  }

  public String mismatchedStatic(String param) {
    return null;
  }

  public static String okMethod() {
    return null;
  }

  public static TestContextImpl okMethodProxy() {
    return null;
  }

  public static String mismatchedNonStatic(String param) {
    return null;
  }
}

