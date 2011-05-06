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

package com.google.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;

/**
 * A linker that adds a script tag to the iframe rather than downloading the
 * code as a string and then installing it into the iframe.
 */
public class DirectInstallLinker extends CrossSiteIframeLinker {
  @Override
  protected String getJsInstallScript(LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/installScriptDirect.js";
  }
  
  @Override
  protected boolean shouldInstallCode(LinkerContext context) {
    return false;
  }
}
