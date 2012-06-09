/*
 * Copyright 2010 Google Inc.
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
package elemental.html;

import static elemental.client.Browser.getDocument;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Tests for HTMLElement.
 */
public class ElementTest extends GWTTestCase {

  private Element btn;

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests that addEventListener() actually fires events.
   */
  public void testEventListener() {
    final boolean[] clicked = new boolean[1];
    btn.addEventListener("click", new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        clicked[0] = true;
      }
    }, false);

    TestUtils.click(btn);
    assertTrue(clicked[0]);
  }
  
  /**
   * Tests {@link Element#hasClassName(String)}.
   */
  public void testHasClassName() {
    final Element e = btn;
    e.setClassName("jimmy crack corn");
    assertTrue(e.hasClassName("jimmy"));
    assertTrue(e.hasClassName("crack"));
    assertTrue(e.hasClassName("corn"));
    assertFalse(e.hasClassName("jim"));
    assertFalse(e.hasClassName("popcorn"));   

    e.setClassName("turtles");
    assertTrue(e.hasClassName("turtles"));
  }

  /**
   * Tests that setting Element.onclick actually fires events.
   */
  public void testOnClick() {
    final boolean[] clicked = new boolean[1];
    EventListener listener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        clicked[0] = true;
      }
    };
    btn.setOnClick(listener);

    TestUtils.click(btn);
    assertTrue(clicked[0]);
    assertEquals(listener, btn.getOnClick());
  }

  /**
   * Tests that the {@link UncaughtExceptionHandler} gets called correctly when
   * events are fired from {@link Element#setOnClick(EventListener)}.
   */
  public void testUncaughtException() {
    // Create a button with an event handler that will throw an exception.
    Document doc = Browser.getDocument();
    ButtonElement btn = doc.createButtonElement();
    doc.getBody().appendChild(btn);

    btn.setOnClick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        throw new RuntimeException("w00t!");
      }
    });

    // Setup the UncaughtExceptionHandler.
    final Throwable[] ex = new Throwable[1];
    GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void onUncaughtException(Throwable e) {
        ex[0] = e;
      }
    });

    // Click it and make sure the exception got caught.
    TestUtils.click(btn);
    assertNotNull(ex[0]);
    assertEquals("w00t!", ex[0].getMessage());

    // Clean up.
    GWT.setUncaughtExceptionHandler(null);
    doc.getBody().removeChild(btn);
  }

  @Override
  protected void gwtSetUp() throws Exception {
    btn = getDocument().createElement("button");
    getDocument().getBody().appendChild(btn);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    getDocument().getBody().removeChild(btn);
  }
}
