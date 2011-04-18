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

package com.google.gwt.event.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;

import java.util.Set;

/**
 * Handler manager test. Very redundant with {@link SimpleEventBusTest}, but
 * preserved to guard against regressions.
 */
public class HandlerManagerTest extends HandlerTestBase {

  public void testAddAndRemoveHandlers() {
    HandlerManager manager = new HandlerManager("bogus source");
    manager.addHandler(MouseDownEvent.getType(), mouse1);
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), adaptor1);
    manager.fireEvent(new MouseDownEvent() {
    });
    assertEquals(3, manager.getHandlerCount(MouseDownEvent.getType()));
    assertFired(mouse1, mouse2, adaptor1);
    manager.addHandler(MouseDownEvent.getType(), mouse3);
    assertEquals(4, manager.getHandlerCount(MouseDownEvent.getType()));

    manager.addHandler(MouseDownEvent.getType(), mouse1);
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), adaptor1);

    // You can indeed add handlers twice, they will only be removed one at a
    // time though.
    assertEquals(7, manager.getHandlerCount(MouseDownEvent.getType()));
    manager.addHandler(ClickEvent.getType(), adaptor1);
    manager.addHandler(ClickEvent.getType(), click1);
    manager.addHandler(ClickEvent.getType(), click2);

    assertEquals(7, manager.getHandlerCount(MouseDownEvent.getType()));
    assertEquals(3, manager.getHandlerCount(ClickEvent.getType()));

    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3, adaptor1);
    assertNotFired(click1, click2);
    // Gets rid of first instance.
    manager.removeHandler(MouseDownEvent.getType(), adaptor1);
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3, adaptor1);
    assertNotFired(click1, click2);

    // Gets rid of second instance.
    manager.removeHandler(MouseDownEvent.getType(), adaptor1);
    reset();
    manager.fireEvent(new MouseDownEvent() {
    });

    assertFired(mouse1, mouse2, mouse3);
    assertNotFired(adaptor1, click1, click2);

    // Checks to see if click events are still working.
    reset();
    manager.fireEvent(new ClickEvent() {
    });

    assertNotFired(mouse1, mouse2, mouse3);
    assertFired(click1, click2, adaptor1);
  }

  public void testConcurrentAdd() {
    final HandlerManager manager = new HandlerManager("bogus source");
    final MouseDownHandler two = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        add(this);
      }
    };
    MouseDownHandler one = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        manager.addHandler(MouseDownEvent.getType(), two);
        add(this);
      }
    };
    manager.addHandler(MouseDownEvent.getType(), one);
    manager.addHandler(MouseDownEvent.getType(), mouse1);
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), mouse3);
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);

    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, two, mouse1, mouse2, mouse3);
  }

  class ShyHandler implements MouseDownHandler {
    HandlerRegistration r;

    public void onMouseDown(MouseDownEvent event) {
      add(this);
      r.removeHandler();
    }
  }

  public void testConcurrentRemove() {
    final HandlerManager manager = new HandlerManager("bogus source");

    ShyHandler h = new ShyHandler();

    manager.addHandler(MouseDownEvent.getType(), mouse1);
    h.r = manager.addHandler(MouseDownEvent.getType(), h);
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), mouse3);

    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(h, mouse1, mouse2, mouse3);
    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3);
    assertNotFired(h);
  }

  public void testConcurrentAddAndRemoveByNastyUsersTryingToHurtUs() {
    final HandlerManager manager = new HandlerManager("bogus source");
    final MouseDownHandler two = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        add(this);
      }

      @Override
      public String toString() {
        return "two";
      }
    };
    MouseDownHandler one = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        manager.addHandler(MouseDownEvent.getType(), two).removeHandler();
        add(this);
      }

      @Override
      public String toString() {
        return "one";
      }
    };
    manager.addHandler(MouseDownEvent.getType(), one);
    manager.addHandler(MouseDownEvent.getType(), mouse1);
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), mouse3);
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);

    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);
  }

  public void testConcurrentAddAfterRemoveIsNotClobbered() {
    final HandlerManager manager = new HandlerManager("bogus source");

    MouseDownHandler one = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        manager.removeHandler(MouseDownEvent.getType(), mouse1);
        manager.addHandler(MouseDownEvent.getType(), mouse1);
        add(this);
      }
    };
    manager.addHandler(MouseDownEvent.getType(), one);

    boolean assertsOn = getClass().desiredAssertionStatus();

    if (assertsOn) {
      try {
        manager.fireEvent(new MouseDownEvent() {
        });
        fail("Should have thrown on remove");
      } catch (AssertionError e) { /* pass */
      }
      return;
    }

    // Production Mode, no asserts, so remove will quietly succeed.
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one);
    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1);
  }

  public void testMultiFiring() {

    final MouseDownEvent masterEvent = new MouseDownEvent() {
    };

    HandlerManager manager = new HandlerManager("source1");

    final HandlerManager manager2 = new HandlerManager("source2");

    manager.addHandler(MouseDownEvent.getType(), mouse1);

    manager.addHandler(MouseDownEvent.getType(), new MouseDownHandler() {

      public void onMouseDown(MouseDownEvent event) {
        manager2.fireEvent(event);
      }

    });
    manager.addHandler(MouseDownEvent.getType(), mouse3);
    manager2.addHandler(MouseDownEvent.getType(), adaptor1);
    manager2.addHandler(MouseDownEvent.getType(), new MouseDownHandler() {

      public void onMouseDown(MouseDownEvent event) {
        assertEquals("source2", event.getSource());
        assertSame(masterEvent, event);
      }

    });
    manager.addHandler(MouseDownEvent.getType(), new MouseDownHandler() {

      public void onMouseDown(MouseDownEvent event) {
        assertEquals("source1", event.getSource());
        assertSame(masterEvent, event);
      }

    });

    reset();
    manager.fireEvent(masterEvent);
    assertFired(mouse1, adaptor1, mouse3);
    assertFalse("Event should be dead", masterEvent.isLive());
  }

  // This test is disabled because it fails '-ea'
  public void notestRemoveUnhandledType() {
    final HandlerManager manager = new HandlerManager("bogus source");
    HandlerRegistration reg = manager.addHandler(MouseDownEvent.getType(),
        mouse1);
    reg.removeHandler();

    if (!GWT.isScript()) {
      try {
        reg.removeHandler();
        fail("Should have thrown assertion error");
      } catch (AssertionError e) {
        /* pass */
      }
    } else {
      reg.removeHandler();
      /* pass, we didn't hit an NPE */
    }
  }

  public void testReverseOrder() {
    // Add some handlers to a manager
    final HandlerManager manager = new HandlerManager("source1", true);
    final MouseDownHandler handler0 = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        add(this);
      }
    };
    final MouseDownHandler handler1 = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        assertNotFired(handler0);
        add(this);
      }
    };
    final MouseDownHandler handler2 = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        assertNotFired(handler0, handler1);
        add(this);
      }
    };
    manager.addHandler(MouseDownEvent.getType(), handler0);
    manager.addHandler(MouseDownEvent.getType(), handler1);
    manager.addHandler(MouseDownEvent.getType(), handler2);

    // Fire the event
    reset();
    manager.fireEvent(new MouseDownEvent() {
    });
    assertFired(handler0, handler1, handler2);
  }

  static class ThrowingHandler implements MouseDownHandler {
    private final RuntimeException e;

    public ThrowingHandler(RuntimeException e) {
      this.e = e;
    }

    public void onMouseDown(MouseDownEvent event) {
      throw e;
    }    
  }

  public void testHandlersThrow() {
    RuntimeException exception1 = new RuntimeException("first exception");
    RuntimeException exception2 = new RuntimeException("second exception");

    final HandlerManager manager = new HandlerManager("bogus source");

    manager.addHandler(MouseDownEvent.getType(), mouse1);
    manager.addHandler(MouseDownEvent.getType(), new ThrowingHandler(exception1));
    manager.addHandler(MouseDownEvent.getType(), mouse2);
    manager.addHandler(MouseDownEvent.getType(), new ThrowingHandler(exception2));
    manager.addHandler(MouseDownEvent.getType(), mouse3);

    MouseDownEvent event = new MouseDownEvent() {
    };

    try {
      manager.fireEvent(event);
      fail("Manager should have thrown");
    } catch (UmbrellaException e) {
      Set<Throwable> causes = e.getCauses();
      assertEquals("Exception should wrap the two thrown exceptions", 2, causes.size());
      assertTrue("First exception should be under the umbrella", causes.contains(exception1));
      assertTrue("Second exception should be under the umbrella", causes.contains(exception2));
    }

    // Exception should not have prevented all three mouse handlers from getting
    // the event.
    assertFired(mouse1, mouse2, mouse3);
  }
  
  public void testNullSourceOkay() {
    SimpleEventBus reg = new SimpleEventBus();
    
    MouseDownHandler handler = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        add(this);
        assertNull(event.getSource());
      }
    };
    reg.addHandler(MouseDownEvent.getType(), handler);
    reg.fireEvent(new MouseDownEvent() {
    });
    assertFired(handler);
  }
}
