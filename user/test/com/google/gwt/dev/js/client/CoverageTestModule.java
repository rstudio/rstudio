/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.dev.js.client;

import com.google.gwt.core.client.GWT;

/**
 * Dummy class used to verify that coverage is sane.
 */
public class CoverageTestModule {
  public static void method() {
    Integer x = new Integer(42);
    int y = x + 10;
    if (x < y) {
      // This comment will be ignored
      GWT.log("This line should be covered");
    } else {
      GWT.log("This line should not be covered");
    }
  }
}
