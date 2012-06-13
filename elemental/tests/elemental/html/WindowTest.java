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
import static elemental.client.Browser.getWindow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.events.Event;
import elemental.events.EventListener;

/**
 * Tests for Window.
 */
public class WindowTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "elemental.Elemental";
  }

  /**
   * Tests Window.addEventListener() catches events from the body.
   */
  public void testEventListener() {
    final boolean[] clicked = new boolean[1];
    getWindow().addEventListener("click", new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        clicked[0] = true;
      }
    }, false);
    TestUtils.click(getDocument().getBody());
    assertTrue(clicked[0]);
  }

  /**
   * Tests Window.getSelection().
   * TODO(knorton): Expand this into a more complete test.
   */
  public void testGetSelection() {
    final Window window = getWindow();
    final DOMSelection selection = window.getSelection();
    assertNotNull(selection);
  }

  /**
   * Tests that Window.open() and Window.clearOpener().
   */
  public void testOpener() {
    final Window window = getWindow();
    final Window proxy = window.open("about:blank");
    assertNotNull(proxy.getOpener());
    proxy.clearOpener();
    assertNull(proxy.getOpener());
    proxy.close();
  }

  /**
   * Tests that Window.setTimeout() works.
   */
  public void testTimeout() {
    delayTestFinish(1000);
    getWindow().setTimeout(new Window.TimerCallback() {
      @Override
      public void fire() {
        finishTest();        
      }
    }, 500);
  }

  /**
   * Tests that Window.setInterval() works repeatedly.
   */
  public void testInterval() {
    final int[] handle = new int[1];
    Window.TimerCallback listener = new Window.TimerCallback() {
      int count;
      @Override
      public void fire() {
        // Make sure we see at least two events.
        ++count;
        if (count >= 2) {
          getWindow().clearInterval(handle[0]);
          finishTest();
        }
      }
    };

    delayTestFinish(1000);
    handle[0] = getWindow().setInterval(listener, 100);
  }

  /**
   * Tests that the {@link UncaughtExceptionHandler} gets called correctly when
   * setTimeout() and setInterval() throw exceptions.
   */
  public void testUncaughtException() {
    // Setup an UncaughtExceptionHandler to catch exceptions from setTimeout()
    // and setInterval().
    final Throwable[] ex = new Throwable[2];
    GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      int count;
      @Override
      public void onUncaughtException(Throwable e) {
        ex[count++] = e;
      }
    });

    // Set a timeout and an interval, both of which will throw a RuntimException.
    getWindow().setTimeout(new Window.TimerCallback() {
      @Override
      public void fire() {
        throw new RuntimeException("w00t!");
      }
    }, 1);

    final int[] intervalHandle = new int[1];
    intervalHandle[0] = getWindow().setInterval(new Window.TimerCallback() {
      @Override
      public void fire() {
        // We only want this to happen once, so clear the interval timer on the
        // first fire.
        getWindow().clearInterval(intervalHandle[0]);
        throw new RuntimeException("w00t!");
      }
    }, 1);

    // Wait for the test to finish asynchronously, and setup another timer to
    // check that the exceptions got caught (this is kind of ugly, but there's
    // no way around it if we want to test the "real" timer implementation as
    // opposed to a mock implementation.
    delayTestFinish(5000);
    getWindow().setTimeout(new Window.TimerCallback() {
      @Override
      public void fire() {
        // Assert that exceptions got caught.
        assertNotNull(ex[0]);
        assertNotNull(ex[1]);
        assertEquals("w00t!", ex[0].getMessage());
        assertEquals("w00t!", ex[1].getMessage());

        // Clean up and finish.
        GWT.setUncaughtExceptionHandler(null);
        finishTest();
      }
    }, 500);
  } 
}
