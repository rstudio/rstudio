/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.i18n.client.gen;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Interface to represent the messages contained in resource bundle:
 * 'com/google/gwt/i18n/client/gen/TestMessages.properties'.
 */
public interface TestMessages extends com.google.gwt.i18n.client.Messages {

  /**
   * Translated "no args".
   * 
   * @return translated "no args"
   */
  @DefaultMessage("no args")
  @Key("args0")
  String args0();

  @DefaultMessage("no args")
  @Key("args0")
  SafeHtml args0AsSafeHtml();

  /**
   * Translated "{0} is a arg".
   * 
   * @return translated "{0} is a arg"
   */
  @DefaultMessage("{0} is a arg")
  @Key("args1")
  String args1(String arg0);

  @DefaultMessage("{0} is a arg")
  @Key("args1")
  SafeHtml args1AsSafeHtml(String arg0);

  /**
   * Translated "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}".
   * 
   * @return translated "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}"
   */
  @DefaultMessage("{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}")
  @Key("args10")
  String args10(String arg0, String arg1, String arg2, String arg3,
      String arg4, String arg5, String arg6, String arg7, String arg8,
      String arg9);

  @DefaultMessage("{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}")
  @Key("args10")
  SafeHtml args10AsSafeHtml(String arg0, String arg1, String arg2, String arg3,
      String arg4, String arg5, String arg6, String arg7, String arg8,
      String arg9);

  /**
   * Translated "{1} is the second arg, {0} is the first".
   * 
   * @return translated "{1} is the second arg, {0} is the first"
   */
  @DefaultMessage("{1} is the second arg, {0} is the first")
  @Key("args2")
  String args2(String arg0, String arg1);

  @DefaultMessage("{1} is the second arg, {0} is the first")
  @Key("args2")
  SafeHtml args2AsSafeHtml(String arg0, String arg1);

  /**
   * Translated "arg0arg1 arg0,arg1 {0}arg4".
   * 
   * @return translated "arg0arg1 arg0,arg1 {0}arg4"
   */
  @DefaultMessage("arg0arg1 arg0,arg1 {0}arg4")
  @Key("argsTest")
  String argsTest(String arg0);

  @DefaultMessage("arg0arg1 arg0,arg1 {0}arg4")
  @Key("argsTest")
  SafeHtml argsTestAsSafeHtml(String arg0);

  /**
   * Translated "{0},{1}, \"a\",\"b\", \"{0}\", \"{1}\", ''a'', 'b', '{0}',
   * ''{1}''".
   * 
   * @return translated "{0},{1}, \"a\",\"b\", \"{0}\", \"{1}\", ''a'', 'b',
   *         '{0}', ''{1}''"
   */
  @DefaultMessage("{0},{1}, \"a\",\"b\", \"{0}\", \"{1}\", ''a'', 'b', '{0}', ''{1}''")
  @Key("argsWithQuotes")
  String argsWithQuotes(String arg0, String arg1);

  @DefaultMessage("{0},{1}, \"a\",\"b\", \"{0}\", \"{1}\", ''a'', 'b', '{0}', ''{1}''")
  @Key("argsWithQuotes")
  SafeHtml argsWithQuotesAsSafeHtml(String arg0, String arg1);

  /**
   * Translated "".
   * 
   * @return translated ""
   */
  @DefaultMessage("")
  @Key("empty")
  String empty();

  @DefaultMessage("")
  @Key("empty")
  SafeHtml emptyAsSafeHtml();

  /**
   * Translated "'{'quoted'}'".
   * 
   * @return translated "'{'quoted'}'"
   */
  @DefaultMessage("'{'quoted'}'")
  @Key("quotedBraces")
  String quotedBraces();

  @DefaultMessage("'{'quoted'}'")
  @Key("quotedBraces")
  SafeHtml quotedBracesAsSafeHtml();

  /**
   * Translated "{0}".
   * 
   * @return translated "{0}"
   */
  @DefaultMessage("{0}")
  @Key("simpleMessageTest")
  String simpleMessageTest(String arg0);

  @DefaultMessage("{0}")
  @Key("simpleMessageTest")
  SafeHtml simpleMessageTestAsSafeHtml(String arg0);

  /**
   * Translated "repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}".
   * 
   * @return translated "repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}"
   */
  @DefaultMessage("repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}")
  @Key("testLotsOfUsageOfArgs")
  String testLotsOfUsageOfArgs(String arg0, String arg1);

  @DefaultMessage("repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}")
  @Key("testLotsOfUsageOfArgs")
  SafeHtml testLotsOfUsageOfArgsAsSafeHtml(String arg0, String arg1);

  /**
   * Translated "\"~\" ~~ \"~~~~ \"\"".
   * 
   * @return translated "\"~\" ~~ \"~~~~ \"\""
   */
  @DefaultMessage("\"~\" ~~ \"~~~~ \"\"")
  @Key("testWithXs")
  String testWithXs();

  @DefaultMessage("\"~\" ~~ \"~~~~ \"\"")
  @Key("testWithXs")
  SafeHtml testWithXsAsSafeHtml();

  /**
   * Translated "お{0}你{1}好".
   * 
   * @return translated "お{0}你{1}好"
   */
  @DefaultMessage("お{0}你{1}好")
  @Key("unicode")
  String unicode(String arg0, String arg1);

  @DefaultMessage("お{0}你{1}好")
  @Key("unicode")
  SafeHtml unicodeAsSafeHtml(String arg0, String arg1);
}
