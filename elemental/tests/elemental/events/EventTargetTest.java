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
package elemental.events;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.client.Browser;
import elemental.html.ButtonElement;
import elemental.html.Document;
import elemental.html.Element;
import elemental.html.TestUtils;

/**
 * Tests for {@link EventTarget}.
 */
public class EventTargetTest extends GWTTestCase {

  private static class ListenerDidFire implements EventListener {
    private boolean didFire;
    public boolean didFire() {
      return didFire;
    }
    
    @Override
    public void handleEvent(Event evt) {
      didFire = true;
    }    
  }

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests that addEventListener() correctly adds a listener.
   */
  public void testAddEventListener() {
    final Element body = Browser.getDocument().getBody();

    final ListenerDidFire a = new ListenerDidFire();
    final ListenerDidFire b = new ListenerDidFire();
    
    // Ensure that addEventListener works.
    body.addEventListener("click", a, false);
    // Ensure that setOnClick also works.
    body.setOnClick(b);
    
    assertEquals(b, body.getOnClick());

    TestUtils.click(body);
    
    assertTrue(a.didFire());
    assertTrue(b.didFire());
  }
  
  /**
   * Tests that removeEventListener() correctly removes the listener, so that no
   * events are fired afterwards.
   */
  @SuppressWarnings("deprecation")
  public void testRemoveEventListener() {
    final Element body = Browser.getDocument().getBody();

    final ListenerDidFire listener = new ListenerDidFire();
    
    // Ensure that EventRemover works.
    body.addEventListener("click", listener, false).remove();
    TestUtils.click(body);
    assertFalse(listener.didFire());
    
    // Ensure that removeEventListener works.
    body.addEventListener("click", listener, false);
    body.removeEventListener("click", listener, false);
    TestUtils.click(body);
    assertFalse(listener.didFire());
    
    // Ensure that onclick = null works.
    body.setOnClick(listener);
    body.setOnClick(null);
    TestUtils.click(body);    
    assertFalse(listener.didFire());    
  }
  
  /**
   * Tests that the {@link UncaughtExceptionHandler} gets called correctly when
   * events are fired from a subinterface of {@link EventTarget}.
   */
  public void testUncaughtException() {
    // Create a button with an event handler that will throw an exception.
    Document doc = Browser.getDocument();
    ButtonElement btn = doc.createButtonElement();
    doc.getBody().appendChild(btn);

    btn.addEventListener(Event.CLICK, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        throw new RuntimeException("w00t!");
      }
    }, false);

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
}
