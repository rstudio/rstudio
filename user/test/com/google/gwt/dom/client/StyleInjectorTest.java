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
package com.google.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

/**
 * Tests StyleInjector by looking for effects of injected CSS on DOM elements.
 */
public class StyleInjectorTest extends GWTTestCase {

  private static final int TEST_DELAY = 1000;

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  @SuppressWarnings("deprecation")
  public void testOldMethods() {
    final DivElement elt = Document.get().createDivElement();
    elt.setId("styleInjectorTest");
    elt.setInnerHTML("Hello StyleInjector!");
    Document.get().getBody().appendChild(elt);

    StyleInjector.injectStylesheet("#styleInjectorTest {position: absolute; left: 100px; width: 50px; height 50px;}");
    StyleInjector.injectStylesheetAtStart("#styleInjectorTest {left: 25px; width: 100px !important;}");
    StyleInjector.injectStylesheetAtEnd("#styleInjectorTest {height: 100px;}");

    // We need to allow the document to be redrawn
    delayTestFinish(TEST_DELAY);

    DeferredCommand.addCommand(new Command() {
      public void execute() {
        assertEquals(100, elt.getOffsetLeft());
        assertEquals(100, elt.getClientHeight());
        assertEquals(100, elt.getClientWidth());

        finishTest();
      }
    });
  }

  /**
   * Ensure that the IE createStyleSheet compatibility code is exercised.
   */
  @SuppressWarnings("deprecation")
  public void testOldMethodsWithLotsOfStyles() {
    StyleElement[] elements = new StyleElement[100];
    for (int i = 0, j = elements.length; i < j; i++) {
      elements[i] = StyleInjector.injectStylesheet("#styleInjectorTest" + i
          + " {position: absolute; left: 100px; width: 50px; height 50px;}");
    }

    String id = "styleInjectorTest" + (elements.length - 1);
    StyleInjector.injectStylesheetAtStart("#" + id
        + " {left: 25px; width: 100px !important;}");
    StyleInjector.injectStylesheetAtEnd("#" + id + " {height: 100px;}");

    final DivElement elt = Document.get().createDivElement();
    elt.setId(id);
    elt.setInnerHTML("Hello StyleInjector!");
    Document.get().getBody().appendChild(elt);

    // We need to allow the document to be redrawn
    delayTestFinish(TEST_DELAY);

    DeferredCommand.addCommand(new Command() {
      public void execute() {
        assertEquals(100, elt.getOffsetLeft());
        assertEquals(100, elt.getClientHeight());
        assertEquals(100, elt.getClientWidth());
        finishTest();
      }
    });
  }

  /*
   * Tests against issue #879: Ensure that empty history tokens do not add
   * additional characters after the '#' symbol in the URL.
   */
  public void testStyleInjectorBatched() {
    testStyleInjector("testStyleInjectorBatched", false);
  }

  public void testStyleInjectorImmediate() {
    testStyleInjector("testStyleInjectorImmediate", true);
  }

  private void testStyleInjector(String testName, final boolean immediate) {

    final DivElement elt = Document.get().createDivElement();
    elt.setId(testName);
    elt.setInnerHTML("Hello");
    Document.get().getBody().appendChild(elt);

    StyleInjector.inject("#" + testName
        + " {position: absolute; left: 100px; width: 50px; height 50px;}",
        immediate);
    StyleInjector.injectAtStart("#" + testName
        + " {left: 25px; width: 100px !important;}", immediate);
    StyleInjector.injectAtEnd("#" + testName + " {height: 100px;}", immediate);

    Command command = new Command() {
      public void execute() {
        assertEquals(100, elt.getOffsetLeft());
        assertEquals(100, elt.getClientHeight());
        assertEquals(100, elt.getClientWidth());

        if (!immediate) {
          finishTest();
        }
      }
    };

    if (immediate) {
      command.execute();
    } else {
      DeferredCommand.addCommand(command);
      // We need to allow the BatchedCommands to execute
      delayTestFinish(TEST_DELAY);
    }
  }
}
