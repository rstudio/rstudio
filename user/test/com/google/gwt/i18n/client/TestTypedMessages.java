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
package com.google.gwt.i18n.client;

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Test messages that take particular types for parameters.
 */
public interface TestTypedMessages extends Messages {

  /**
   * testAllTypes = int({0}) float({1}), long({2}), boolean({3}), Object({4},
   * char({5}), byte({6}), short({7});.
   */
  String testAllTypes(int a, float f, long l, boolean bool, Object obj, char c,
      byte b, short s);

  @Key("testAllTypes")
  SafeHtml testAllTypesAsSafeHtml(int a, float f, long l, boolean bool, Object obj, char c,
      byte b, short s);

  // testLotsOfInts = {0}, {1},{2},{3}
  String testLotsOfInts(int a, int b, int c, int d);

  @Key("testLotsOfInts")
  SafeHtml testLotsOfIntsAsSafeHtml(int a, int b, int c, int d);

  // testSomeObjectTypes = this({0}), StringBuffer({1}), Integer({2}), null{3});
  String testSomeObjectTypes(Object test, StringBuffer buf, Integer i,
      Object giveMeANull);

  @Key("testSomeObjectTypes")
  SafeHtml testSomeObjectTypesAsSafeHtml(Object test, StringBuffer buf,
      Integer i, Object giveMeANull);

  // testSingleQuotes = ''A'', ''{0}'', '','''
  String testSingleQuotes(String someArg);

  @Key("testSingleQuotes")
  SafeHtml testSingleQuotesAsSafeHtml(String someArg);

  // simpleMessageTest={0}
  String simpleMessageTest(float arg);

  @Key("simpleMessageTest")
  SafeHtml simpleMessageTestAsSafeHtml(float arg);

  // stringEscaping= "'\ \\ \\\ & \t \n\r\"\' \ end
  String stringEscaping(int a);

  @Key("stringEscaping")
  SafeHtml stringEscapingAsSafeHtml(int a);
}
