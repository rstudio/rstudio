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
 * An ArgHandler to enable draft compiles.
 */
public class ArgHandlerDraftCompile extends ArgHandlerFlag {

  private final OptionAggressivelyOptimize aggressivelyOptimizeOption;
  private final OptionOptimize optimizeOption;

  public <T extends OptionOptimize & OptionAggressivelyOptimize> ArgHandlerDraftCompile(T option) {
    this.optimizeOption = option;
    this.aggressivelyOptimizeOption = option;
  }

  @Override
  public String getPurpose() {
    return "Enable faster, but less-optimized, compilations";
  }

  @Override
  public String getTag() {
    return "-draftCompile";
  }

  @Override
  public boolean setFlag() {
    optimizeOption.setOptimizationLevel(OptionOptimize.OPTIMIZE_LEVEL_DRAFT);
    aggressivelyOptimizeOption.setAggressivelyOptimize(false);
    return true;
  }
}
