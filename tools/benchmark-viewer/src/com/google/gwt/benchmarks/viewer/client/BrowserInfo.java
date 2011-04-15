/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.benchmarks.viewer.client;

/**
 * Provides information about a browser (vendor,version,operating system,etc...)
 * based on user agent and other easily accessible information.
 * 
 * This is not meant to be a "detect script" to implement browser workarounds,
 * but rather a "pretty printer" for the browser information.
 * 
 * This code is a derivation of Browser Detect v2.1.6 documentation:
 * http://www.dithered.com/javascript/browser_detect/index.html license:
 * http://creativecommons.org/licenses/by/1.0/ code by Chris Nott
 * (chris[at]dithered[dot]com)
 * 
 * It has been transliterated from JavaScript to Java with additional changes
 * along the way.
 */
public class BrowserInfo {

  /**
   * Retrieves a "pretty" version of the browser version information.
   * 
   * @param userAgent - The HTTP user agent string.
   * @return A pretty-printed version of the browser including the a) vendor b)
   *         version c) and operating system
   */
  @SuppressWarnings("unused")
  public static String getBrowser(String userAgent) {

    userAgent = userAgent.toLowerCase();

    // browser engine name
    boolean isGecko = userAgent.indexOf("gecko") != -1 && userAgent.indexOf("safari") == -1;
    boolean isAppleWebKit = userAgent.indexOf("applewebkit") != -1;

    // browser name
    boolean isKonqueror = userAgent.indexOf("konqueror") != -1;
    boolean isSafari = userAgent.indexOf("safari") != -1;
    boolean isOmniweb = userAgent.indexOf("omniweb") != -1;
    boolean isOpera = userAgent.indexOf("opera") != -1;
    boolean isIcab = userAgent.indexOf("icab") != -1;
    boolean isAol = userAgent.indexOf("aol") != -1;
    boolean isIE =
        userAgent.indexOf("msie") != -1 && !isOpera && (userAgent.indexOf("webtv") == -1);
    boolean isMozilla = isGecko && userAgent.indexOf("gecko/") + 14 == userAgent.length();
    boolean isFirefox = userAgent.indexOf("firefox/") != -1 || userAgent.indexOf("firebird/") != -1;
    boolean isNS =
        isGecko ? userAgent.indexOf("netscape") != -1 : userAgent.indexOf("mozilla") != -1
            && !isOpera && !isSafari && userAgent.indexOf("spoofer") == -1
            && userAgent.indexOf("compatible") == -1 && userAgent.indexOf("webtv") == -1
            && userAgent.indexOf("hotjava") == -1;

    // spoofing and compatible browsers
    boolean isIECompatible = userAgent.indexOf("msie") != -1 && !isIE;
    boolean isNSCompatible = userAgent.indexOf("mozilla") != -1 && !isNS && !isMozilla;

    // rendering engine versions
    String geckoVersion =
        isGecko ? userAgent.substring(userAgent.lastIndexOf("gecko/") + 6, userAgent
            .lastIndexOf("gecko/") + 14) : "-1";
    String equivalentMozilla = isGecko ? userAgent.substring(userAgent.indexOf("rv:") + 3) : "-1";
    String appleWebKitVersion =
        isAppleWebKit ? userAgent.substring(userAgent.indexOf("applewebkit/") + 12) : "-1";

    // float versionMinor = parseFloat(navigator.appVersion);
    String versionMinor = "";

    // correct version number
    if (isGecko && !isMozilla) {
      versionMinor =
          userAgent.substring(userAgent.indexOf("/", userAgent.indexOf("gecko/") + 6) + 1);
    } else if (isMozilla) {
      versionMinor = userAgent.substring(userAgent.indexOf("rv:") + 3);
    } else if (isIE) {
      versionMinor = userAgent.substring(userAgent.indexOf("msie ") + 5);
    } else if (isKonqueror) {
      versionMinor = userAgent.substring(userAgent.indexOf("konqueror/") + 10);
    } else if (isSafari) {
      versionMinor = userAgent.substring(userAgent.lastIndexOf("safari/") + 7);
    } else if (isOmniweb) {
      versionMinor = userAgent.substring(userAgent.lastIndexOf("omniweb/") + 8);
    } else if (isOpera) {
      versionMinor = userAgent.substring(userAgent.indexOf("opera") + 6);
    } else if (isIcab) {
      versionMinor = userAgent.substring(userAgent.indexOf("icab") + 5);
    }

    String version = getVersion(versionMinor);

    // dom support
    // boolean isDOM1 = (document.getElementById);
    // boolean isDOM2Event = (document.addEventListener &&
    // document.removeEventListener);

    // css compatibility mode
    // this.mode = document.compatMode ? document.compatMode : "BackCompat";

    // platform
    boolean isWin = userAgent.indexOf("win") != -1;
    boolean isWin32 =
        isWin && userAgent.indexOf("95") != -1 || userAgent.indexOf("98") != -1
            || userAgent.indexOf("nt") != -1 || userAgent.indexOf("win32") != -1
            || userAgent.indexOf("32bit") != -1 || userAgent.indexOf("xp") != -1;

    boolean isMac = userAgent.indexOf("mac") != -1;
    boolean isUnix =
        userAgent.indexOf("unix") != -1 || userAgent.indexOf("sunos") != -1
            || userAgent.indexOf("bsd") != -1 || userAgent.indexOf("x11") != -1;

    boolean isLinux = userAgent.indexOf("linux") != -1;

    // specific browser shortcuts
    /*
     * this.isNS4x = (this.isNS && this.versionMajor == 4); this.isNS40x =
     * (this.isNS4x && this.versionMinor < 4.5); this.isNS47x = (this.isNS4x &&
     * this.versionMinor >= 4.7); this.isNS4up = (this.isNS && this.versionMinor
     * >= 4); this.isNS6x = (this.isNS && this.versionMajor == 6); this.isNS6up
     * = (this.isNS && this.versionMajor >= 6); this.isNS7x = (this.isNS &&
     * this.versionMajor == 7); this.isNS7up = (this.isNS && this.versionMajor
     * >= 7);
     * 
     * this.isIE4x = (this.isIE && this.versionMajor == 4); this.isIE4up =
     * (this.isIE && this.versionMajor >= 4); this.isIE5x = (this.isIE &&
     * this.versionMajor == 5); this.isIE55 = (this.isIE && this.versionMinor ==
     * 5.5); this.isIE5up = (this.isIE && this.versionMajor >= 5); this.isIE6x =
     * (this.isIE && this.versionMajor == 6); this.isIE6up = (this.isIE &&
     * this.versionMajor >= 6);
     * 
     * this.isIE4xMac = (this.isIE4x && this.isMac);
     */

    String name =
        isGecko ? "Gecko" : isAppleWebKit ? "Apple WebKit" : isKonqueror ? "Konqueror" : isSafari
            ? "Safari" : isOpera ? "Opera" : isIE ? "IE" : isMozilla ? "Mozilla" : isFirefox
                ? "Firefox" : isNS ? "Netscape" : "";

    name +=
        " " + version + " on "
            + (isWin ? "Windows" : isMac ? "Mac" : isUnix ? "Unix" : isLinux ? "Linux" : "Unknown");

    return name;
  }

  // Reads the version from a string which begins with a version number
  // and contains additional character data
  private static String getVersion(String versionPlusCruft) {
    for (int index = 0; index < versionPlusCruft.length(); ++index) {
      char c = versionPlusCruft.charAt(index);
      if (c != '.' && !Character.isDigit(c)) {
        return versionPlusCruft.substring(0, index);
      }
    }
    return versionPlusCruft;
  }
}
