/*
 * Copyright 2006 Google Inc.
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
import com.google.gwt.thirdparty.guava.common.base.Objects;

/**
 * A deferred binding condition to determine whether the type being rebound is
 * exactly a particular type.
 */
//TODO(stalcup): guard against attempts to replace classes that have special prototype handling
//like String and Array.
public class ConditionWhenTypeIs extends Condition {

  private final String exactTypeSourceName;

  public ConditionWhenTypeIs(String exactTypeSourceName) {
    this.exactTypeSourceName = exactTypeSourceName;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ConditionWhenTypeIs) {
      ConditionWhenTypeIs that = (ConditionWhenTypeIs) object;
      return Objects.equal(this.exactTypeSourceName, that.exactTypeSourceName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(exactTypeSourceName);
  }

  @Override
  public String toSource() {
    return String.format("requestTypeClass == @%s::class", exactTypeSourceName);
  }

  @Override
  public String toString() {
    return "<when-type-is class='" + exactTypeSourceName + "'/>";
  }

  @Override
  protected boolean doEval(TreeLogger logger, DeferredBindingQuery query) {
    return exactTypeSourceName.equals(query.getTestType());
  }

  @Override
  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes, the requested type was an exact match";
    } else {
      return "Not an exact match";
    }
  }

  @Override
  protected String getEvalBeforeMessage(String testType) {
    return toString();
  }
}
