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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link RootPanel}.
 */
public class RootPanelTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Ensures that {@link RootPanel#get(String)} behaves properly.
   */
  public void testGetById() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();
    doc.getBody().appendChild(div);
    div.setInnerHTML("<div id='a'></div><div id='b'></div>");

    // You should get the same RootPanel for subsequent calls to get() with the
    // same id. But you should get *different* RootPanels for calls with
    // different ids.
    RootPanel aRoot = RootPanel.get("a");
    RootPanel bRoot = RootPanel.get("b");

    assertSame(
        "RootPanel.get() should return the same instancefor the same id",
        aRoot, RootPanel.get("a"));
    assertSame(
        "RootPanel.get() should return the same instancefor the same id",
        bRoot, RootPanel.get("b"));
    assertNotSame("RootPanels a and b should be different", aRoot, bRoot);

    // If a RootPanel's element is replaced in the DOM, you should get a
    // new RootPanel instance if you ask for it again (see issue 1937).
    Element aElem = doc.getElementById("a");
    Element newAElem = doc.createDivElement();
    newAElem.setId("a");
    aElem.getParentElement().replaceChild(newAElem, aElem);

    RootPanel newARoot = RootPanel.get("a");
    assertNotSame("New RootPanel should not be same as old", newARoot, aRoot);
  }
}
