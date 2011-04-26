/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.touch.client.TouchScroller;

/**
 * Tests the ScrollPanel widget.
 */
public class ScrollPanelTest extends SimplePanelTestBase<ScrollPanel> {

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testGetMaximumScrollPosition() {
    final ScrollPanel scrollPanel = createPanel();
    scrollPanel.setPixelSize(200, 300);
    RootPanel.get().add(scrollPanel);

    Label content = new Label("Hello World");
    content.setPixelSize(500, 700);
    scrollPanel.setWidget(content);

    delayTestFinish(3000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        int maxHorizontalPos = scrollPanel.getMaximumHorizontalScrollPosition();
        int maxVerticalPos = scrollPanel.getMaximumVerticalScrollPosition();

        // Account for scrollbars up to 50 pixels.
        assertTrue(maxHorizontalPos >= 300 && maxHorizontalPos < 350);
        assertTrue(maxVerticalPos >= 400 && maxHorizontalPos < 450);
        RootPanel.get().remove(scrollPanel);
        finishTest();
      }
    });
  }

  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testScrollToPosition() {
    final ScrollPanel scrollPanel = createPanel();
    scrollPanel.setPixelSize(200, 300);
    RootPanel.get().add(scrollPanel);

    Label content = new Label("Hello World");
    content.setPixelSize(500, 700);
    scrollPanel.setWidget(content);
    
    delayTestFinish(3000);
    Scheduler.get().scheduleDeferred(new ScheduledCommand() {
      public void execute() {
        scrollPanel.scrollToBottom();
        assertEquals(scrollPanel.getMaximumVerticalScrollPosition(), scrollPanel
            .getVerticalScrollPosition());

        scrollPanel.scrollToTop();
        assertEquals(0, scrollPanel.getVerticalScrollPosition());

        scrollPanel.scrollToRight();
        assertEquals(scrollPanel.getMaximumHorizontalScrollPosition(), scrollPanel
            .getHorizontalScrollPosition());

        scrollPanel.scrollToLeft();
        assertEquals(0, scrollPanel.getHorizontalScrollPosition());

        finishTest();
      }
    });
  }

  public void testSetTouchScrollingDisabled() {
    ScrollPanel scrollPanel = createPanel();

    // Touch support is enabled by default for browsers that support it.
    assertEquals(TouchScroller.isSupported(),
        !scrollPanel.isTouchScrollingDisabled());

    // Disable touch support.
    scrollPanel.setTouchScrollingDisabled(true);
    assertTrue(scrollPanel.isTouchScrollingDisabled());

    // Enable touch support.
    scrollPanel.setTouchScrollingDisabled(false);
    assertEquals(TouchScroller.isSupported(),
        !scrollPanel.isTouchScrollingDisabled());
  }

  @Override
  protected ScrollPanel createPanel() {
    return new ScrollPanel();
  }
}
