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
package com.google.gwt.junit;

import com.google.gwt.junit.RunStyleSelenium.RCSelenium;

import junit.framework.TestCase;

/**
 * Tests of {@link RunStyleSelenium}.
 */
public class RunStyleSeleniumTest extends TestCase {

  public void testRCSeleniumGetSpecifier() {
    RCSelenium rcs = new RCSelenium("localhost:4444/*firefox");
    assertEquals("localhost:4444/*firefox", rcs.getSpecifier());
  }

  public void testRCSeleniumParseSpecifier() {
    // Standard selenium targets.
    assertRcSeleniumComponents("localhost:4444/*firefox", "localhost", 4444,
        "*firefox");
    assertRcSeleniumComponents("localhost:4444/*iexplore", "localhost", 4444,
        "*iexplore");
    assertRcSeleniumComponents("localhost:4444/*googlechrome", "localhost",
        4444, "*googlechrome");

    // Using a remote host.
    assertRcSeleniumComponents("gwt-remote-host:1234/*firefox",
        "gwt-remote-host", 1234, "*firefox");
    assertRcSeleniumComponents("gwt.remote.google.com:4444/*firefox",
        "gwt.remote.google.com", 4444, "*firefox");
    assertRcSeleniumComponents("127.0.0.1:4444/*firefox", "127.0.0.1", 4444,
        "*firefox");

    // Specifying path to executable.
    assertRcSeleniumComponents("localhost:4444/*firefox /usr/bin/firefox",
        "localhost", 4444, "*firefox /usr/bin/firefox");
    assertRcSeleniumComponents(
        "localhost:4444/*iexplore c:\\Program Files\\iexplore.exe",
        "localhost", 4444, "*iexplore c:\\Program Files\\iexplore.exe");
    assertRcSeleniumComponents("localhost:4444/madeup /usr/bin/madeup",
        "localhost", 4444, "madeup /usr/bin/madeup");
    assertRcSeleniumComponents(
        "localhost:4444/complexpath c:\\Complex(Path)~!@*&^%$#\\to\\browser.cmd",
        "localhost", 4444,
        "complexpath c:\\Complex(Path)~!@*&^%$#\\to\\browser.cmd");
  }

  public void testRCSeleniumParseSpecifierIllegal() {
    // Invalid host name.
    assertRcSeleniumIllegalSpecifier(":4444/*firefox");
    assertRcSeleniumIllegalSpecifier("with space:4444/*firefox");
    assertRcSeleniumIllegalSpecifier("front/slash:4444/*firefox");

    // Invalid port.
    assertRcSeleniumIllegalSpecifier("localhost:/*firefox");
    assertRcSeleniumIllegalSpecifier("localhost:abc/*firefox");

    // Invalid browser.
    assertRcSeleniumIllegalSpecifier("localhost:4444");
    assertRcSeleniumIllegalSpecifier("localhost:4444/");
  }

  /**
   * Assert that the {@link RCSelenium} created by the given specifier is parsed
   * to the specified browser, host, and port.
   * 
   * @param specifier the remote specifier
   * @param host the expected host
   * @param port the expected port
   * @param browser the expected browser
   */
  private void assertRcSeleniumComponents(String specifier, String host,
      int port, String browser) {
    RCSelenium rcs = new RCSelenium(specifier);
    assertEquals(host, rcs.host);
    assertEquals(port, rcs.port);
    assertEquals(browser, rcs.browser);
  }

  /**
   * Assert that the specifier is invalid.
   * 
   * @param specifier the specifier.
   */
  private void assertRcSeleniumIllegalSpecifier(String specifier) {
    try {
      new RCSelenium(specifier);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected.
    }
  }
}
