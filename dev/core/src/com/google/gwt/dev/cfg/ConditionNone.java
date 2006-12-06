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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.Iterator;

public class ConditionNone extends CompoundCondition {

  public ConditionNone() {
  }

  protected boolean doEval(TreeLogger logger, GeneratorContext context,
      String testType) throws UnableToCompleteException {
    for (Iterator iter = getConditions().iterator(); iter.hasNext();) {
      Condition condition = (Condition) iter.next();
      if (condition.isTrue(logger, context, testType)) {
        return false;
      }
    }
    return true;
  }

  protected String getEvalAfterMessage(String testType, boolean result) {
    if (result) {
      return "Yes: All subconditions were false";
    } else {
      return "No: One or more subconditions was true";
    }
  }

  protected String getEvalBeforeMessage(String testType) {
    return "Checking if all subconditions are false (<none>)";
  }
}
