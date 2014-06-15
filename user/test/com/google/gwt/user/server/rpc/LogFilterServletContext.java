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
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

@SuppressWarnings(value = {"deprecation", "unchecked"})
abstract class LogFilterServletContext implements ServletContext {
  private final ServletContext realContext;

  public LogFilterServletContext(ServletContext realContext) {
    this.realContext = realContext;
  }

  @Override
  public Object getAttribute(String name) {
    return realContext.getAttribute(name);
  }

  @Override
  public Enumeration getAttributeNames() {
    return realContext.getAttributeNames();
  }

  @Override
  public ServletContext getContext(String uripath) {
    return realContext.getContext(uripath);
  }

  @Override
  public String getContextPath() {
    return realContext.getContextPath();
  }

  @Override
  public String getInitParameter(String name) {
    return realContext.getInitParameter(name);
  }

  @Override
  public Enumeration getInitParameterNames() {
    return realContext.getInitParameterNames();
  }

  @Override
  public int getMajorVersion() {
    return realContext.getMajorVersion();
  }

  @Override
  public String getMimeType(String file) {
    return realContext.getMimeType(file);
  }

  @Override
  public int getMinorVersion() {
    return realContext.getMinorVersion();
  }

  @Override
  public RequestDispatcher getNamedDispatcher(String name) {
    return realContext.getNamedDispatcher(name);
  }

  @Override
  public String getRealPath(String path) {
    return realContext.getRealPath(path);
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return realContext.getRequestDispatcher(path);
  }

  @Override
  public URL getResource(String path) throws MalformedURLException {
    return realContext.getResource(path);
  }

  @Override
  public InputStream getResourceAsStream(String path) {
    return realContext.getResourceAsStream(path);
  }

  @Override
  public Set getResourcePaths(String path) {
    return realContext.getResourcePaths(path);
  }

  @Override
  public String getServerInfo() {
    return realContext.getServerInfo();
  }

  @Override
  public Servlet getServlet(String name) throws ServletException {
    return realContext.getServlet(name);
  }

  @Override
  public String getServletContextName() {
    return realContext.getServletContextName();
  }

  @Override
  public Enumeration getServletNames() {
    return realContext.getServletNames();
  }

  @Override
  public Enumeration getServlets() {
    return realContext.getServlets();
  }

  @Override
  public void log(Exception exception, String msg) {
    if (shouldLog(exception, msg)) {
      realContext.log(exception, msg);
    }
  }

  @Override
  public void log(String msg) {
    if (shouldLog(null, msg)) {
      realContext.log(msg);
    }
  }

  @Override
  public void log(String msg, Throwable throwable) {
    if (shouldLog(throwable, msg)) {
      realContext.log(msg, throwable);
    }
  }

  @Override
  public void removeAttribute(String name) {
    realContext.removeAttribute(name);
  }

  @Override
  public void setAttribute(String name, Object object) {
    realContext.setAttribute(name, object);
  }

  public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
    return realContext.addFilter(arg0, arg1);
  }

  public Dynamic addFilter(String arg0, Filter arg1) {
    return realContext.addFilter(arg0, arg1);
  }

  public Dynamic addFilter(String arg0, String arg1) {
    return realContext.addFilter(arg0, arg1);
  }

  public void addListener(Class<? extends EventListener> arg0) {
    realContext.addListener(arg0);
  }

  public void addListener(String arg0) {
    realContext.addListener(arg0);
  }

  public <T extends EventListener> void addListener(T arg0) {
    realContext.addListener(arg0);
  }

  public javax.servlet.ServletRegistration.Dynamic addServlet(
      String arg0, Class<? extends Servlet> arg1) {
    return realContext.addServlet(arg0, arg1);
  }

  public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
    return realContext.addServlet(arg0, arg1);
  }

  public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
    return realContext.addServlet(arg0, arg1);
  }

  public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
    return realContext.createFilter(arg0);
  }

  public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
    return realContext.createListener(arg0);
  }

  public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
    return realContext.createServlet(arg0);
  }

  public void declareRoles(String... arg0) {
    realContext.declareRoles(arg0);
  }

  public ClassLoader getClassLoader() {
    return realContext.getClassLoader();
  }

  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return realContext.getDefaultSessionTrackingModes();
  }

  public int getEffectiveMajorVersion() {
    return realContext.getEffectiveMajorVersion();
  }

  public int getEffectiveMinorVersion() {
    return realContext.getEffectiveMinorVersion();
  }

  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return realContext.getEffectiveSessionTrackingModes();
  }

  public FilterRegistration getFilterRegistration(String arg0) {
    return realContext.getFilterRegistration(arg0);
  }

  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    return realContext.getFilterRegistrations();
  }

  public JspConfigDescriptor getJspConfigDescriptor() {
    return realContext.getJspConfigDescriptor();
  }

  public ServletRegistration getServletRegistration(String arg0) {
    return realContext.getServletRegistration(arg0);
  }

  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    return realContext.getServletRegistrations();
  }

  public SessionCookieConfig getSessionCookieConfig() {
    return realContext.getSessionCookieConfig();
  }

  public boolean setInitParameter(String arg0, String arg1) {
    return realContext.setInitParameter(arg0, arg1);
  }

  public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
    realContext.setSessionTrackingModes(arg0);
  }

  public String getVirtualServerName() {
    return realContext.getVirtualServerName();
  }

  protected abstract boolean shouldLog(Throwable t, String msg);
}