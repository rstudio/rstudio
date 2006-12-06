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
package com.google.gwt.sample.i18n.client;

import java.util.Map;

/**
 * Constants for I18N.
 */
public interface MyConstants extends com.google.gwt.i18n.client.Constants {

  /**
   * Translated "
   * <h4>Use of <i>Constants</i> interface:</h4>
   * Create a simple form.
   * <p>".
   * 
   * @return translated "
   *         <h4>Use of <i>Constants</i> interface:</h4>
   *         Create a simple form.
   *         <p>"
   * @gwt.key constantsExample
   */
  String constantsExample();

  /**
   * Translated "
   * <h4> Use of <i>ConstantsWithLookup</i> interface: </h4>
   * Translates colors from English into the current locale.".
   * 
   * @return translated "
   *         <h4> Use of <i>ConstantsWithLookup</i> interface: </h4>
   *         Translates colors from English into the current locale."
   * @gwt.key constantsWithLookupExample
   */
  String constantsWithLookupExample();

  /**
   * Translated "111".
   * 
   * @return translated "111"
   * @gwt.key defaultMB
   */
  String defaultMB();

  /**
   * Translated "Last Name".
   * 
   * @return translated "Last Name"
   * @gwt.key defaultRequired
   */
  String defaultRequired();

  /**
   * Translated "anything".
   * 
   * @return translated "anything"
   * @gwt.key defaultResource
   */
  String defaultResource();

  /**
   * Translated "puny".
   * 
   * @return translated "puny"
   * @gwt.key defaultSecurity
   */
  String defaultSecurity();

  /**
   * Translated
   * <h4>Use of <i>Dictionary</i> class:</h4>
   * Use constants defined on the surrounding HTML page. This is primarily a
   * mechanism for a server generating HTML pagesto supply strings to the
   * client. A Dictionary's values depend only on the containing HTML, not the
   * locale argument. The javaScript code below can be seen in the head element
   * of this HTML page
   * 
   * @return translated
   *         <h4>Use of <i>Dictionary</i> class:</h4>
   *         Use constants defined on the surrounding HTML page. This is
   *         primarily a mechanism for a server generating HTML pages to supply
   *         strings to the client. A Dictionary's values depend only on the
   *         containing HTML, not the locale argument. The javaScript code below
   *         can be seen in the head element of this HTML page
   */
  String dictionaryExample();

  /**
   * <PRE>
   * 
   * window.userInfo = { name : "Emily Crutcher", timeZone: "EST", userID
   * :"123", lastLogOn: "2/2/2006" }
   * 
   * </PRE>.
   * 
   * @return
   * 
   * <PRE>
   * 
   * window.userInfo = { name : "Emily Crutcher", timeZone: "EST", userID
   * :"123", lastLogOn: "2/2/2006" }
   * 
   * </PRE>
   */
  String dictionaryHTML();

  /**
   * Translated "(enter an int)".
   * 
   * @return translated "(enter an int)"
   * @gwt.key enterInt
   */
  String enterInt();

  /**
   * Translated "(enter a string)".
   * 
   * @return translated "(enter a string)"
   * @gwt.key enterString
   */
  String enterString();

  /**
   * Translated "First Name".
   * 
   * @return translated "First Name"
   * @gwt.key firstName
   */
  String firstName();

  /**
   * Translated "Gender".
   * 
   * @return translated "Gender"
   * @gwt.key gender
   */
  String gender();

  /**
   * Translated "f,m,u".
   * 
   * @return translated "f,m,u"
   * @gwt.key genderMap
   */
  Map genderMap();

  /**
   * Translated version of Messages.info().
   * 
   * @return info message.
   */
  String infoMessage();

  /**
   * Translated "Last Name".
   * 
   * @return translated "Last Name"
   * @gwt.key lastName
   */
  String lastName();

  /**
   * Translated "Male".
   * 
   * @return translated "Male"
   * @gwt.key m
   */
  String m();

  /**
   * Translated "Argument for {0}".
   * 
   * @return translated "Argument for {0}"
   * @gwt.key messageArgumentOne
   */
  String messageArgumentOne();

  /**
   * Translated "Argument for {1}".
   * 
   * @return translated "Argument for {1}"
   * @gwt.key messageArgumentTwo
   */
  String messageArgumentTwo();

  /**
   * Translated "
   * <h4>Use of <i>Messages</i> interface: </h4>
   * Here are some sample user messages that might be created in an
   * internationalized application. ".
   * 
   * @return translated "
   *         <h4>Use of <i>Messages</i> interface: </h4>
   *         Here are some sample user messages that might be created in an
   *         internationalized application. "
   * @gwt.key messagesExample
   */
  String messagesExample();

  /**
   * Translated "Message Template".
   * 
   * @return translated "Message Template"
   * @gwt.key messageTemplates
   */
  String messageTemplates();

  /**
   * Translated "<b> No Result </b>".
   * 
   * @return translated "<b> No Result </b>"
   * @gwt.key noResult
   */
  String noResult();

  /**
   * Translated "Result".
   * 
   * @return translated "Result"
   * @gwt.key result
   */
  String result();

  /**
   * Translated "Show message".
   * 
   * @return translated "Show message"
   * @gwt.key showMessage
   */
  String showMessage();

  /**
   * Translated "Translate".
   * 
   * @return translated "Translate"
   * @gwt.key translate
   */
  String translate();

  /**
   * Translated "Type Color Here".
   * 
   * @return translated "Type Color Here"
   * @gwt.key typeColorHere
   */
  String typeColorHere();

}
