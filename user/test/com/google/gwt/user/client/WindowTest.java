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
package com.google.gwt.user.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Test Case for {@link Cookies}.
 */
public class WindowTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testLocation() {
    // testing reload, replace, and assign seemed to hang our junit harness. Therefore
    // only testing subset of Location that is testable.

    // As we have no control over these values we cannot assert much about them.
    String hash = Window.Location.getHash();
    String host = Window.Location.getHost();
    String hostName = Window.Location.getHostName();
    String href = Window.Location.getHref();
    assertNull(Window.Location.getParameter("fuzzy bunny"));
    String path = Window.Location.getPath();
    String port = Window.Location.getPort();
    String protocol = Window.Location.getProtocol();
    String query = Window.Location.getQueryString();

    // Check that the sum is equal to its parts.
    assertEquals(host, hostName + ":" + port);
    assertEquals(href, protocol + "//" + host + path + query + hash);
  }

  /**
   * Tests the ability of the Window to get the client size correctly with and
   * without visible scroll bars.
   */
  public void disabledTestGetClientSize() {
    // Get the dimensions without any scroll bars
    Window.enableScrolling(false);
    final int oldClientHeight = Window.getClientHeight();
    final int oldClientWidth = Window.getClientWidth();
    assertTrue(oldClientHeight > 0);
    assertTrue(oldClientWidth > 0);

    // Compare to the dimensions with scroll bars
    Window.enableScrolling(true);
    final Label largeDOM = new Label();
    largeDOM.setPixelSize(oldClientWidth + 100, oldClientHeight + 100);
    RootPanel.get().add(largeDOM);
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        int newClientHeight = Window.getClientHeight();
        int newClientWidth = Window.getClientWidth();
        assertTrue(newClientHeight < oldClientHeight);
        assertTrue(newClientWidth < oldClientWidth);
        finishTest();
      }
    });
    delayTestFinish(200);
  }
}
