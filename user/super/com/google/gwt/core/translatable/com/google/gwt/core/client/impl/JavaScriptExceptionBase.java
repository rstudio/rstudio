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

import com.google.gwt.core.client.GwtScriptOnly;

/**
 * A super-source version of JavaScriptExceptionBase so that we can call super constructor that
 * disables #fillInStackTrace without breaking Java6 compatibility.
 */
@GwtScriptOnly
public class JavaScriptExceptionBase extends RuntimeException {

  public JavaScriptExceptionBase() {
    // Stack trace is not-writeable from outside and we don't want unnecessary fillInStackTrace
    // calls from super constructor as well.
    super(null, null, true, false);
  }

  public JavaScriptExceptionBase(String message) {
    super(message);
  }
}
