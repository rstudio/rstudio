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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link ResizeLayoutPanel}.
 */
public class ResizeLayoutPanelTest extends
    SimplePanelTestBase<ResizeLayoutPanel> {

  /**
   * A custom implementation of {@link ResizeHandler} used for testing.
   */
  private static class CustomResizeHandler implements ResizeHandler {

    private boolean resizeFired;

    public void assertResizeFired(boolean expected) {
      assertEquals(expected, resizeFired);
      resizeFired = false;
    }

    public void onResize(ResizeEvent event) {
      assertFalse(resizeFired);
      resizeFired = true;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that a resize event is fired on attach.
   */
  public void testAttach() {
    final ResizeLayoutPanel panel = createPanel();
    panel.setWidget(new Label("hello world"));
    final CustomResizeHandler handler = new CustomResizeHandler();
    panel.addResizeHandler(handler);
    handler.assertResizeFired(false);

    delayTestFinish(10000);
    RootPanel.get().add(panel);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        handler.assertResizeFired(true);
        panel.removeFromParent();
        finishTest();
      }
    });
  }

  /**
   * Test that changing the font size triggers a resize event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testChangeFontSize() {
    // Create a panel and add a handler.
    ResizeLayoutPanel panel = createPanel();
    panel.setWidget(new Label("hello world"));
    panel.setWidth("10em");
    panel.setHeight("10em");
    final CustomResizeHandler handler = new CustomResizeHandler();
    panel.addResizeHandler(handler);
    handler.assertResizeFired(false);

    // Create an outer container and attach it.
    final SimplePanel container = new SimplePanel();
    container.getElement().getStyle().setFontSize(10, Unit.PT);
    container.setHeight("10em");
    container.setWidth("10em");
    container.setWidget(panel);
    RootPanel.get().add(container);

    // Wait for the resize event from attaching.
    delayTestFinish(10000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        handler.assertResizeFired(true); // Triggered by attach.
        handler.assertResizeFired(false);

        // Change the font size.
        container.getElement().getStyle().setFontSize(12, Unit.PT);
        new Timer() {
          @Override
          public void run() {
            handler.assertResizeFired(true);
            container.removeFromParent();
            finishTest();
          }
        }.schedule(250);
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testEnlargeContainerHeight() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(100, 100);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setHeight("101px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testEnlargeContainerWidth() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(100, 100);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setWidth("101px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event even if the
   * dimensions are too small to render a scrollbar.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testEnlargeSmallContainerHeight() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(20, 20);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setHeight("21px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event even if the
   * dimensions are too small to render a scrollbar.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testEnlargeSmallContainerWidth() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(20, 20);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setWidth("21px");
      }
    });
  }

  public void testProvidesResize() {
    final List<String> resized = new ArrayList<String>();
    ResizeLayoutPanel panel = createPanel();
    panel.setWidget(new LayoutPanel() {
      @Override
      public void onResize() {
        super.onResize();
        resized.add("resized");
      }
    });

    delayTestFinish(10000);
    RootPanel.get().add(panel);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        assertEquals(1, resized.size());
        finishTest();
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testShrinkContainerHeight() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(100, 100);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setHeight("99px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testShrinkContainerWidth() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(100, 100);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setWidth("99px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event even if the
   * dimensions are too small to render a scrollbar.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testShrinkSmallContainerHeight() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(21, 21);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setHeight("20px");
      }
    });
  }

  /**
   * Test that resizing the outer container triggers a resize event even if the
   * dimensions are too small to render a scrollbar.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testShrinkSmallContainerWidth() {
    final SimplePanel container = new SimplePanel();
    container.setPixelSize(21, 21);
    testResizeContainer(container, new Command() {
      public void execute() {
        container.setWidth("20px");
      }
    });
  }

  @Override
  protected ResizeLayoutPanel createPanel() {
    return new ResizeLayoutPanel();
  }

  /**
   * Test that resizing the outer container triggers a resize event.
   * 
   * @param container the container that will hold the panel
   * @param resizeCommand the command that resizes the container
   */
  private void testResizeContainer(final SimplePanel container,
      final Command resizeCommand) {
    // Create a panel and add a handler.
    ResizeLayoutPanel panel = createPanel();
    panel.setWidget(new Label("hello world"));
    panel.setWidth("100%");
    panel.setHeight("100%");
    final CustomResizeHandler handler = new CustomResizeHandler();
    panel.addResizeHandler(handler);
    handler.assertResizeFired(false);

    // Create an outer container and attach it.
    container.setWidget(panel);
    RootPanel.get().add(container);

    // Wait for the resize event from attaching.
    delayTestFinish(10000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        handler.assertResizeFired(true); // Triggered by attach.

        // Change the size of the container.
        resizeCommand.execute();
        new Timer() {
          @Override
          public void run() {
            handler.assertResizeFired(true);
            container.removeFromParent();
            finishTest();
          }
        }.schedule(250);
      }
    });
  }
}
