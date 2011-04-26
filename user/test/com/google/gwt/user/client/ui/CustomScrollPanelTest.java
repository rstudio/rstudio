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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.user.client.Timer;

/**
 * Tests the ScrollPanel widget.
 */
public class CustomScrollPanelTest extends ScrollPanelTest {

  /**
   * The time to wait for a scroll event to fire, in milliseconds.
   */
  private static final int SCROLL_EVENT_TIMEOUT = 1000;

  private Widget content;
  private HorizontalScrollbar hScrollbar;
  private Element hScrollbarContainer;
  private VerticalScrollbar vScrollbar;
  private Element vScrollbarContainer;
  private CustomScrollPanel panel;

  /**
   * Test that both the horizontal scrollbar and vertical scrollbar appear when
   * the content flows in both directions.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testOnResizeBothScrollbars() {
    // Scrollbar not needed.
    content.setPixelSize(400, 400);
    panel.onResize();
    assertEquals(0, hScrollbarContainer.getOffsetHeight());
    assertEquals(0, vScrollbarContainer.getOffsetWidth());

    // Both scrollbars needed.
    content.setPixelSize(600, 600);
    panel.onResize();
    assertTrue(hScrollbarContainer.getOffsetHeight() > 0);
    assertTrue(vScrollbarContainer.getOffsetWidth() > 0);

    // Verify that the scrollbars leave a gap in the corner.
    assertTrue(hScrollbarContainer.getOffsetWidth() < 490);
    assertTrue(vScrollbarContainer.getOffsetHeight() < 490);
  }

  /**
   * Test that the horizontal scrollbar appears/disappears when the widget is
   * resized.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testOnResizeHorizontally() {
    // Scrollbar not needed.
    content.setPixelSize(400, 400);
    panel.onResize();
    assertEquals(0, hScrollbarContainer.getOffsetHeight());

    // Scrollbar needed.
    panel.setWidth("300px");
    panel.onResize();
    assertTrue(hScrollbarContainer.getOffsetHeight() > 0);

    // Scrollbar not needed.
    panel.setWidth("500px");
    panel.onResize();
    assertEquals(0, hScrollbarContainer.getOffsetHeight());

    // Scrollbar always visible.
    panel.setAlwaysShowScrollBars(true);
    assertTrue(hScrollbarContainer.getOffsetHeight() > 0);
  }

  /**
   * Test that the vertical scrollbar appears/disappears when the widget is
   * resized.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testOnResizeVertically() {
    // Scrollbar not needed.
    content.setPixelSize(400, 400);
    panel.onResize();
    assertEquals(0, vScrollbarContainer.getOffsetWidth());

    // Scrollbar needed.
    panel.setHeight("300px");
    panel.onResize();
    assertTrue(vScrollbarContainer.getOffsetWidth() > 0);

    // Scrollbar not needed.
    panel.setHeight("500px");
    panel.onResize();
    assertEquals(0, vScrollbarContainer.getOffsetWidth());

    // Scrollbar always visible.
    panel.setAlwaysShowScrollBars(true);
    panel.onResize();
    assertTrue(vScrollbarContainer.getOffsetWidth() > 0);
  }

  public void testRemove() {
    // Remove a child of another parent.
    Label otherChild = new Label("Not a child");
    SimplePanel otherParent = new SimplePanel(otherChild);
    assertFalse(panel.remove(otherChild));
    assertEquals(otherParent, otherChild.getParent());

    // Remove the child.
    assertTrue(panel.remove(content));
    assertNull(panel.getWidget());
    assertNull(content.getParent());
    assertFalse(panel.remove(content));

    // Remove horizontal scrollbar.
    assertTrue(panel.remove(hScrollbar));
    assertNull(panel.getHorizontalScrollbar());
    assertNull(hScrollbar.asWidget().getParent());
    assertFalse(panel.remove(hScrollbar));

    // Remove vertical scrollbar.
    assertTrue(panel.remove(vScrollbar));
    assertNull(panel.getVerticalScrollbar());
    assertNull(vScrollbar.asWidget().getParent());
    assertFalse(panel.remove(vScrollbar));
  }

  public void testRemoveScrollbar() {
    // Remove horizontal scrollbar.
    panel.removeHorizontalScrollbar();
    assertNull(panel.getHorizontalScrollbar());
    assertNull(hScrollbar.asWidget().getParent());
    panel.removeHorizontalScrollbar();

    // Remove vertical scrollbar.
    panel.removeVerticalScrollbar();
    assertNull(panel.getVerticalScrollbar());
    assertNull(vScrollbar.asWidget().getParent());
    panel.removeVerticalScrollbar();

    // Force a redraw to ensure we handle null scrollbars.
    panel.onResize();
  }

  /**
   * Test that the horizontal scrollbar appears/disappears when the content is
   * resized.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testResizeContentHorizontally() {
    delayTestFinish(SCROLL_EVENT_TIMEOUT * 5);

    // Scrollbar not needed.
    content.setPixelSize(100, 100);
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, hScrollbarContainer.getOffsetHeight());

        // Scrollbar needed.
        content.setWidth("1000px");
        new Timer() {
          @Override
          public void run() {
            assertTrue(hScrollbarContainer.getOffsetHeight() > 0);

            // Scrollbar not needed.
            content.setPixelSize(100, 100);
            new Timer() {
              @Override
              public void run() {
                assertEquals(0, hScrollbarContainer.getOffsetHeight());

                // Scrollbar always visible.
                panel.setAlwaysShowScrollBars(true);
                assertTrue(hScrollbarContainer.getOffsetHeight() > 0);
                finishTest();
              }
            }.schedule(SCROLL_EVENT_TIMEOUT);
          }
        }.schedule(SCROLL_EVENT_TIMEOUT);
      }
    }.schedule(SCROLL_EVENT_TIMEOUT);
  }

  /**
   * Test that the vertical scrollbar appears/disappears when the content is
   * resized.
   */
  @DoNotRunWith(Platform.HtmlUnitLayout)
  public void testResizeContentVertically() {
    delayTestFinish(SCROLL_EVENT_TIMEOUT * 5);

    // Scrollbar not needed.
    content.setPixelSize(100, 100);
    new Timer() {
      @Override
      public void run() {
        assertEquals(0, vScrollbarContainer.getOffsetWidth());

        // Scrollbar needed.
        content.setHeight("1000px");
        new Timer() {
          @Override
          public void run() {
            assertTrue(vScrollbarContainer.getOffsetWidth() > 0);

            // Scrollbar not needed.
            content.setPixelSize(100, 100);
            new Timer() {
              @Override
              public void run() {
                assertEquals(0, vScrollbarContainer.getOffsetWidth());

                // Scrollbar always visible.
                panel.setAlwaysShowScrollBars(true);
                assertTrue(vScrollbarContainer.getOffsetWidth() > 0);
                finishTest();
              }
            }.schedule(SCROLL_EVENT_TIMEOUT);
          }
        }.schedule(SCROLL_EVENT_TIMEOUT);
      }
    }.schedule(SCROLL_EVENT_TIMEOUT);
  }

  @Override
  protected CustomScrollPanel createPanel() {
    return new CustomScrollPanel();
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    // Create and attach a panel.
    panel = createPanel();
    hScrollbar = panel.getHorizontalScrollbar();
    hScrollbarContainer = hScrollbar.asWidget().getElement().getParentElement();
    vScrollbar = panel.getVerticalScrollbar();
    vScrollbarContainer = vScrollbar.asWidget().getElement().getParentElement();
    RootPanel.get().add(panel);
    panel.setPixelSize(500, 500);

    // Add content to the panel.
    content = new Label("Hello World");
    content.getElement().getStyle().setOverflow(Overflow.HIDDEN);
    panel.setWidget(content);
  }
}
