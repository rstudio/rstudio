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

package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;

/**
 * A condition that is true when the active linkers include the one specified.
 */
public class ConditionWhenLinkerAdded extends Condition {
  private final String linkerName;

  public ConditionWhenLinkerAdded(String linkerName) {
    this.linkerName = linkerName;
  }

  @Override
  public String toString() {
    return "<when-linkers-include name='" + linkerName + "'/>";
  }

  @Override
  protected boolean doEval(TreeLogger logger, DeferredBindingQuery query) {
    return query.getLinkerNames().contains(linkerName);
  }

  @Override
  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes, the requested linker is active";
    } else {
      return "No, the requested linker is not active";
    }
  }

  @Override
  protected String getEvalBeforeMessage(String testType) {
    return toString();
  }
}
