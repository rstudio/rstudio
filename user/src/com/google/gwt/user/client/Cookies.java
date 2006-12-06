/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Date;

/**
 * Provides access to browser cookies stored on the client.  Because of browser
 * restrictions, you will only be able to access cookies associated with the
 * current page's domain.
 */
public class Cookies {

  /**
   * Gets the cookie associated with the given key.
   * 
   * @param key the key of the cookie to be retrieved
   * @return the cookie's value.
   */
  public static native String getCookie(String key) /*-{
    var cookies = @com.google.gwt.user.client.Cookies::loadCookies()();
    var value = cookies[key];
    return (value == null) ? null : value;
  }-*/;

  /**
   * Sets a cookie.
   * 
   * @param name the cookie's name
   * @param value the cookie's value
   * @param expires when the cookie expires
   */
  public static void setCookie(String name, String value, Date expires) {
    setCookie(name, value, expires, null, null, false);
  }

  /**
   * Sets a cookie.
   * 
   * @param name the cookie's name
   * @param value the cookie's value
   * @param expires when the cookie expires
   * @param domain the domain to be associated with this cookie
   * @param path the path to be associated with this cookie
   * @param secure <code>true</code> to make this a secure cookie
   */
  public static void setCookie(String name, String value, Date expires,
      String domain, String path, boolean secure) {
    setCookie(name, value, expires.getTime(), domain, path, secure);
  }

  static native JavaScriptObject loadCookies() /*-{
    var cookies = {};

    var docCookie = $doc.cookie;
    if (docCookie && docCookie != '') {
      var crumbs = docCookie.split('; ');
      for (var i = 0; i < crumbs.length; ++i) {
        var name, value;

        var eqIdx = crumbs[i].indexOf('=');
        if (eqIdx == -1) {
          name = crumbs[i];
          value = '';
        } else {
          name = crumbs[i].substring(0, eqIdx);
          value = crumbs[i].substring(eqIdx + 1);
        }

        cookies[decodeURIComponent(name)] = decodeURIComponent(value);
      }
    }

    return cookies;
  }-*/;

  private static native void setCookie(String name, String value, long expires,
      String domain, String path, boolean secure) /*-{
    var date = new Date(expires);

    var c = encodeURIComponent(name) + '=' + encodeURIComponent(value);
    c += ';expires=' + date.toGMTString();

    if (domain)
      c += ';domain=' + domain;
    if (path)
      c += ';path=' + path;
    if (secure)
      c += ';secure';

    $doc.cookie = c;
  }-*/;

  private Cookies() {
  }
}
