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
package com.google.gwt.dev.cfg;

import com.google.gwt.thirdparty.guava.common.collect.Maps;

import java.util.Map;

/**
 * Generator used to generate runtime rebind rule class definitions.<br />
 */
public class RuntimeRebindRuleGenerator {

  public static final Map<String, String> RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME =
      Maps.newLinkedHashMap();
  public static int runtimeRebindRuleCount = 0;

  /**
   * Generates and stores a runtime rebind rule class definition for the given match expression and
   * instance creation expression.<br />
   *
   * The provided instance creation expression *must* be JSNI to avoid private access restrictions
   * on the types being instantiated.
   */
  public void generate(String jsniCreateInstanceExpression, String matchesExpression) {
    String typeShortName = "RuntimeRebindRule" + runtimeRebindRuleCount++;

    StringBuilder typeBody = new StringBuilder();
    typeBody.append(
        "private static class " + typeShortName + " extends RuntimeRebindRule {\n");
    typeBody.append("  public native Object createInstance() /*-{\n");
    typeBody.append("    " + jsniCreateInstanceExpression + "\n");
    typeBody.append("  }-*/;\n");
    typeBody.append("  public native boolean matches(Class<?> requestTypeClass) /*-{\n");
    typeBody.append("    " + matchesExpression + "\n");
    typeBody.append("  }-*/;\n");
    typeBody.append("}\n");

    RUNTIME_REBIND_RULE_SOURCES_BY_SHORT_NAME.put(typeShortName, typeBody.toString());
  }
}
