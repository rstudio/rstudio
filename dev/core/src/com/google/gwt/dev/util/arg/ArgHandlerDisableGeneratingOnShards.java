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
 * An undocumented option to disable running generators on CompilePerms shards.
 * This is present as a safety valve, in case something is new with the newer
 * staging. Note that the old staging is used, regardless of this option's
 * setting, if any linker is seen that has been updated. Thus, this option is
 * useful only when all linkers have been updated but nonetheless there is a
 * problem.
 */
public class ArgHandlerDisableGeneratingOnShards extends ArgHandlerFlag {
  private OptionEnableGeneratingOnShards options;

  public ArgHandlerDisableGeneratingOnShards(
      OptionEnableGeneratingOnShards options) {
    this.options = options;
  }

  @Override
  public String getPurpose() {
    return "Disables running generators on CompilePerms shards, even when it would be a likely speedup";
  }

  @Override
  public String getTag() {
    return "-XdisableGeneratingOnShards";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag() {
    options.setEnabledGeneratingOnShards(false);
    return true;
  }
}
