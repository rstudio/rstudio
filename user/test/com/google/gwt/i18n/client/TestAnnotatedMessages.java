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

package com.google.gwt.i18n.client;

import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.Generate;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;

import java.util.Date;

/**
 * Test of Messages generation using annotations.
 */
@DefaultLocale("en-US")
// @GenerateKeys("com.google.gwt.i18n.rebind.keygen.MD5KeyGenerator")
@GenerateKeys("com.google.gwt.i18n.rebind.keygen.MethodNameKeyGenerator")
// default
@Generate(format = "com.google.gwt.i18n.rebind.format.PropertiesFormat")
public interface TestAnnotatedMessages extends Messages {

  /**
   * Test of property file lookup on nested classes.
   * 
   * nestedDollar() is redefined in a property file with a $ in it.
   * nestedUnderscore() is redefined in a property file with a _ in it.
   */
  public interface Nested extends Messages {

    @DefaultMessage("nested dollar")
    String nestedDollar();

    @DefaultMessage("nested underscore")
    String nestedUnderscore();
  }

  @DefaultMessage("Test me")
  String basicText();

  @DefaultMessage("Once more, with meaning")
  @Meaning("Mangled quote")
  String withMeaning();

  @DefaultMessage("One argument: {0}")
  String oneArgument(String value);

  @DefaultMessage("One argument, which is optional")
  String optionalArgument(@Optional
  String value);

  @DefaultMessage("Two arguments, {1} and {0}, inverted")
  String invertedArguments(String one, String two);

  @DefaultMessage("Don''t tell me I can''t '{'quote things in braces'}'")
  String quotedText();

  @DefaultMessage("This '{0}' would be an argument if not quoted")
  String quotedArg();

  @DefaultMessage("Total is {0,number,currency}")
  String currencyFormat(double value);

  @DefaultMessage("Default number format is {0,number}")
  String defaultNumberFormat(double value);

  @DefaultMessage("It is {0,time,short} on {0,date,full}")
  String getTimeDate(Date value);

  @DefaultMessage("{0} widgets")
  @PluralText({"one", "A widget"})
  String pluralWidgetsOther(@PluralCount
  int count);

  @DefaultMessage("{1} {0}")
  @PluralText({"one", "A {0}"})
  String twoParamPlural(String name, @PluralCount
  int count);
  
  @DefaultMessage("Total is {0,number,currency}")
  String withNumberCurrency(Number value);
  
  @DefaultMessage("Distance is {0,number,##0.0##E0}")
  String withNumberExponent(Number value);
}
