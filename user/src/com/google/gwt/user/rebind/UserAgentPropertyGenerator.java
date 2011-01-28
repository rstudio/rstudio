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

package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

/**
 * Generator which writes out the JavaScript for determining the value of the
 * <code>user.agent</code> selection property.
 */
public class UserAgentPropertyGenerator implements PropertyProviderGenerator {

  /**
   * List of valid user agent selection property values, which helps ensure that
   * UserAgent.gwt.xml stays in sync with the
   * {@link #writeUserAgentPropertyJavaScript(SourceWriter)} method body of this
   * class.
   */
  private static final List<String> VALID_VALUES = Arrays.asList(new String[]{
      "ie6", "ie8", "gecko1_8", "safari", "opera"});

  /**
   * Writes out the JavaScript function body for determining the value of the
   * <code>user.agent</code> selection property. This method is used to create
   * the selection script and by {@link UserAgentGenerator} to assert at runtime
   * that the correct user agent permutation is executing. The list of
   * <code>user.agent</code> values listed here should be kept in sync with
   * {@link #VALID_VALUES} and <code>UserAgent.gwt.xml</code>.
   */
  static void writeUserAgentPropertyJavaScript(SourceWriter body) {
    body.println("var ua = navigator.userAgent.toLowerCase();");
    body.println("var makeVersion = function(result) {");
    body.indent();
    body.println("return (parseInt(result[1]) * 1000) + parseInt(result[2]);");
    body.outdent();
    body.println("};");
    body.println("if (ua.indexOf('opera') != -1) {");
    body.indent();
    body.println("return 'opera';");
    body.outdent();
    body.println("} else if (ua.indexOf('webkit') != -1) {");
    body.indent();
    body.println("return 'safari';");
    body.outdent();
    body.println("} else if (ua.indexOf('msie') != -1) {");
    body.indent();
    body.println("if ($doc.documentMode >= 8) {");
    body.indent();
    body.println("return 'ie8';");
    body.outdent();
    body.println("} else {");
    body.indent();
    body.println("var result = /msie ([0-9]+)\\.([0-9]+)/.exec(ua);");
    body.println("if (result && result.length == 3) {");
    body.indent();
    body.println("var v = makeVersion(result);");
    body.println("if (v >= 6000) {");
    body.indent();
    body.println("return 'ie6';");
    body.outdent();
    body.println("}");
    body.outdent();
    body.println("}");
    body.outdent();
    body.println("}");
    body.outdent();
    body.println("} else if (ua.indexOf('gecko') != -1) {");
    body.indent();
    body.println("return 'gecko1_8';");
    body.outdent();
    body.println("}");
    body.println("return 'unknown';");
  }

  public String generate(TreeLogger logger, SortedSet<String> possibleValues,
      String fallback, SortedSet<ConfigurationProperty> configProperties) {
    for (String value : possibleValues) {
      if (!VALID_VALUES.contains(value)) {
        logger.log(TreeLogger.ERROR, "Unrecognized user.agent property value '"
            + value + "', possibly due to UserAgent.gwt.xml and "
            + UserAgentPropertyGenerator.class.getName() + " being out of sync");
      }
    }

    StringSourceWriter body = new StringSourceWriter();
    body.println("{");
    body.indent();
    writeUserAgentPropertyJavaScript(body);
    body.outdent();
    body.println("}");

    return body.toString();
  }
}
