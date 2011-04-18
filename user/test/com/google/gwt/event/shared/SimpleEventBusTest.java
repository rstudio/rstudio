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

package com.google.gwt.event.shared;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent.Type;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.testing.CountingEventBus;

import junit.framework.AssertionFailedError;

import java.util.Set;

/**
 * Eponymous unit test.
 */
public class SimpleEventBusTest extends HandlerTestBase {

  public void testAddAndRemoveHandlers() {
    CountingEventBus eventBus = new CountingEventBus(new SimpleEventBus());
    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    HandlerRegistration reg1 = eventBus.addHandler(MouseDownEvent.getType(),
        adaptor1);
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertEquals(3, eventBus.getCount(MouseDownEvent.getType()));
    assertFired(mouse1, mouse2, adaptor1);
    eventBus.addHandler(MouseDownEvent.getType(), mouse3);
    assertEquals(4, eventBus.getCount(MouseDownEvent.getType()));

    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    HandlerRegistration reg2 = eventBus.addHandler(MouseDownEvent.getType(),
        adaptor1);

    /*
     * You can indeed add handlers twice, they will only be removed one at a
     * time though.
     */
    assertEquals(7, eventBus.getCount(MouseDownEvent.getType()));
    eventBus.addHandler(ClickEvent.getType(), adaptor1);
    eventBus.addHandler(ClickEvent.getType(), click1);
    eventBus.addHandler(ClickEvent.getType(), click2);

    assertEquals(7, eventBus.getCount(MouseDownEvent.getType()));
    assertEquals(3, eventBus.getCount(ClickEvent.getType()));

    reset();
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3, adaptor1);
    assertNotFired(click1, click2);

