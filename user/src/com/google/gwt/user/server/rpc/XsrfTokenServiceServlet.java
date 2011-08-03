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

import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.XsrfToken;
import com.google.gwt.user.client.rpc.XsrfTokenService;
import com.google.gwt.user.server.Util;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;

import javax.servlet.http.Cookie;

/**
 * EXPERIMENTAL and subject to change. Do not use this in production code.
 * <p>
 *
 * </p>
 * RPC service to generate XSRF tokens.
 * <p>
 * Sample use of {@link XsrfTokenService}:
 *
 * <ol>
 * <li> Add {@link XsrfTokenServiceServlet} to {@code web.xml}:
 *
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;xsrf&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;
 *     com.google.gwt.user.server.rpc.XsrfTokenServiceServlet
 *   &lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;xsrf&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/gwt/xsrf&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * <li> Specify session cookie name that is used for authentication. MD5 hash of
 * the session cookie's value will be used as an XSRF token:
 *
 * <pre>
 * &lt;context-param&gt;
 *   &lt;param-name&gt;gwt.xsrf.session_cookie_name&lt;/param-name&gt;
 *   &lt;param-value>JSESSIONID&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 *
 * <li> To enforce XSRF token validation on each method call either mark RPC
 * interface as XSRF protected using {@link XsrfProtect} annotation or extend
 * {@link com.google.gwt.user.client.rpc.XsrfProtectedService} instead of
 * RemoteService. Use {@link NoXsrfProtect} to mark methods as not requiring
 * XSRF protection:
 *
 * <pre class="code">
 * public interface MyRpcService extends XsrfProtectedService {
 *   public void doStuff();
 * }
 * </pre>
 *
 * <li> Ensure that RPC's servlet implementation extends {@link
 * XsrfProtectedServiceServlet} instead of {@link RemoteServiceServlet}:
 *
 * <pre class="code">
 * public class MyRpcServiceServlet extends XsrfProtectedServiceServlet
 *     implements MyRpcService {
 *
 *   public void doStuff() {
 *     // ...
 *   }
 * }
 * </pre>
 *
 * <li> Obtain {@link XsrfToken} and set it on the RPC end point:
 *
 * <pre class="code">
 * XsrfTokenServiceAsync xsrf = (XsrfTokenServiceAsync)GWT.create(XsrfTokenService.class);
 *
 * ((ServiceDefTarget)xsrf).setServiceEntryPoint(GWT.getModuleBaseURL() + "xsrf");
 *
 * xsrf.getNewXsrfToken(new AsyncCallback&lt;XsrfToken&gt;() {
 *   public void onSuccess(XsrfToken result) {
 *     MyRpcServiceAsync rpc = (MyRpcServiceAsync)GWT.create(MyRpcService.class);
 *     ((HasRpcToken) rpc).setRpcToken(result);
 *     // make XSRF protected RPC call
 *     rpc.doStuff(new AsyncCallback&lt;Void&gt;() {
 *       // ...
 *     });
 *
 *   }
 *
 *   public void onFailure(Throwable caught) {
 *     try {
 *       throw caught;
 *     } catch (RpcTokenException e) {
 *       // Can be thrown for several reasons:
 *       //   - duplicate session cookie, which may be a sign of a cookie
 *       //     overwrite attack
 *       //   - XSRF token cannot be generated because session cookie isn't
 *       //     present
 *     } catch (Throwable e) {
 *       // unexpected
 *     }
 * });
 * </pre>
 * </ol>
 * </p>
 *
 * @see XsrfProtectedServiceServlet
 * @see XsrfProtect
 * @see NoXsrfProtect
 */
public class XsrfTokenServiceServlet extends RemoteServiceServlet
    implements XsrfTokenService {

  /**
   * Session cookie name initialization parameter.
   */
  public static final String COOKIE_NAME_PARAM =
    "gwt.xsrf.session_cookie_name";

  static final String COOKIE_NAME_NOT_SET_ERROR_MSG =
      "Session cookie name not set! Use '" + COOKIE_NAME_PARAM +
      "' context-param to specify session cookie name";

  /**
   * Session cookie name. Cookie's value is used to generate XSRF cookie.
   */
  private String sessionCookieName = null;

  /**
   * Default constructor.
   */
  public XsrfTokenServiceServlet() {
    this(null);
  }

  /**
   * Alternative constructor that accepts session cookie name instead of getting
   * it from {@link javax.servlet.ServletConfig} or {@link
   * javax.servlet.ServletContext}.
   */
  public XsrfTokenServiceServlet(String sessionCookieName) {
    this.sessionCookieName = sessionCookieName;
  }

  /**
   * Generates and returns new XSRF token.
   */
  public XsrfToken getNewXsrfToken() {
    return new XsrfToken(generateTokenValue());
  }

  /**
   * Servlet initialization.
   */
  @Override
  public void init() {
    // do not overwrite values set via constructor
    if (sessionCookieName == null) {
      sessionCookieName = getInitParameterValue(COOKIE_NAME_PARAM);
    }
    if (sessionCookieName == null) {
      throw new IllegalStateException(COOKIE_NAME_NOT_SET_ERROR_MSG);
    }
  }

  /**
   * Generates new XSRF token.
   *
   * @return session cookie MD5 hash.
   */
  private String generateTokenValue() {
    if (sessionCookieName == null) {
      throw new IllegalStateException(COOKIE_NAME_NOT_SET_ERROR_MSG);
    }
    // generate XSRF cookie using session cookie
    Cookie sessionCookie = Util.getCookie(getThreadLocalRequest(),
        sessionCookieName, false);
    if (sessionCookie == null || sessionCookie.getValue() == null ||
        sessionCookie.getValue().length() == 0) {
      throw new RpcTokenException("Session cookie is not set or empty! " +
          "Unable to generate XSRF cookie");
    }
    byte[] cookieBytes =  sessionCookie.getValue().getBytes();
    return StringUtils.toHexString(Md5Utils.getMd5Digest(cookieBytes));
  }

  /**
   * Retrieves and returns specified initialization parameter first from
   * {@link ServletConfig} followed by {@link ServletContext}, if former returns
   * {@code null}.
   */
  private String getInitParameterValue(String name) {
    String paramValue = null;
    paramValue = getServletConfig().getInitParameter(name);
    if (paramValue == null) {
      paramValue = getServletContext().getInitParameter(name);
    }
    return paramValue;
  }
}
