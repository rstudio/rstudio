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
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.StandardGeneratorContext;

/**
 * Abstract base class for various kinds of deferred binding rules.
 */
public abstract class Rule {

  protected RuntimeRebindRuleGenerator runtimeRebindRuleGenerator =
      new RuntimeRebindRuleGenerator();
  private int fallbackEvalCost = Integer.MAX_VALUE;
  private final ConditionAll rootCondition = new ConditionAll();

  /**
   * Generate runtime rebind classes that perform the same rebinding behavior represented by the
   * current Rule.
   */
  @SuppressWarnings("unused")
  public void generateRuntimeRebindClasses(
      TreeLogger logger, ModuleDef module, GeneratorContext context)
      throws UnableToCompleteException {
    // Defaults to doing nothing.
  }

  /**
   * Returns the cost of evaluation fallback binding values.
   * when isApplicable() is true, this value is meaningless
   * when isApplicable() is false, [1,Integer.MAX_VALUE-1] means
   * a relative scale of cost, where cost c is better than cost c+k
   * Integer.MAX_VALUE implies no match.
   * @return cost of evaluating fallback values.
   */
  public int getFallbackEvaluationCost() {
    return fallbackEvalCost;
  }

  public ConditionAll getRootCondition() {
    return rootCondition;
  }

  public boolean isApplicable(TreeLogger logger, StandardGeneratorContext context, String typeName)
      throws UnableToCompleteException {
    DeferredBindingQuery query = new DeferredBindingQuery(
        context.getPropertyOracle(), context.getActiveLinkerNames(),
        context.getCompilationState(), typeName);
    boolean result = rootCondition.isTrue(logger, query);
    fallbackEvalCost = query.getFallbackEvaluationCost();
    return result;
  }

  public abstract RebindResult realize(TreeLogger logger,
      StandardGeneratorContext context, String typeName)
      throws UnableToCompleteException;

  protected void dispose() {
  }

  /**
   * Generate and return a String of Java source that will create an instance of whatever type this
   * Rule intends to rebind to.
   */
  protected abstract String generateCreateInstanceExpression();

  /**
   * Generate and return a String of Java source that will act as a condition to filter the runtime
   * environment and only pass when the Rule's intended conditions are met.
   */
  protected abstract String generateMatchesExpression();
}
