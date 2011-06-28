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
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * An ArgHandler to provide the -XenableGeneratorResultCaching flag.
 * 
 * Note: This is no longer needed since generator result caching is now enabled by default.
 * It's left here for an interim period, so that uses of the flag can be removed.
 * TODO(jbrosenberg): remove this after interim period.
 */
public class ArgHandlerEnableGeneratorResultCaching extends ArgHandlerFlag {

  public ArgHandlerEnableGeneratorResultCaching() {
  }

  @Override
  public String getPurpose() {
    return "Enables generator result caching, for those generators that implement it";
  }

  @Override
  public String getTag() {
    return "-XenableGeneratorResultCaching";
  }
  
  @Override
  public boolean isUndocumented() {
    return true;
  }
 
  @Override
  public boolean setFlag() {
    return true;
  }
}
