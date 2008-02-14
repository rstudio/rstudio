/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.linker.impl;

import com.google.gwt.dev.linker.ModuleScriptResource;

import java.net.URL;

/**
 * The standard implementation of {@link ModuleScriptResource}.
 */
public class StandardScriptResource extends StandardModuleResource implements
    ModuleScriptResource {

  public StandardScriptResource(String src) {
    this(src, null);
  }

  public StandardScriptResource(String src, URL url) {
    super(src, url);
  }

  public String getSrc() {
    return getId();
  }
}
