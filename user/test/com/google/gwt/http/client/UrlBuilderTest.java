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
package com.google.gwt.http.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Test Case for {@link UrlBuilder}.
 */
public class UrlBuilderTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.http.HttpSuite";
  }

  /**
   * Test that the URL is encoded correctly.
   */
  public void testBuildStringEncode() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");
    builder.setPath("path to file");
    builder.setParameter("the key", "the value");
    assertEquals("http://google.com/path%20to%20file?the+key=the+value",
        builder.buildString());

    builder = new UrlBuilder();
    builder.setHost("google.com");
    builder.setPath("path");
    builder.setHash("hash");

    builder.setParameter("a_b", "a+b");
    assertEquals("http://google.com/path?a_b=a%2Bb#hash",
                 builder.buildString());

    builder.setParameter("a_b", "a&b");
    assertEquals("http://google.com/path?a_b=a%26b#hash",
                 builder.buildString());

    builder.setParameter("a_b", "a%b");
    assertEquals("http://google.com/path?a_b=a%25b#hash",
                 builder.buildString());
  }

  public void testBuildStringEntireUrl() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Host only.
    assertEquals("http://google.com", builder.buildString());

    // Host:Port
    builder.setPort(100);
    assertEquals("http://google.com:100", builder.buildString());

    // Host:Port/Path
    builder.setPath("path/to/file");
    assertEquals("http://google.com:100/path/to/file", builder.buildString());

    // Host:Port/Path?Param
    builder.setParameter("key", "value");
    assertEquals("http://google.com:100/path/to/file?key=value",
        builder.buildString());

    // Host:Port/Path?Param#Hash
    builder.setHash("token");
    assertEquals("http://google.com:100/path/to/file?key=value#token",
        builder.buildString());
  }

  public void testBuildStringEntireUrlWithReturns() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com").setPort(100).setPath("path/to/file").setParameter(
        "key", "value").setHash("token");
    assertEquals("http://google.com:100/path/to/file?key=value#token",
        builder.buildString());
  }

  public void testBuildStringParts() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Host only.
    assertEquals("http://google.com", builder.buildString());

    // Host:Port
    builder.setPort(100);
    assertEquals("http://google.com:100", builder.buildString());
    builder.setPort(UrlBuilder.PORT_UNSPECIFIED);

    // Host/Path
    builder.setPath("path/to/file");
    assertEquals("http://google.com/path/to/file", builder.buildString());
    builder.setPath(null);

    // Host?Param
    builder.setParameter("key", "value");
    assertEquals("http://google.com?key=value", builder.buildString());
    builder.removeParameter("key");

    // Host#Hash
    builder.setHash("token");
    assertEquals("http://google.com#token", builder.buildString());
    builder.setHash(null);
  }

  public void testSetHash() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Hash not specified
    assertEquals("http://google.com", builder.buildString());

    // # added if not present
    builder.setHash("myHash");
    assertEquals("http://google.com#myHash", builder.buildString());

    // Null hash
    builder.setHash(null);
    assertEquals("http://google.com", builder.buildString());

    // # not added if present
    builder.setHash("#myHash2");
    assertEquals("http://google.com#myHash2", builder.buildString());
  }

  public void testSetHost() {
    UrlBuilder builder = new UrlBuilder();

    // Host not specified.
    assertEquals("http://", builder.buildString());

    // Null host.
    builder.setHost(null);
    assertEquals("http://", builder.buildString());

    // Empty host.
    builder.setHost("");
    assertEquals("http://", builder.buildString());

    // google.com
    builder.setHost("google.com");
    assertEquals("http://google.com", builder.buildString());

    // google.com:80
    builder.setHost("google.com:80");
    assertEquals("http://google.com:80", builder.buildString());

    // google.com:80 with overridden port.
    builder.setHost("google.com:80");
    builder.setPort(1000);
    assertEquals("http://google.com:1000", builder.buildString());

    // google.com:80 with overridden port in host.
    builder.setPort(1000);
    builder.setHost("google.com:80");
    assertEquals("http://google.com:80", builder.buildString());

    // Specify to many ports.
    // google.com:80:90
    try {
      builder.setHost("google.com:80:90");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Specify invalid port.
    // google.com:test
    try {
      builder.setHost("google.com:test");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testSetParameter() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Parameters not specified.
    assertEquals("http://google.com", builder.buildString());

    // Simple parameter.
    builder.setParameter("key", "value");
    assertEquals("http://google.com?key=value", builder.buildString());

    // Remove simple parameter.
    builder.removeParameter("key");
    assertEquals("http://google.com", builder.buildString());

    // List parameter.
    List<String> values = new ArrayList<String>();
    builder.setParameter("key", "value0", "value1", "value2");
    assertEquals("http://google.com?key=value0&key=value1&key=value2",
        builder.buildString());

    // Remove list parameter.
    builder.removeParameter("key");
    assertEquals("http://google.com", builder.buildString());

    // Multiple parameters.
    builder.setParameter("key0", "value0", "value1", "value2");
    builder.setParameter("key1", "simpleValue");

    // The order of query params is not defined, so either URL is acceptable.
    String url = builder.buildString();
    assertTrue(url.equals("http://google.com?key0=value0&key0=value1&key0=value2&key1=simpleValue")
        || url.equals("http://google.com?key1=simpleValue&key0=value0&key0=value1&key0=value2"));

    // Empty list of multiple parameters.
    builder.setParameter("key0", "value0", "value1", "value2");
    builder.setParameter("key1", "simpleValue");
    assertTrue(url.equals("http://google.com?key0=value0&key0=value1&key0=value2&key1=simpleValue")
        || url.equals("http://google.com?key1=simpleValue&key0=value0&key0=value1&key0=value2"));
  }

  public void testSetParameterToNull() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    try {
      builder.setParameter(null, "value");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      builder.setParameter(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      builder.setParameter("key", new String[0]);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      builder.setParameter("key", (String[]) null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Null values are okay.
    builder.setParameter("key", (String) null);
    assertEquals("http://google.com?key=", builder.buildString());
  }

  public void testSetPath() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Path not specified.
    assertEquals("http://google.com", builder.buildString());

    // Null path.
    builder.setPath(null);
    assertEquals("http://google.com", builder.buildString());

    // Empty path.
    builder.setPath("");
    assertEquals("http://google.com", builder.buildString());

    // path/to/file.html
    builder.setPath("path/to/file.html");
    assertEquals("http://google.com/path/to/file.html", builder.buildString());

    // /path/to/file.html
    builder.setPath("/path/to/file.html");
    assertEquals("http://google.com/path/to/file.html", builder.buildString());
  }

  public void testSetPort() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Port not specified.
    assertEquals("http://google.com", builder.buildString());

    // Port 1000.
    builder.setPort(1000);
    assertEquals("http://google.com:1000", builder.buildString());

    // PORT_UNSPECIFIED.
    builder.setPort(UrlBuilder.PORT_UNSPECIFIED);
    assertEquals("http://google.com", builder.buildString());
  }

  public void testSetProtocol() {
    UrlBuilder builder = new UrlBuilder();
    builder.setHost("google.com");

    // Protocol not specified.
    assertEquals("http://google.com", builder.buildString());

    // Null host.
    try {
      builder.setProtocol(null);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // Empty host.
    try {
      builder.setProtocol("");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    // ftp
    builder.setProtocol("ftp");
    assertEquals("ftp://google.com", builder.buildString());

    // tcp:
    builder.setProtocol("tcp:");
    assertEquals("tcp://google.com", builder.buildString());

    // http:/
    builder.setProtocol("http:/");
    assertEquals("http://google.com", builder.buildString());

    // http://
    builder.setProtocol("http://");
    assertEquals("http://google.com", builder.buildString());
  }
}
