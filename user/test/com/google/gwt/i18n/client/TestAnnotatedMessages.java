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
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.Date;
import java.util.List;

/**
 * Test of Messages generation using annotations.
 */
@DefaultLocale("en")
// @GenerateKeys("com.google.gwt.i18n.server.keygen.MD5KeyGenerator")
@GenerateKeys("com.google.gwt.i18n.server.keygen.MethodNameKeyGenerator") // default
@Generate(format = "com.google.gwt.i18n.server.PropertyCatalogFactory")
public interface TestAnnotatedMessages extends Messages {

  /**
   * Represents the gender of a person in a message.
   */
  public enum Gender {
    MALE,
    FEMALE,
    UNKNOWN
  }

  /**
   * Test of property file lookup on nested classes.
   *
   * nestedDollar() is redefined in a property file with a $ in it.
   * nestedUnderscore() is redefined in a property file with a _ in it.
   */
  public interface Nested extends Messages {

    @DefaultMessage("nested dollar")
    String nestedDollar();

    @DefaultMessage("nested dollar")
    @Key("nestedDollar")
    SafeHtml nestedDollarAsSafeHtml();

    @DefaultMessage("nested underscore")
    String nestedUnderscore();

    @DefaultMessage("nested underscore")
    @Key("nestedUnderscore")
    SafeHtml nestedUnderscoreAsSafeHtml();
  }

  @DefaultMessage("Test me")
  String basicText();

  @DefaultMessage("Test me")
  @Key("basicText")
  SafeHtml basicTextAsSafeHtml();

  @DefaultMessage("The total is {0,number:curcode=AUD,currency}")
  String australianDollars(double amount);

  @DefaultMessage("The total is {0,number:curcode=AUD,currency}")
  @Key("australianDollars")
  SafeHtml australianDollarsAsSafeHtml(double amount);

  @DefaultMessage("The total is {0,number:curcode=$curCode,currency}")
  String totalAmount(double amount, @Optional String curCode);

  @DefaultMessage("The total is {0,number:curcode=$curCode,currency}")
  @Key("totalAmount")
  SafeHtml totalAmountAsSafeHtml(double amount, @Optional String curCode);

  @DefaultMessage("in GMT: {0,localdatetime:tz=0,yMd hms}")
  String gmt(Date date);

  @DefaultMessage("in timezone: {0,localdatetime:tz=$tz,yMd hms}")
  String inTimezone(Date date, @Optional TimeZone tz);

  @DefaultMessage("in GMT: {0,localdatetime:tz=0,yMd hms}")
  SafeHtml gmtAsSafeHtml(Date date);

  @DefaultMessage("in timezone: {0,localdatetime:tz=$tz,yMd hms}")
  SafeHtml inTimezoneAsSafeHtml(Date date, @Optional TimeZone tz);

  @DefaultMessage("Short: {0,localdatetime,predef:DATE_SHORT}")
  String predef(Date date);

  @DefaultMessage("Once more, with meaning")
  @Meaning("Mangled quote")
  String withMeaning();

  @DefaultMessage("Once more, with meaning")
  @Meaning("Mangled quote")
  @Key("withMeaning")
  SafeHtml withMeaningAsSafeHtml();

  @DefaultMessage("One argument: {0}")
  String oneArgument(String value);

  @DefaultMessage("One argument: {0}")
  @Key("oneArgument")
  SafeHtml oneArgumentAsSafeHtml(String value);

  @DefaultMessage("One argument: {0}")
  @Key("oneArgument")
  SafeHtml oneArgumentAsSafeHtml(SafeHtml value);

  @DefaultMessage("One argument, which is optional")
  String optionalArgument(@Optional String value);

  @DefaultMessage("One argument, which is optional")
  @Key("optionalArgument")
  SafeHtml optionalArgumentAsSafeHtml(@Optional String value);

  @DefaultMessage("Two arguments, {1} and {0}, inverted")
  String invertedArguments(String one, String two);

  @DefaultMessage("Two arguments, {1} and {0}, inverted")
  @Key("invertedArguments")
  SafeHtml invertedArgumentsAsSafeHtml(String one, String two);

  @DefaultMessage("Two arguments, {1} and {0}, inverted")
  @Key("invertedArguments")
  SafeHtml invertedArgumentsAsSafeHtml(SafeHtml one, String two);

  @DefaultMessage("Don''t tell me I can''t '{'quote things in braces'}'")
  String quotedText();

  @DefaultMessage("Don''t tell me I can''t '{'quote things in braces'}'")
  @Key("quotedText")
  SafeHtml quotedTextAsSafeHtml();

  @DefaultMessage("This '{0}' would be an argument if not quoted")
  String quotedArg();

  @DefaultMessage("This '{0}' would be an argument if not quoted")
  @Key("quotedArg")
  SafeHtml quotedArgAsSafeHtml();

  @DefaultMessage("Total is {0,number,currency}")
  String currencyFormat(double value);

  @DefaultMessage("Total is {0,number,currency}")
  @Key("currencyFormat")
  SafeHtml currencyFormatAsSafeHtml(double value);

