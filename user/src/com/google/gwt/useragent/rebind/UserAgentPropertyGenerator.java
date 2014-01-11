/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * Generator which writes out the JavaScript for determining the value of the
 * <code>user.agent</code> selection property.
 */
public class UserAgentPropertyGenerator implements PropertyProviderGenerator {

  /**
   * The list of {@code user.agent} values listed here should be kept in sync with
   * {@code UserAgent.gwt.xml}.
   * <p>Note that the order of enums matter as the script selection is based on running
   * these predicates in order and matching the first one that returns {@code true}.
   */
  private enum UserAgent {
    opera("return (ua.indexOf('opera') != -1);"),
    safari("return (ua.indexOf('webkit') != -1);"),
    ie10("return (ua.indexOf('msie') != -1 && ($doc.documentMode == 10));"),
    ie9("return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 9));"),
    ie8("return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 8));"),
    gecko1_8("return (ua.indexOf('gecko') != -1);");

    private final String predicateBlock;

    private UserAgent(String predicateBlock) {
      this.predicateBlock = predicateBlock;
    }

    private static Set<String> getKnownAgents() {
      HashSet<String> userAgents = new HashSet<String>();
      for (UserAgent userAgent : values()) {
        userAgents.add(userAgent.name());
      }
      return userAgents;
    }
  }

  /**
   * Writes out the JavaScript function body for determining the value of the
   * <code>user.agent</code> selection property. This method is used to create
   * the selection script and by {@link UserAgentGenerator} to assert at runtime
   * that the correct user agent permutation is executing.
   */
  static void writeUserAgentPropertyJavaScript(SourceWriter body,
      SortedSet<String> possibleValues) {

    // write preamble
    body.println("var ua = navigator.userAgent.toLowerCase();");

    for (UserAgent userAgent : UserAgent.values()) {
      // write only selected user agents
      if (possibleValues.contains(userAgent.name())) {
        body.println("if ((function() { ");
        body.indentln(userAgent.predicateBlock);
        body.println("})()) return '%s';", userAgent.name());
      }
    }

    // default return
    body.println("return 'unknown';");
  }

  @Override
  public String generate(TreeLogger logger, SortedSet<String> possibleValues, String fallback,
      SortedSet<ConfigurationProperty> configProperties) {
    assertUserAgents(logger, possibleValues);

    StringSourceWriter body = new StringSourceWriter();
    body.println("{");
    body.indent();
    writeUserAgentPropertyJavaScript(body, possibleValues);
    body.outdent();
    body.println("}");

    return body.toString();
  }

  private static void assertUserAgents(TreeLogger logger, SortedSet<String> possibleValues) {
    HashSet<String> unknownValues = new HashSet<String>(possibleValues);
    unknownValues.removeAll(UserAgent.getKnownAgents());
    if (!unknownValues.isEmpty()) {
      logger.log(TreeLogger.WARN, "Unrecognized " + UserAgentGenerator.PROPERTY_USER_AGENT
          + " values " + unknownValues + ", possibly due to UserAgent.gwt.xml and "
          + UserAgentPropertyGenerator.class.getName() + " being out of sync.");
    }
  }
}
