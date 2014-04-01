/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base servlet for GWT server-side code which extracts properties from the
 * request and sets them for this thread.
 * <p>
 * For now, subclasses should override {@link #init()} and set the locale
 * configuration fields - eventually this will be read from a deploy artifact.
 */
public class GwtServletBase extends HttpServlet {

  // These defaults should be kept in sync with I18N.gwt.xml
  protected String[] localeSearchOrder = new String[] {
    "queryparam", "cookie", "meta", "useragent",
  };
  protected String defaultLocale = "default";
  protected String localeCookie = null;
  protected String localeQueryParam = "locale";

  @Override
  public void init() throws ServletException {
    // TODO(jat): implement reading config from a deploy artifact
  }

  /**
   * Fetch a cookie from the HTTP request.
   *
   * @param req
   * @param cookieName
   * @return the value of the cookie or null if not found
   */
  protected final String getCookie(HttpServletRequest req, String cookieName) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (cookie.getName().equals(cookieName)) {
        return cookie.getValue();
      }
    }
    return null;
  }

  /**
   * Get the GWT locale to use from this request.
   *
   * @param req
   * @return the GWT locale to use as a String
   */
  protected String getGwtLocale(HttpServletRequest req) {
    // set the locale
    String locale = null;
    for (String localeMethod : localeSearchOrder) {
      if ("cookie".equals(localeMethod)) {
        if (localeCookie != null) {
          locale = getCookie(req, localeCookie);
        }
      } else if ("queryparam".equals(localeMethod)) {
        if (localeQueryParam != null) {
          locale = req.getParameter(localeQueryParam);
        }
      } else if ("useragent".equals(localeMethod)) {
        // TODO(jat): implement Accept-Language processing
      } else if ("usemeta".equals(localeMethod)) {
        // ignored on the server
      } else {
        // TODO(jat): log ignored method?
      }
      if (locale != null) {
        return locale;
      }
    }
    return defaultLocale;
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    setGwtProperties(req);
    super.service(req, resp);
  }

  /**
   * Sets all GWT properties from the request.
   * <p>
   * If this method is overridden, this version should be called first and then
   * any modifications to property values should be done.
   *
   * @param req
   */
  protected void setGwtProperties(HttpServletRequest req) {
    ServerGwtBridge.getInstance().setThreadProperty("locale", getGwtLocale(req));
    // TODO(jat): other properties, such as user.agent?
  }
}
