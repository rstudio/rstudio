/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

/**
 * TODO: document me.
 */
public class CookieTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void test() {
    // Make the cookie expire in one minute, so that they don't hang around
    // past the end of this test.
    Date expires = new Date(new Date().getTime() + (60 * 1000));

    // Test setting a simple cookie.
    Cookies.setCookie("foo", "bar", expires);
    assertEquals("bar", Cookies.getCookie("foo"));

    // Make sure that parsing cookies with embedded '=' works correctly.
    Cookies.setCookie("foo1", "foo=bar", expires);
    assertEquals("foo=bar", Cookies.getCookie("foo1"));

    // Make sure that setting the second cookie doesn't clobber the first.
    assertEquals("bar", Cookies.getCookie("foo"));

    // Make sure embedded ';' works as well.
    Cookies.setCookie("foo2", "foo;bar", expires);

    // Differentiate null cookie from '' cookie.
    Cookies.setCookie("novalue", "", expires);
    assertEquals(Cookies.getCookie("novalue"), "");
    assertEquals(Cookies.getCookie("notpresent"), null);
  }
}
