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

import java.util.Collection;
import java.util.Date;

/**
 * Test Case for {@link Cookies}.
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
  
  /*
   * Test that the cookie will expire correctly after a set amount of time,
   * but does not expire before that time. 
   */
  public void testExpires() {
    // Test that the cookie expires in 5 seconds
    Date expiresEarly = new Date(new Date().getTime() + (5 * 1000));
    Date expiresLate  = new Date(new Date().getTime() + (60 * 1000));
    Cookies.setCookie("shouldExpireEarly", "early", expiresEarly);
    Cookies.setCookie("shouldExpireLate", "late", expiresLate);
    Cookies.setCookie("shouldNotExpire", "forever", null);

    // Wait until the cookie expires before checking it
    Timer timer = new Timer() {
      public void run() {
        // Verify that the early expiring cookie does NOT exist
        assertNull(Cookies.getCookie("shouldExpireEarly"));

        // Verify that the late expiring cookie does exist
        assertEquals(Cookies.getCookie("shouldExpireLate"), "late");

        // Verify the session cookie doesn't expire
        assertEquals(Cookies.getCookie("shouldNotExpire"), "forever");
        Cookies.removeCookie("shouldNotExpire");
        assertNull(Cookies.getCookie("shouldNotExpire"));
        
        // Finish the test
        finishTest();
      }
    };
    timer.schedule(5010);
    delayTestFinish(6 * 1000);
  }
  
  /**
   * Test that removing cookies works correctly.
   */
  public void testRemoveCookie() {
    // First clear all cookies
    clearCookies();
    
    // Set a few cookies
    Cookies.setCookie("test1", "value1");
    Cookies.setCookie("test2", "value2");
    Cookies.setCookie("test3", "value3");
    Collection<String> cookies = Cookies.getCookieNames();
    assertEquals(3, cookies.size());
    
    // Remove a cookie
    Cookies.removeCookie("test2");
    assertEquals("value1", Cookies.getCookie("test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove another cookie
    Cookies.removeCookie("test1");
    assertEquals(null, Cookies.getCookie("test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove last cookie
    Cookies.removeCookie("test3");
    assertEquals(null, Cookies.getCookie("test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals(null, Cookies.getCookie("test3"));
    cookies = Cookies.getCookieNames();
    assertEquals(0, cookies.size());
  }
  
  /**
   * Clear out all existing cookies.
   */
  private void clearCookies() { 
    Collection<String> cookies = Cookies.getCookieNames();
    for (String cookie : cookies) {
      Cookies.removeCookie(cookie);
    }
  }
}
