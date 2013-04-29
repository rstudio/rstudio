/*
 * Copyright 2007 Google Inc.
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
 * Handles the -XdisableAggressiveOptimization command line flag.<br />
 *
 * Has been deprecated but preserved for backwards compatibility. The impact it has now is via its
 * cascaded modification of five more specific options (each of which is also modifiable via flag).
 */
@Deprecated
public final class ArgHandlerDisableAggressiveOptimization extends ArgHandlerFlag {

  private final OptionAggressivelyOptimize aggressivelyOptimizeOption;
  private final OptionClusterSimilarFunctions clusterSimilarFunctionsOption;
  private final OptionInlineLiteralParameters inlineLiteralParametersOption;
  private final OptionOptimizeDataflow optimizeDataflowOption;
  private final OptionOrdinalizeEnums ordinalizeEnumsOption;
  private final OptionRemoveDuplicateFunctions removeDuplicateFunctionsOption;

  public <
      T extends OptionAggressivelyOptimize & OptionClusterSimilarFunctions &
          OptionInlineLiteralParameters & OptionOptimizeDataflow &
          OptionOrdinalizeEnums & OptionRemoveDuplicateFunctions>
          ArgHandlerDisableAggressiveOptimization(T option) {
    this.aggressivelyOptimizeOption = option;
    this.clusterSimilarFunctionsOption = option;
    this.inlineLiteralParametersOption = option;
    this.optimizeDataflowOption = option;
    this.ordinalizeEnumsOption = option;
    this.removeDuplicateFunctionsOption = option;
  }

  @Override
  public String getPurpose() {
    return "Troubleshooting: Prevent the Production Mode compiler from "
        + "performing aggressive optimizations.";
  }

  @Override
  public String getTag() {
    return "-XdisableAggressiveOptimization";
  }

  @Override
  public boolean isUndocumented() {
    return true;
  }

  @Override
  public boolean setFlag() {
    aggressivelyOptimizeOption.setAggressivelyOptimize(false);
    clusterSimilarFunctionsOption.setClusterSimilarFunctions(false);
    inlineLiteralParametersOption.setInlineLiteralParameters(false);
    optimizeDataflowOption.setOptimizeDataflow(false);
    ordinalizeEnumsOption.setOrdinalizeEnums(false);
    removeDuplicateFunctionsOption.setRemoveDuplicateFunctions(false);
    return true;
  }
}
