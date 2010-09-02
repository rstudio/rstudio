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
package com.google.gwt.safehtml.shared;

import junit.framework.TestCase;

/**
 * Unit tests for SanitizedHtml.
 */
public class SimpleHtmlSanitizerTest extends TestCase {

  public void testSimple() {
    // simple case
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("foobar");
    assertEquals("foobar", html.asString());
  }

  public void testDontChangeWhiteSpace() {
    // shouldn't change whitespace or newlines
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml(
        "things are breezy\nand jolly\tgood");
    assertEquals("things are breezy\nand jolly\tgood", html.asString());
  }

  public void testEscapeHtmlMetaCharacters() {
    // need to escape HTML metacharacters appearing on their own
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("foo < bar & that's good");
    assertEquals("foo &lt; bar &amp; that&#39;s good", html.asString());
  }

  public void testDontDoubleEscape() {
    // but don't double-escape HTML entities
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml(
        "foo < bar &amp; that's good");
    assertEquals("foo &lt; bar &amp; that&#39;s good", html.asString());
  }

  public void testEscapeLoneMetacharacters() {
    // need to escape HTML metacharacters appearing on their own
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml(
        "\"foo < bar & that's good\"");
    assertEquals(
        "&quot;foo &lt; bar &amp; that&#39;s good&quot;", html.asString());
  }

  public void testDontEscapeValidTags() {
    // leave simple tags alone
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("foo <em>bar</em>");
    assertEquals("foo <em>bar</em>", html.asString());
  }

  public void testTagAtBeginning() {
    // correctly deal with a tag at the beginnign
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("<em>bar</em>");
    assertEquals("<em>bar</em>", html.asString());
  }

  public void testNonTagAtBeginning() {
    // correctly deal with a non-tag at the beginnig
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("<yow <em>bar</em>");
    assertEquals("&lt;yow <em>bar</em>", html.asString());
  }

  public void testNonTagAtEnd() {
    // correctly deal with a non-tag at the end
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("<em>bar</em> foo <");
    assertEquals("<em>bar</em> foo &lt;", html.asString());
  }

  public void testNullTag() {
    // correctly deal with bogus empty tag
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("<>bar</em> foo<>");
    assertEquals("&lt;&gt;bar</em> foo&lt;&gt;", html.asString());
  }

  public void testNullEndTag() {
    // correctly deal with bogus empty end tag
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("</>bar</em> foo</>");
    assertEquals("&lt;/&gt;bar</em> foo&lt;/&gt;", html.asString());
  }

  public void testSimpleTagsAndHtmlMetaChars() {
    // mix of simple tags and HTML metacharacters appearing on their own
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml(
        "foo < bar & that's <b>good</b>");
    assertEquals("foo &lt; bar &amp; that&#39;s <b>good</b>", html.asString());
  }

  public void testEvilTags() {
    // escape tags we don't know
    SafeHtml html = SimpleHtmlSanitizer.sanitizeHtml("<script>evil()</script>");
    assertEquals("&lt;script&gt;evil()&lt;/script&gt;", html.asString());
  }
}
