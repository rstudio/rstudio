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

/**
 * Test for {@link DeckLayoutPanel}.
 */
public class DeckLayoutPanelTest extends PanelTestBase<DeckLayoutPanel> {

  /**
   * Test that forcing layout without changing the widget doesn't cause the
   * widget to disappear.
   */
  public void testForceLayoutSameWidget() {
    DeckLayoutPanel deck = createPanel();
    Label[] labels = new Label[2];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
      deck.add(labels[i]);
    }

    // Show widget at index 1, make sure it becomes visible.
    deck.showWidget(1);
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());
    deck.forceLayout();
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());

    // Force layout and make sure that widget 1 is still visible.
    deck.forceLayout();
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());
  }

  public void testSetWidget() {
    DeckLayoutPanel deck = createPanel();
    Label[] labels = new Label[2];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
    }

    // Set a widget that isn't a child.
    deck.setWidget(labels[0]);
    assertEquals(deck, labels[0].getParent());
    assertEquals(0, deck.getVisibleWidgetIndex());
    assertEquals(labels[0], deck.getVisibleWidget());

    // Set another widget that isn't a child.
    deck.setWidget(labels[1]);
    assertEquals(deck, labels[0].getParent());
    assertEquals(deck, labels[1].getParent());
    assertEquals(0, deck.getWidgetIndex(labels[0]));
    assertEquals(1, deck.getWidgetIndex(labels[1]));
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());

    // Set a widget that is a child.
    deck.setWidget(labels[0]);
    assertEquals(deck, labels[0].getParent());
    assertEquals(deck, labels[1].getParent());
    assertEquals(0, deck.getWidgetIndex(labels[0]));
    assertEquals(1, deck.getWidgetIndex(labels[1]));
    assertEquals(0, deck.getVisibleWidgetIndex());
    assertEquals(labels[0], deck.getVisibleWidget());

    // Set the widget to null.
    deck.setWidget(null);
    assertEquals(deck, labels[0].getParent());
    assertEquals(deck, labels[1].getParent());
    assertEquals(0, deck.getWidgetIndex(labels[0]));
    assertEquals(1, deck.getWidgetIndex(labels[1]));
    assertEquals(-1, deck.getVisibleWidgetIndex());
    assertEquals(null, deck.getVisibleWidget());
  }

  /**
   * Tests both {@link DeckLayoutPanel#showWidget(int)} and
   * {@link DeckLayoutPanel#showWidget(Widget)}.
   */
  public void testShowWidget() {
    DeckLayoutPanel deck = createPanel();
    Label[] labels = new Label[3];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
      deck.add(labels[i]);
    }

    // Show widget at index 1, make sure it becomes visible.
    deck.showWidget(1);
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());
    deck.forceLayout();
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());
    assertFalse(labels[2].isVisible());

    // Show widget at index 0, make sure widget 1 becomes invisible.
    deck.showWidget(labels[0]);
    assertEquals(0, deck.getVisibleWidgetIndex());
    assertEquals(labels[0], deck.getVisibleWidget());
    deck.forceLayout();
    assertTrue(labels[0].isVisible());
    assertFalse(labels[1].isVisible());
    assertFalse(labels[2].isVisible());
  }

  /**
   * Test that toggling a widget out and back in within the same event loop
   * doesn't cause the widget to be hidden.
   */
  public void testShowWidgetToggle() {
    DeckLayoutPanel deck = createPanel();
    Label[] labels = new Label[2];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = new Label("content" + i);
      deck.add(labels[i]);
    }

    // Show widget at index 1, make sure it becomes visible.
    deck.showWidget(1);
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());
    deck.forceLayout();
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());

    // Toggle the widget out and back in.
    deck.showWidget(0);
    deck.showWidget(1);
    assertEquals(1, deck.getVisibleWidgetIndex());
    assertEquals(labels[1], deck.getVisibleWidget());
    deck.forceLayout();
    assertFalse(labels[0].isVisible());
    assertTrue(labels[1].isVisible());
  }

  @Override
  protected DeckLayoutPanel createPanel() {
    return new DeckLayoutPanel();
  }
}
