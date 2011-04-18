/*
 * Copyright 2011 Google Inc.
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

package com.google.web.bindery.event.shared;

import com.google.web.bindery.event.shared.testing.CountingEventBus;

import junit.framework.AssertionFailedError;

import java.util.Set;

/**
 * Eponymous unit test.
 */
public class SimpleEventBusTest extends EventBusTestBase {

  interface Command {
    void execute();
  }

  class ShyHandler implements FooEvent.Handler {
    HandlerRegistration r;

    public void onFoo(FooEvent event) {
      add(this);
      r.removeHandler();
    }
  }

  class SourcedHandler implements FooEvent.Handler {
    final String expectedSource;

    SourcedHandler(String source) {
      this.expectedSource = source;
    }

    public void onFoo(FooEvent event) {
      add(this);
      assertEquals(expectedSource, event.getSource());
    }
  }

  static class ThrowingHandler implements FooEvent.Handler {
    private final RuntimeException e;

    public ThrowingHandler(RuntimeException e) {
      this.e = e;
    }

    public void onFoo(FooEvent event) {
      throw e;
    }
  }

  public void testAddAndRemoveHandlers() {
    CountingEventBus eventBus = new CountingEventBus(new SimpleEventBus());
    FooEvent.register(eventBus, fooHandler1);
    FooEvent.register(eventBus, fooHandler2);
    HandlerRegistration reg1 = FooEvent.register(eventBus, adaptor1);
    eventBus.fireEvent(new FooEvent());
    assertEquals(3, eventBus.getCount(FooEvent.TYPE));
    assertFired(fooHandler1, fooHandler2, adaptor1);
    FooEvent.register(eventBus, fooHandler3);
    assertEquals(4, eventBus.getCount(FooEvent.TYPE));

    FooEvent.register(eventBus, fooHandler1);
    FooEvent.register(eventBus, fooHandler2);
    HandlerRegistration reg2 = FooEvent.register(eventBus, adaptor1);

    /*
     * You can indeed add handlers twice, they will only be removed one at a
     * time though.
     */
    assertEquals(7, eventBus.getCount(FooEvent.TYPE));
    eventBus.addHandler(BarEvent.TYPE, adaptor1);
    eventBus.addHandler(BarEvent.TYPE, barHandler1);
    eventBus.addHandler(BarEvent.TYPE, barHandler2);

    assertEquals(7, eventBus.getCount(FooEvent.TYPE));
    assertEquals(3, eventBus.getCount(BarEvent.TYPE));

    reset();
    eventBus.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler2, fooHandler3, adaptor1);
    assertNotFired(barHandler1, barHandler2);

