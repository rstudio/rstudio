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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.RpcToken;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.XsrfToken;
import com.google.gwt.user.server.Util;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;

import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 * The servlet base class for RPC service implementations using default XSRF
 * protection tied to authentication session cookie.
 * </p>
 *
 * <p>
 * XSRF token validation is performed by generating MD5 hash of the session
 * cookie and comparing supplied {@link XsrfToken} with the generated hash.
 * Session cookie name is specified by the {@value
 * com.google.gwt.user.server.rpc.XsrfTokenServiceServlet#COOKIE_NAME_PARAM}
 * context parameter in {@code web.xml}.
 * </p>
 *
 * <p>
 * {@link com.google.gwt.user.client.rpc.XsrfTokenService} can be used by
 * clients to obtain {@link XsrfToken}s that will pass validation performed by
 * this class.
 * </p>
 *
 * @see XsrfTokenServiceServlet
 * @see AbstractXsrfProtectedServiceServlet
 */
public class XsrfProtectedServiceServlet
    extends AbstractXsrfProtectedServiceServlet {

  // @VisibleForTesting
  String sessionCookieName = null;

  public XsrfProtectedServiceServlet() {
    this(null);
  }

  public XsrfProtectedServiceServlet(String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
  }

  public XsrfProtectedServiceServlet(Object delegate) {
    this(delegate, null);
  }

  public XsrfProtectedServiceServlet(Object delegate,
      String sessionCookieName) {
    super(delegate);
    this.sessionCookieName = sessionCookieName;
  }

  @Override
  public void init() throws ServletException {
    super.init();
    // do not overwrite if value is supplied in constructor
    if (sessionCookieName == null) {
      // servlet configuration precedes context configuration
      sessionCookieName = getServletConfig().getInitParameter(
          XsrfTokenServiceServlet.COOKIE_NAME_PARAM);
      if (sessionCookieName == null) {
        sessionCookieName = getServletContext().getInitParameter(
            XsrfTokenServiceServlet.COOKIE_NAME_PARAM);
      }
      if (sessionCookieName == null) {
        throw new IllegalStateException(
            XsrfTokenServiceServlet.COOKIE_NAME_NOT_SET_ERROR_MSG);
      }
    }
  }

  /**
   * Validates {@link XsrfToken} included with {@link RPCRequest} against XSRF
   * cookie.
   */
  @Override
  protected void validateXsrfToken(RpcToken token, Method method)
      throws RpcTokenException {
    if (token == null) {
      throw new RpcTokenException("XSRF token missing");
    }
    Cookie sessionCookie = Util.getCookie(getThreadLocalRequest(),
        sessionCookieName, false);
    if (sessionCookie == null || sessionCookie.getValue() == null ||
        sessionCookie.getValue().length() == 0) {
      throw new RpcTokenException("Session cookie is missing or empty! " +
          "Unable to verify XSRF cookie");
    }

    String expectedToken = StringUtils.toHexString(
        Md5Utils.getMd5Digest(sessionCookie.getValue().getBytes()));
    XsrfToken xsrfToken = (XsrfToken) token;

    if (!expectedToken.equals(xsrfToken.getToken())) {
      throw new RpcTokenException("Invalid XSRF token");
    }
  }
}
