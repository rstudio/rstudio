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
import com.google.gwt.dev.util.collect.Sets;

import java.io.Serializable;
import java.util.Set;

/**
 * Abstract base class for various kinds of deferred binding conditions.
 */
public abstract class Condition implements Serializable {
  /**
   * Returns the set of property names that the Condition requires in order to
   * be evaluated.
   */
  public Set<String> getRequiredProperties() {
    return Sets.create();
  }

  /**
   * Test the condition with the given parameters. If <code>testType</code> is
   * <code>null</code>, then this condition isn't being used to remap a type,
   * and <code>typeOracle</code> can also be <code>null</code>.
   */
  public final boolean isTrue(TreeLogger logger, DeferredBindingQuery query)
      throws UnableToCompleteException {

    boolean logDebug = logger.isLoggable(TreeLogger.DEBUG);

    if (logDebug) {
      String startMsg = getEvalBeforeMessage(query.getTestType());
      logger = logger.branch(TreeLogger.DEBUG, startMsg, null);
    }

    boolean result = doEval(logger, query);

    if (logDebug) {
      String afterMsg = getEvalAfterMessage(query.getTestType(), result);
      logger.log(TreeLogger.DEBUG, afterMsg, null);
    }

    return result;
  }

  protected abstract boolean doEval(TreeLogger logger, DeferredBindingQuery query)
      throws UnableToCompleteException;

  protected abstract String getEvalAfterMessage(String testType, boolean result);

  protected abstract String getEvalBeforeMessage(String testType);
}
