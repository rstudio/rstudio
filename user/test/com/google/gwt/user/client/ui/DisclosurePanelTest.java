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

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;

/**
 * Tests core functionality of {@link DisclosurePanel}.
 */
public class DisclosurePanelTest extends GWTTestCase {
  private static final int OPEN = 0;

  private static final int CLOSE = 1;

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  /**
   * Test to ensure css style changes that control core functionality do change
   * appropriately.
   */
  public void testCoreFunctionality() {
    DisclosurePanel panel = createTestPanel();
    assertTrue(DOM.getStyleAttribute(panel.getContent().getElement(), "display").equalsIgnoreCase(
        "none"));

    panel.setOpen(true);
    assertTrue(DOM.getStyleAttribute(panel.getContent().getElement(), "display").trim().equals(
        ""));
  }

  /**
   * Test to ensure that event handler dispatch function appropriately.
   */
  public void testEventHandlers() {

    final boolean[] aDidFire = new boolean[2];
    final boolean[] bDidFire = new boolean[2];
    final DisclosurePanel panel = createTestPanel();

    DisclosureHandler handleA = new DisclosureHandler() {
      public void onClose(DisclosureEvent event) {
        aDidFire[CLOSE] = true;
      }

      public void onOpen(DisclosureEvent event) {
        aDidFire[OPEN] = true;
      }
    };

    DisclosureHandler handleB = new DisclosureHandler() {
      public void onClose(DisclosureEvent event) {
        assertEquals(event.getSource(), panel);
        bDidFire[CLOSE] = true;
      }

      public void onOpen(DisclosureEvent event) {
        assertEquals(event.getSource(), panel);
        bDidFire[OPEN] = true;
      }
    };

    panel.addEventHandler(handleA);
    panel.addEventHandler(handleB);

    panel.setOpen(true);
    // We expect onOpen to fire and onClose to not fire.
    assertTrue(aDidFire[OPEN] && bDidFire[OPEN] && !aDidFire[CLOSE]
        && !bDidFire[CLOSE]);

    aDidFire[OPEN] = bDidFire[OPEN] = false;

    panel.setOpen(false);
    // We expect onOpen to fire and onClose to not fire.
    assertTrue(aDidFire[CLOSE] && bDidFire[CLOSE] && !aDidFire[OPEN]
        && !bDidFire[OPEN]);

    aDidFire[OPEN] = bDidFire[CLOSE] = false;

    panel.removeEventHandler(handleB);

    panel.setOpen(true);
    panel.setOpen(false);
    // We expect a to have fired both events, and b to have fired none.
    assertTrue(aDidFire[OPEN] && aDidFire[CLOSE] && !bDidFire[OPEN]
        && !bDidFire[CLOSE]);
  }

  private DisclosurePanel createTestPanel() {
    DisclosurePanel panel = new DisclosurePanel("Test Subject", false);
    panel.setContent(new SimplePanel());
    return panel;
  }
}
