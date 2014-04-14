/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents what a composite unique key for individual tabs in the
 * development mode window.
 */
class DevelModeTabKey {
  private final String remoteHost;
  private final String tabKey;
  private final String url;
  private final String userAgent;

  /**
   * Create a key.
   *
   * @param userAgent user agent string (not null)
   * @param url top-level URL (may be null for old clients)
   * @param tabKey opaque identifier for a browser tab - must be unique
   *     within a particular browser (user agent + remoteSocket) or an
   *     empty string if not support (may be null for old clients)
   * @param remoteHost host portion of endpoint identifier of browser
   *     (not null)
   * @throws IllegalArgumentException if userAgent or remoteHost is null
   */
  public DevelModeTabKey(String userAgent, String url, String tabKey,
      String remoteHost) {
    if (url == null) {
      url = "";
    }
    if (tabKey == null) {
      tabKey = "";
    }
    if (userAgent == null) {
      throw new IllegalArgumentException("userAgent cannot be null");
    }
    if (remoteHost == null) {
      throw new IllegalArgumentException("remoteHost cannot be null");
    }
    this.userAgent = userAgent;
    try {
      // Strip off the query part and the hash part
      // TODO(jat): is it correct to strip off the query part?
      URL fullUrl = new URL(url);
      StringBuilder buf = new StringBuilder();
      buf.append(fullUrl.getProtocol()).append(':');
      if (fullUrl.getAuthority() != null && fullUrl.getAuthority().length() > 0) {
        buf.append("//").append(fullUrl.getAuthority());
      }
      if (fullUrl.getPath() != null) {
        buf.append(fullUrl.getPath());
      }
      url = buf.toString();
    } catch (MalformedURLException e) {
      // use URL as-is if it appears to be malformed
    }
    this.url = url;
    this.tabKey = tabKey;
    this.remoteHost = remoteHost;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DevelModeTabKey other = (DevelModeTabKey) obj;
    return tabKey.equals(other.tabKey) && url.equals(other.url)
        && userAgent.equals(other.userAgent)
        && remoteHost.equals(other.remoteHost);
  }

  public String getRemoteSocket() {
    return remoteHost;
  }

  public String getTabKey() {
    return tabKey;
  }

  public String getUrl() {
    return url;
  }

  public String getUserAgent() {
    return userAgent;
  }

  @Override
  public int hashCode() {
    return remoteHost.hashCode() * 7 + tabKey.hashCode() * 11
        + url.hashCode() * 13 + userAgent.hashCode() * 17;
  }
}