    // Gets rid of first instance.
    reg1.removeHandler();
    eventBus.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler2, fooHandler3, adaptor1);
    assertNotFired(barHandler1, barHandler2);

    // Gets rid of second instance.
    reg2.removeHandler();
    reset();
    eventBus.fireEvent(new FooEvent());

    assertFired(fooHandler1, fooHandler2, fooHandler3);
    assertNotFired(adaptor1, barHandler1, barHandler2);

    // Checks to see if barHandler events are still working.
    reset();
    eventBus.fireEvent(new BarEvent());

    assertNotFired(fooHandler1, fooHandler2, fooHandler3);
    assertFired(barHandler1, barHandler2, adaptor1);
  }

  public void testAssertThrowsNpe() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    try {
      assertThrowsNpe(new Command() {
        public void execute() {
          FooEvent.register(eventBus, fooHandler1);
        }
      });
      fail("expected AssertionFailedError");
    } catch (AssertionFailedError e) {
      /* pass */
    }
  }

  public void testConcurrentAdd() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    final FooEvent.Handler two = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        add(this);
      }
    };
    FooEvent.Handler one = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        FooEvent.register(eventBus, two);
        add(this);
      }
    };
    FooEvent.register(eventBus, one);
    FooEvent.register(eventBus, fooHandler1);
    FooEvent.register(eventBus, fooHandler2);
    FooEvent.register(eventBus, fooHandler3);
    eventBus.fireEvent(new FooEvent());
    assertFired(one, fooHandler1, fooHandler2, fooHandler3);
    assertNotFired(two);

    reset();
    eventBus.fireEvent(new FooEvent());
    assertFired(one, two, fooHandler1, fooHandler2, fooHandler3);
  }

  public void testConcurrentAddAfterRemoveIsNotClobbered() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    FooEvent.Handler one = new FooEvent.Handler() {
      HandlerRegistration reg = addIt();

      public void onFoo(FooEvent event) {
        reg.removeHandler();
        addIt();
        add(this);
      }

      private HandlerRegistration addIt() {
        return FooEvent.register(eventBus, fooHandler1);
      }
    };

    FooEvent.register(eventBus, one);

    eventBus.fireEvent(new FooEvent());
    assertFired(one);

    reset();

    eventBus.fireEvent(new FooEvent());
    assertFired(one, fooHandler1);
  }

  public void testConcurrentAddAndRemoveByNastyUsersTryingToHurtUs() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    final FooEvent.Handler two = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        add(this);
      }

      @Override
      public String toString() {
        return "two";
      }
    };
    FooEvent.Handler one = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        FooEvent.register(eventBus, two).removeHandler();
        add(this);
      }

      @Override
      public String toString() {
        return "one";
      }
    };
    FooEvent.register(eventBus, one);
    FooEvent.register(eventBus, fooHandler1);
    FooEvent.register(eventBus, fooHandler2);
    FooEvent.register(eventBus, fooHandler3);
    eventBus.fireEvent(new FooEvent());
    assertFired(one, fooHandler1, fooHandler2, fooHandler3);
    assertNotFired(two);

    reset();
    eventBus.fireEvent(new FooEvent());
    assertFired(one, fooHandler1, fooHandler2, fooHandler3);
    assertNotFired(two);
  }

  public void testConcurrentRemove() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    ShyHandler h = new ShyHandler();

    FooEvent.register(eventBus, fooHandler1);
    h.r = FooEvent.register(eventBus, h);
    FooEvent.register(eventBus, fooHandler2);
    FooEvent.register(eventBus, fooHandler3);

    eventBus.fireEvent(new FooEvent());
    assertFired(h, fooHandler1, fooHandler2, fooHandler3);
    reset();
    eventBus.fireEvent(new FooEvent());
    assertFired(fooHandler1, fooHandler2, fooHandler3);
    assertNotFired(h);
  }

  public void testFromSource() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    SourcedHandler global = new SourcedHandler("able");
    SourcedHandler able = new SourcedHandler("able");
    SourcedHandler baker = new SourcedHandler("baker");

    FooEvent.register(eventBus, global);
    FooEvent.register(eventBus, "able", able);
    FooEvent.register(eventBus, "baker", baker);

    eventBus.fireEventFromSource(new FooEvent(), "able");
    assertFired(global, able);
    assertNotFired(baker);
  }

  public void testHandlersThrow() {
    RuntimeException exception1 = new RuntimeException("first exception");
    RuntimeException exception2 = new RuntimeException("second exception");

    final SimpleEventBus eventBus = new SimpleEventBus();

    FooEvent.register(eventBus, fooHandler1);
    FooEvent.register(eventBus, new ThrowingHandler(exception1));
    FooEvent.register(eventBus, fooHandler2);
    FooEvent.register(eventBus, new ThrowingHandler(exception2));
    FooEvent.register(eventBus, fooHandler3);

    FooEvent event = new FooEvent();

    try {
      eventBus.fireEvent(event);
      fail("eventBus should have thrown");
    } catch (UmbrellaException e) {
      Set<Throwable> causes = e.getCauses();
      assertEquals("Exception should wrap the two thrown exceptions", 2, causes.size());
      assertTrue("First exception should be under the umbrella", causes.contains(exception1));
      assertTrue("Second exception should be under the umbrella", causes.contains(exception2));
    }

    /*
     * Exception should not have prevented all three mouse handlers from getting
     * the event.
     */
    assertFired(fooHandler1, fooHandler2, fooHandler3);
  }
  
  public void testNoDoubleRemove() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    HandlerRegistration reg = FooEvent.register(eventBus, fooHandler1);
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

  public void testNoSource() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    SourcedHandler global = new SourcedHandler(null);
    SourcedHandler able = new SourcedHandler("able");
    SourcedHandler baker = new SourcedHandler("baker");

    FooEvent.register(eventBus, global);
    FooEvent.register(eventBus, "able", able);
    FooEvent.register(eventBus, "baker", baker);

    eventBus.fireEvent(new FooEvent());
    assertFired(global);
    assertNotFired(able, baker);
  }

  public void testNullChecks() {
    final SimpleEventBus eventBus = new SimpleEventBus();
    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.addHandler(null, fooHandler1);
      }
    });

    assertThrowsNpe(new Command() {
      public void execute() {
        FooEvent.register(eventBus, "foo", null);
      }
    });
    assertThrowsNpe(new Command() {
      public void execute() {
        FooEvent.register(eventBus, null, fooHandler1);
      }
    });
    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.addHandlerToSource(null, "foo", fooHandler1);
      }
    });

    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.fireEvent(null);
      }
    });

    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.fireEventFromSource(null, "");
      }
    });

    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.fireEventFromSource(new FooEvent() {
        }, null);
      }
    });
    assertThrowsNpe(new Command() {
      public void execute() {
        eventBus.fireEventFromSource(null, "baker");
      }
    });
  }

  public void testNullSourceOkay() {
    SimpleEventBus reg = new SimpleEventBus();

    FooEvent.Handler handler = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        add(this);
        assertNull(event.getSource());
      }
    };
    reg.addHandler(FooEvent.TYPE, handler);
    reg.fireEvent(new FooEvent());
    assertFired(handler);
  }

  public void testRemoveSelf() {
    final SimpleEventBus eventBus = new SimpleEventBus();

    FooEvent.Handler h = new FooEvent.Handler() {
      HandlerRegistration reg = FooEvent.register(eventBus, this);

      public void onFoo(FooEvent event) {
        add(this);
        reg.removeHandler();
      }
    };

    eventBus.fireEvent(new FooEvent());
    assertFired(h);

    reset();

    eventBus.fireEvent(new FooEvent());
    assertNotFired(h);
  }

  public void testReverseOrder() {
    @SuppressWarnings("deprecation")
    final SimpleEventBus eventBus = new SimpleEventBus(true);
    final FooEvent.Handler handler0 = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        add(this);
      }
    };
    final FooEvent.Handler handler1 = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        assertNotFired(handler0);
        add(this);
      }
    };
    final FooEvent.Handler handler2 = new FooEvent.Handler() {
      public void onFoo(FooEvent event) {
        assertNotFired(handler0, handler1);
        add(this);
      }
    };
    FooEvent.register(eventBus, handler0);
    FooEvent.register(eventBus, handler1);
    FooEvent.register(eventBus, handler2);

    reset();
    eventBus.fireEvent(new FooEvent());
    assertFired(handler0, handler1, handler2);
  }

  private void assertThrowsNpe(Command command) {
    try {
      command.execute();
      fail("expected NullPointerException");
    } catch (NullPointerException e) {
      /* pass */
    }
  }
}
