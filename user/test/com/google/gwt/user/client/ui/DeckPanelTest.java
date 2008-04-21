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

package com.google.gwt.user.client.ui;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Test for {@link DeckPanel}.
 */
public class DeckPanelTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test that the offsetHeight/Width of a widget are defined when the widget is
   * added to the DeckPanel.
   */
  public void testWidgetOffsetDimensionsOnload() {
    DeckPanel deck = new DeckPanel();
    RootPanel.get().add(deck);

    // Add a widget to the DeckPanel
    Label content = new Label("detached") {
      @Override
      public void onLoad() {
        // Verify that the offsetWidth/Height are greater than zero
        assertTrue(this.getOffsetHeight() > 0);
        assertTrue(this.getOffsetWidth() > 0);
        setText("attached");
      }
    };
    deck.add(content);

    // Verify content.onLoad was actually called
    assertEquals("attached", content.getText());
  }
}
