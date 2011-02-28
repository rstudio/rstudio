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

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.TestAnnotatedMessages.Gender;
import com.google.gwt.i18n.client.TestAnnotatedMessages.Nested;
import com.google.gwt.i18n.client.constants.TimeZoneConstants;
import com.google.gwt.i18n.client.gen.Colors;
import com.google.gwt.i18n.client.gen.TestBadKeys;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Test the same things as I18NTest but with a different module which
 * uses different locales.
 */
public class I18N2Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18N2Test";
  }

  @SuppressWarnings("deprecation")
  public void testAnnotatedMessages() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("Test me", m.basicText());
    assertEquals("Once more, with meaning", m.withMeaning());
    assertEquals("One argument: one", m.oneArgument("one"));
    assertEquals("One argument, which is optional",
        m.optionalArgument("where am I?"));
    assertEquals("Two arguments, second and first, inverted",
        m.invertedArguments("first", "second"));
    assertEquals("Don't tell me I can't {quote things in braces}", m.quotedText());
    assertEquals("This {0} would be an argument if not quoted", m.quotedArg());
    assertEquals("Total is US$11,305.01", m.currencyFormat(11305.01));
    assertEquals("Default number format is 1,017.1", m.defaultNumberFormat(1017.1));
    assertEquals("It is 12:01 on Saturday, 2007 December 01",
        m.getTimeDate(new Date(107, 11, 1, 12, 1, 2)));
    assertEquals("13 widgets", m.pluralWidgetsOther(13));
