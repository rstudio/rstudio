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

import com.google.gwt.util.tools.ArgHandlerInt;

/**
 * Set the optimization level from the command line.  For now, level 1 is the same
 * as draft compile, and level 9 is the same as the default (maximium optimization).
 * 
 * TODO(zundel): In theory, a level 0 should be possible, where all optimizers 
 * are eliminated for the fastest possible compile.  In practice, code generation 
 * depends on some optimizers being run.
 */
public class ArgHandlerOptimize extends ArgHandlerInt {
  
  private final OptionOptimize options;

  public ArgHandlerOptimize(OptionOptimize options) {
    this.options = options;
  }
  
  @Override
  public String getPurpose() {
    return "Sets the optimization level used by the compiler.  0=none 9=maximum.";
  }

  @Override
  public String getTag() {
    return "-optimize";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"level"};
  }

  @Override
  public void setInt(int level) {
    if (level <= OptionOptimize.OPTIMIZE_LEVEL_MAX) {
      options.setOptimizationLevel(Math.max(level, OptionOptimize.OPTIMIZE_LEVEL_DRAFT));  
    } else {
      options.setOptimizationLevel(OptionOptimize.OPTIMIZE_LEVEL_MAX);
    }
  }
}
