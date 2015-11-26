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

import java.util.Arrays;
import java.util.List;

/**
 * Enables run-time cast checking.
 */
public class ArgHandlerDisableCastChecking extends ArgHandlerFlag {

  private OptionSetProperties setProperties;

  public ArgHandlerDisableCastChecking(OptionSetProperties setProperties) {
    this.setProperties = setProperties;
    addTagValue("-XdisableCastChecking", false);
  }

  @Override
  public String getPurposeSnippet() {
    return "DEPRECATED: use jre.checks.checkLevel instead.";
  }

  @Override
  public String getLabel() {
    return "checkCasts";
  }

  @Override
  public boolean setFlag(boolean value) {
    List<String> propertyValue = Arrays.asList(value ? "ENABLED" : "DISABLED");
    setProperties.setPropertyValues("jre.checks.type", propertyValue);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return false;
  }
}
