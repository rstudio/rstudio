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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Timer;

/**
 * Base tests the subclasses of {@link AbstractNativeScrollbar}.
 * 
 * @param <S> the type of scrollbar
 */
public abstract class NativeScrollbarTestBase<S extends AbstractNativeScrollbar> extends
    WidgetTestBase {

  /**
   * A scroll handler used for testing.
   */
  abstract static class TestScrollHandler implements ScrollHandler {
    private boolean isFinished;

    public void finish() {
      if (isFinished) {
        fail("ScrollHandler already finished.");
      }
      this.isFinished = true;
    }

    public boolean isFinished() {
      return isFinished;
    }
  }

  /**
   * The time to wait for a scroll event to fire, in milliseconds.
   */
  private static final int SCROLL_EVENT_TIMEOUT = 1000;

  /**
   * The test timeout in milliseconds.
   */
  private static final int TEST_TIMEOUT = 5000;

  private S scrollbar;

  /**
   * Test that changing the scrollbar size can affect the scroll position and
   * fires a scroll event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testSetScrollbarSizeFiresScrollEvent() {
    setScrollSize(scrollbar, 400);
    assertEquals(400, getScrollSize(scrollbar));

    // Wait for the scroll size to take effect.
    delayTestFinish(TEST_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        // Scroll to position 100.
        setScrollPosition(scrollbar, 100);
        assertEquals(100, getScrollPosition(scrollbar));

        // Wait for the scroll position to take effect.
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          public void execute() {
            // Add a scroll handler.
            final TestScrollHandler handler = new TestScrollHandler() {
              public void onScroll(ScrollEvent event) {
                finish();
              }
            };
            scrollbar.addScrollHandler(handler);

            // Reduce scroll size to smaller than the scrollbar size.
            setScrollbarSize(scrollbar, "400px");

            // Wait for the new scroll size to take effect.
            new Timer() {
              @Override
              public void run() {
                assertEquals(0, getScrollPosition(scrollbar));
                assertTrue(handler.isFinished());
                finishTest();
              }
            }.schedule(SCROLL_EVENT_TIMEOUT);
          }
        });
      }
    });
  }

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testSetScrollPosition() {
    // Add a scroll handler.
    final TestScrollHandler handler = new TestScrollHandler() {
      public void onScroll(ScrollEvent event) {
        finish();
      }
    };
    scrollbar.addScrollHandler(handler);

    // Scroll to a new position.
    setScrollSize(scrollbar, 500);

    // Wait for the scroll size to take effect.
    delayTestFinish(TEST_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        // Set the scroll position.
        setScrollPosition(scrollbar, 100);
        assertEquals(100, getScrollPosition(scrollbar));
        assertEquals(0, getMinimumScrollPosition(scrollbar));
        assertEquals(300, getMaximumScrollPosition(scrollbar));

        // Wait for the scroll event.
        new Timer() {
          @Override
          public void run() {
            assertTrue(handler.isFinished());
            finishTest();
          }
        }.schedule(SCROLL_EVENT_TIMEOUT);
      }
    });
  }

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testSetScrollSize() {
    setScrollSize(scrollbar, 500);
    assertEquals(500, getScrollSize(scrollbar));
  }

  /**
   * Test that changing the scroll size can affect the scroll position.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testSetScrollSizeFiresScrollEvent() {
    setScrollSize(scrollbar, 500);
    assertEquals(500, getScrollSize(scrollbar));

    // Wait for the scroll size to take effect.
    delayTestFinish(TEST_TIMEOUT);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        // Scroll to position 100.
        setScrollPosition(scrollbar, 100);
        assertEquals(100, getScrollPosition(scrollbar));

        // Wait for the scroll position to take effect.
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
          public void execute() {
            // Add a scroll handler.
            final TestScrollHandler handler = new TestScrollHandler() {
              public void onScroll(ScrollEvent event) {
                finish();
              }
            };
            scrollbar.addScrollHandler(handler);

            // Reduce scroll size to smaller than the scrollbar size.
            setScrollSize(scrollbar, 50);

            // Wait for the new scroll size to take effect.
            new Timer() {
              @Override
              public void run() {
                assertEquals(0, getScrollPosition(scrollbar));
                assertTrue(handler.isFinished());
                finishTest();
              }
            }.schedule(SCROLL_EVENT_TIMEOUT);
          }
        });
      }
    });
  }

  /**
   * Create a new scrollbar.
   * 
   * @return the scrollbar
   */
  protected abstract S createScrollbar();

  /**
   * Get the maximum position of the scrollbar in the direction of scrolling.
   */
  protected abstract int getMaximumScrollPosition(S scrollbar);

  /**
   * Get the minimum position of the scrollbar in the direction of scrolling.
   */
  protected abstract int getMinimumScrollPosition(S scrollbar);

  /**
   * Get the current position of the scrollbar in the direction of scrolling.
   */
  protected abstract int getScrollPosition(S scrollbar);

  /**
   * Get the size of the content within the scrollbar.
   * 
   * @return the size in pixels
   */
  protected abstract int getScrollSize(S scrollbar);

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    scrollbar = createScrollbar();
    setScrollbarSize(scrollbar, "200px");
    RootPanel.get().add(scrollbar);
  }

  /**
   * Set the size of the scrollbar in the direction of scrolling.
   * 
   * @param size the height or widget, depending on the direction of scroll
   */
  protected abstract void setScrollbarSize(S scrollbar, String size);

  /**
   * Set the scroll position.
   */
  protected abstract void setScrollPosition(S scrollbar, int position);

  /**
   * Set the size of the content within the scrollbar.
   * 
   * @param size the size in pixels
   */
  protected abstract void setScrollSize(S scrollbar, int size);
}
