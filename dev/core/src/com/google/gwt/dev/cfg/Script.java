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
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.js.ast.JsFunction;

/**
 * Represents configuration for a dynamically-injected script.  
 */
public class Script {

  public Script(String src, JsFunction jsReadyFn) {
    this.src = src;
    this.jsReadyFn = jsReadyFn;
  }

  public String getSrc() {
    return src;
  }

  public JsFunction getJsReadyFunction() {
    return jsReadyFn;
  }

  private final String src;
  private final JsFunction jsReadyFn;
}