  @DefaultMessage("Default number format is {0,number}")
  String defaultNumberFormat(double value);

  @DefaultMessage("Default number format is {0,number}")
  @Key("defaultNumberFormat")
  SafeHtml defaultNumberFormatAsSafeHtml(double value);

  @DefaultMessage("{1} wants to sell their car")
  @AlternateMessage({
    "FEMALE", "{1} wants to sell her car",
    "MALE", "{1} wants to sell his car"
  })
  String gender(@Select Gender gender, String name);

  @DefaultMessage("It is {0,time,short} on {0,date,full}")
  String getTimeDate(Date value);

  @DefaultMessage("It is {0,time,short} on {0,date,full}")
  @Key("getTimeDate")
  SafeHtml getTimeDateAsSafeHtml(Date value);

  @DefaultMessage("{1}, {2}, and {0} others liked their {3} messages")
  @AlternateMessage({
    "=0|other|other", "Nobody liked their {3} messages",
    "=0|other|FEMALE", "Nobody liked her {3} messages",
    "=0|other|MALE", "Nobody liked his {3} messages",
    "=0|one|other", "Nobody liked their message",
    "=0|one|FEMALE", "Nobody liked her message",
    "=0|one|MALE", "Nobody liked his message",
    "=1|other|other", "{1} liked their {3} messages",
    "=1|other|FEMALE", "{1} liked her {3} messages",
    "=1|other|MALE", "{1} liked his {3} messages",
    "=1|one|other", "{1} liked their message",
    "=1|one|FEMALE", "{1} liked her message",
    "=1|one|MALE", "{1} liked his message",
    "=2|other|other", "{1} and {2} liked their {3} messages",
    "=2|other|FEMALE", "{1} and {2} liked her {3} messages",
    "=2|other|MALE", "{1} and {2} liked his {3} messages",
    "=2|one|other", "{1} and {2} liked their message",
    "=2|one|FEMALE", "{1} and {2} liked her message",
    "=2|one|MALE", "{1} and {2} liked his message",
    "one|other|other", "{1}, {2}, and one other liked their {3} messages",
    "one|other|FEMALE", "{1}, {2}, and one other liked her {3} messages",
    "one|other|MALE", "{1}, {2}, and one other liked his {3} messages",
    "one|one|other", "{1}, {2}, and one other liked their message",
    "one|one|FEMALE", "{1}, {2}, and one other liked her message",
    "one|one|MALE", "{1}, {2}, and one other liked his message",
    "other|one|other", "{1}, {2}, and {0} others liked their message",
    "other|one|MALE", "{1}, {2}, and {0} others liked his message",
    "other|one|FEMALE", "{1}, {2}, and {0} others liked her message",
    "other|other|MALE", "{1}, {2}, and {0} others liked his {3} messages",
    "other|other|FEMALE", "{1}, {2}, and {0} others liked her {3} messages"
  })
  String multiSelectString(@PluralCount @Offset(2) List<String> names,
      String name1, String name2, @PluralCount int msgCount,
      @Select String gender);

  @DefaultMessage("{1}, {2}, and {0} others liked their {3} messages")
  @AlternateMessage({
    "=0|other|other", "Nobody liked their {3} messages",
    "=0|other|FEMALE", "Nobody liked her {3} messages",
    "=0|other|MALE", "Nobody liked his {3} messages",
    "=0|one|other", "Nobody liked their message",
    "=0|one|FEMALE", "Nobody liked her message",
    "=0|one|MALE", "Nobody liked his message",
    "=1|other|other", "{1} liked their {3} messages",
    "=1|other|FEMALE", "{1} liked her {3} messages",
    "=1|other|MALE", "{1} liked his {3} messages",
    "=1|one|other", "{1} liked their message",
    "=1|one|FEMALE", "{1} liked her message",
    "=1|one|MALE", "{1} liked his message",
    "=2|other|other", "{1} and {2} liked their {3} messages",
    "=2|other|FEMALE", "{1} and {2} liked her {3} messages",
    "=2|other|MALE", "{1} and {2} liked his {3} messages",
    "=2|one|other", "{1} and {2} liked their message",
    "=2|one|FEMALE", "{1} and {2} liked her message",
    "=2|one|MALE", "{1} and {2} liked his message",
    "one|other|other", "{1}, {2}, and one other liked their {3} messages",
    "one|other|FEMALE", "{1}, {2}, and one other liked her {3} messages",
    "one|other|MALE", "{1}, {2}, and one other liked his {#} messages",
    "one|one|other", "{1}, {2}, and one other liked their message",
    "one|one|FEMALE", "{1}, {2}, and one other liked her message",
    "one|one|MALE", "{1}, {2}, and one other liked his message",
    "other|one|other", "{1}, {2}, and {0} others liked their message",
    "other|one|MALE", "{1}, {2}, and {0} others liked his message",
    "other|one|FEMALE", "{1}, {2}, and {0} others liked her message",
    "other|other|MALE", "{1}, {2}, and {0} others liked his {3} messages",
    "other|other|FEMALE", "{1}, {2}, and {0} others liked her {3} messages"
  })
  SafeHtml multiSelectEnum(@PluralCount @Offset(2) List<String> names,
      String name1, String name2, @PluralCount int msgCount,
      @Select Gender gender);

