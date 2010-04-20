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
  static HashMap<String, String> cachedCookies = null;

  /**
   * Raw cookie string stored to allow cached cookies to be invalidated on
   * write.
   */
  // Used only in JSNI.
  static String rawCookies;

  /**
   * Indicates whether or not cookies are enabled.
   */
  private static boolean isCookieEnabled = false;

  /**
   * Indicates whether or not we've checked if cookies are enabled.
   */
  private static boolean isCookieChecked = false;

  /**
   * Flag that indicates whether cookies should be URIencoded (when set) and
   * URIdecoded (when retrieved). Defaults to URIencoding.
   */
  private static boolean uriEncoding = true;

  /**
   * Gets the cookie associated with the given name.
   * 
   * @param name the name of the cookie to be retrieved
   * @return the cookie's value, or <code>null</code> if the cookie doesn't
   *         exist
   */
  public static String getCookie(String name) {
    Map<String, String> cookiesMap = ensureCookies();
    return cookiesMap.get(name);
  }

  /**
   * Gets the names of all cookies in this page's domain.
   * 
   * @return the names of all cookies
   */
  public static Collection<String> getCookieNames() {
    return ensureCookies().keySet();
  }

  /**
   * Gets the URIencode flag.
   */
  public static boolean getUriEncode() {
    return uriEncoding;
  }

  /**
   * Checks whether or not cookies are enabled or disabled.
   * 
   * @return true if a cookie can be set, false if not
   */
  public static boolean isCookieEnabled() {
    if (!isCookieChecked) {
      // The only way to know for sure that cookies are enabled is to set and
      // retrieve one. Checking navigator.cookieEnabled may return the wrong
      // value if the browser has security software installed. In IE, it alerts
      // the user of an unspecified security risk when the app is embedded in an
      // iframe.
      isCookieChecked = true;
      Cookies.setCookie("__gwtCookieCheck", "isEnabled");
      isCookieEnabled = "isEnabled".equals(Cookies.getCookie("__gwtCookieCheck"));
      Cookies.removeCookie("__gwtCookieCheck");
    }
    return isCookieEnabled;
  }

  /**
   * Removes the cookie associated with the given name.
   * 
   * @param name the name of the cookie to be removed
   */
  public static void removeCookie(String name) {
    if (uriEncoding) {
      name = uriEncode(name);
    }
    removeCookieNative(name);
  }

  /**
   * Removes the cookie associated with the given name.
   * 
   * @param name the name of the cookie to be removed
   * @param path the path to be associated with this cookie (which should match
   *          the path given in {@link #setCookie})
   */
  public static void removeCookie(String name, String path) {
    if (uriEncoding) {
      name = uriEncode(name);
    }
    removeCookieNative(name, path);
  }

  /**
   * Native method to remove a cookie with a path.
   */
  public static native void removeCookieNative(String name, String path) /*-{
    $doc.cookie = name + "=;path=" + path + ";expires=Fri, 02-Jan-1970 00:00:00 GMT"; 
  }-*/;

  /**
   * Sets a cookie. The cookie will expire when the current browser session is
   * ended.
   * 
   * @param name the cookie's name
   * @param value the cookie's value
   */
  public static void setCookie(String name, String value) {
    setCookie(name, value, null, null, null, false);
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
   * Sets a cookie. If uriEncoding is false, it checks the validity of name and
   * value. Name: Must conform to RFC 2965. Not allowed: = , ; white space. Also
   * can't begin with $. Value: No = or ;
   * 
   * @param name the cookie's name
   * @param value the cookie's value
   * @param expires when the cookie expires
   * @param domain the domain to be associated with this cookie
   * @param path the path to be associated with this cookie
   * @param secure <code>true</code> to make this a secure cookie (that is, only
   *          accessible over an SSL connection)
   */
  public static void setCookie(String name, String value, Date expires,
      String domain, String path, boolean secure) {
    if (uriEncoding) {
      name = uriEncode(name);
      value = uriEncode(value);
    } else if (!isValidCookieName(name)) {
      throw new IllegalArgumentException("Illegal cookie format: " + name + " is not a valid cookie name.");
    } else if (!isValidCookieValue(value)) {
      throw new IllegalArgumentException("Illegal cookie format: " + value + " is not a valid cookie value.");
    }
    setCookieImpl(name, value, (expires == null) ? 0 : expires.getTime(),
        domain, path, secure);
  }

  /**
   * Updates the URIencode flag and empties the cached cookies set.
   */
  public static void setUriEncode(boolean encode) {
    if (encode != uriEncoding) {
      uriEncoding = encode;
      cachedCookies = null;
    }
  }

  static native void loadCookies(HashMap<String, String> m) /*-{
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
        if (@com.google.gwt.user.client.Cookies::uriEncoding) {
          try {
            name = decodeURIComponent(name);
          } catch (e) {
            // ignore error, keep undecoded name
          }
          try {
            value = decodeURIComponent(value);
          } catch (e) {
            // ignore error, keep undecoded value
          }
        }
        m.@java.util.Map::put(Ljava/lang/Object;Ljava/lang/Object;)(name,value);
      }
    }
  }-*/;

  private static HashMap<String, String> ensureCookies() {
    if (cachedCookies == null || needsRefresh()) {
      HashMap<String, String> newCachedCookies = new HashMap<String, String>();
      loadCookies(newCachedCookies);
      cachedCookies = newCachedCookies;
    }
    return cachedCookies;
  }

  /**
   * Checks whether a cookie name is valid: can't contain '=', ';', ',', or
   * whitespace. Can't begin with $.
   * 
   * @param name the cookie's name
   */
  private static boolean isValidCookieName(String name) {
    if (uriEncoding) {
      // check not necessary
      return true;
    } else if (name.contains("=") || name.contains(";") || name.contains(",")
        || name.startsWith("$") || name.matches(".*\\s+.*")) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Checks whether a cookie value is valid. A cookie cannot contain '=' or ';'.
   * 
   * @param value the cookie's value
   */
  private static boolean isValidCookieValue(String value) {
    if (uriEncoding) {
      // check not necessary
      return true;
    }
    if (value.contains("=") || value.contains(";")) {
      return false;
    } else {
      return true;
    }
  }

  private static native boolean needsRefresh() /*-{
    var docCookie = $doc.cookie;
        
    // Check to see if cached cookies need to be invalidated.
    if (docCookie != @com.google.gwt.user.client.Cookies::rawCookies) {  
      @com.google.gwt.user.client.Cookies::rawCookies = docCookie;
      return true;
    } else {
      return false;
    } 
  }-*/;

  /**
   * Native method to remove a cookie.
   */
  private static native void removeCookieNative(String name) /*-{
    $doc.cookie = name + "=;expires=Fri, 02-Jan-1970 00:00:00 GMT"; 
  }-*/;

  private static native void setCookieImpl(String name, String value,
      double expires, String domain, String path, boolean secure) /*-{
    var c = name + '=' + value;
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

  private static native String uriEncode(String s) /*-{
    return encodeURIComponent(s);
  }-*/;

  private Cookies() {
  }
}
