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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to browser cookies stored on the client. Because of browser
 * restrictions, you will only be able to access cookies associated with the
 * current page's domain.
 */
public class Cookies {

  /**
   * Cached copy of cookies.
   */
  static HashMap cachedCookies = null;

  /**
   * Raw cookie string stored to allow cached cookies to be invalidated on
   * write.
   */
  // Used only in JSNI.
  static String rawCookies;

  /**
   * Gets the cookie associated with the given name.
   * 
   * @param name the name of the cookie to be retrieved
   * @return the cookie's value, or <code>null</code> if the cookie doesn't exist
   */
  public static String getCookie(String name) {
    Map cookiesMap = ensureCookies();
    return (String) cookiesMap.get(name);
  }

  /**
   * Gets the names of all cookies in this page's domain.
   * 
   * @return the names of all cookies
   */
  public static Collection getCookieNames() {
    return ensureCookies().keySet();
  }

  /**
   * Removes the cookie associated with the given name.
   * 
   * @param name the name of the cookie to be removed
   */
  public static native void removeCookie(String name) /*-{
    $doc.cookie = name + "='';expires='Fri, 02-Jan-1970 00:00:00 GMT'"; 
  }-*/;

  /**
   * Sets a cookie. The cookie will expire when the current browser session is
   * ended.
   * 
   * @param name the cookie's name
   * @param value the cookie's value
   */
  public static void setCookie(String name, String value) {
    setCookieImpl(name, value, 0, null, null, false);
  }

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
    setCookieImpl(name, value, (expires == null) ? 0 : expires.getTime(), domain, path, secure);
  }

  static native void loadCookies(HashMap m) /*-{
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
        name = decodeURIComponent(name);
        value = decodeURIComponent(value);
        m.@java.util.Map::put(Ljava/lang/Object;Ljava/lang/Object;)(name,value);
      }
    }
  }-*/;

  private static HashMap ensureCookies() {
    if (cachedCookies == null || needsRefresh()) {
      cachedCookies = new HashMap();
      loadCookies(cachedCookies);
    }
    return cachedCookies;
  }

  private static native boolean needsRefresh() /*-{
    var docCookie = $doc.cookie;
        
    // Check to see if cached cookies need to be invalidated.
    if (docCookie != '' && docCookie != @com.google.gwt.user.client.Cookies::rawCookies) {  
      @com.google.gwt.user.client.Cookies::rawCookies = docCookie;
      return true;
    } else {
      return false;
    } 
  }-*/;

  private static native void setCookieImpl(String name, String value,
      long expires, String domain, String path, boolean secure) /*-{
    var c = encodeURIComponent(name) + '=' + encodeURIComponent(value);
    if ( expires )
      c += ';expires=' + (new Date(expires)).toGMTString();
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
