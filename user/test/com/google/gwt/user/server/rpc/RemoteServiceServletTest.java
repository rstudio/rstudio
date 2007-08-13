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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Test some of the failure modes associated with
 * {@link RemoteServiceServlet#doGetSerializationPolicy(HttpServletRequest, String, String)}.
 * 
 * TODO: test caching of policies?
 */
public class RemoteServiceServletTest extends TestCase {

  private static class Bar implements Serializable {
  }

  private static class Baz {
  }

  private static class Foo implements IsSerializable {
  }

  private static class MockHttpServletRequest implements HttpServletRequest {
    private String contextPath;

    public Object getAttribute(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration getAttributeNames() {
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
      return contextPath;
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

    public Enumeration getHeaderNames() {
      throw new UnsupportedOperationException();
    }

    public Enumeration getHeaders(String arg0) {
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

    public Enumeration getLocales() {
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

    public Map getParameterMap() {
      throw new UnsupportedOperationException();
    }

    public Enumeration getParameterNames() {
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

  private class MockServletConfig implements ServletConfig {
    private ServletContext context;

    public String getInitParameter(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration getInitParameterNames() {
      throw new UnsupportedOperationException();
    }

    public ServletContext getServletContext() {
      return context;
    }

    public String getServletName() {
      throw new UnsupportedOperationException();
    }

    void setContext(ServletContext context) {
      this.context = context;
    }
  }

  private class MockServletContext implements ServletContext {
    private ServletConfig config;
    private Throwable exLogged;
    private String messageLogged;

    public MockServletContext() {
    }

    public Object getAttribute(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration getAttributeNames() {
      throw new UnsupportedOperationException();
    }

    public ServletContext getContext(String arg0) {
      throw new UnsupportedOperationException();
    }

    public String getInitParameter(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration getInitParameterNames() {
      throw new UnsupportedOperationException();
    }

    public int getMajorVersion() {
      throw new UnsupportedOperationException();
    }

    public String getMimeType(String arg0) {
      throw new UnsupportedOperationException();
    }

    public int getMinorVersion() {
      throw new UnsupportedOperationException();
    }

    public RequestDispatcher getNamedDispatcher(String arg0) {
      throw new UnsupportedOperationException();
    }

    public String getRealPath(String arg0) {
      throw new UnsupportedOperationException();
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
      throw new UnsupportedOperationException();
    }

    public URL getResource(String arg0) throws MalformedURLException {
      throw new UnsupportedOperationException();
    }

    public InputStream getResourceAsStream(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Set getResourcePaths(String arg0) {
      throw new UnsupportedOperationException();
    }

    public String getServerInfo() {
      throw new UnsupportedOperationException();
    }

    public Servlet getServlet(String arg0) throws ServletException {
      throw new UnsupportedOperationException();
    }

    public String getServletContextName() {
      throw new UnsupportedOperationException();
    }

    public Enumeration getServletNames() {
      throw new UnsupportedOperationException();
    }

    public Enumeration getServlets() {
      throw new UnsupportedOperationException();
    }

    public void log(Exception arg0, String arg1) {
      log(arg1, arg0);
    }

    public void log(String arg0) {
      log(arg0, null);
    }

    public void log(String arg0, Throwable arg1) {
      messageLogged = arg0;
      exLogged = arg1;
    }

    public void removeAttribute(String arg0) {
    }

    public void setAttribute(String arg0, Object arg1) {
      throw new UnsupportedOperationException();
    }

    void setConfig(ServletConfig config) {
      this.config = config;
    }
  }

  public void testDoGetSerializationPolicy_FailToOpenMD5Resource()
      throws ServletException {
    MockServletConfig mockConfig = new MockServletConfig();
    MockServletContext mockContext = new MockServletContext() {
      public InputStream getResourceAsStream(String resource) {
        return null;
      }
    };
    mockConfig.context = mockContext;
    mockContext.config = mockConfig;

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    rss.init(mockConfig);

    mockRequest.contextPath = "/MyModule";

    SerializationPolicy serializationPolicy = rss.doGetSerializationPolicy(
        mockRequest, "http://www.google.com/MyModule", "12345");
    assertNull(serializationPolicy);
    assertNotNull(mockContext.messageLogged);
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.RemoteServiceServlet#doGetSerializationPolicy(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)}.
   * 
   * This method tests that if the module path is in a different context than
   * the RemoteServiceServlet which is processing the request, a message will be
   * logged and null is returned for the SerializationPolicy.
   */
  public void testDoGetSerializationPolicy_ModuleInSeparateServlet()
      throws ServletException {
    MockServletConfig mockConfig = new MockServletConfig();
    MockServletContext mockContext = new MockServletContext();
    mockConfig.context = mockContext;
    mockContext.config = mockConfig;

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    rss.init(mockConfig);

    mockRequest.contextPath = "/foo";
    SerializationPolicy serializationPolicy = rss.doGetSerializationPolicy(
        mockRequest, "http://www.google.com/MyModule", "");
    assertNotNull(mockContext.messageLogged);
    assertNull(serializationPolicy);
  }

  /**
   * Test method for
   * {@link com.google.gwt.user.server.rpc.RemoteServiceServlet#doGetSerializationPolicy(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)}.
   * 
   * This method tests the success case. The resource exists and is in the same
   * path at the web application.
   */
  public void testDoGetSerializationPolicy_Success() throws ServletException,
      SerializationException {
    final String resourceHash = "12345";
    final String resourcePath = SerializationPolicyLoader.getSerializationPolicyFileName(resourceHash);
    MockServletConfig mockConfig = new MockServletConfig();
    MockServletContext mockContext = new MockServletContext() {
      public InputStream getResourceAsStream(String resource) {
        if (resourcePath.equals(resource)) {
          try {
            String payLoad = Foo.class.getName() + ",true\n"
                + Bar.class.getName() + ",false\n";
            return new ByteArrayInputStream(
                payLoad.getBytes(SerializationPolicyLoader.SERIALIZATION_POLICY_FILE_ENCODING));
          } catch (UnsupportedEncodingException e) {
            return null;
          }
        }

        return null;
      }
    };
    mockConfig.context = mockContext;
    mockContext.config = mockConfig;

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    rss.init(mockConfig);

    mockRequest.contextPath = "/MyModule";

    SerializationPolicy serializationPolicy = rss.doGetSerializationPolicy(
        mockRequest, "http://www.google.com/MyModule", resourceHash);
    assertNotNull(serializationPolicy);

    assertDeserializeFields(serializationPolicy, Foo.class);
    assertValidDeserialize(serializationPolicy, Foo.class);

    assertDeserializeFields(serializationPolicy, Bar.class);
    assertNotValidDeserialize(serializationPolicy, Bar.class);

    assertNotDeserializeFields(serializationPolicy, Baz.class);
    assertNotValidDeserialize(serializationPolicy, Baz.class);
  }

  private void assertDeserializeFields(SerializationPolicy policy, Class clazz) {
    assertTrue(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotDeserializeFields(SerializationPolicy policy,
      Class clazz) {
    assertFalse(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotValidDeserialize(SerializationPolicy policy, Class clazz) {
    try {
      policy.validateDeserialize(clazz);
      fail("assertNotValidDeserialize: " + clazz.getName()
          + " failed to throw an exception");
    } catch (SerializationException e) {
      // expected
    }
  }

  private void assertValidDeserialize(SerializationPolicy policy, Class clazz)
      throws SerializationException {
    policy.validateDeserialize(clazz);
  }
}
