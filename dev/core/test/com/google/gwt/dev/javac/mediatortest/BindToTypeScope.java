/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.dev.javac.mediatortest;

/**
 * A class to test loading of nested classes.<p>
 *
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleUpdaterTestBase}
 */
public class BindToTypeScope {
  /**
   * A simple nested class.
   */
  public static class Object { }
  // Fails when loaded from bytecode
  /**
   * A nested class that extends another nested class from the same scope.
   */
  public static class DerivedObject extends Object { }
}
