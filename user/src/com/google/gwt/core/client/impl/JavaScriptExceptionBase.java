/*
 * Copyright 2015 Google Inc.
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

/**
 * A helper so that we can change the parent of JavaScriptException via super-source. Otherwise we
 * would need to copy whole content of the class and also dev mode would choke.
 */
public class JavaScriptExceptionBase extends RuntimeException {
  public JavaScriptExceptionBase(Object e) {
    super();
  }
}