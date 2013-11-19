/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.core.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A runtime rebinder of requested types as well as the central registry for rebind rules to use to
 * service these rebind requests.
 */
public class RuntimeRebinder {

  /**
   * A cache of rebind rules that have been already found to be the correct handler for a given type
   * name.
   */
  private static Map<String, RuntimeRebindRule> runtimeRebindRuleByRequestTypeName =
      new HashMap<String, RuntimeRebindRule>();

  /**
   * The registered list of rebind rules to apply when servicing requests. Most recently added rules
   * are appended to the end and given higher precedence.
   */
  private static List<RuntimeRebindRule> runtimeRebindRules = new ArrayList<RuntimeRebindRule>();

  /**
   * Returns an instance to satisfy the requested type by first finding the registered rebind rule
   * that matches the requested type and current browser state and then uses that rebind rule to
   * create an object instance.
   */
  public static Object createInstance(String requestTypeName) {
    // If the matching rebind rule has been previously located.
    if (runtimeRebindRuleByRequestTypeName.containsKey(requestTypeName)) {
      // Grab it.
      RuntimeRebindRule runtimeRebindRule = runtimeRebindRuleByRequestTypeName.get(requestTypeName);
      // And use it to create an instance to fulfill the request.
      return runtimeRebindRule.createInstance();
    }

    // Otherwise look at the registered rebind rules in reverse order, so that recently added rules
    // have precedence.
    for (int i = runtimeRebindRules.size() - 1; i >= 0; i--) {
      RuntimeRebindRule runtimeRebindRule = runtimeRebindRules.get(i);
      // If the current rule matches the requested type and current browser state.
      if (runtimeRebindRule.matches(requestTypeName)) {
        // Then cache the rule for later.
        runtimeRebindRuleByRequestTypeName.put(requestTypeName, runtimeRebindRule);
        // And use the rule to create an instance to fulfill the request.
        return runtimeRebindRule.createInstance();
      }
    }

    throw new RuntimeException("Could not rebind " + requestTypeName + ".");
  }

  /**
   * Registers a rebind rule in a list that will be used to satisfy future rebind requests.
   */
  public static void registerRuntimeRebindRule(RuntimeRebindRule runtimeRebindRule) {
    runtimeRebindRules.add(runtimeRebindRule);
  }
}
