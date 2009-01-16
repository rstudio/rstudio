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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SuppressWarnings(value = {"deprecation", "unchecked"})
abstract class LogFilterServletContext implements ServletContext {
  private final ServletContext realContext;

  public LogFilterServletContext(ServletContext realContext) {
    this.realContext = realContext;
  }

  public Object getAttribute(String name) {
    return realContext.getAttribute(name);
  }

  public Enumeration getAttributeNames() {
    return realContext.getAttributeNames();
  }

  public ServletContext getContext(String uripath) {
    return realContext.getContext(uripath);
  }

  public String getContextPath() {
    return realContext.getContextPath();
  }

  public String getInitParameter(String name) {
    return realContext.getInitParameter(name);
  }

  public Enumeration getInitParameterNames() {
    return realContext.getInitParameterNames();
  }

  public int getMajorVersion() {
    return realContext.getMajorVersion();
  }

  public String getMimeType(String file) {
    return realContext.getMimeType(file);
  }

  public int getMinorVersion() {
    return realContext.getMinorVersion();
  }

  public RequestDispatcher getNamedDispatcher(String name) {
    return realContext.getNamedDispatcher(name);
  }

  public String getRealPath(String path) {
    return realContext.getRealPath(path);
  }

  public RequestDispatcher getRequestDispatcher(String path) {
    return realContext.getRequestDispatcher(path);
  }

  public URL getResource(String path) throws MalformedURLException {
    return realContext.getResource(path);
  }

  public InputStream getResourceAsStream(String path) {
    return realContext.getResourceAsStream(path);
  }

  public Set getResourcePaths(String path) {
    return realContext.getResourcePaths(path);
  }

  public String getServerInfo() {
    return realContext.getServerInfo();
  }

  public Servlet getServlet(String name) throws ServletException {
    return realContext.getServlet(name);
  }

  public String getServletContextName() {
    return realContext.getServletContextName();
  }

  public Enumeration getServletNames() {
    return realContext.getServletNames();
  }

  public Enumeration getServlets() {
    return realContext.getServlets();
  }

  public void log(Exception exception, String msg) {
    if (shouldLog(exception, msg)) {
      realContext.log(exception, msg);
    }
  }

  public void log(String msg) {
    if (shouldLog(null, msg)) {
      realContext.log(msg);
    }
  }

  public void log(String msg, Throwable throwable) {
    if (shouldLog(throwable, msg)) {
      realContext.log(msg, throwable);
    }
  }

  public void removeAttribute(String name) {
    realContext.removeAttribute(name);
  }

  public void setAttribute(String name, Object object) {
    realContext.setAttribute(name, object);
  }

  protected abstract boolean shouldLog(Throwable t, String msg);
}