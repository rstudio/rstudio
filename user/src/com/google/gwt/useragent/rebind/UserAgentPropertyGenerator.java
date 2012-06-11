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
   * {@link #writeUserAgentPropertyJavaScript(SourceWriter,SortedSet)} method body of this
   * class.
   */
  private static final List<String> VALID_VALUES = Arrays.asList(new String[]{
      "ie6", "ie8", "gecko1_8", "safari", "opera", "ie9"});

  /**
   * List of predicates to identify user agent.
   * The order of evaluation is from top to bottom, i.e., the first matching
   * predicate will have the associated ua token returned.
   * ua is defined in an outer scope and is therefore visible in
   * the predicate javascript fragment.
   */
  private static UserAgentPropertyGeneratorPredicate[] predicates =
    new UserAgentPropertyGeneratorPredicate[] {

      // opera
      new UserAgentPropertyGeneratorPredicate("opera")
      .getPredicateBlock()
        .println("return (ua.indexOf('opera') != -1);")
      .returns("'opera'"),

      // webkit family
      new UserAgentPropertyGeneratorPredicate("safari")
      .getPredicateBlock()
        .println("return (ua.indexOf('webkit') != -1);")
      .returns("'safari'"),

      // IE9
      new UserAgentPropertyGeneratorPredicate("ie9")
      .getPredicateBlock()
        .println("return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 9));")
      .returns("'ie9'"),
      
      // IE8
      new UserAgentPropertyGeneratorPredicate("ie8")
      .getPredicateBlock()
        .println("return (ua.indexOf('msie') != -1 && ($doc.documentMode >= 8));")
      .returns("'ie8'"),

      // IE6
      new UserAgentPropertyGeneratorPredicate("ie6")
      .getPredicateBlock()
        .println("var result = /msie ([0-9]+)\\.([0-9]+)/.exec(ua);")
        .println("if (result && result.length == 3)")
        .indent()
          .println("return (makeVersion(result) >= 6000);")
        .outdent()
      .returns("'ie6'"),

      // gecko family
      new UserAgentPropertyGeneratorPredicate("gecko1_8")
      .getPredicateBlock()
        .println("return (ua.indexOf('gecko') != -1);")
      .returns("'gecko1_8'"),
  };

  /**
   * Writes out the JavaScript function body for determining the value of the
   * <code>user.agent</code> selection property. This method is used to create
   * the selection script and by {@link UserAgentGenerator} to assert at runtime
   * that the correct user agent permutation is executing. The list of
   * <code>user.agent</code> values listed here should be kept in sync with
   * {@link #VALID_VALUES} and <code>UserAgent.gwt.xml</code>.
   */
  static void writeUserAgentPropertyJavaScript(SourceWriter body, 
      SortedSet<String> possibleValues) {

    // write preamble
    body.println("var ua = navigator.userAgent.toLowerCase();");
    body.println("var makeVersion = function(result) {");
    body.indent();
    body.println("return (parseInt(result[1]) * 1000) + parseInt(result[2]);");
    body.outdent();
    body.println("};");

    // write only selected user agents 
    for (int i = 0; i < predicates.length; i++) {
      if (possibleValues.contains(predicates[i].getUserAgent())) {
        body.println("if ((function() { ");
        body.indent();
        body.print(predicates[i].toString());
        body.outdent();
        body.println("})()) return " + predicates[i].getReturnValue() + ";");
      }
    }
    
    // default return
    body.println("return 'unknown';");
  }

  @Override
  public String generate(TreeLogger logger, SortedSet<String> possibleValues,
      String fallback, SortedSet<ConfigurationProperty> configProperties) {
    for (String value : possibleValues) {
      if (!VALID_VALUES.contains(value)) {
        logger.log(TreeLogger.WARN, "Unrecognized "
            + UserAgentGenerator.PROPERTY_USER_AGENT + " property value '"
            + value + "', possibly due to UserAgent.gwt.xml and "
            + UserAgentPropertyGenerator.class.getName()
            + " being out of sync." + " Use <set-configuration-property name=\""
            + UserAgentGenerator.PROPERTY_USER_AGENT_RUNTIME_WARNING
            + "\" value=\"false\"/> to suppress this warning message.");
      }
    }
    // make sure that the # of ua in VALID_VALUES
    // is the same of predicates. maybe should iterate
    // to make sure each one has a match.
    assert predicates.length == VALID_VALUES.size();
    StringSourceWriter body = new StringSourceWriter();
    body.println("{");
    body.indent();
    writeUserAgentPropertyJavaScript(body, possibleValues);
    body.outdent();
    body.println("}");

    return body.toString();
  }
}