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
 * A class that declares an inner class in a method that has some member. <p>
 *
 * This code must be kept in sync with {@link com.google.gwt.dev.javac.TypeOracleUpdaterTestBase}.
 */
public class EnclosingLocalWithMember {
  public int foo;
  public Object getLocal() {
    /**
     * Class defined in a method.
     */
    class MyObject {
      int getFoo() {
        return foo;
      }
    }
    return new MyObject() {
    };
  }
}
