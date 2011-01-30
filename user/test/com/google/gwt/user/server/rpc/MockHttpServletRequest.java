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
package com.google.gwt.user.server.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * A dummy class for testing methods that require an HttpServletRequest.
 */
public class MockHttpServletRequest implements HttpServletRequest {

  public Object getAttribute(String arg0) {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getAttributeNames() {
    throw new UnsupportedOperationException();
  }

  public String getAuthType() {
    throw new UnsupportedOperationException();
  }

  public String getCharacterEncoding() {
    throw new UnsupportedOperationException();
  }

  public int getContentLength() {
    throw new UnsupportedOperationException();
  }

  public String getContentType() {
    throw new UnsupportedOperationException();
  }

  public String getContextPath() {
    throw new UnsupportedOperationException();
  }

  public Cookie[] getCookies() {
    throw new UnsupportedOperationException();
  }

  public long getDateHeader(String arg0) {
    throw new UnsupportedOperationException();
  }

  public String getHeader(String arg0) {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getHeaderNames() {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getHeaders(String arg0) {
    throw new UnsupportedOperationException();
  }

  public ServletInputStream getInputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  public int getIntHeader(String arg0) {
    throw new UnsupportedOperationException();
  }

  public String getLocalAddr() {
    throw new UnsupportedOperationException();
  }

  public Locale getLocale() {
    throw new UnsupportedOperationException();
  }

  public Enumeration<Locale> getLocales() {
    throw new UnsupportedOperationException();
  }

  public String getLocalName() {
    throw new UnsupportedOperationException();
  }

  public int getLocalPort() {
    throw new UnsupportedOperationException();
  }

  public String getMethod() {
    throw new UnsupportedOperationException();
  }

  public String getParameter(String arg0) {
    throw new UnsupportedOperationException();
  }

  public Map<String, String[]> getParameterMap() {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getParameterNames() {
    throw new UnsupportedOperationException();
  }

  public String[] getParameterValues(String arg0) {
    throw new UnsupportedOperationException();
  }

  public String getPathInfo() {
    throw new UnsupportedOperationException();
  }

  public String getPathTranslated() {
    throw new UnsupportedOperationException();
  }

  public String getProtocol() {
    throw new UnsupportedOperationException();
  }

  public String getQueryString() {
    throw new UnsupportedOperationException();
  }

  public BufferedReader getReader() throws IOException {
    throw new UnsupportedOperationException();
  }

  public String getRealPath(String arg0) {
    throw new UnsupportedOperationException();
  }

  public String getRemoteAddr() {
    throw new UnsupportedOperationException();
  }

  public String getRemoteHost() {
    throw new UnsupportedOperationException();
  }

  public int getRemotePort() {
    throw new UnsupportedOperationException();
  }

  public String getRemoteUser() {
    throw new UnsupportedOperationException();
  }

  public RequestDispatcher getRequestDispatcher(String arg0) {
    throw new UnsupportedOperationException();
  }

  public String getRequestedSessionId() {
    throw new UnsupportedOperationException();
  }

  public String getRequestURI() {
    throw new UnsupportedOperationException();
  }

  public StringBuffer getRequestURL() {
    throw new UnsupportedOperationException();
  }

  public String getScheme() {
    throw new UnsupportedOperationException();
  }

  public String getServerName() {
    throw new UnsupportedOperationException();
  }

  public int getServerPort() {
    throw new UnsupportedOperationException();
  }

  public String getServletPath() {
    throw new UnsupportedOperationException();
  }

  public HttpSession getSession() {
    throw new UnsupportedOperationException();
  }

  public HttpSession getSession(boolean arg0) {
    throw new UnsupportedOperationException();
  }

  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException();
  }

  public boolean isSecure() {
    throw new UnsupportedOperationException();
  }

  public boolean isUserInRole(String arg0) {
    throw new UnsupportedOperationException();
  }

  public void removeAttribute(String arg0) {
    throw new UnsupportedOperationException();
  }

  public void setAttribute(String arg0, Object arg1) {
    throw new UnsupportedOperationException();
  }

  public void setCharacterEncoding(String arg0) {
    throw new UnsupportedOperationException();
  }
}
