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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jjs.ast.JBinaryOperator;
import com.google.gwt.thirdparty.guava.common.base.Objects;

import java.util.Iterator;

/**
 * A compound condition that is only satisfied if all of its children are
 * unsatisfied.
 */
public class ConditionNone extends CompoundCondition {

  public ConditionNone(Condition... conditions) {
    super(conditions);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ConditionNone) {
      ConditionNone that = (ConditionNone) object;
      return Objects.equal(this.conditions, that.conditions);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(conditions);
  }

  @Override
  public String toSource() {
    return "!(" + super.toSource() + ")";
  }

  @Override
  protected boolean doEval(TreeLogger logger, DeferredBindingQuery query)
      throws UnableToCompleteException {
    for (Iterator<Condition> iter = getConditions().iterator(); iter.hasNext();) {
      Condition condition = iter.next();
      if (condition.isTrue(logger, query)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected String getBinaryOperator() {
    return JBinaryOperator.OR.toString();
  }

  @Override
  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes: All subconditions were false";
    } else {
      return "No: One or more subconditions was true";
    }
  }

  @Override
  protected String getEvalBeforeMessage(String testType) {
    return "Checking if all subconditions are false (<none>)";
  }
}
