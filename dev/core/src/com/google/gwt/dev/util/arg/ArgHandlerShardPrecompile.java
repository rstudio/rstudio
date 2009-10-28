/*
 * Copyright 2009 Google Inc.
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
 * An argument handler that enables running generators on shards.
 */
public class ArgHandlerShardPrecompile extends ArgHandlerFlag {
  private OptionEnableGeneratingOnShards options;

  public ArgHandlerShardPrecompile(OptionEnableGeneratingOnShards options) {
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return "Enables running generators on CompilePerms shards";
  }

  @Override
  public String getTag() {
    return "-XshardPrecompile";
  }

  @Override
  public boolean setFlag() {
    options.setEnabledGeneratingOnShards(true);
    return true;
  }
}
