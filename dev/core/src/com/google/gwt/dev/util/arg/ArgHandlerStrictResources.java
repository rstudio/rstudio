/*
 * Copyright 2013 Google Inc.
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
 * Generally whether to be strict about resource loading and in particular whether to implicitly add
 * "client" and "public" resource dependencies when none are mentioned.
 */
public class ArgHandlerStrictResources extends ArgHandlerFlag {

  private final OptionStrictSourceResources optionStrictSourceResources;
  private final OptionStrictPublicResources optionStrictPublicResources;

  public <T extends OptionStrictSourceResources &
                    OptionStrictPublicResources> ArgHandlerStrictResources(T options) {
    this.optionStrictSourceResources = options;
    this.optionStrictPublicResources = options;

    addTagValue("-XstrictResources", true);
  }

  @Override
  public boolean getDefaultValue() {
    assert optionStrictSourceResources.enforceStrictSourceResources()
        == optionStrictPublicResources.enforceStrictPublicResources();
    return optionStrictSourceResources.enforceStrictSourceResources();
  }

  @Override
  public String getLabel() {
    return "enforceStrictResources";
  }

  @Override
  public String getPurposeSnippet() {
    return "Avoid adding implicit dependencies on \"client\" and \"public\" for "
        + "modules that don't define any dependencies.";
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean setFlag(boolean value) {
    optionStrictSourceResources.setEnforceStrictSourceResources(value);
    optionStrictPublicResources.setEnforceStrictPublicResources(value);
    return true;
  }
}
