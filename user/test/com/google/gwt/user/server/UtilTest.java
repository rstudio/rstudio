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
package com.google.gwt.user.server;

import com.google.gwt.user.server.rpc.MockHttpServletRequest;
import com.google.gwt.user.server.rpc.NoXsrfProtect;
import com.google.gwt.user.server.rpc.XsrfProtect;

import junit.framework.TestCase;

import javax.servlet.http.Cookie;

/**
 * Utility methods tests.
 */
public class UtilTest extends TestCase {

  @NoXsrfProtect
  private class parent {
  }

  private class child extends parent {
  }

  @NoXsrfProtect
  private interface parentIntf {
  }

  private interface childIntf extends parentIntf {
  }

  public void testGetClassAnnotation() throws Exception {
    assertNotNull(Util.getClassAnnotation(parent.class, NoXsrfProtect.class));
    assertNotNull(Util.getClassAnnotation(child.class, NoXsrfProtect.class));
    assertNotNull(Util.getClassAnnotation(parentIntf.class,
        NoXsrfProtect.class));
    assertNotNull(Util.getClassAnnotation(childIntf.class,
        NoXsrfProtect.class));

    assertNull(Util.getClassAnnotation(child.class, XsrfProtect.class));
  }

  private class MockHttpServletRequestWithCookies extends
      MockHttpServletRequest {
    private Cookie[] cookies;

    public MockHttpServletRequestWithCookies(Cookie[] cookies) {
      this.cookies = cookies;
    }

    public Cookie[] getCookies() {
      return cookies;
    }
  }

  public void testGetCookie() throws Exception {
    Cookie[] cookies = new Cookie[2];
    MockHttpServletRequestWithCookies req =
      new MockHttpServletRequestWithCookies(cookies);

    cookies[0] = new Cookie("chocolate", "chip");
    assertEquals("chip", Util.getCookie(req, "chocolate", true).getValue());

    cookies[1] = new Cookie("chocolate", "oatmeal");
    assertEquals("chip", Util.getCookie(req, "chocolate", true).getValue());
    try {
      Util.getCookie(req, "chocolate", false);
      fail("Should've thrown IllegalStateException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
