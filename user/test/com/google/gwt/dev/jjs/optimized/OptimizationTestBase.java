/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev.jjs.optimized;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Base class for optimizations tests.
 */
public abstract class OptimizationTestBase extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuiteOptimized";
  }

  public static void assertFunctionMatches(String functionDef, String pattern) {
    String content = getFunctionContent(functionDef);
    String regex = createRegex(pattern);
    assertTrue("content: \"" + content + "\" does not match pattern: " + regex,
        content.matches(regex));
  }

  private static String getFunctionContent(String functionDef) {
    String stripped = functionDef.replaceAll("\\s", "");
    String funcDeclare = "function(){";
    assertTrue("Resulting function: " + functionDef, stripped.startsWith(funcDeclare));
    stripped = stripped.substring(funcDeclare.length());
    assertTrue("Resulting function: " + functionDef, stripped.endsWith("}"));
    stripped = stripped.substring(0, stripped.length() - 1);
    stripped = stripped.replace('"', '\''); // for HtmlUnit
    return stripped;
  }

  private static String createRegex(String pattern) {
    for (char toBeEscaped : ".[](){}+=?".toCharArray()) {
      pattern = pattern.replace("" + toBeEscaped, "\\" + toBeEscaped);
    }
    pattern = pattern.replace("\\(\\)", "(\\(\\))?"); // to account for the removal of ()
                                                      // in new operations.
    pattern = pattern.replace("<obf>", "[\\w$_]+");
    return pattern + ";?";
  }
}