  @SuppressWarnings("deprecation")
  @DefaultMessage("{0} widgets")
  @PluralText({"one", "A widget"})
  String pluralWidgetsOther(@PluralCount int count);

  @DefaultMessage("{0} widgets")
  @AlternateMessage({"one", "A widget"})
  @Key("pluralWidgetsOther")
  SafeHtml pluralWidgetsOtherAsSafeHtml(@PluralCount int count);

  @DefaultMessage("{1} {0}")
  @AlternateMessage({"one", "A {0}"})
  String twoParamPlural(String name, @PluralCount int count);

  @DefaultMessage("#: {0,number}")
  @AlternateMessage({"false", "Message Count: {0,number}"})
  SafeHtml selectBoolean(int count, @Select boolean shortMsg);

  @DefaultMessage("Updated order {0}")
  @AlternateMessage({"0", "Created new order"})
  String selectInt(@Select int orderId);

  @DefaultMessage("Updated order {0}")
  @AlternateMessage({"0", "Created new order"})
  SafeHtml selectLong(@Select long orderId);

  @DefaultMessage("{0} widgets")
  @AlternateMessage({"=0", "No widgets",
      "=1", "A widget",
      "one", "{0} widget"
  })
  String specialPlurals(@Optional @PluralCount int count);

  @DefaultMessage("{0} widgets")
  @AlternateMessage({"=0", "No widgets",
      "=1", "A widget",
      "one", "{0} widget"
  })
  @Key("specialPlurals")
  SafeHtml specialPluralsAsSafeHtml(@Optional @PluralCount int count);

  @DefaultMessage("This is {startBold,<b>}bold{endBold,</b>}")
  String staticArgs();

  @DefaultMessage("This is {startBold,<b>}bold{endBold,</b>}")
  @Key("staticArgs")
  SafeHtml staticArgsSafeHtml();

  @DefaultMessage("{1} {0}")
  @AlternateMessage({"one", "A {0}"})
  @Key("twoParamPlural")
  SafeHtml twoParamPluralAsSafeHtml(String name, @PluralCount int count);

  @DefaultMessage("{1} {0}")
  @AlternateMessage({"one", "A {0}"})
  @Key("twoParamPlural")
  SafeHtml twoParamPluralAsSafeHtml(SafeHtml name, @PluralCount int count);
  
  @DefaultMessage("Total is {0,number,currency}")
  String withNumberCurrency(Number value);
  
  @DefaultMessage("Distance is {0,number,##0.0##E0}")
  String withNumberExponent(Number value);

  @DefaultMessage("{1}, {2}, and {0,number} others have reviewed this movie")
  @AlternateMessage({
    "=0", "No one has reviewed this movie",
    "=1", "{1} has reviewed this movie",
    "=2", "{1} and {2} have reviewed this movie",
    "one",  "{1}, {2}, and one other have reviewed this movie"})
  String reviewers(@PluralCount @Offset(2) int size,
       String name1, String name2);

  @DefaultMessage("{1}, {2}, and {0,number} others have reviewed this movie")
  @AlternateMessage({
    "=0", "No one has reviewed this movie",
    "=1", "{1} has reviewed this movie",
    "=2", "{1} and {2} have reviewed this movie",
    "one",  "{1}, {2}, and one other have reviewed this movie"})
  @Key("reviewers")
  SafeHtml reviewersAsSafeHtml(@PluralCount @Offset(2) int size,
       String name1, SafeHtml name2);

  @DefaultMessage("The values are {0,list,number}")
  @AlternateMessage({
    "=0", "There are no values",
    "=1", "The value is {0,list,number}"})
  String valuesArray(@PluralCount int[] values);

  @DefaultMessage("The values are {0,list,number}")
  @AlternateMessage({
    "=0", "There are no values",
    "=1", "The value is {0,list,number}"})
  @Key("valuesArray")
  String valuesList(@PluralCount List<Integer> list);

  @DefaultMessage("The values are {0,list,number}")
  @AlternateMessage({
    "=0", "There are no values",
    "=1", "The value is {0,list,number}"})
  @Key("valuesArray")
  String valuesVarArgs(@PluralCount int... values);

  @DefaultMessage("The values are {0,list,number}")
  @AlternateMessage({
    "=0", "There are no values",
    "=1", "The value is {0,list,number}"})
  @Key("valuesArray")
  SafeHtml valuesArrayAsSafeHtml(@PluralCount int[] values);

  @DefaultMessage("The values are {0,list,number}")
  @AlternateMessage({
    "=0", "There are no values",
    "=1", "The value is {0,list,number}"})
  @Key("valuesArray")
  SafeHtml valuesListAsSafeHtml(@PluralCount List<Integer> list);

  @DefaultMessage("The names are {0,list}")
  @AlternateMessage({
    "=0", "There are no names",
    "=1", "The name is {0,list}"})
  SafeHtml valuesVarArgsAsSafeHtml(@PluralCount SafeHtml... values);
}