//    assertEquals("A widget", m.pluralWidgetsOther(1));
  }

  public void testBadKeys() {
    TestBadKeys test = GWT.create(TestBadKeys.class);
    assertEquals("zh_spacer", test.zh_spacer());
    assertEquals("zh_spacer", test.getString("zh_spacer"));
    assertEquals("logger_org_hibernate_jdbc", test.logger_org_hibernate_jdbc());
    assertEquals("logger_org_hibernate_jdbc",
        test.getString("logger_org_hibernate_jdbc"));
    assertEquals("cell_2_5", test.cell_2_5());
    assertEquals("cell_2_5", test.getString("cell_2_5"));
    assertEquals("_level", test._level());
    assertEquals("_level", test.getString("_level"));
    assertEquals("__s", test.__s());
    assertEquals("__s", test.getString("__s"));
    assertEquals(
        "________________________________________________________________",
        test.________________________________________________________________());
    assertEquals(
        "________________________________________________________________",
        test.getString("________________________________________________________________"));
    assertEquals("_", test._());
    assertEquals("_", test.getString("_"));
    assertEquals("maven_jdiff_old_tag", test.maven_jdiff_old_tag());
    assertEquals("maven_jdiff_old_tag", test.getString("maven_jdiff_old_tag"));
    assertEquals("maven_checkstyle_properties",
        test.maven_checkstyle_properties());
    assertEquals("maven_checkstyle_properties",
        test.getString("maven_checkstyle_properties"));
    assertEquals("_1_2_3_4", test._1_2_3_4());
    assertEquals("_1_2_3_4", test.getString("_1_2_3_4"));
    assertEquals("entity_160", test.entity_160());
    assertEquals("entity_160", test.getString("entity_160"));
    assertEquals("a__b", test.a__b());
    assertEquals("a__b", test.getString("a__b"));
    assertEquals("AWT_f5", test.AWT_f5());
    assertEquals("AWT_f5", test.getString("AWT_f5"));
    assertEquals("Cursor_MoveDrop_32x32_File",
        test.Cursor_MoveDrop_32x32_File());
    assertEquals("Cursor_MoveDrop_32x32_File",
        test.getString("Cursor_MoveDrop_32x32_File"));
    assertEquals("_c_____", test._c_____());
    assertEquals("_c_____", test.getString("_c_____"));
    assertEquals("__s_dup", test.__s_dup());
    assertEquals("__s_dup", test.getString("__s_dup"));
    assertEquals("__dup", test.__dup());
    assertEquals("__dup", test.getString("__dup"));
    assertEquals("AWT_end", test.AWT_end());
    assertEquals("AWT_end", test.getString("AWT_end"));
    assertEquals("permissions_755", test.permissions_755());
    assertEquals("permissions_755", test.getString("permissions_755"));
    assertEquals("a_b_c", test.a_b_c());
    assertEquals("a_b_c", test.getString("a_b_c"));
    assertEquals("__s_dup_dup", test.__s_dup_dup());
    assertEquals("e in b_C_d", test.getString("__dup_dup"));
    assertEquals("e in b_C_d", test.__dup_dup());
    assertEquals("andStar", test.getString("__"));
    assertEquals("andStar", test.__());
  }

  public void testBinding() {
    TestBinding t = GWT.create(TestBinding.class);
    assertEquals("b_c_d", t.a());
    assertEquals("default", t.b());
  }

  public void testCheckColorsAndShapes() {
    ColorsAndShapes s = GWT.create(ColorsAndShapes.class);
    // Red comes from Colors_b_C_d
    assertEquals("red_b_C_d", s.red());
    // Blue comes from Colors_b_C
    assertEquals("blue_b_C", s.blue());
    // Yellow comes from Colors_b
    assertEquals("yellow_b", s.yellow());
    // RedSquare comes from ColorsAndShapes
    assertEquals("red square", s.redSquare());
    // Circle comes from Shapes
    assertEquals("a circle", s.circle());
  }

  public void testDynamicCurrency() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("The total is AU$1,001.02", m.totalAmount(1001.02, "AUD"));
    assertEquals("The total is US$1,001.02", m.totalAmount(1001.02, "USD"));

    assertEquals("The total is AU$1,001.02", m.totalAmountAsSafeHtml(1001.02, "AUD").asString());
    assertEquals("The total is US$1,001.02", m.totalAmountAsSafeHtml(1001.02, "USD").asString());
  }

  @SuppressWarnings("deprecation")
  public void testDynamicTimeZone() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    Date date = new Date(Date.UTC(2010 - 1900, 1, 2, 3, 4, 5));
    TimeZoneConstants timeZoneData = GWT.create(TimeZoneConstants.class);
    String str = timeZoneData.americaLosAngeles();
    TimeZoneInfo tzInfo = TimeZoneInfo.buildTimeZoneData(str);
    TimeZone tz = TimeZone.createTimeZone(tzInfo);
    assertEquals("in timezone: 2/1/2010 7:04:05 PM", m.inTimezone(date, tz));

    assertEquals("in timezone: 2/1/2010 7:04:05 PM", m.inTimezoneAsSafeHtml(date, tz).asString());
}

  public void testListWithArray() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("There are no values", m.valuesArray(new int[0]));
    assertEquals("The value is -1,001", m.valuesArray(new int[]{-1001}));
    assertEquals("The values are 1 and 2", m.valuesArray(new int[]{1, 2}));
    assertEquals("The values are 1, 3, 5, 7, and 9,009",
        m.valuesArray(new int[]{1, 3, 5, 7, 9009}));

    assertEquals(
        "There are no values", m.valuesArrayAsSafeHtml(new int[0]).asString());
    assertEquals("The value is -1,001",
        m.valuesArrayAsSafeHtml(new int[]{-1001}).asString());
    assertEquals("The values are 1 and 2",
        m.valuesArrayAsSafeHtml(new int[]{1, 2}).asString());
    assertEquals("The values are 1, 3, 5, 7, and 9,009",
        m.valuesArrayAsSafeHtml(new int[]{1, 3, 5, 7, 9009}).asString());
  }

  public void testListWithList() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals(
        "There are no values", m.valuesList(Arrays.<Integer> asList()));
    assertEquals("The value is -1,001", m.valuesList(Arrays.asList(-1001)));
    assertEquals("The values are 1 and 2", m.valuesList(Arrays.asList(1, 2)));
    assertEquals("The values are 1, 3, 5, 7, and 9,009",
        m.valuesList(Arrays.asList(1, 3, 5, 7, 9009)));

    assertEquals("There are no values",
        m.valuesListAsSafeHtml(Arrays.<Integer> asList()).asString());
    assertEquals("The value is -1,001",
        m.valuesListAsSafeHtml(Arrays.asList(-1001)).asString());
    assertEquals("The values are 1 and 2",
        m.valuesListAsSafeHtml(Arrays.asList(1, 2)).asString());
    assertEquals("The values are 1, 3, 5, 7, and 9,009",
        m.valuesListAsSafeHtml(Arrays.asList(1, 3, 5, 7, 9009)).asString());
  }

  public void testListWtihVarArgs() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("There are no values", m.valuesVarArgs());
    assertEquals("The value is -1,001", m.valuesVarArgs(-1001));
    assertEquals("The values are 1 and 2", m.valuesVarArgs(1, 2));
    assertEquals("The values are 1, 3, 5, 7, and 9,009",
        m.valuesVarArgs(1, 3, 5, 7, 9009));

    assertEquals("There are no names", m.valuesVarArgsAsSafeHtml().asString());
    assertEquals(
        "The name is John", m.valuesVarArgsAsSafeHtml(sh("John")).asString());
    assertEquals("The names are John and Jeff",
        m.valuesVarArgsAsSafeHtml(sh("John"), sh("Jeff")).asString());
    assertEquals("The names are John, Jeff, and Bob",
        m.valuesVarArgsAsSafeHtml(sh("John"), sh("Jeff"), sh("Bob")).asString(
        ));
  }

  /**
   * Verifies correct output for multiple, nested selectors, using an enum
   * for gender selection (and SafeHtml output).
   */
  public void testMultiSelectEnum() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    List<String> names = new ArrayList<String>();
    
    // empty list of names
    assertEquals("Nobody liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("Nobody liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("Nobody liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("Nobody liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("Nobody liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, null).asString());
    assertEquals("Nobody liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, Gender.UNKNOWN).asString());

    // one name
    names.add("John");
    assertEquals("John liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("John liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("John liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("John liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("John liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("John liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, null).asString());

    // two names
    names.add("Bob");
    assertEquals("John and Bob liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("John and Bob liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("John and Bob liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("John and Bob liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("John and Bob liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, null).asString());
    assertEquals("John and Bob liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, Gender.UNKNOWN).asString());

    // three names
    names.add("Alice");
    assertEquals("John, Bob, and one other liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("John, Bob, and one other liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("John, Bob, and one other liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("John, Bob, and one other liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("John, Bob, and one other liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("John, Bob, and one other liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, null).asString());

    // four names
    names.add("Carol");
    assertEquals("John, Bob, and 2 others liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("John, Bob, and 2 others liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("John, Bob, and 2 others liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("John, Bob, and 2 others liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("John, Bob, and 2 others liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("John, Bob, and 2 others liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, null).asString());
  }

  /**
   * Verifies correct output for multiple, nested selectors, using a string
   * for gender selection.
   */
  public void testMultiSelectString() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    List<String> names = new ArrayList<String>();
    
    // empty list of names
    assertEquals("Nobody liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("Nobody liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("Nobody liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("Nobody liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("Nobody liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("Nobody liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // one name
    names.add("John");
    assertEquals("John liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("John liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("John liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("John liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("John liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("John liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // two names
    names.add("Bob");
    assertEquals("John and Bob liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("John and Bob liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("John and Bob liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("John and Bob liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("John and Bob liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("John and Bob liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // three names
    names.add("Alice");
    assertEquals("John, Bob, and one other liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("John, Bob, and one other liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("John, Bob, and one other liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("John, Bob, and one other liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("John, Bob, and one other liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("John, Bob, and one other liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // four names
    names.add("Carol");
    assertEquals("John, Bob, and 2 others liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("John, Bob, and 2 others liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("John, Bob, and 2 others liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("John, Bob, and 2 others liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("John, Bob, and 2 others liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("John, Bob, and 2 others liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));
  }

  /**
   * Verify that nested annotations are looked up with both A$B names
   * and A_B names.  Note that $ takes precedence and only one file for a
   * given level in the inheritance tree will be used, so A$B_locale will
   * be used and A_B_locale ignored.
   */
  public void testNestedAnnotations() {
    Nested m = GWT.create(Nested.class);
    assertEquals("nested dollar b_C", m.nestedDollar());
    assertEquals("nested underscore b", m.nestedUnderscore());
  }

  @SuppressWarnings("deprecation")
  public void testPredefDateFormat() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    Date date = new Date(Date.UTC(2010 - 1900, 1, 2, 3, 4, 5));
    assertEquals("Short: 2010-02-01", m.predef(date));
  }

  public void testSpecialPlurals() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("No widgets", m.specialPlurals(0));
    assertEquals("A widget", m.specialPlurals(1));
    assertEquals("2 widgets", m.specialPlurals(2));
    assertEquals("No one has reviewed this movie", m.reviewers(0, null, null));
    assertEquals(
        "John Doe has reviewed this movie", m.reviewers(1, "John Doe", null));
    assertEquals("John Doe and Betty Smith have reviewed this movie",
        m.reviewers(2, "John Doe", "Betty Smith"));
    assertEquals(
        "John Doe, Betty Smith, and one other have reviewed this movie",
        m.reviewers(3, "John Doe", "Betty Smith"));
    assertEquals(
        "John Doe, Betty Smith, and 3 others have reviewed this movie",
        m.reviewers(5, "John Doe", "Betty Smith"));

    assertEquals("No widgets", m.specialPluralsAsSafeHtml(0).asString());
    assertEquals("A widget", m.specialPluralsAsSafeHtml(1).asString());
    assertEquals("2 widgets", m.specialPluralsAsSafeHtml(2).asString());
    assertEquals("No one has reviewed this movie",
        m.reviewersAsSafeHtml(0, null, null).asString());
    assertEquals("John Doe has reviewed this movie", m.reviewersAsSafeHtml(1,
        "John Doe", null).asString());
    assertEquals("John Doe and Betty Smith have reviewed this movie",
        m.reviewersAsSafeHtml(2, "John Doe", sh("Betty Smith")).asString());
    assertEquals(
        "John Doe, Betty Smith, and one other have reviewed this movie",
        m.reviewersAsSafeHtml(3, "John Doe", sh("Betty Smith")).asString());
    assertEquals(
        "John Doe, Betty Smith, and 3 others have reviewed this movie",
        m.reviewersAsSafeHtml(5, "John Doe", sh("Betty Smith")).asString());
}

  public void testStaticArg() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("This is <b>bold</b>", m.staticArgs());
    assertEquals("This is <b>bold</b>", m.staticArgsSafeHtml().asString());
  }

  public void testStaticCurrency() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("The total is AU$1,001.02", m.australianDollars(1001.02));

    assertEquals("The total is AU$1,001.02", m.australianDollarsAsSafeHtml(
        1001.02).asString());
  }

  @SuppressWarnings("deprecation")
  public void testStaticTimeZone() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    Date date = new Date(Date.UTC(2010 - 1900, 1, 2, 3, 4, 5));
    assertEquals("in GMT: 2/2/2010 3:04:05 AM", m.gmt(date));

    assertEquals("in GMT: 2/2/2010 3:04:05 AM", m.gmtAsSafeHtml(date).asString(
        ));
  }

  public void testWalkUpColorTree() {
    Colors colors = GWT.create(Colors.class);
    assertEquals("red_b_C_d", colors.red());
    assertEquals("blue_b_C", colors.blue());
    assertEquals("yellow_b", colors.yellow());
  }

  /**
   * Wrapper to easily convert a String literal to a SafeHtml instance.
   * 
   * @param string
   * @return a SafeHtml wrapper around the supplied string
   */
  private SafeHtml sh(String string) {
    SafeHtmlBuilder buf = new SafeHtmlBuilder();
    buf.appendHtmlConstant(string);
    return buf.toSafeHtml();
  }
}