    // Gets rid of first instance.
    reg1.removeHandler();
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3, adaptor1);
    assertNotFired(click1, click2);

    // Gets rid of second instance.
    reg2.removeHandler();
    reset();
    eventBus.fireEvent(new MouseDownEvent() {
    });

    assertFired(mouse1, mouse2, mouse3);
    assertNotFired(adaptor1, click1, click2);

    // Checks to see if click events are still working.
    reset();
    eventBus.fireEvent(new ClickEvent() {
    });

    assertNotFired(mouse1, mouse2, mouse3);
    assertFired(click1, click2, adaptor1);
  }

  public void testConcurrentAdd() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    final MouseDownHandler two = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        add(this);
      }
    };
    MouseDownHandler one = new MouseDownHandler() {
      public void onMouseDown(MouseDownEvent event) {
        eventBus.addHandler(MouseDownEvent.getType(), two);
        add(this);
      }
    };
    eventBus.addHandler(MouseDownEvent.getType(), one);
    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    eventBus.addHandler(MouseDownEvent.getType(), mouse3);
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);

    reset();
    eventBus.fireEvent(new MouseDownEvent() {
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
    final SimpleEventBus eventBus = new SimpleEventBus();

    ShyHandler h = new ShyHandler();

    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    h.r = eventBus.addHandler(MouseDownEvent.getType(), h);
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    eventBus.addHandler(MouseDownEvent.getType(), mouse3);

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(h, mouse1, mouse2, mouse3);
    reset();
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(mouse1, mouse2, mouse3);
    assertNotFired(h);
  }

  class SourcedHandler implements MouseDownHandler {
    final String expectedSource;

    SourcedHandler(String source) {
      this.expectedSource = source;
    }

    public void onMouseDown(MouseDownEvent event) {
      add(this);
      assertEquals(expectedSource, event.getSource());
    }
  }

  public void testAssertThrowsNpe() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    try {
      assertThrowsNpe(new ScheduledCommand() {
        public void execute() {
          eventBus.addHandler(MouseDownEvent.getType(), mouse1);
        }
      });
      fail("expected AssertionFailedError");
    } catch (AssertionFailedError e) {
      /* pass */
    }
  }

  public void testNullChecks() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    final Type<MouseDownHandler> type = MouseDownEvent.getType();

    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.addHandler(null, mouse1);
      }
    });

    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.addHandlerToSource(type, "foo", null);
      }
    });
    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.addHandlerToSource(type, null, mouse1);
      }
    });
    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.addHandlerToSource(null, "foo", mouse1);
      }
    });

    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.fireEvent(null);
      }
    });

    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.fireEventFromSource(null, "");
      }
    });

    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.fireEventFromSource(new MouseDownEvent() {
        }, null);
      }
    });
    assertThrowsNpe(new ScheduledCommand() {
      public void execute() {
        eventBus.fireEventFromSource(null, "baker");
      }
    });
  }

  private void assertThrowsNpe(ScheduledCommand command) {
    try {
      command.execute();
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      /* pass */
    }
  }

  public void testFromSource() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    SourcedHandler global = new SourcedHandler("able");
    SourcedHandler able = new SourcedHandler("able");
    SourcedHandler baker = new SourcedHandler("baker");

    eventBus.addHandler(MouseDownEvent.getType(), global);
    eventBus.addHandlerToSource(MouseDownEvent.getType(), "able", able);
    eventBus.addHandlerToSource(MouseDownEvent.getType(), "baker", baker);

    eventBus.fireEventFromSource(new MouseDownEvent() {
    }, "able");
    assertFired(global, able);
    assertNotFired(baker);
  }

  public void testNoSource() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    SourcedHandler global = new SourcedHandler(null);
    SourcedHandler able = new SourcedHandler("able");
    SourcedHandler baker = new SourcedHandler("baker");

    eventBus.addHandler(MouseDownEvent.getType(), global);
    eventBus.addHandlerToSource(MouseDownEvent.getType(), "able", able);
    eventBus.addHandlerToSource(MouseDownEvent.getType(), "baker", baker);

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(global);
    assertNotFired(able, baker);
  }

  public void testConcurrentAddAndRemoveByNastyUsersTryingToHurtUs() {
    final SimpleEventBus eventBus = new SimpleEventBus();
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
        eventBus.addHandler(MouseDownEvent.getType(), two).removeHandler();
        add(this);
      }

      @Override
      public String toString() {
        return "one";
      }
    };
    eventBus.addHandler(MouseDownEvent.getType(), one);
    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    eventBus.addHandler(MouseDownEvent.getType(), mouse3);
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);

    reset();
    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1, mouse2, mouse3);
    assertNotFired(two);
  }

  public void testRemoveSelf() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    MouseDownHandler h = new MouseDownHandler() {
      HandlerRegistration reg = eventBus.addHandler(MouseDownEvent.getType(),
          this);

      public void onMouseDown(MouseDownEvent event) {
        add(this);
        reg.removeHandler();
      }
    };

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(h);

    reset();

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertNotFired(h);
  }

  public void testNoDoubleRemove() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    HandlerRegistration reg = eventBus.addHandler(MouseDownEvent.getType(),
        mouse1);
    reg.removeHandler();

    boolean assertsOn = getClass().desiredAssertionStatus();

    if (assertsOn) {
      try {
        reg.removeHandler();
        fail("Should have thrown on remove");
      } catch (AssertionError e) { /* pass */
      }
    } else {
      reg.removeHandler();
      // Succeed on no assert failure
    }
  }

  public void testConcurrentAddAfterRemoveIsNotClobbered() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    MouseDownHandler one = new MouseDownHandler() {
      HandlerRegistration reg = addIt();

      public void onMouseDown(MouseDownEvent event) {
        reg.removeHandler();
        addIt();
        add(this);
      }

      private HandlerRegistration addIt() {
        return eventBus.addHandler(MouseDownEvent.getType(), mouse1);
      }
    };

    eventBus.addHandler(MouseDownEvent.getType(), one);

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(one);

    reset();

    eventBus.fireEvent(new MouseDownEvent() {
    });
    assertFired(one, mouse1);
  }

  public void testReverseOrder() {
    @SuppressWarnings("deprecation")
    final SimpleEventBus eventBus = new SimpleEventBus(true);
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
    eventBus.addHandler(MouseDownEvent.getType(), handler0);
    eventBus.addHandler(MouseDownEvent.getType(), handler1);
    eventBus.addHandler(MouseDownEvent.getType(), handler2);

    reset();
    eventBus.fireEvent(new MouseDownEvent() {
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

    final SimpleEventBus eventBus = new SimpleEventBus();

    eventBus.addHandler(MouseDownEvent.getType(), mouse1);
    eventBus.addHandler(MouseDownEvent.getType(), new ThrowingHandler(
        exception1));
    eventBus.addHandler(MouseDownEvent.getType(), mouse2);
    eventBus.addHandler(MouseDownEvent.getType(), new ThrowingHandler(
        exception2));
    eventBus.addHandler(MouseDownEvent.getType(), mouse3);

    MouseDownEvent event = new MouseDownEvent() {
    };

    try {
      eventBus.fireEvent(event);
      fail("eventBus should have thrown");
    } catch (UmbrellaException e) {
      Set<Throwable> causes = e.getCauses();
      assertEquals("Exception should wrap the two thrown exceptions", 2,
          causes.size());
      assertTrue("First exception should be under the umbrella",
          causes.contains(exception1));
      assertTrue("Second exception should be under the umbrella",
          causes.contains(exception2));
    }

    /*
     * Exception should not have prevented all three mouse handlers from getting
     * the event.
     */
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
