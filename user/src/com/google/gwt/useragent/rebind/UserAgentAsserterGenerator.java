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

package com.google.gwt.useragent.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.useragent.client.UserAgentAsserter;
import com.google.gwt.useragent.client.UserAgentAsserter.UserAgentAsserterDisabled;

/**
 * Generator to enable/disable {@link UserAgentAsserter}. This generator exists because we can't
 * deferred-bind via configuration property.
 */
@RunsLocal(requiresProperties = {"user.agent", "user.agent.runtimeWarning"})
public class UserAgentAsserterGenerator extends Generator {

  private static final String PROPERTY_USER_AGENT_RUNTIME_WARNING = "user.agent.runtimeWarning";

  private static final String USER_AGENT_ASSERTER = UserAgentAsserter.class.getCanonicalName();
  private static final String USER_AGENT_ASSERTER_DISABLED =
      UserAgentAsserterDisabled.class.getCanonicalName();

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    try {
      ConfigurationProperty property =
          context.getPropertyOracle().getConfigurationProperty(PROPERTY_USER_AGENT_RUNTIME_WARNING);
      if (Boolean.valueOf(property.getValues().get(0)) == false) {
        return USER_AGENT_ASSERTER_DISABLED;
      }
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.WARN,
          "Unable to find value for '" + PROPERTY_USER_AGENT_RUNTIME_WARNING + "'", e);
    }
    return USER_AGENT_ASSERTER;
  }
}
