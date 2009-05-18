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

import junit.framework.TestCase;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

/**
 * Tests some of the methods in {@link RPCServletUtils}.
 * 
 */
public class RPCServletUtilsTest extends TestCase {

  /**
   * Mocks a request with the specified Content-Type.
   */
  class MockReqContentType extends MockHttpServletRequest {
    String mockContent = "abcdefg";
    final String mockContentType;

    public MockReqContentType(String contentType) {
      this.mockContentType = contentType;
    }

    @Override
    public String getCharacterEncoding() {
      return "charset=utf-8";
    }

    @Override
    public int getContentLength() {
      return mockContent.length();
    }

    @Override
    public String getContentType() {
      return mockContentType;
    }

    @Override
    public String getHeader(String name) {
      if (name.toLowerCase().equals("Content-Type")) {
        return mockContentType;
      }
      return "";
    }

    @SuppressWarnings("unused")
    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new MockServletInputStream(mockContent);
    }
  }

  static class MockServletInputStream extends ServletInputStream {
    private boolean readOnce = false;
    final private String value;

    MockServletInputStream(String value) {
      this.value = value;
    }

    @Override
    public int read() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (readOnce) {
        // simulate EOF
        return -1;
      }
      readOnce = true;

      int pos = 0;
      int i;
      for (i = off; i < len; ++i, ++pos) {
        b[i] = (byte) (this.value.charAt(pos) % 0xff);
      }
      return i;
    }

    @Override
    public int readLine(byte[] b, int off, int len) throws IOException {
      return read(b, off, len);
    }
  }

  /**
   * Content type doesn't match x-gwt-rpc, but ignore it.
   */
  public void testIgnoreContentType() throws IOException, ServletException {
    HttpServletRequest m = new MockReqContentType(
        "application/www-form-encoded");
    RPCServletUtils.readContentAsUtf8(m, false);
  }

  /**
   * Character type doesn't match UTF-8, but ignore it.
   */
  public void testIgnoreCharacterEncoding() throws IOException,
      ServletException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc") {

      @Override
      public String getCharacterEncoding() {
        return "charset=EBCDIC-US";
      }
    };

    RPCServletUtils.readContentAsUtf8(m, false);
  }

  /**
   * A non UTF-8 character encoding should be rejected.
   */
  public void testReadBadCharacterEncoding() throws IOException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc") {

      @Override
      public String getCharacterEncoding() {
        return "charset=EBCDIC-US";
      }
    };
    boolean gotException = false;

    try {
      RPCServletUtils.readContentAsUtf8(m);
    } catch (ServletException se) {
      if (se.getMessage().indexOf("Character Encoding") != 0) {
        fail(" Unexpected exception " + se);
      }
      gotException = true;
    }

    if (!gotException) {
      fail("Expected exception from illegal character encoding");
    }
  }

  /**
   * Implement a content type other than text/x-gwt-rpc.
   */
  public void testReadBadContentType() throws IOException {
    HttpServletRequest m = new MockReqContentType(
        "application/www-form-encoded");
    boolean gotException = false;
    try {
      RPCServletUtils.readContentAsUtf8(m);
    } catch (ServletException se) {
      if (se.getMessage().indexOf("Content-Type") != 0) {
        fail(" Unexpected exception " + se);
      }
      gotException = true;
    }
    if (!gotException) {
      fail("Expected exception from illegal content type");
    }
  }

  /**
   * Implement a test that returns content-type text/x-gwt-rpc.
   */
  public void testReadGoodContentType() throws IOException, ServletException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc");
    RPCServletUtils.readContentAsUtf8(m);
  }

  /**
   * A null character encoding should be rejected.
   */
  public void testReadNullCharacterEncoding() throws IOException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc") {

      @Override
      public String getCharacterEncoding() {
        return null;
      }
    };
    boolean gotException = false;
    try {
      RPCServletUtils.readContentAsUtf8(m);
    } catch (ServletException se) {
      if (se.getMessage().indexOf("Character Encoding") != 0) {
        fail(" Unexpected exception " + se);
      }
      gotException = true;
    }
    if (!gotException) {
      fail("Expected exception from null character encoding");
    }
  }

  /**
   * A null content type should be rejected.
   */
  public void testReadNullContentType() throws IOException {
    HttpServletRequest m = new MockReqContentType(null);
    boolean gotException = false;
    try {
      RPCServletUtils.readContentAsUtf8(m);
    } catch (ServletException se) {
      if (se.getMessage().indexOf("Content-Type") != 0) {
        fail(" Unexpected exception " + se);
      }
      gotException = true;
    }
    if (!gotException) {
      fail("Expected exception from null content type");
    }
  }
}
