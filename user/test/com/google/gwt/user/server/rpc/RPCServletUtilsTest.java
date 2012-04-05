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

import com.google.gwt.user.client.rpc.UnicodeEscapingTest;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

/**
 * Tests some of the methods in {@link RPCServletUtils}.
 */
public class RPCServletUtilsTest extends TestCase {

  /**
   * Mocks a request with the specified Content-Type.
   */
  class MockReqContentType extends MockHttpServletRequest {
    final String mockContent;
    final String mockContentType;

    public MockReqContentType(String contentType) {
      this(contentType, "abcdefg");
    }

    public MockReqContentType(String contentType, String content) {
      this.mockContentType = contentType;
      this.mockContent = content;
    }

    @Override
    public String getCharacterEncoding() {
      return "utf-8";
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
      return null;
    }

    @SuppressWarnings("unused")
    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new MockServletInputStream(mockContent);
    }
  }

  static class MockServletInputStream extends ServletInputStream {
    private ByteArrayInputStream realStream;

    MockServletInputStream(String mockContent) throws UnsupportedEncodingException {
      realStream = new ByteArrayInputStream(mockContent.getBytes("UTF-8"));
    }

    @Override
    public int read() throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return realStream.read(b, off, len);
    }
  }

  /**
   * Large content length should be read correctly.
   */
  public void testContentLengthLarge() throws IOException, ServletException {
    // Choose a non trivial size RPC payload
    int contentLength = 50000;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * Content length smaller than the buffer size should be read correctly.
   */
  public void testContentLengthLessThanBufferSize() throws IOException, ServletException {
    // Choose a value smaller than the buffer
    int contentLength = RPCServletUtils.BUFFER_SIZE - 1;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * Content length which is an integer multiple of buffer size should be read
   * correctly.
   */
  public void testContentLengthMultipleOfBufferSize() throws IOException, ServletException {
    // Choose a value which is not a multiple of the buffer size
    int contentLength = RPCServletUtils.BUFFER_SIZE * 3;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * Content length which is not an integer multiple of buffer size should be
   * read correctly.
   */
  public void testContentLengthNotMultipleOfBufferSize() throws IOException, ServletException {
    // Choose a value which is not a multiple of the buffer size
    int contentLength = RPCServletUtils.BUFFER_SIZE * 3 + 1;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * Content length smaller than the buffer size should be read correctly.
   */
  public void testContentLengthSlightlyLargerThanBufferSize() throws IOException, ServletException {
    // Choose a value slightly larger than the buffer
    int contentLength = RPCServletUtils.BUFFER_SIZE + 1;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * Zero content length is never expected, but being able to correctly read
   * zero length content is a useful boundary condition test.
   */
  public void testContentLengthZero() throws IOException, ServletException {
    // While zero content length is not actually useful, a test
    int contentLength = 0;
    String content = UnicodeEscapingTest.getStringContainingCharacterRange(0, contentLength);
    String result = readContentAsUtf8(content);
    assertEquals(content, result);
  }

  /**
   * RPCServletUtils#getCharset() should return the same instance for
   * every invocation of a given encoding.
   */
  public void testGetCharsetInstances() {
    // Default UTF-8 character set.
    assertSame(RPCServletUtils.getCharset(null),
        RPCServletUtils.getCharset(null));
    assertSame(RPCServletUtils.getCharset("UTF-8"),
        RPCServletUtils.getCharset("UTF-8"));
    assertSame(RPCServletUtils.getCharset("US-ASCII"),
        RPCServletUtils.getCharset("US-ASCII"));
    assertSame(RPCServletUtils.getCharset("ISO-8859-1"),
        RPCServletUtils.getCharset("ISO-8859-1"));
  }

  /**
   * Test that RPCServletUtils#getCharset() returns the correct
   * default UTF-8 charachter set when passed a null encoding value.
   */
  public void testGetDefaultCharset() {
    assertEquals(Charset.forName("UTF-8"), RPCServletUtils.CHARSET_UTF8);
    assertSame(RPCServletUtils.CHARSET_UTF8, RPCServletUtils.getCharset(null));
  }

  /**
   * Character type doesn't match UTF-8, but ignore it.
   */
  public void testIgnoreCharacterEncoding() throws IOException,
      ServletException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc") {

      @Override
      public String getCharacterEncoding() {
        return "EBCDIC-US";
      }
    };

    RPCServletUtils.readContent(m, null, null);
  }

  /**
   * Content type doesn't match x-gwt-rpc, but ignore it.
   */
  public void testIgnoreContentType() throws IOException, ServletException {
    HttpServletRequest m = new MockReqContentType("application/www-form-encoded");
    RPCServletUtils.readContent(m, null, null);
  }

  /**
   * A non UTF-8 character encoding should be rejected.
   */
  public void testReadBadCharacterEncoding() throws IOException {
    HttpServletRequest m = new MockReqContentType("text/x-gwt-rpc") {

      @Override
      public String getCharacterEncoding() {
        return "EBCDIC-US";
      }
    };
    boolean gotException = false;

    try {
      RPCServletUtils.readContentAsGwtRpc(m);
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
      RPCServletUtils.readContentAsGwtRpc(m);
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
    RPCServletUtils.readContentAsGwtRpc(m);
  }

  /**
   * Content-Type validation should ignore case.
   */
  public void testReadGoodContentTypeIgnoreCase()
      throws IOException, ServletException {
    HttpServletRequest m = new MockReqContentType("tExt/X-gwt-rPc");
    RPCServletUtils.readContentAsGwtRpc(m);
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
      RPCServletUtils.readContentAsGwtRpc(m);
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
      RPCServletUtils.readContentAsGwtRpc(m);
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

  private String readContentAsUtf8(String content) throws IOException, ServletException {
    HttpServletRequest m = new MockReqContentType(null, content);
    // ignore Content-Type, read as UTF-8
    return RPCServletUtils.readContent(m, null, null);
  }
}
