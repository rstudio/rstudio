package com.google.gwt.user.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Date;

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
