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

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Collection;
import java.util.Date;

/**
 * Test Case for {@link Cookies}.
 */
public class CookieTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    // Sets URI Encode to default so we don't depend on execution order of test functions
    // between JDK versions.
    Cookies.setUriEncode(true);
  }

  public void test() {
    // Make the cookie expire in one minute, so that they don't hang around
    // past the end of this test.
    Date expires = new Date(getClientTime() + (60 * 1000));

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
    // Generate a random ID for the cookies. Since cookies are shared across
    // browser instances, its possible for multiple instances of this test to
    // run concurrently (eg. hosted and Production Mode tests). If that happens,
    // the cookies will be cleared while we wait for the timer to fire.
    int uniqueId = Random.nextInt(9000000) + 1000000;
    final String earlyCookie = "shouldExpireEarly" + uniqueId;
    final String lateCookie = "shouldExpireLate" + uniqueId;
    final String sessionCookie = "shouldNotExpire" + uniqueId;

    // Test that the cookie expires in 5 seconds
    Date expiresEarly = new Date(getClientTime() + (5 * 1000));
    Date expiresLate  = new Date(getClientTime() + (60 * 1000));
    Cookies.setCookie(earlyCookie, "early", expiresEarly);
    Cookies.setCookie(lateCookie, "late", expiresLate);
    Cookies.setCookie(sessionCookie, "forever", null);

    delayTestFinish(7000);
    // Wait until the cookie expires before checking it
    Timer timer = new Timer() {
      @Override
      public void run() {
        // Verify that the early expiring cookie does NOT exist
        assertNull(Cookies.getCookie(earlyCookie));

        // Verify that the late expiring cookie does exist
        assertEquals(Cookies.getCookie(lateCookie), "late");

        // Verify the session cookie doesn't expire
        assertEquals(Cookies.getCookie(sessionCookie), "forever");
        Cookies.removeCookie(sessionCookie);
        assertNull(Cookies.getCookie(sessionCookie));

        // Finish the test
        finishTest();
      }
    };
    timer.schedule(6000);
  }

  public void testIsCookieEnabled() {
    assertTrue(Cookies.isCookieEnabled());
  }

  /**
   * Test that removing cookies works correctly.
   */
  public void testRemoveCookie() {
    // First clear all cookies
    clearCookies();

    // Set a few cookies
    int curCount = Cookies.getCookieNames().size();
    Cookies.setCookie("test1", "value1");
    Cookies.setCookie("test2", "value2");
    Cookies.setCookie("test3", "value3");
    Collection<String> cookies = Cookies.getCookieNames();
    assertEquals(curCount + 3, cookies.size());

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
    assertEquals(curCount, cookies.size());

    // Add/remove URI encoded cookies
    Cookies.setCookie("test1-test1", "value1 value1");
    Cookies.removeCookie("test1-test1");
    cookies = Cookies.getCookieNames();
    assertEquals(curCount, cookies.size());

    // Add/remove cookies that are not URI encoded
    Cookies.setUriEncode(false);
    Cookies.setCookie("test1+test1", "value1+value1");
    Cookies.removeCookie("test1+test1");
    cookies = Cookies.getCookieNames();
    assertEquals(curCount, cookies.size());
    
    // Make sure cookie names are URI encoded
    Cookies.setUriEncode(true);
    Cookies.setCookie("test1.,/?:@&=+$#", "value1");
    assertEquals(curCount + 1, Cookies.getCookieNames().size());
    Cookies.setUriEncode(false);
    Cookies.removeCookie("test1.,/?:@&=+$#");
    assertEquals(curCount + 1, Cookies.getCookieNames().size());
    Cookies.setUriEncode(true);
    Cookies.removeCookie("test1.,/?:@&=+$#");
    assertEquals(curCount, Cookies.getCookieNames().size());

    // Make sure cookie values are URI encoded
    Cookies.setUriEncode(true);
    Cookies.setCookie("testencodedvalue", "value1,/?:@&=+$#");
    Cookies.setUriEncode(false);
    String encodedValue = Cookies.getCookie("testencodedvalue");
    assertTrue(encodedValue.compareTo("value1%2C%2F%3F%3A%40%26%3D%2B%24%23") == 0);
    
    // Make sure unencoded cookies with bogus format are not added
    try {
      Cookies.setCookie("test1=test1", "value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("test1;test1", "value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("test1,test1", "value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("test1 test1", "value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("$test1", "value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("test1", "value1;value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
    try {
      Cookies.setCookie("test1", "value1=value1");
      fail("Should have thrown an IllegalArgumentException for bad cookie format.");
    } catch (IllegalArgumentException e) {
      // Success.
    }
  }

  /**
   * Clear out all existing cookies, except the ones used in
   * {@link #testExpires()}.
   */
  private void clearCookies() {
    Collection<String> cookies = Cookies.getCookieNames();
    for (String cookie : cookies) {
      if (!cookie.startsWith("should")) {
        Cookies.removeCookie(cookie);
      }
    }
  }

  /**
   * Test that removing cookies with a path works correctly.
   * 
   * Note that we do not verify failure to remove a cookie set with a path but
   * removed without one as browser behavior differs.
   */
  public void testRemoveCookiePath() {
    // First clear all cookies
    clearCookies();

    // Test first without UriEncoding
    Cookies.setUriEncode(false);

    // Set a few cookies
    int curCount = Cookies.getCookieNames().size();
    Cookies.setCookie("test1+test1", "value1", null, null, "/", false);
    Cookies.setCookie("test2", "value2+value2", null, null, "/", false);
    Cookies.setCookie("test3", "value3", null, null, "/", false);
    Collection<String> cookies = Cookies.getCookieNames();
    assertEquals(curCount + 3, cookies.size());

    // Remove a cookie
    Cookies.removeCookie("test2", "/");
    assertEquals("value1", Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove another cookie
    Cookies.removeCookie("test1+test1", "/");
    assertEquals(null, Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove last cookie
    Cookies.removeCookie("test3", "/");
    assertEquals(null, Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals(null, Cookies.getCookie("test3"));
    cookies = Cookies.getCookieNames();
    assertEquals(curCount, cookies.size());

    // First clear all cookies
    clearCookies();

    // Test with UriEncoding
    Cookies.setUriEncode(true);

    // Set a few cookies
    Cookies.setCookie("test1+test1", "value1", null, null, "/", false);
    Cookies.setCookie("test2", "value2+value2", null, null, "/", false);
    Cookies.setCookie("test3", "value3", null, null, "/", false);
    cookies = Cookies.getCookieNames();
    assertEquals(curCount + 3, cookies.size());

    // Remove a cookie
    Cookies.removeCookie("test2", "/");
    assertEquals("value1", Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove another cookie
    Cookies.removeCookie("test1+test1", "/");
    assertEquals(null, Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals("value3", Cookies.getCookie("test3"));

    // Remove last cookie
    Cookies.removeCookie("test3", "/");
    assertEquals(null, Cookies.getCookie("test1+test1"));
    assertEquals(null, Cookies.getCookie("test2"));
    assertEquals(null, Cookies.getCookie("test3"));
    cookies = Cookies.getCookieNames();
    assertEquals(curCount, cookies.size());
  }

  /**
   * Get the current time in milliseconds from the client. Some tests rely on
   * exact timings that will fail in development mode if the current time on the
   * host and client are off by more than 1000ms.
   * 
   * @return the time on the client
   */
  private long getClientTime() {
    if (GWT.isScript()) {
      return (new Date()).getTime();
    }
    return (long) getClientTimeImpl();
  }

  private native double getClientTimeImpl() /*-{
    return (new Date()).getTime();
  }-*/;
}
