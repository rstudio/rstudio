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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

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

  private static class MockHttpServletRequestContextPath extends
      MockHttpServletRequest {
    private String contextPath;

    @Override
    public String getContextPath() {
      return contextPath;
    }
  }

  private static class MockServletConfig implements ServletConfig {
    private ServletContext context;

    public MockServletConfig(ServletContext context) {
      this.context = context;
    }

    public String getInitParameter(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration<String> getInitParameterNames() {
      throw new UnsupportedOperationException();
    }

    public ServletContext getServletContext() {
      return context;
    }

    public String getServletName() {
      return "MockServlet";
    }
  }

  private class MockServletContext implements ServletContext {
    private String messageLogged;

    public MockServletContext() {
    }

    public Object getAttribute(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration<String> getAttributeNames() {
      throw new UnsupportedOperationException();
    }

    public ServletContext getContext(String arg0) {
      throw new UnsupportedOperationException();
    }

    public String getContextPath() {
      throw new UnsupportedOperationException();
    }

    public String getInitParameter(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Enumeration<String> getInitParameterNames() {
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

    public Set<String> getResourcePaths(String arg0) {
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

    public Enumeration<String> getServletNames() {
      throw new UnsupportedOperationException();
    }

    public Enumeration<Servlet> getServlets() {
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
    }

    public void removeAttribute(String arg0) {
    }

    public void setAttribute(String arg0, Object arg1) {
      throw new UnsupportedOperationException();
    }

    public Dynamic addFilter(String arg0, String arg1) {
      throw new UnsupportedOperationException();
    }

    public Dynamic addFilter(String arg0, Filter arg1) {
      throw new UnsupportedOperationException();
    }

    public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
      throw new UnsupportedOperationException();
    }

    public void addListener(String arg0) {
      throw new UnsupportedOperationException();
    }

    public <T extends EventListener> void addListener(T arg0) {
      throw new UnsupportedOperationException();
    }

    public void addListener(Class<? extends EventListener> arg0) {
      throw new UnsupportedOperationException();
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
      throw new UnsupportedOperationException();
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
      throw new UnsupportedOperationException();
    }

    public javax.servlet.ServletRegistration.Dynamic addServlet(
        String arg0, Class<? extends Servlet> arg1) {
      throw new UnsupportedOperationException();
    }

    public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
      throw new UnsupportedOperationException();
    }

    public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
      throw new UnsupportedOperationException();
    }

    public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
      throw new UnsupportedOperationException();
    }

    public void declareRoles(String... arg0) {
      throw new UnsupportedOperationException();
    }

    public ClassLoader getClassLoader() {
      throw new UnsupportedOperationException();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
      throw new UnsupportedOperationException();
    }

    public int getEffectiveMajorVersion() {
      throw new UnsupportedOperationException();
    }

    public int getEffectiveMinorVersion() {
      throw new UnsupportedOperationException();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
      throw new UnsupportedOperationException();
    }

    public FilterRegistration getFilterRegistration(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
      throw new UnsupportedOperationException();
    }

    public JspConfigDescriptor getJspConfigDescriptor() {
      throw new UnsupportedOperationException();
    }

    public ServletRegistration getServletRegistration(String arg0) {
      throw new UnsupportedOperationException();
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
      throw new UnsupportedOperationException();
    }

    public SessionCookieConfig getSessionCookieConfig() {
      throw new UnsupportedOperationException();
    }

    public boolean setInitParameter(String arg0, String arg1) {
      throw new UnsupportedOperationException();
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
      throw new UnsupportedOperationException();
    }

    public String getVirtualServerName() {
      throw new UnsupportedOperationException();
    }
  }

  public void testDoGetSerializationPolicy_FailToOpenMD5Resource()
      throws ServletException {
    MockServletContext mockContext = new MockServletContext() {
      public InputStream getResourceAsStream(String resource) {
        return null;
      }
    };
    MockServletConfig mockConfig = new MockServletConfig(mockContext);

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequestContextPath mockRequest = new MockHttpServletRequestContextPath();
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
    MockServletContext mockContext = new MockServletContext();
    MockServletConfig mockConfig = new MockServletConfig(mockContext);

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequestContextPath mockRequest = new MockHttpServletRequestContextPath();
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
    MockServletConfig mockConfig = new MockServletConfig(mockContext);

    RemoteServiceServlet rss = new RemoteServiceServlet();

    MockHttpServletRequestContextPath mockRequest = new MockHttpServletRequestContextPath();
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

  private void assertDeserializeFields(SerializationPolicy policy,
      Class<?> clazz) {
    assertTrue(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotDeserializeFields(SerializationPolicy policy,
      Class<?> clazz) {
    assertFalse(policy.shouldDeserializeFields(clazz));
  }

  private void assertNotValidDeserialize(SerializationPolicy policy,
      Class<?> clazz) {
    try {
      policy.validateDeserialize(clazz);
      fail("assertNotValidDeserialize: " + clazz.getName()
          + " failed to throw an exception");
    } catch (SerializationException e) {
      // expected
    }
  }

  private void assertValidDeserialize(SerializationPolicy policy, Class<?> clazz)
      throws SerializationException {
    policy.validateDeserialize(clazz);
  }
}
