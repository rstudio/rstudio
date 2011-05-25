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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.TestAnnotatedMessages.Gender;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the same things as I18NTest but with a different module which
 * uses different locales.
 */
public class I18N_en_US_Test extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_en";
  }

  public void testSelect() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("#: 14", m.selectBoolean(14, true).asString());
    assertEquals("Message Count: 14", m.selectBoolean(14, false).asString());
    assertEquals("Created new order", m.selectInt(0));
    assertEquals("Updated order 42", m.selectInt(42));
    assertEquals("Created new order", m.selectLong(0).asString());
    assertEquals("Updated order 42", m.selectLong(42).asString());
  }

  /**
   * Verifies correct output for multiple, nested selectors, using an enum
   * for gender selection (and SafeHtml output).
   */
  public void testMultiSelectEnum() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    List<String> names = new ArrayList<String>();
    
    // empty list of names
    assertEquals("test: Nobody liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("test: Nobody liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("test: Nobody liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("test: Nobody liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("test: Nobody liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, null).asString());
    assertEquals("test: Nobody liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, Gender.UNKNOWN).asString());

    // one name
    names.add("John");
    assertEquals("test: John liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("test: John liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("test: John liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("test: John liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("test: John liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("test: John liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, null).asString());

    // two names
    names.add("Bob");
    assertEquals("test: John and Bob liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("test: John and Bob liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("test: John and Bob liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("test: John and Bob liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("test: John and Bob liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, null).asString());
    assertEquals("test: John and Bob liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, Gender.UNKNOWN).asString());

    // three names
    names.add("Alice");
    assertEquals("test: John, Bob, and one other liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("test: John, Bob, and one other liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("test: John, Bob, and one other liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("test: John, Bob, and one other liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("test: John, Bob, and one other liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("test: John, Bob, and one other liked their 4 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, null).asString());

    // four names
    names.add("Carol");
    assertEquals("test: John, Bob, and 2 others liked his message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.MALE).asString());
    assertEquals("test: John, Bob, and 2 others liked his 2 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, Gender.MALE).asString());
    assertEquals("test: John, Bob, and 2 others liked her message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.FEMALE).asString());
    assertEquals("test: John, Bob, and 2 others liked her 3 messages",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, Gender.FEMALE).asString());
    assertEquals("test: John, Bob, and 2 others liked their message",
        m.multiSelectEnum(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, Gender.UNKNOWN).asString());
    assertEquals("test: John, Bob, and 2 others liked their 4 messages",
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
    assertEquals("test: Nobody liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("test: Nobody liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("test: Nobody liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("test: Nobody liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("test: Nobody liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("test: Nobody liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // one name
    names.add("John");
    assertEquals("test: John liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("test: John liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("test: John liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("test: John liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("test: John liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("test: John liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // two names
    names.add("Bob");
    assertEquals("test: John and Bob liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("test: John and Bob liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("test: John and Bob liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("test: John and Bob liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("test: John and Bob liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("test: John and Bob liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // three names
    names.add("Alice");
    assertEquals("test: John, Bob, and one other liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("test: John, Bob, and one other liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("test: John, Bob, and one other liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("test: John, Bob, and one other liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("test: John, Bob, and one other liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("test: John, Bob, and one other liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));

    // four names
    names.add("Carol");
    assertEquals("test: John, Bob, and 2 others liked his message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "MALE"));
    assertEquals("test: John, Bob, and 2 others liked his 2 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 2, "MALE"));
    assertEquals("test: John, Bob, and 2 others liked her message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "FEMALE"));
    assertEquals("test: John, Bob, and 2 others liked her 3 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 3, "FEMALE"));
    assertEquals("test: John, Bob, and 2 others liked their message",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 1, "unknown"));
    assertEquals("test: John, Bob, and 2 others liked their 4 messages",
        m.multiSelectString(names, names.size() > 0 ? names.get(0) : null,
        names.size() > 1 ? names.get(1) : null, 4, "unknown"));
  }

  public void testSpecialPlurals() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("No widgets", m.specialPlurals(0));
    assertEquals("A widget", m.specialPlurals(1));
    assertEquals("2 widgets", m.specialPlurals(2));
    assertEquals("No one has reviewed this movie", m.reviewers(0, null, null));
    assertEquals(
        "test: John Doe has reviewed this movie", m.reviewers(1, "test: John Doe", null));
    assertEquals("test: John Doe and Betty Smith have reviewed this movie",
        m.reviewers(2, "test: John Doe", "Betty Smith"));
    assertEquals(
        "test: John Doe, Betty Smith, and one other have reviewed this movie",
        m.reviewers(3, "test: John Doe", "Betty Smith"));
    assertEquals(
        "test: John Doe, Betty Smith, and 3 others have reviewed this movie",
        m.reviewers(5, "test: John Doe", "Betty Smith"));

    assertEquals("No widgets", m.specialPluralsAsSafeHtml(0).asString());
    assertEquals("A widget", m.specialPluralsAsSafeHtml(1).asString());
    assertEquals("2 widgets", m.specialPluralsAsSafeHtml(2).asString());
    assertEquals("No one has reviewed this movie",
        m.reviewersAsSafeHtml(0, null, null).asString());
    assertEquals("test: John Doe has reviewed this movie", m.reviewersAsSafeHtml(1,
        "test: John Doe", null).asString());
    assertEquals("test: John Doe and Betty Smith have reviewed this movie",
        m.reviewersAsSafeHtml(2, "test: John Doe", sh("Betty Smith")).asString());
    assertEquals(
        "test: John Doe, Betty Smith, and one other have reviewed this movie",
        m.reviewersAsSafeHtml(3, "test: John Doe", sh("Betty Smith")).asString());
    assertEquals(
        "test: John Doe, Betty Smith, and 3 others have reviewed this movie",
        m.reviewersAsSafeHtml(5, "test: John Doe", sh("Betty Smith")).asString());
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
